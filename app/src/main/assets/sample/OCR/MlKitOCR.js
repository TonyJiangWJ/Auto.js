const img = images.read("./test.png")
console.show()
let start = new Date()
// 识别图片中的文字，返回完整识别信息（兼容百度OCR格式）。
let result = $mlKitOcr.detect(img)
log('识别耗时：' + (new Date() - start) + 'ms')
// 可以使用简化的调用命令，默认参数：cpuThreadNum = 4, useSlim = true
// const result = $mlKitOcr.detect(img)
toastLog("完整识别信息: " + JSON.stringify(result))
start = new Date()
// 识别图片中的文字，只返回文本识别信息（字符串列表）。当前版本可能存在文字顺序错乱的问题 建议先使用detect后自行排序
const stringList = $mlKitOcr.recognizeText(img)
log('纯文本识别耗时：' + (new Date() - start) + 'ms')
// 可以使用简化的调用命令，默认参数：cpuThreadNum = 4, useSlim = true
// const stringList = $mlKitOcr.recognizeText(img)
toastLog("文本识别信息（字符串列表）: " + JSON.stringify(stringList))

// 回收图片
img.recycle()
