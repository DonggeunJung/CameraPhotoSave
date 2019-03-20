package com.example.cameraphotosave;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/*
 * CameraPhotoSave : Camera preview capture & save & read
 * Author : DONGGEUN JUNG (Dennis)
 * Email : topsan72@gmail.com / topofsan@naver.com
 */

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_RESULT = 1;
    private final static String TAG = "tag";

    private TextureView mTextureView;
    private ImageView ivPicture;
    private CameraControl mCC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (TextureView)findViewById(R.id.texture);
        ivPicture = (ImageView) findViewById(R.id.ivPicture);
        mCC = new CameraControl(this, mTextureView);

        // 사용자 권한 체크
        checkPermission();

        // 이미지 파일을 화면에 표시
        image2Screen();
    }

    // 사용자 권한 체크
    private void checkPermission() {
        // 안드로이드 마시멜로우 이후 버전이라면 사용자에게 Permission 을 획득한다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // 사용자가 Permission 을 부여하지 않았다면 권한을 요청하는 팝업창을 표시
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED){
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
                    Toast.makeText(this,"No Permission to use the Camera services", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {android.Manifest.permission.CAMERA},REQUEST_CAMERA_RESULT);
                return;
            }
        }
    }

    // 권한 요청 결과 이벤트 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            // 카메라 사용 권한 요청 결과 일때
            case REQUEST_CAMERA_RESULT:
                // 사용자가 권한 부여를 거절 했을때
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                // 사용자가 권한을 부여했을때
                else {
                    mCC.openCamera();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        mCC.CloseCamera();
    }

    // 이미지 파일을 화면에 표시
    void image2Screen() {
        File file = getImageFile();
        if( file.exists() ) {
            Bitmap bmpFile = BitmapFactory.decodeFile(file.getPath());
            ivPicture.setImageBitmap(bmpFile);
        }
    }

    File getImageFile() {
        File file = getFileStreamPath("camerashot.jpg");
        return file;
    }

    public void onClick(View v) {
        // TextureView 에서 이미지를 캡쳐한다
        Bitmap bmp = mTextureView.getBitmap();

        // 캡쳐 이미지 데이터를 JPEG 형식으로 변경
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //저장된이미지를 jpeg로 포맷 품질 95으로하여 출력
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, bos);
        byte[] bytes = bos.toByteArray();

        // 이미지를 파일로 저장
        File file = getImageFile();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            Toast.makeText(getApplicationContext(), "Image File saved!",
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d("tag", "File Write Error");
            return;
        }

        // 이미지 파일을 화면에 표시
        image2Screen();
    }

}
