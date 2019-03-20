package com.example.cameraphotosave;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import java.util.Arrays;
import static android.content.ContentValues.TAG;

/*
 * CameraPhotoSave : Camera preview capture & save & read
 * Author : DONGGEUN JUNG (Dennis)
 * Email : topsan72@gmail.com / topofsan@naver.com
 */

public class CameraControl {
    private TextureView mTextureView;
    private Activity mParent;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;

    public CameraControl(Activity parent, TextureView tv) {
        mParent = parent;
        mTextureView = tv;
        // TextureView 이벤트 리스너 지정
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    // TextureView 이벤트 리스너
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable, width=" + width + ",height=" + height);
            // 카메라를 연다
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    // 카메라를 연다
    public void openCamera() {
        // 안드로이드 마시멜로우 이후 버전이라면 사용자에게 Permission 을 획득한다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // 사용자가 Permission 을 부여하지 않았다면 권한을 요청하는 팝업창을 표시
            if(ContextCompat.checkSelfPermission(mParent, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
        openCamera(mStateCallback);
        //Log.e(TAG, "openCamera-2");
    }

    // 카메라 오픈(사용자 권한 획득한 상태일때만 수행됨)
    private void openCamera(CameraDevice.StateCallback stateCallback) {
        // 카메라 관리자 핸들 구하기
        CameraManager manager = (CameraManager) mParent.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 후면 카메라의 ID를 구한다
            String cameraId = manager.getCameraIdList()[0];
            // 프리뷰 영상의 크기를 구한다
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            try {
                // 카메라를 연다
                manager.openCamera(cameraId, stateCallback, null);
            } catch (SecurityException e) {
                Log.e(TAG, "manager.openCamera Error: " + e.toString());
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 카메라 상태변경 이벤트 리스너
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            // 카메라 프리뷰 영상을 TextureView 에 출력 시작
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError");
        }
    };

    // 카메라 프리뷰 영상을 TextureView 에 출력 시작
    protected void startPreview() {
        // TextureView 의 표면을 구한다
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == mCameraDevice || !mTextureView.isAvailable()
                || null == mPreviewSize || null == texture) {
            Log.e(TAG, "startPreview fail, return");
            return;
        }

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            mPreviewBuilder =
                    mCameraDevice.createCaptureRequest( CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mPreviewSession = session;
                            // 프리뷰 영상 갱신
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(mParent, "onConfigureFailed", Toast.LENGTH_LONG).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 프리뷰 영상 갱신
    protected void updatePreview() {
        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
            return;
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void CloseCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

}
