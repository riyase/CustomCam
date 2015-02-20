package com.riyase.customcam;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener {

    ImageView ivOut;
    Button bLaunch;

    public static final int ACTIVITY_NEXT=100;


    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupViews();
    }
    
    private void setupViews(){

        ivOut=(ImageView)findViewById(R.id.ivOut);
        bLaunch=(Button)findViewById(R.id.bLaunch);
        bLaunch.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bLaunch:
                Intent intentCam=new Intent(this,CustomCam.class);
                intentCam.putExtra(CustomCam.CAMERA_FACING, Camera.CameraInfo.CAMERA_FACING_FRONT);
                startActivityForResult(intentCam, ACTIVITY_NEXT);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
       /* if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            ciPhoto.setImageBitmap(photo);
        }*/
        if(resultCode==RESULT_OK)
        {
            //System.gc();
           /* Toast.makeText(this,"Result_ok",Toast.LENGTH_SHORT).show();
            ivOut.post(new Runnable() {
                @Override
                public void run() {*/
                    //ivOut.setImageURI(Crop.getOutput(data));

               /* }
            });*/

            File file=new File(Crop.getOutput(data).getPath());//(File)data.getSerializableExtra(Key.FILE);
            final Bitmap bmp= BitmapFactory.decodeFile(file.getAbsolutePath());
            if(bmp!=null)
                ivOut.setImageBitmap(bmp);
        }
    }

}
