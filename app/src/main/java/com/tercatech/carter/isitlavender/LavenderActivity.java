package com.tercatech.carter.isitlavender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
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

import org.apache.http.HttpConnection;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


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

    public void shareImage(final String message){
        new AlertDialog.Builder(this)
                .setTitle("Share Image")
                .setMessage("Share your image")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        StoreByteImage(context, currentImage, 50, message);
                    }
                })
                .setPositiveButton("Upload", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface d, int w){
                        uploadImage(StoreByteImage(context, currentImage, 50, message));
                    }
                })
                .setIcon(android.R.drawable.btn_star)
                .show();
    }


    public void endAnalysis(){

        final String endMessage;
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
                        shareImage(endMessage);
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

        mCamera.startPreview();
        previewing = true;
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

    public Bitmap overlayAnalysis(Bitmap image, String message){
        Bitmap overlayed  = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
        Canvas canvas = new Canvas(overlayed);

        Paint textPaint = new Paint();
        textPaint.setARGB(255, 0, 128, 255);
        textPaint.setTextSize(50);
        canvas.drawBitmap(image, 0, 0, null);
        canvas.drawText(message, image.getWidth()/2-25, image.getHeight()-25, textPaint);
        Log.v("overlaying", message);

        return overlayed;
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera c) {
            if (imageData != null) {
                currentImage = imageData;
            }
        }
    };

    public String StoreByteImage(Context context, byte[] imageData, int quality, String message){
        File imageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/lavender");
        String writePath = imageDirectory.toString() +"/lavender" + String.valueOf(System.currentTimeMillis()) + ".jpg";

        Log.v("StoreByteImage", imageDirectory.toString());
        try {
            imageDirectory.mkdir();
            FileOutputStream outStream = new FileOutputStream(writePath);

            BitmapFactory.Options imgOptions = new BitmapFactory.Options();
            imgOptions.inSampleSize = 5;

            Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, imgOptions);

            Bitmap overImage = overlayAnalysis(image, message);

            BufferedOutputStream bufferedOutStream = new BufferedOutputStream(outStream);

            overImage.compress(Bitmap.CompressFormat.JPEG, quality, bufferedOutStream);

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

        return writePath;
    }

    public void uploadImage(final String imagePath){
        new Thread() {
            public void run() {
                try{
                    File imageFile = new File(imagePath);

                    String filename = imageFile.getName();
                    Log.v("Upload", "Uploading " + imageFile.toString());

                    URL uploadURL = new URL("http://73.162.155.107/lavender/upload.php");

                    HttpURLConnection conn = (HttpURLConnection) uploadURL.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
                    conn.setRequestProperty("uploaded_file", filename);

                    DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
                    outStream.writeBytes("--*****\r\n");
                    outStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + filename + "\"\r\n");
                    outStream.writeBytes("\r\n");

                    FileInputStream inStream = new FileInputStream(imagePath);

                    int availableBytes = inStream.available();
                    int bufSize = Math.min(availableBytes, 1024 * 1024);
                    byte[] buffer = new byte[bufSize];

                    int bytesRead = inStream.read(buffer, 0, bufSize);

                    while(bytesRead > 0){
                        outStream.write(buffer, 0, bufSize);

                        availableBytes = inStream.available();
                        bufSize = Math.min(availableBytes, 1024 * 1024);
                        bytesRead = inStream.read(buffer, 0, bufSize);

                        final int tmpBytesRead = bytesRead;
                        statusHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                status.setText("Uploading " + tmpBytesRead);
                            }
                        });
                    }

                    outStream.writeBytes("\r\n");
                    outStream.writeBytes("--*****--\r\n");

                    int respCode = conn.getResponseCode();
                    String respMess = conn.getResponseMessage();
                    Log.v("Upload response", respMess + " " + respCode);
                    statusHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            status.setText("Upload complete");
                        }
                    });

                    inStream.close();
                    outStream.flush();
                    outStream.close();

                }
                catch(MalformedURLException e){
                    e.printStackTrace();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
        status.setText("Uploading");
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
