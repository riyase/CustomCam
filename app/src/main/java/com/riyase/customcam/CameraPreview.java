package com.riyase.customcam;

import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
{
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private CustomCam mCustomCam;
    // Constructor that obtains context and camera
    @SuppressWarnings("deprecation")
    public CameraPreview(CustomCam customCam, Camera camera) {
        super(customCam);
        mCustomCam=customCam;
        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);

        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public SurfaceHolder getSurfaceHolder(){
        return this.mSurfaceHolder;
    }
    public void destroy()
    {
        mSurfaceHolder.removeCallback(this);
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            mCamera.setParameters(mCustomCam.getParams(mCustomCam.getCurrentCamID()));
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) 
        {
            // left blank for now
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.getHolder().removeCallback(this);
        //mCamera.stopPreview();
        //mCamera.release();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
                               int width, int height) {
        // start preview with new settings
        try {
            mCamera.setParameters(mCustomCam.getParams(mCustomCam.getCurrentCamID()));
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        }catch (IOException ioe)
        {
            ioe.printStackTrace();
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

}