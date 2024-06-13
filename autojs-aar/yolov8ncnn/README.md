# ncnn yolov8推理

## 注意事项

- 当前ncnn和paddle-lite存在兼容问题，一旦通过paddle-lite进行了ocr推理，则无法继续使用ncnn进行yolo推理，必须重启autojs才行，具体原因未知
- 因此需要使用ncnn时最好不要使用paddleocr，改用mlkitocr即可

## 首次编译需要下载opencv包和ncnn-android-vulkan包

- 运行downloadAndExtractNcnnArchives即可自动下载，网络不好的话直接通过github下载即可

## 对于模型训练后导出ncnn需要注意以下内容

- yolov8训练后导出onnx需要调整修改ultralytics代码, 以下内容为 ultralytics==8.2.27 其他版本参考修改即可

```python
  #ultralytics/nn/modules/block.py
  class C2f(nn.Module):
     # ...
     def forward(self, x):
         """Forward pass through C2f layer."""
         # CHANGED
         # y = list(self.cv1(x).chunk(2, 1))
         # y.extend(m(y[-1]) for m in self.m)
         # return self.cv2(torch.cat(y, 1))
         # CHANGED
         print("ook")
         x = self.cv1(x)
         x = [x, x[:, self.c:, ...]]
         x.extend(m(x[-1]) for m in self.m)
         x.pop(1)
         return self.cv2(torch.cat(x, 1))

  #ultralytics/nn/modules/head.py
  class Detect(nn.Module):
     def forward(self, x):
         """Concatenates and returns predicted bounding boxes and class probabilities."""
         for i in range(self.nl):
             x[i] = torch.cat((self.cv2[i](x[i]), self.cv3[i](x[i])), 1)
         if self.training:  # Training path
             return x

         # Inference path
         shape = x[0].shape  # BCHW

         # 转成ncnn格式时，请使用以下两行
 ##      #CHANGED
         pred = torch.cat([xi.view(shape[0], self.no, -1) for xi in x], 2).permute(0, 2, 1)
         return pred
 ##      #CHANGED

```


- 导出ncnn
```python
    model_path = 'best.pt'
    model = YOLO(model_path)
    model.export(task="detect", format="onnx", opset=12, imgsz=480, simplify=True)
```

- 导出onnx模型后通过在线模型转换成ncnn模型 https://convertmodel.com/