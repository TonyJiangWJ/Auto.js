const img = images.read("./test.png")
console.show()
setTimeout(() -> console.hide(), 15000)
let cpuThreadNum = 4
// PaddleOCR 移动端提供了两种模型：ocr_v3_for_cpu与ocr_v3_for_cpu(slim)，此选项用于选择加载的模型,默认true使用v3的slim版(速度更快)，false使用v3的普通版(准确率更高）
let useSlim = true
// 是否使用v4版本
let useV4 = false
let start = new Date()
// 识别图片中的文字，返回完整识别信息（兼容百度OCR格式）。
// 使用v4版本模型进行初始化
$ocr.initWithModel({ useV4 })
let result = $ocr.detect(img, { cpuThreadNum, useSlim })
img.recycle()
log('slim识别耗时：' + (new Date() - start) + 'ms')

let model_path = '/sdcard/脚本/best.bin'
let param_path = '/sdcard/脚本/best.param'
if (!files.exists(model_path) || !files.exists(param_path)) {
    toastLog('请确认已下载了模型文件')
    exit()
}

let yoloInit = $yolo.init({
  type: 'ncnn',
  paramPath: files.path(param_path),
  binPath: files.path(model_path),
  imageSize: 480,
  labels: [
    'booth_btn', 'collect_coin', 'collect_egg', 'collect_food', 'cook', 'countdown', 'donate',
    'eating_chicken', 'employ', 'empty_booth', 'feed_btn', 'friend_btn', 'has_food', 'has_shit',
    'hungry_chicken', 'item', 'kick-out', 'no_food', 'not_ready', 'operation_booth', 'plz-go',
    'punish_booth', 'punish_btn', 'signboard', 'sleep', 'speedup', 'sports', 'stopped_booth',
    'thief_chicken', 'close_btn', 'collect_muck', 'confirm_btn', 'working_chicken',
  ]
})

if (!yoloInit) {
  toast('初始化失败')
  exit()
}

const yoloimg = images.read("./manor.jpg")
start = new Date()
result = $yolo.forward(yoloimg)
toastLog('predict result:' + JSON.stringify(result, null, 4))
log('ncnn cost: ' + (new Date() - start) + 'ms')
yoloimg.recycle()