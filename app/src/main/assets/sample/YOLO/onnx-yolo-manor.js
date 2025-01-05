let model_path = '/sdcard/脚本/manor_lite.onnx'
if (!files.exists(model_path)) {
    toastLog('请确认已下载了模型文件')
    exit()
}
console.show()
setTimeout(() => console.hide(), 15000)
let yoloInit = $yolo.init({
  type: 'onnx',
  modelPath: files.path(model_path),
  imageSize: 480,
  labels: [
    'booth_btn', 'collect_coin', 'collect_egg', 'collect_food', 'cook', 'countdown', 'donate',
    'eating_chicken', 'employ', 'empty_booth', 'feed_btn', 'friend_btn', 'has_food', 'has_shit',
    'hungry_chicken', 'item', 'kick-out', 'no_food', 'not_ready', 'operation_booth', 'plz-go',
    'punish_booth', 'punish_btn', 'signboard', 'sleep', 'speedup', 'sports', 'stopped_booth',
    'thief_chicken', 'close_btn', 'collect_muck', 'confirm_btn', 'working_chicken', 'bring_back',
    'leave_msg', 'speedup_eating',
  ]
})

if (!yoloInit) {
  toast('初始化失败')
  exit()
}

const img = images.read("./manor.jpg")
let start = new Date()
const result = $yolo.forward(img)
log('predict result:' + JSON.stringify(result, null, 4))
toastLog('onnx cost: ' + (new Date() - start) + 'ms')
img.recycle()