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
package com.king.mlkit.vision.barcode.analyze

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.BarcodeFormat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import androidx.camera.core.ImageProxy
import com.king.mlkit.vision.camera.analyze.Analyzer.OnAnalyzeListener
import com.king.mlkit.vision.camera.AnalyzeResult
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import com.king.mlkit.vision.camera.util.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import com.king.mlkit.vision.barcode.utils.Utils
import com.king.mlkit.vision.camera.analyze.Analyzer
import java.lang.Exception
import com.king.mlkit.vision.camera.util.LogUtils
import org.jetbrains.annotations.NotNull
import java.io.File

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
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
    ) {
    }

    constructor(options: BarcodeScannerOptions) {
        mDetector = BarcodeScanning.getClient(options)
    }

    override fun analyze(imageProxy: ImageProxy, listener: OnAnalyzeListener<AnalyzeResult<List<Barcode>>>) {
        try {
            val bitmap = BitmapUtils.getBitmap(imageProxy)
            //            final Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
//            @SuppressLint("UnsafeExperimentalUsageError")
//            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(),imageProxy.getImageInfo().getRotationDegrees());
            val inputImage = InputImage.fromBitmap(bitmap!!, 0)
            mDetector!!.process(inputImage)
                .addOnSuccessListener { result: List<Barcode>? ->
                    if (result == null || result.isEmpty()) {
                        listener.onFailure()
                    } else {
                        listener.onSuccess(AnalyzeResult<List<Barcode>>(bitmap, result))
                    }
                }.addOnFailureListener { e: Exception? -> listener.onFailure() }
        } catch (e: Exception) {
            LogUtils.w(e)
        }
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
                mDetector!!.process(inputImage)
                    .addOnSuccessListener { result: List<Barcode>? ->
                        if (result == null || result.isEmpty()) {
                            listener.onFailure()
                        } else {
                            listener.onSuccess(AnalyzeResult<List<Barcode>>(bitmap, result))
                        }
                    }.addOnFailureListener { e: Exception? -> listener.onFailure() }
            }
        } catch (e: Exception) {
            LogUtils.w(e)
        }
    }
}