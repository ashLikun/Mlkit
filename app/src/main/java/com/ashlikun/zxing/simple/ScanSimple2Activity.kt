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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashlikun.zxing.simple.databinding.QrcodeScanActivity2Binding
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.ashlikun.mlkit.vision.barcode.view.BaseScanView
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import java.io.File

/**
 * @author　　: 李坤
 * 创建时间: 2022/10/26 15:14
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：扫码案例
 */

class ScanSimple2Activity : AppCompatActivity() {
    val binding by lazy {
        QrcodeScanActivity2Binding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
//        binding.scanView.init(this)
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

            val config = AppDialogConfig(this, R.layout.barcode_result_dialog)
            config.setContent(buffer).setOnClickConfirm {
                AppDialog.INSTANCE.dismissDialog()
                binding.scanView.cameraScan.setAnalyzeImage(true)
            }.setOnClickCancel {
                AppDialog.INSTANCE.dismissDialog()
                finish()
            }
            val imageView = config.getView<ImageView>(R.id.ivDialogContent)
            imageView.setImageBitmap(bitmap)
            AppDialog.INSTANCE.showDialog(config, false)
        }
        binding.scanView.onFailure = {
            if (it) {
                Toast.makeText(this@ScanSimple2Activity, "识别错误", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("aaaaaa", "111111111111111111")
            }
        }
        binding.rightTextView.setOnClickListener {
            if (!checkPermissionRW()) {
                requstPermissionRW()
                return@setOnClickListener
            }
            Matisse.from(this)
                .choose(MimeType.ofAll())
                .countable(true)
                .maxSelectable(9)
                .gridExpectedSize(300)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(GlideEngine())
                .showPreview(false) // Default is `true`
                .forResult(1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val path = Matisse.obtainPathResult(data)[0]
                binding.scanView.parseFile(File(path))
            }
        }
    }

    fun checkPermissionRW(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            return true
        }
    }

    fun requstPermissionRW() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 200)
        }
    }


}