/*
 * Copyright (C) Jenly, MLKit Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashlikun.zxing.simple

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.ashlikun.mlkit.vision.barcode.XScanBar
import com.ashlikun.mlkit.vision.camera.config.ResolutionCameraConfig

/**
 * @author　　: 李坤
 * 创建时间: 2022/10/26 15:14
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：扫码案例
 */

class ScanSimpleActivity : AppCompatActivity() {

    val xScanBar by lazy {
        XScanBar.get(this).apply {
            previewView = findViewById(R.id.previewView)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qrcode_scan_activity)
        xScanBar.cameraScan.apply {
            setPlayBeep(true)
            setVibrate(true)
            setCameraConfig(ResolutionCameraConfig(this@ScanSimpleActivity))//设置CameraConfig
        }
        xScanBar.onResult = { result ->
            xScanBar.cameraScan.setAnalyzeImage(false)
            val buffer = StringBuilder()
            val bitmap = result.bitmap.drawRect { canvas, paint ->
                for ((index, data) in result.result.withIndex()) {
                    buffer.append("[$index] ").append(data.displayValue).append("\n")
                    data.boundingBox?.let { box ->
                        canvas.drawRect(box, paint)
                    }
                }
            }

            val config = AppDialogConfig(this, R.layout.barcode_result_dialog)
            config.setContent(buffer).setOnClickConfirm {
                AppDialog.INSTANCE.dismissDialog()
                xScanBar.cameraScan.setAnalyzeImage(true)
            }.setOnClickCancel {
                AppDialog.INSTANCE.dismissDialog()
                finish()
            }
            val imageView = config.getView<ImageView>(R.id.ivDialogContent)
            imageView.setImageBitmap(bitmap)
            AppDialog.INSTANCE.showDialog(config, false)
        }
        xScanBar.startCamera()
    }


}