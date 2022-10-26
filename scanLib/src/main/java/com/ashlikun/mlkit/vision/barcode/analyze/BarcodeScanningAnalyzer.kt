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
package com.ashlikun.mlkit.vision.barcode.analyze

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.common.Barcode.BarcodeFormat
import com.google.mlkit.vision.common.InputImage
import com.ashlikun.mlkit.vision.barcode.utils.Utils
import com.ashlikun.mlkit.vision.camera.AnalyzeResult
import com.ashlikun.mlkit.vision.camera.analyze.Analyzer
import com.ashlikun.mlkit.vision.camera.analyze.Analyzer.OnAnalyzeListener
import com.ashlikun.mlkit.vision.camera.util.BitmapUtils
import com.ashlikun.mlkit.vision.camera.util.LogUtils
import java.io.File

/**
 * 扫码解析类
 */
class BarcodeScanningAnalyzer : Analyzer<List<Barcode>> {
    private var mDetector: BarcodeScanner

    constructor() {
        mDetector = BarcodeScanning.getClient()
    }

    constructor(@BarcodeFormat barcodeFormat: Int, @BarcodeFormat vararg barcodeFormats: Int) : this(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormat, *barcodeFormats)
            .build()
    )

    constructor(options: BarcodeScannerOptions) {
        mDetector = BarcodeScanning.getClient(options)
    }

    override fun analyze(imageProxy: ImageProxy, listener: OnAnalyzeListener<AnalyzeResult<List<Barcode>>>): ByteArray? {
        try {
            val nv21Buffer = BitmapUtils.yuv420ThreePlanesToNV21(imageProxy.image?.planes, imageProxy.width, imageProxy.height)
            val bitmap = BitmapUtils.getBitmap(nv21Buffer, imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)
            //            final Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
//            @SuppressLint("UnsafeExperimentalUsageError")
//            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(),imageProxy.getImageInfo().getRotationDegrees());
            val inputImage = InputImage.fromBitmap(bitmap!!, 0)
            mDetector.process(inputImage)
                .addOnSuccessListener { result: List<Barcode>? ->
                    if (result == null || result.isEmpty()) {
                        listener.onFailure()
                    } else {
                        listener.onSuccess(AnalyzeResult<List<Barcode>>(bitmap, result))
                    }
                }.addOnFailureListener { e: Exception? -> listener.onFailure() }
            return nv21Buffer?.array()
        } catch (e: Exception) {
            listener.onFailure()
            LogUtils.w(e)
        }
        return null
    }

    override fun parseFile(context: Context, file: File, listener: OnAnalyzeListener<AnalyzeResult<List<Barcode>>>) {
        try {
            val bitmap: Bitmap? = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(context.contentResolver, Utils.getMediaUriFromPath(context, file.path))
                    .let {
                        ImageDecoder.decodeBitmap(it) { decoder, _, _ ->
                            decoder.setTargetSampleSize(2)
                            decoder.isMutableRequired = true
                        }
                    }
            } else
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                    inSampleSize = 2
                })) ?: null

            if (bitmap != null) {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                mDetector.process(inputImage)
                    .addOnSuccessListener { result: List<Barcode>? ->
                        if (result == null || result.isEmpty()) {
                            listener.onFailure()
                        } else {
                            listener.onSuccess(AnalyzeResult<List<Barcode>>(bitmap, result))
                        }
                    }.addOnFailureListener { e: Exception? -> listener.onFailure() }
            }
        } catch (e: Exception) {
            listener.onFailure()
            LogUtils.w(e)
        }
    }

    fun parseBitmap(bitmap: Bitmap, listener: OnAnalyzeListener<AnalyzeResult<List<Barcode>>>) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            mDetector.process(inputImage)
                .addOnSuccessListener { result: List<Barcode>? ->
                    if (result == null || result.isEmpty()) {
                        listener.onFailure()
                    } else {
                        listener.onSuccess(AnalyzeResult<List<Barcode>>(bitmap, result))
                    }
                }.addOnFailureListener { e: Exception? -> listener.onFailure() }
        } catch (e: Exception) {
            listener.onFailure()
            LogUtils.w(e)
        }
    }
}