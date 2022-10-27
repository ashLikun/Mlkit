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
package com.ashlikun.mlkit.vision.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.ashlikun.mlkit.vision.camera.analyze.Analyzer;
import com.ashlikun.mlkit.vision.camera.config.CameraConfig;
import com.ashlikun.mlkit.vision.camera.manager.BeepManager;
import com.ashlikun.mlkit.vision.camera.util.MlImageUtils;
import com.ashlikun.mlkit.vision.camera.util.MlLogUtils;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * @author　　: 李坤
 * 创建时间: 2022/10/26 19:54
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：抽象解析主体
 */

public class BaseCameraScan<T> extends CameraScan<T> {

    /**
     * Defines the maximum duration in milliseconds between a touch pad
     * touch and release for a given touch to be considered a tap (click) as
     * opposed to a hover movement gesture.
     */
    private static final int HOVER_TAP_TIMEOUT = 150;

    /**
     * Defines the maximum distance in pixels that a touch pad touch can move
     * before being released for it to be considered a tap (click) as opposed
     * to a hover movement gesture.
     */
    private static final int HOVER_TAP_SLOP = 20;
    private static final int FLASH_CHANG = 22;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private PreviewView mPreviewView;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private Camera mCamera;

    private CameraConfig mCameraConfig;
    private Analyzer<T> mAnalyzer;

    private int STANDVALUES = 100;

    private Boolean isBright = null;

    /**
     * 是否分析
     */
    private volatile boolean isAnalyze = true;

    /**
     * 是否已经分析出结果
     */
    private volatile boolean isAnalyzeResult;

    private View flashlightView;

    private MutableLiveData<AnalyzeResult<T>> mResultLiveData;

    private OnScanResultCallback mOnScanResultCallback;
    private Analyzer.OnAnalyzeListener<AnalyzeResult<T>> mOnAnalyzeListener;

    private BeepManager mBeepManager;

    private long mLastHoveTapTime;
    private boolean isClickTap;
    private float mDownX;
    private float mDownY;
    private Handler mainHandle = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == FLASH_CHANG) {
                changFlash((Boolean) msg.obj);
            }
        }
    };

    public BaseCameraScan(Context context, @NonNull LifecycleOwner lifecycleOwner, @NonNull PreviewView previewView) {
        this.mLifecycleOwner = lifecycleOwner;
        this.mContext = context;
        this.mPreviewView = previewView;
        initData();
    }

    public BaseCameraScan(@NonNull ComponentActivity activity, @NonNull PreviewView previewView) {
        this.mLifecycleOwner = activity;
        this.mContext = activity;
        this.mPreviewView = previewView;
        initData();
    }

    public BaseCameraScan(@NonNull Fragment fragment, @NonNull PreviewView previewView) {
        this.mLifecycleOwner = fragment;
        this.mContext = fragment.getContext();
        this.mPreviewView = previewView;
        initData();
    }

    private ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            if (mCamera != null) {
                float ratio = mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                zoomTo(ratio * scale);
                return true;
            }
            return false;
        }

    };

    private void initData() {
        mResultLiveData = new MutableLiveData<>();
        mResultLiveData.observe(mLifecycleOwner, result -> {
            isAnalyzeResult = false;
            if (result != null) {
                handleAnalyzeResult(result);
            } else if (mOnScanResultCallback != null) {
                mOnScanResultCallback.onScanResultFailure();
            }
        });

        mOnAnalyzeListener = new Analyzer.OnAnalyzeListener<AnalyzeResult<T>>() {

            @Override
            public void onSuccess(@NonNull AnalyzeResult<T> result) {
                mResultLiveData.postValue(result);
            }

            @Override
            public void onFailure() {
                mResultLiveData.postValue(null);
            }
        };

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);
        mPreviewView.setOnTouchListener((v, event) -> {
            handlePreviewViewClickTap(event);
            if (isNeedTouchZoom()) {
                return scaleGestureDetector.onTouchEvent(event);
            }
            return false;
        });
        mBeepManager = new BeepManager(mContext);

    }

    private void handleFlash(int avDark) {
        Message msg = Message.obtain();
        msg.what = FLASH_CHANG;
        if (avDark > STANDVALUES && (isBright == null || isBright)) {
            isBright = false;
            msg.obj = false;
        }
        if (avDark < STANDVALUES && (isBright == null || !isBright)) {
            isBright = true;
            msg.obj = true;
        }
        if (msg.obj != null) {
            mainHandle.sendMessage(msg);
        }
    }

    private void changFlash(boolean dark) {
        if (dark) {
            if (flashlightView.getVisibility() != View.VISIBLE) {
                flashlightView.setVisibility(View.VISIBLE);
                flashlightView.setSelected(isTorchEnabled());
            }
        } else if (flashlightView.getVisibility() == View.VISIBLE && !isTorchEnabled()) {
            flashlightView.setVisibility(View.INVISIBLE);
            flashlightView.setSelected(false);
        }
    }

    private void handlePreviewViewClickTap(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isClickTap = true;
                    mDownX = event.getX();
                    mDownY = event.getY();
                    mLastHoveTapTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    isClickTap = distance(mDownX, mDownY, event.getX(), event.getY()) < HOVER_TAP_SLOP;
                    break;
                case MotionEvent.ACTION_UP:
                    if (isClickTap && mLastHoveTapTime + HOVER_TAP_TIMEOUT > System.currentTimeMillis()) {
                        startFocusAndMetering(event.getX(), event.getY());
                    }
                    break;
            }
        }
    }

    private float distance(float aX, float aY, float bX, float bY) {
        float xDiff = aX - bX;
        float yDiff = aY - bY;
        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    private void startFocusAndMetering(float x, float y) {
        if (mCamera != null) {
            MlLogUtils.d("startFocusAndMetering:" + x + "," + y);
            MeteringPoint point = mPreviewView.getMeteringPointFactory().createPoint(x, y);
            mCamera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point).build());
        }
    }


    private void initConfig() {
        if (mCameraConfig == null) {
            mCameraConfig = new CameraConfig();
        }
    }


    @Override
    public CameraScan setCameraConfig(CameraConfig cameraConfig) {
        if (cameraConfig != null) {
            this.mCameraConfig = cameraConfig;
        }
        return this;
    }

    @Override
    public void startCamera() {
        initConfig();
        mCameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        mCameraProviderFuture.addListener(() -> {

            try {
                Preview preview = mCameraConfig.options(new Preview.Builder());

                //相机选择器
                CameraSelector cameraSelector = mCameraConfig.options(new CameraSelector.Builder());
                //设置SurfaceProvider
                preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

                //图像分析
                ImageAnalysis imageAnalysis = mCameraConfig.options(new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST));
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), image -> {
                    if (isAnalyze && !isAnalyzeResult && mAnalyzer != null) {
                        isAnalyzeResult = true;
                        byte[] bytes = mAnalyzer.analyze(image, mOnAnalyzeListener);
                        if (bytes != null && flashlightView != null) {
                            handleFlash(MlImageUtils.getAvDark(bytes));
                        }
                    }
                    image.close();
                });
                if (mCamera != null) {
                    mCameraProviderFuture.get().unbindAll();
                }
                //绑定到生命周期
                mCamera = mCameraProviderFuture.get().bindToLifecycle(mLifecycleOwner, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                MlLogUtils.e(e);
            }

        }, ContextCompat.getMainExecutor(mContext));
    }

    /**
     * 解析文件
     */
    @Override
    public void parseFile(File file) {
        mAnalyzer.parseFile(mContext, file, mOnAnalyzeListener);
    }

    /**
     * 处理分析结果
     *
     * @param result
     */
    private synchronized void handleAnalyzeResult(AnalyzeResult<T> result) {

        if (isAnalyzeResult || !isAnalyze) {
            return;
        }

        if (mBeepManager != null) {
            mBeepManager.playBeepSoundAndVibrate();
        }

        if (mOnScanResultCallback != null) {
            mOnScanResultCallback.onScanResultCallback(result);
        }
    }


    @Override
    public void stopCamera() {
        if (mCameraProviderFuture != null) {
            try {
                mCameraProviderFuture.get().unbindAll();
            } catch (Exception e) {
                MlLogUtils.e(e);
            }
        }
    }

    @Override
    public CameraScan setAnalyzeImage(boolean analyze) {
        isAnalyze = analyze;
        return this;
    }

    @Override
    public CameraScan setAnalyzer(Analyzer<T> analyzer) {
        mAnalyzer = analyzer;
        return this;
    }

    @Override
    public void zoomIn() {
        if (mCamera != null) {
            float ratio = mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio() + 0.1f;
            float maxRatio = mCamera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
            if (ratio <= maxRatio) {
                mCamera.getCameraControl().setZoomRatio(ratio);
            }
        }
    }

    @Override
    public void zoomOut() {
        if (mCamera != null) {
            float ratio = mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio() - 0.1f;
            float minRatio = mCamera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
            if (ratio >= minRatio) {
                mCamera.getCameraControl().setZoomRatio(ratio);
            }
        }
    }


    @Override
    public void zoomTo(float ratio) {
        if (mCamera != null) {
            ZoomState zoomState = mCamera.getCameraInfo().getZoomState().getValue();
            float maxRatio = zoomState.getMaxZoomRatio();
            float minRatio = zoomState.getMinZoomRatio();
            float zoom = Math.max(Math.min(ratio, maxRatio), minRatio);
            mCamera.getCameraControl().setZoomRatio(zoom);
        }
    }

    @Override
    public void lineZoomIn() {
        if (mCamera != null) {
            float zoom = mCamera.getCameraInfo().getZoomState().getValue().getLinearZoom() + 0.1f;
            if (zoom <= 1f) {
                mCamera.getCameraControl().setLinearZoom(zoom);
            }
        }
    }

    @Override
    public void lineZoomOut() {
        if (mCamera != null) {
            float zoom = mCamera.getCameraInfo().getZoomState().getValue().getLinearZoom() - 0.1f;
            if (zoom >= 0f) {
                mCamera.getCameraControl().setLinearZoom(zoom);
            }
        }
    }

    @Override
    public void lineZoomTo(@FloatRange(from = 0.0, to = 1.0) float linearZoom) {
        if (mCamera != null) {
            mCamera.getCameraControl().setLinearZoom(linearZoom);
        }
    }

    @Override
    public void enableTorch(boolean torch) {
        if (mCamera != null && hasFlashUnit()) {
            mCamera.getCameraControl().enableTorch(torch);
        }
    }

    @Override
    public boolean isTorchEnabled() {
        if (mCamera != null) {
            return mCamera.getCameraInfo().getTorchState().getValue() == TorchState.ON;
        }
        return false;
    }

    /**
     * 是否支持闪光灯
     *
     * @return
     */
    @Override
    public boolean hasFlashUnit() {
        if (mCamera != null) {
            return mCamera.getCameraInfo().hasFlashUnit();
        }
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public CameraScan setVibrate(boolean vibrate) {
        if (mBeepManager != null) {
            mBeepManager.setVibrate(vibrate);
        }
        return this;
    }

    @Override
    public CameraScan setPlayBeep(boolean playBeep) {
        if (mBeepManager != null) {
            mBeepManager.setPlayBeep(playBeep);
        }
        return this;
    }

    @Override
    public CameraScan setOnScanResultCallback(OnScanResultCallback callback) {
        this.mOnScanResultCallback = callback;
        return this;
    }

    @Nullable
    @Override
    public Camera getCamera() {
        return mCamera;
    }


    @Override
    public void release() {
        isAnalyze = false;
        flashlightView = null;
        if (mBeepManager != null) {
            mBeepManager.close();
        }
        stopCamera();
        mainHandle.removeCallbacksAndMessages(null);
    }

    @Override
    public CameraScan bindFlashlightView(@Nullable View v) {
        flashlightView = v;
        flashlightView.setVisibility(View.INVISIBLE);
        return this;
    }

}
