package com.king.mlkit.vision.barcode.view

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
 */
class ScanStyle1View @JvmOverloads constructor(
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