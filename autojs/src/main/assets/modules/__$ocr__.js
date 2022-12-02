module.exports = function(runtime, global) {
  let javaOcr = runtime.ocr
  var $ocr = function () {
  }
  $ocr.initWithModel = function (options) {
    options = options || {};
    let modelPath = files.path(options.modelPath);
    let labelPath = files.path(options.labelPath);
    let scoreThreshold = options.threshold || 0.1;
    $ocr.setScoreThreshold(scoreThreshold);
    if (files.exists(modelPath) && files.exists(labelPath)) {
      let detName = options.detFileName;
      let recName = options.recFileName;
      let clsName = options.clsFileName;
      if (detName && recName && clsName) {
        return $ocr.initWithSpecificModels(modelPath, labelPath, detName, recName, clsName);
      } else {
        return $ocr.initWithCustomModel(modelPath, labelPath);
      }
    }
    // 使用默认模型
    return true;
  }

  $ocr.recognizeText = function (img, options) {
    options = options || {}
    let region = options.region
    if (region) {
      let r = buildRegion(region, img)
      let o = img
      img = images.clip(img, r.x, r.y, r.width, r.height)
      o.recycle()
    }
    let text = javaOcr.recognizeText(img, options.cpuThreadNum || 4, options.useSlim || true)
    if (text) {
      return JSON.parse(JSON.stringify(text))
    }
    return null
  }

  $ocr.detect = function (img, options) {
    options = options || {}
    let region = options.region
    if (region) {
      let r = buildRegion(region, img)
      let o = img
      img = images.clip(img, r.x, r.y, r.width, r.height)
      o.recycle()
    }
    let resultList = runtime.bridges.bridges.toArray(javaOcr.detect(img, options.cpuThreadNum || 4, options.useSlim || true))
    if (region && region.length > 1 && resultList && resultList.length > 0) {
      resultList.forEach(r => r.bounds.offset(region[0], region[1]))
    }
    return resultList
  }

  return $ocr;


  function buildRegion (region, img) {
    if (region == undefined) {
      region = [];
    }
    var x = region[0] === undefined ? 0 : region[0];
    var y = region[1] === undefined ? 0 : region[1];
    var width = region[2] === undefined ? img.getWidth() - x : region[2];
    var height = region[3] === undefined ? (img.getHeight() - y) : region[3];
    var r = new org.opencv.core.Rect(x, y, width, height);
    if (x < 0 || y < 0 || x + width > img.width || y + height > img.height) {
      throw new Error("out of region: region = [" + [x, y, width, height] + "], image.size = [" + [img.width, img.height] + "]");
    }
    return r;
  }
};