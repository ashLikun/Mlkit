package com.ashlikun.mlkit.vision.barcode.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView
import com.king.mlkit.vision.barcode.R

/**
 * 作者　　: 李坤
 * 创建时间: 2022/10/26　16:37
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：
 * ```xml
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

 */
open class ScanStyle1View @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    def: Int = 0
) : BaseScanView(context, attributeSet, def) {

    override val layoutId: Int
        get() = R.layout.ml_qrcode_scan_view1
    override val previewView: PreviewView
        get() = findViewById(R.id.previewView)
    override val flashlightView: View
        get() = findViewById(R.id.ivFlashlight)

}