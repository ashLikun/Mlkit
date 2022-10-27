package com.ashlikun.mlkit.vision.barcode.helper

import android.content.Context
import android.graphics.Bitmap
import com.ashlikun.mlkit.vision.barcode.analyze.BarcodeScanningAnalyzer
import com.ashlikun.mlkit.vision.barcode.view.OnScanSuccess
import com.ashlikun.mlkit.vision.camera.AnalyzeResult
import com.ashlikun.mlkit.vision.camera.analyze.Analyzer.OnAnalyzeListener
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.File

/**
 * @author　　: 李坤
 * 创建时间: 2022/5/2 20:50
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：创建二维码图片
 */
object ScanQRCodeHelper {
    /**
     * 解码
     */
    fun syncDecodeFile(context: Context, filePath: String, failuer: (() -> Unit)? = null, success: OnScanSuccess) {
        val file = File(filePath)
        if (!file.exists())
            return
        val de = BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)
        de.parseFile(context, file, object : OnAnalyzeListener<AnalyzeResult<List<Barcode>>> {
            override fun onSuccess(result: AnalyzeResult<List<Barcode>>) {
                success.invoke(result)
            }

            override fun onFailure() {
                failuer?.invoke()
            }
        })
    }

    /**
     * 解码
     */
    fun syncDecodeBitmap(bitmap: Bitmap?, failuer: (() -> Unit)? = null, success: OnScanSuccess) {
        if (bitmap == null)
            return
        val de = BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)
        de.parseBitmap(bitmap, object : OnAnalyzeListener<AnalyzeResult<List<Barcode>>> {
            override fun onSuccess(result: AnalyzeResult<List<Barcode>>) {
                success.invoke(result)
            }

            override fun onFailure() {
                failuer?.invoke()
            }
        })
    }

}