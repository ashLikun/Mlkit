package com.ashlikun.mlkit.vision.barcode.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.ashlikun.mlkit.vision.barcode.analyze.BarcodeScanningAnalyzer
import com.ashlikun.mlkit.vision.barcode.utils.Utils
import com.ashlikun.mlkit.vision.camera.AnalyzeResult
import com.ashlikun.mlkit.vision.camera.BaseCameraScan
import com.ashlikun.mlkit.vision.camera.CameraScan.OnScanResultCallback
import java.io.File

/**
 * 作者　　: 李坤
 * 创建时间: 2022/10/26　15:16
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：基础的扫码view
 */
typealias OnScanSuccess = (AnalyzeResult<List<Barcode>>) -> Unit
typealias OnScanResultFailure = (isParseFile: Boolean) -> Unit

abstract class BaseScanView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    def: Int = 0
) : FrameLayout(context, attributeSet, def), DefaultLifecycleObserver {

    /**
     * 结果回调，提供给外部使用
     */
    var onResult: OnScanSuccess? = null
    var onFailure: OnScanResultFailure? = null

    /**
     * 预览的View
     */
    open abstract protected val previewView: PreviewView

    /**
     * 闪光灯view
     */
    open abstract protected val flashlightView: View?

    /**
     * 创建分析器，默认分析所有条码格式
     */
    open val analyzer by lazy {
        BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)
    }
    lateinit var lifecycleOwner: LifecycleOwner

    /**
     * 相机类
     */
    open val cameraScan by lazy {
        BaseCameraScan<List<Barcode>>(context, lifecycleOwner, previewView!!)
            .setAnalyzer(analyzer)
            .setOnScanResultCallback(onScanResultCallback)
    }

    /**
     * 扫码结果回调
     */
    protected open val onScanResultCallback = object : OnScanResultCallback<List<Barcode>> {
        override fun onScanResultCallback(result: AnalyzeResult<List<Barcode>>) {
            onResult?.invoke(result)
        }

        override fun onScanResultFailure(isParseFile: Boolean) {
            super.onScanResultFailure(isParseFile)
            onFailure?.invoke(isParseFile)
        }
    }


    protected abstract val layoutId: Int

    init {
        LayoutInflater.from(context).inflate(layoutId, this, false).let {
            addView(it)
        }
        //手电筒点击
        flashlightView?.setOnClickListener {
            toggleTorchState()
        }
    }

    /**
     * 切换闪光灯状态（开启/关闭）
     */
    protected open fun toggleTorchState() {
        val isTorch: Boolean = cameraScan.isTorchEnabled()
        cameraScan.enableTorch(!isTorch)
        flashlightView?.isSelected = !isTorch
    }


    /**
     * 初始化
     */
    fun init(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        cameraScan.bindFlashlightView(flashlightView)
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 启动相机预览
     */
    fun startCamera() {
        if (Utils.checkPermissionCamera(context)) {
            cameraScan.startCamera()
        } else {
            Utils.requstPermission(context)
        }
    }

    /***
     *  外部使用该方法启动相机
     */
    fun cameraResume() {
        if (Utils.checkPermissionCamera(context) && cameraScan.camera == null) {
            startCamera()
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
        cameraResume()
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

    /**
     * 解析文件
     */
    open fun parseFile(file: File) {
        cameraScan.parseFile(file)
    }

}