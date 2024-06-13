let model_path = '/sdcard/脚本/yolov8n.bin'
let param_path = '/sdcard/脚本/yolov8n.param'
if (!files.exists(model_path) || !files.exists(param_path)) {
    toastLog('请确认已下载了模型文件')
    exit()
}
console.show()
setTimeout(() -> console.hide(), 15000)
let yoloInit = $yolo.init({
  type: 'ncnn',
  paramPath: files.path(param_path),
  binPath: files.path(model_path),
  imageSize: 480,
  labels: [
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
    "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
    "hair drier", "toothbrush"
  ]
})

if (!yoloInit) {
  toast('初始化失败')
  exit()
}

const img = images.read("./bus.jpg")
let start = new Date()
const result = $yolo.forward(img)
toastLog('ncnn cost: ' + (new Date() - start) + 'ms')
log('predict result:' + JSON.stringify(result, null, 4))

img.recycle()
