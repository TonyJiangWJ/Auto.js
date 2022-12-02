module.exports = function (runtime, global) {
  let javaOcr = runtime.mlKitOCR
  var $mlKitOcr = function () {
  }
  $mlKitOcr.recognizeText = function (img, options) {
    options = options || {}
    let region = options.region
    if (region) {
      let r = buildRegion(region, img)
      img = images.clip(img, r.x, r.y, r.width, r.height)
    }
    let text = javaOcr.recognizeText(img)
    if (text) {
      text = JSON.parse(JSON.stringify(text))
    }
    if (region) {
      img.recycle()
    }
    return text
  }

  $mlKitOcr.detect = function (img, options) {
    options = options || {}
    let region = options.region
    if (region) {
      let r = buildRegion(region, img)
      img = images.clip(img, r.x, r.y, r.width, r.height)
    }
    let resultList = runtime.bridges.bridges.toArray(javaOcr.detect(img))
    if (region && region.length > 1 && resultList && resultList.length > 0) {
      resultList.forEach(r => {
        r.bounds.offset(region[0], region[1])
        if (r.elements && r.elements.length > 0) {
          r.elements.forEach(e => e.bounds.offset(region[0], region[1]))
        }
      })
    }
    if (region) {
      img.recycle()
    }
    return resultList
  }

  return $mlKitOcr;


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