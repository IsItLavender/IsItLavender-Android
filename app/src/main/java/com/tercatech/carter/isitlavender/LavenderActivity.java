package com.tercatech.carter.isitlavender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class LavenderActivity extends Activity implements SurfaceHolder.Callback {

    private Camera mCamera;
    private boolean previewing;
    private SurfaceHolder cameraViewHolder;
    private ProgressBar progress;
    private TextView status;
    private byte[] currentImage;
    private Context context;

    static String[] statuses = new String[]{
            "Retrieving image",
            "Computing network size",
            "Analyzing image density",
            "Calculating network weights",
            "Generating hidden layers",
            "Optimizing image",
            "Recomputing network layout",
            "Analyzing image"
    };

    int retryCount;
    final static int RETRY_LEVEL_CHANGE = 5;
    static String[] endMessagesLevel1 = new String[] {
            "Maybe Lavender",
            "< 90% Probability Not Lavender",
            "Maybe Lavender",
            "< 90% Probability Not Lavender",
            "Maybe Lavender",
            "Low Image Quality",
            "Unable To Identify"
    };
    static String[] endMessagesLevel2 = new String[] {
            "Please Retake Image",
            "Wormy",
            "Please Retake Image",
            "Image Analysis Not Possible",
            "Please Retake Image",
            "Image Analysis Not Possible",
            "Image Corrupted"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("Lavender", "Creating");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_lavender);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_preview);

        cameraViewHolder = cameraView.getHolder();
        cameraViewHolder.addCallback(this);

        cameraViewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setProgress(0);
        progress.setMax(statuses.length-1);

        status = (TextView) findViewById(R.id.statusMessage);
        status.setText("");

        retryCount = 0;

        context = this;

        Log.e("Lavender", "Created");
    }

    Handler statusHandler = new Handler();
    Handler progressHandler = new Handler();
    Handler analysisHandler = new Handler();

    public void analysisLoop(){
        new Thread(){
            public void run(){

                for(int i = 0; i < statuses.length; i++){

                    final int progressProgress = i;

                    progressHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setProgress(progressProgress);
                        }
                    });

                    final String statusMessage = statuses[i];

                    statusHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            status.setText(statusMessage + "\n" + Double.toHexString(Math.random() % 1000000 + 1000));
                        }
                    });

                    try{
                        Thread.sleep(300 + 100*retryCount + (long) (Math.random()*200), 0);
                    } catch(Exception e){
                        Log.e("Lavender", "Someone set an alarm");
                    }
                }

                statusHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        status.setText("");
                    }
                });

                analysisHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        endAnalysis();
                    }
                });
            }
        }.start();
    }

    public void shareImage(){
        new AlertDialog.Builder(this)
                .setTitle("Share Image")
                .setMessage("Share your image")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        StoreByteImage(context, currentImage, 50, "lavender_image");
                    }
                })
                .setPositiveButton("Upload", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface d, int w){
                    }
                })
                .setIcon(android.R.drawable.btn_star)
                .show();
    }


    public void endAnalysis(){

        String endMessage = "";
        if(retryCount < RETRY_LEVEL_CHANGE){
            endMessage = endMessagesLevel1[(int)(Math.random()*endMessagesLevel1.length)];
        }
        else{
            endMessage = endMessagesLevel2[(int)(Math.random()*endMessagesLevel2.length)];
        }

        new AlertDialog.Builder(this)
                .setTitle("Results")
                .setMessage(endMessage)
                .setNegativeButton("Reanalyze", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        retryCount++;
                        analysisLoop();
                    }
                })
                .setNeutralButton("Share", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        shareImage();
                    }
                })
                .setPositiveButton("Take Another Photo", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        retryCount = 0;
                        endAnalyzeImage();
                    }
                })
                .setIcon(android.R.drawable.btn_star)
                .show();
    }

    public void analyzeImage(View v){
        mCamera.takePicture(null, mPictureCallback, mPictureCallback);
        previewing = false;
        analysisLoop();
    }

    public void endAnalyzeImage(){

        mCamera.startPreview();
        previewing = true;
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera c) {
            if (imageData != null) {
                currentImage = imageData;
                mCamera.startPreview();
            }
        }
    };

    public void StoreByteImage(Context context, byte[] imageData, int quality, String expName){
        File imageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/lavender");
        Log.e("StoreByteImage", imageDirectory.toString());
        try {
            imageDirectory.mkdir();
            FileOutputStream outStream = new FileOutputStream(imageDirectory.toString() +"/lavender" + String.valueOf(System.currentTimeMillis() + ".jpg"));

            BitmapFactory.Options imgOptions = new BitmapFactory.Options();
            imgOptions.inSampleSize = 5;

            Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, imgOptions);

            BufferedOutputStream bufferedOutStream = new BufferedOutputStream(outStream);

            image.compress(Bitmap.CompressFormat.JPEG, quality, bufferedOutStream);

            bufferedOutStream.flush();
            bufferedOutStream.close();
        }
        catch(FileNotFoundException e){
            alertError("Could not write to directory");
            e.printStackTrace();
        }
        catch(IOException e){
            alertError("Could not write");
            e.printStackTrace();
        }

        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Image saved to " + imageDirectory.toString())
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.btn_star)
                .show();
    }

    private void alertError(String errorMessage){
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.btn_star)
                .show();
    }

    private void setCameraParameters(){
        Camera.Parameters p = mCamera.getParameters();

        Camera.Size cameraSize = p.getSupportedPreviewSizes().get(0);
        p.setPreviewSize(cameraSize.width, cameraSize.height);

        mCamera.setParameters(p);

    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("Lavender", "surfaceCreated");
        mCamera = Camera.open();
        setCameraParameters();

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e("Lavender", "Could not set camera display");
            e.printStackTrace();
        }

        mCamera.startPreview();
        previewing = true;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.e("Lavender", "surfaceChanged");

        // XXX stopPreview() will crash if preview is not running
        if (previewing) {
            mCamera.stopPreview();
        }

        setCameraParameters();

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e("Lavender", "Could not set camera display");
            e.printStackTrace();
        }

        mCamera.startPreview();
        previewing = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("Lavender", "surfaceDestroyed");

        mCamera.stopPreview();
        previewing = false;
        mCamera.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "Credits");
        menu.add(Menu.NONE, 1, 0, "Exit");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case 0:
                new AlertDialog.Builder(this)
                        .setTitle("Credits")
                        .setMessage("Idea by Lee Holtzman and friends\nCode by Carter")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {}
                        })
                        .setIcon(android.R.drawable.btn_star)
                        .show();
                return true;
            case 1:
                finish();
                return true;
        }
        return false;

    }
}
