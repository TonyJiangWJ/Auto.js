module.exports = function (runtime, global) {
  let yoloCreator = runtime.yolo
  let instanceList = []
  let $yolo = function () {
    this.enabled = false
  }
  $yolo.init = function (options) {
    options = options || {};
    options.imageSize = options.imageSize || 0;
    if (this.type = 'ncnn') {
      if (!options.labels || options.labels.length == 0) {
        throw Error('使用ncnn模型时，labels 不能为空')
      }
      if (options.imageSize <= 0) {
        throw Error('使用ncnn模型时，imageSize 不能为空')
      }
    }
    let type = options.type;
    if (type == 'ncnn') {
      this.type = 'ncnn';
      if (!options.paramPath) {
        throw Error('paramPath 不能为空')
      } else if (files.exists(options.paramPath) == false) {
        throw Error('paramPath: ' + options.paramPath + ' 不存在')
      }
      if (!options.binPath) {
        throw Error('binPath 不能为空')
      } else if (files.exists(options.binPath) == false) {
        throw Error('binPath: ' + options.binPath + ' 不存在')
      }
      this.yoloInstance = yoloCreator.createNcnn(options.paramPath, options.binPath, convertToList(options.labels), options.imageSize, !!options.useGpu);
      this.enabled = this.yoloInstance.isInit();
      instanceList.push(this.yoloInstance)
    } else {
      this.type = 'onnx';

      if (!options.modelPath) {
        throw Error('modelPath 不能为空')
      } else if (files.exists(options.modelPath) == false) {
        throw Error('modelPath: ' + options.modelPath + ' 不存在')
      }
      this.yoloInstance = yoloCreator.createOnnx(options.modelPath, convertToList(options.labels), options.imageSize);
      this.enabled = this.yoloInstance != null;
      instanceList.push(this.yoloInstance)
    }

    if (this.enabled) {
      if (options.confThreshold) {
        this.yoloInstance.setConfThreshold(options.confThreshold)
      }

      if (options.nmsThreshold) {
        this.yoloInstance.setNmsThreshold(options.nmsThreshold)
      }
    }
    return this.enabled
  }

  $yolo.forward = function (img, filterOption, region) {
    filterOption = filterOption || {}
    if (!this.enabled) {
      return []
    }
    if (region) {
      let r = buildRegion(region, img)
      img = images.clip(img, r.x, r.y, r.width, r.height)
    }
    let resultList = util.java.toJsArray(this.yoloInstance.predictYolo(img.mat))
    if (region) {
      img.recycle()
    }
    return filterResult(resultList, filterOption, region)
  }


  $yolo.captureAndForward = function (filterOption, region) {
    filterOption = filterOption || {}
    if (!this.enabled) {
      return []
    }
    let resultList = util.java.toJsArray(this.yoloInstance.captureAndPredict(runtime, buildRegion(region, null, true)))
    return filterResult(resultList, filterOption, region)
  }

  $yolo.release = function () {
    if (this.yoloInstance) {
      this.yoloInstance.release()
      this.yoloInstance = null
    }
  }

  $yolo.getInstance = function () {
    return wrapForward(this.yoloInstance)
  }

  return $yolo;

  events.on('exit', function () {
    if (instanceList.length > 0) {
        instanceList.forEach(instance => instance.release())
    }
  })

  function wrapForward (yoloInstance) {
    return {
      forward: function (img, filterOption, region) {
        return $yolo.forward.call({ yoloInstance: yoloInstance, enabled: !!yoloInstance && yoloInstance.isInit() }, img, filterOption, region)
      },
      release: () => yoloInstance.release()
    }
  }

  function convertToList (jsArray) {
    let list = new java.util.ArrayList();
    if (jsArray) {
      for (let i = 0; i < jsArray.length; i++) {
        list.add(jsArray[i]);
      }
    }
    return list;
  }

  function buildRegion (region, img, nullIfInvalid) {
    if (region == undefined) {
      region = [];
      if (nullIfInvalid) {
        return null
      }
    }
    var x = region[0] === undefined ? 0 : region[0];
    var y = region[1] === undefined ? 0 : region[1];
    var r;
    if (img) {
      var width = region[2] === undefined ? img.getWidth() - x : region[2];
      var height = region[3] === undefined ? (img.getHeight() - y) : region[3];
      r = new org.opencv.core.Rect(x, y, width, height);
      if (x < 0 || y < 0 || x + width > img.width || y + height > img.height) {
        throw new Error("out of region: region = [" + [x, y, width, height] + "], image.size = [" + [img.width, img.height] + "]");
      }
    } else {
      if (region.length < 4) {
        throw new Error("region length is not valid it must be: [x, y, width, height]");
      }
      r = new org.opencv.core.Rect(x, y, region[2], region[3]);
    }
    return r;
  }

  function filterResult (resultList, filterOption, region) {
    if (!resultList || resultList.length < 1) {
      return []
    }
    resultList = resultList.map(box => {
      if (region && region.length > 1) {
        box.left = box.left + region[0]
        box.top = box.top + region[1]
        box.right = box.right + region[0]
        box.bottom = box.bottom + region[1]
        box.bounds = new android.graphics.Rect(box.left, box.top, box.right, box.bottom)
      }
      if (!box.bounds) {
        box.buildRect()
      }
      return {
        label: box.label,
        classId: box.clsId,
        x: box.left,
        y: box.top,
        width: box.right - box.left,
        height: box.bottom - box.top,
        confidence: box.confidence,
        bounds: box.bounds
      }
    }).filter(result => {
      if (filterOption.labelRegex) {
        if (!new RegExp(filterOption.labelRegex).test(result.label)) {
          return false
        }
      }
      if (filterOption.confidence) {
        if (result.confidence < filterOption.confidence) {
          return false
        }
      }
      if (filterOption.clsIds && filterOption.clsIds.length > 0) {
        if (filterOption.clsIds.indexOf(result.classId) < 0) {
          return false
        }
      }
      if (typeof filterOption.boundsInside == 'function') {
        if (!filterOption.boundsInside(result)) {
          return false
        }
      }
      if (typeof filterOption.filter == 'function') {
        if (!filterOption.filter(result)) {
          return false
        }
      }
      return true
    })
    return resultList
  }
};