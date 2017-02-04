package com.example.sewemark.distanceassistance;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity  implements CameraBridgeViewBase.CvCameraViewListener2{

//    String car_cascade_name = "cars.xml";
//    CascadeClassifier car_cascade = new CascadeClassifier();
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private Mat mRgba;
    private Mat mGray;
    private ToneGenerator beep;
    private int frameCounter = 0;
    private Rect areaUnderConsideration;

    private int mAbsoluteFaceSize = 30;

    static{
        if(!OpenCVLoader.initDebug()){
            Log.d("MainActiviy", "OpenCv  NOT loaded");
        }
        else{
            Log.d("MainAcitiv","OpenCV loaded");
        }
    }
  /*  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainAcitiv","balbla");
    }*/
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("Activity", "OpenCV loaded successfully");
                    try{
                        //load cascade
                        InputStream is = getResources().openRawResource(R.raw.cars);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "cars.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while((bytesRead = is.read(buffer)) != -1){
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        mJavaDetector.load( mCascadeFile.getAbsolutePath());
                        if(mJavaDetector.empty()) {
                            Log.e("OCVSample::Activity","Failed to load classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i("OCVSample::Activity", "Loaded cascade classifier");
                        }

                        cascadeDir.delete();

                    } catch(IOException e) {
                        e.printStackTrace();
                        Log.e("OCVSample::Activity", "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    public void onResume()
    {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.d("OCVSample::Activity", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d("OCVSample::Activity", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    private CameraBridgeViewBase mOpenCvCameraView;
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i("acirvit", "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setMaxFrameSize(480,320);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        beep = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
        areaUnderConsideration = new Rect(120,0,240,320);
    }
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Imgproc.equalizeHist(mGray,mGray);
        mGray.convertTo(mGray,-1,1,30);

        MatOfRect faces = new MatOfRect();
        mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(300,300));
        Rect[] facesArray = faces.toArray();
        if(faces.empty() == true){
            frameCounter = 0;
            return mRgba;
        }
        for(int i = 0; i < facesArray.length; i++){
            if(facesArray[i].tl().inside(areaUnderConsideration) != true) return mRgba;
            frameCounter++;
            Imgproc.rectangle(mRgba, facesArray[i].tl(),facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            if(frameCounter >= 10 && (facesArray[i].width*facesArray[i].height >= 10000))  beep.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }
        return mRgba;
    }
}
