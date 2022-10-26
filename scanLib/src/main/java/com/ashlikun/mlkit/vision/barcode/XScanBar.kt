package com.ashlikun.mlkit.vision.barcode

import android.content.Context
import androidx.annotation.CallSuper
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.ashlikun.mlkit.vision.barcode.analyze.BarcodeScanningAnalyzer
import com.ashlikun.mlkit.vision.barcode.utils.Utils
import com.ashlikun.mlkit.vision.barcode.view.OnScanSuccess
import com.ashlikun.mlkit.vision.camera.AnalyzeResult
import com.ashlikun.mlkit.vision.camera.BaseCameraScan
import com.ashlikun.mlkit.vision.camera.CameraScan

/**
 * 作者　　: 李坤
 * 创建时间: 2022/10/26　16:00
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：扫码的封装
 */
class XScanBar private constructor(
    val context: Context,
    val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {
    companion object {
        fun get(actrivity: androidx.core.app.ComponentActivity): XScanBar = XScanBar(actrivity, actrivity)
        fun get(context: Context, lifecycleOwner: LifecycleOwner): XScanBar = XScanBar(context, lifecycleOwner)
        fun get(previewView: PreviewView): XScanBar = XScanBar(previewView.context, previewView.findViewTreeLifecycleOwner()!!).also {
            it.previewView = previewView
        }
    }


    /**
     * 预览的View
     */
    var previewView: PreviewView? = null

    /**
     * 创建分析器，默认分析所有条码格式
     */
    var analyzer = BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)

    /**
     * 结果回调，提供给外部使用
     */
    var onResult: OnScanSuccess? = null

    /**
     * 扫码结果回调
     */
    private val onScanResultCallback = object : CameraScan.OnScanResultCallback<List<Barcode>> {
        override fun onScanResultCallback(result: AnalyzeResult<List<Barcode>>) {
            onResult?.invoke(result)
        }
    }

    /**
     * 相机类
     */
    val cameraScan by lazy {
        BaseCameraScan<List<Barcode>>(context, lifecycleOwner, previewView!!)
            .setAnalyzer(analyzer)
            .setOnScanResultCallback(onScanResultCallback)
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 启动相机预览
     */
    fun startCamera() {
        if (cameraScan.camera != null) return
        if (Utils.checkPermissionCamera(context)) {
            cameraScan.startCamera()
        } else {
            Utils.requstPermission(context)
        }
    }


    @CallSuper
    override fun onCreate(owner: LifecycleOwner) {
        startCamera()
        super.onCreate(owner)
    }

    /**
     * 生命周期
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        startCamera()
    }

    /**
     * 生命周期
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
    }

    /**
     * 释放相机
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        cameraScan.release()
    }
}