package com.riyase.customcam;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomCam extends Activity implements View.OnClickListener{
    private Camera mCamera;
    private CameraPreview mCameraPreview;

    Button bSwitchCam;
    Button bCancel;
    Button bCapture;

    FrameLayout preview;

    int currentCamID;
    public static final String PIC_NAME_TEMP="com.riyase.dp_uncropped";
    public static final String PIC_NAME="customcam";
    public static final String CAMERA_FACING="android.intent.extras.CAMERA_FACING";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFullScreen(this);
        setContentView(R.layout.activity_custom_cam);
        setupViews();
        
        currentCamID=getIntent().getIntExtra(CAMERA_FACING,Camera.CameraInfo.CAMERA_FACING_BACK);
        
        boolean initCamSuccess;
        if(getCameraId(currentCamID)!=-1) {
            initCamSuccess = initCam(currentCamID);
        } else {
            initCamSuccess = initCam(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        
        if(initCamSuccess) initPreview();

    }

    private void setupViews() {
        bSwitchCam=(Button)findViewById(R.id.bSwitchCam);
        bCancel=(Button)findViewById(R.id.bCancel);
        bCapture=(Button)findViewById(R.id.bCapture);
        preview = (FrameLayout) findViewById(R.id.camera_preview);

        bSwitchCam.setOnClickListener(this);
        bCancel.setOnClickListener(this);
        bCapture.setOnClickListener(this);
      
    }
    
    public static void getFullScreen(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    
    private boolean initCam(int camId) {
        if(hasCamera()==false) {
            showToast("Device has no Cam");
            return false;
        }
        
        mCamera = Camera.open(camId);
        
        if(mCamera==null) {
            showToast("Can not access camera");
            finish();
            return false;
        }
        currentCamID=camId;
        return true;
    }
   
    private void initPreview() {
        mCameraPreview = new CameraPreview(this, mCamera);
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    focusOnTouch(event);
                }
                return true;
            }
        });
        preview.addView(mCameraPreview);
    }
    
    public int getCurrentCamID() {
        return currentCamID;
    }
    
    public Camera.Parameters getParams(int camId) {
        
        Camera.Parameters params = mCamera.getParameters();
        
        int screenWidth=getScreenDimens().widthPixels;
        int screenHeight=getScreenDimens().heightPixels;
        Camera.Size previewSize=getOptimalPreviewSize(params.getSupportedPreviewSizes(),screenWidth,screenHeight);
        if(previewSize!=null) {
            params.setPreviewSize(previewSize.width, previewSize.height);
        }
        
        Camera.Size pictureSize=getPictureSize(params);
        if (pictureSize!=null) {
            params.setPictureSize(pictureSize.width, pictureSize.height);
        }
        
        int orientationDisplay=getDisplayOrientation(CustomCam.this);
        int orientationPreview=getPreviewOrientation(mCamera,camId,orientationDisplay);
        mCamera.setDisplayOrientation(orientationPreview);

        int orientationPicFile=getPicFileOrientation(mCamera,camId,orientationDisplay);
        params.setRotation(orientationPicFile);
        
        if(hasAutoFocus()) {
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }

        if(hasFlash()) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        return params;
    }
    
    private DisplayMetrics getScreenDimens() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return  displaymetrics;
    }
    
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    private Camera.Size getPictureSize(
            Camera.Parameters parameters) {
        Camera.Size result=null;
        List<Camera.Size> cSizes=parameters.getSupportedPictureSizes();
        if (cSizes==null||cSizes.size()==0)
            return null;
        
        //gets an average(middle) picture size from available
        result=cSizes.get(cSizes.size()/2);
        
        return(result);
    }

    private int getCameraId(int camId) {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for(int i = 0;i < Camera.getNumberOfCameras();i++){
            Camera.getCameraInfo(i,ci);
            if(ci.facing == camId){
                return camId;
            }
        }
        return -1;
    }

    private int getDisplayOrientation(Activity activity) {
        
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        return degrees;
    }
    
    public static int getPreviewOrientation(
            Camera camera,
            int cameraId,
            int displayRotation) {
    
        Camera.CameraInfo info =   new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + displayRotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - displayRotation + 360) % 360;
        }
       return result;
    }

    private static int getPicFileOrientation(
            Camera camera,
            int cameraId,
            int displayRotation) {
        
        Camera.CameraInfo info =   new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + displayRotation) % 360;
            //do not compensate the mirror; because this is for output picture file.
        } else {  // back-facing
            result = (info.orientation - displayRotation + 360) % 360;
        }
        return result;
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePhotoTask().execute(data);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.bSwitchCam:
                new SwitchCam().execute("");
                break;
            case R.id.bCancel:
                mCamera.release();
                finish();
                break;
            case R.id.bCapture:
                disableAll();
                mCamera.startPreview();
                if(hasAutoFocus()) {
                    mCamera.autoFocus(focusCallbackTakePic);
                } else {
                    mCamera.takePicture(null, null, mPicture);
                }
                break;
        }
    }

    class SwitchCam extends AsyncTask<String, String, Boolean> {
        @Override
        protected void onPreExecute() {
            disableAll();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... jpeg) {
            return switchCam();
        }

        private Boolean switchCam() {

            mCamera.stopPreview();
            mCamera.release();

            if(currentCamID == Camera.CameraInfo.CAMERA_FACING_BACK){
                currentCamID = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            else {
                currentCamID = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            return initCam(currentCamID);
        }

        @Override
        protected void onPostExecute(Boolean switchCamSuccess) {
            super.onPostExecute(switchCamSuccess);
            if (switchCamSuccess) {
                preview.removeAllViews();
                initPreview();
            }
            enableAll();
        }
    }

    class SavePhotoTask extends AsyncTask<byte[], String, File> {
        @Override
        protected void onPreExecute() {
            disableAll();
            super.onPreExecute();
        }

        @Override
        protected File doInBackground(byte[]... jpeg) {
            File photo=//getOutputMediaFile();
                    new File(getCacheDir().getPath() 
                    + File.separator 
                    + PIC_NAME_TEMP+".jpg");
            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());
                fos.write(jpeg[0]);
                fos.close();
            }
            catch (IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return(photo);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            beginCrop(Uri.fromFile(file));
            mCamera.release();
        }

        private void beginCrop(Uri source) {
            
            String filePath=PIC_NAME+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File file=new File(
                    //getCacheDir(),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    filePath);
           
            Uri outputUri = Uri.fromFile(file);
            new Crop(source).output(outputUri).asSquare().start(CustomCam.this);
        }
    }
    
    private void disableAll() {
        bSwitchCam.setEnabled(false);
        bCancel.setEnabled(false);
        bCapture.setEnabled(false);
    }
    
    private void enableAll() {
        bSwitchCam.setEnabled(true);
        bCancel.setEnabled(true);
        bCapture.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Crop.REQUEST_CROP) {

            switch (resultCode)
            {
                case RESULT_OK:
                    setResult(RESULT_OK,data);
                    finish();
                    break;
                
                case Crop.RESULT_RETAKE:
                    enableAll();
                    preview.removeAllViews();
                    initCam(currentCamID);
                    break;
                
                case RESULT_CANCELED:
                    preview.removeAllViews();
                    mCamera.release();
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                
                case Crop.RESULT_ERROR:
                    Toast.makeText(this, Crop.getError(data).getMessage(), Toast.LENGTH_SHORT).show();
                    enableAll();
                    preview.removeAllViews();
                    initCam(currentCamID);
            }
        }
    }

    private boolean hasCamera() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    
    private boolean hasAutoFocus() {
        PackageManager pm = getPackageManager();
        return  pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }
    
    private boolean hasFlash() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void showToast(String msg) {
        Toast.makeText(CustomCam.this,msg,Toast.LENGTH_SHORT).show();
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null && hasAutoFocus()) {

            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0){
                //Log.i(TAG,"fancy !");
                Rect rect = calculateFocusArea(event.getX(), event.getY());

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);
                mCamera.setParameters(parameters);
                mCamera.autoFocus(focusCallback);
            }else {
                mCamera.autoFocus(focusCallback);
            }
        }
    }

    int FOCUS_AREA_SIZE=300;
    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / (float)mCameraPreview.getWidth()) * 2000f - 1000f).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / (float)mCameraPreview.getHeight()) * 2000f - 1000f).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }
    
    
    Camera.AutoFocusCallback focusCallback=new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
                Toast.makeText(CustomCam.this,"focused",Toast.LENGTH_SHORT).show();
        }
    };
    Camera.AutoFocusCallback focusCallbackTakePic=new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mCamera.takePicture(null, null, mPicture);
            //System.gc();
        }
    };
    
    /*private  File getOutputMediaFile() {
        File mediaStorageDir = new File(
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                getCacheDir(),
                "Yara");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        //fileImage=mediaFile;
        return mediaFile;
    }*/
    
    /*private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            Log.i("cam_size_preview_given",size.width+"  "+size.height);
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }*/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mCamera.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }
}