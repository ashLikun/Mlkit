# MLKit

ML Kit是一个能够将谷歌专业的机器学习知识带到应用中的极其简单易用的封装包。无论您是否有机器学习的经验，您都可以在几行代码中实现您想要的功能。甚至，您无需对神经网络或者模型优化有多深入的了解，也能完成您想要做的事情。
基于现有的API您可以很轻松的实现文字识别、条码识别、图像标记、人脸检测、对象检测等功能；另一方面，如果您是一位经验丰富的ML开发人员，ML kit甚至提供了便利的API，可帮助您在移动应用中使用自定义的TensorFlow Lit模型。

## GIF 展示
```xml
 <com.ashlikun.mlkit.vision.barcode.view.ScanStyle1View
        android:id="@+id/scanView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

```
```kotlin
//初始化摄像头
 binding.scanView.init(this)
//获取结果
 binding.scanView.onResult = { result ->
            binding.scanView.cameraScan.setAnalyzeImage(false)
            val buffer = StringBuilder()
            val bitmap = result.bitmap.drawRect { canvas, paint ->
                for ((index, data) in result.result.withIndex()) {
                    buffer.append("[$index] ").append(data.displayValue).append("\n")
                    data.boundingBox?.let { box ->
                        canvas.drawRect(box, paint)
                    }
                }
            }
        }
//相册选择图片解析
binding.scanView.parseFile(File(path))
```
