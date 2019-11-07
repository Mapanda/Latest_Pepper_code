package uni.gesturedetectiononpepper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorKNN;
import org.opencv.video.Video;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import uni.gesturedetectiononpepper.classifier.Classifier;
import uni.gesturedetectiononpepper.classifier.ImageClassifier;
import uni.gesturedetectiononpepper.utils.Constant;
import uni.gesturedetectiononpepper.utils.UtilsClassify;

public class MainActivity1 extends RobotActivity implements RobotLifecycleCallbacks, CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.
                    READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    static {
        OpenCVLoader.initDebug();
    }
    private String TAG = "Gesture Detection";
    private MediaPlayer playerStart;
    private MediaPlayer playerFlash;
    private  MediaPlayer playerSuccess;
    private QiContext qiContext;
    /**Started coding from here newly**/
    private Button backgroundExtractionButton;
    private Button gestureExtractionButton;
    Mat rgbaBilateralFrame;
    int smoothingFactor =5;
    double sigmaColor = 50.0 ;
    double sigmaSpace=100.0;
    int history =0;
    double bgThreshold=50.0;
    boolean isShadowDetected=false;
    BackgroundSubtractorKNN bgModel = Video.createBackgroundSubtractorKNN(history,bgThreshold,isShadowDetected);
    boolean isBackGroundCaptured = false;
    Mat backgroundSubtractionFrame=new Mat();
    boolean isGestureButtonClicked = false;
    boolean isGestureDetected = false;
    Classifier.Recognition bestRecognition;
    private ImageView gestureDetectedView;
    private Toast mToastBackground;
    private Toast mToastGesture;
    private Toast mToast;
    Context context;
    Say say;
    Mat rgba = new Mat();
    //Animation started here:
    // private Animate animate;
    /**ended here**/
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        public void onManagerConnected(int status) {
            System.out.println("Welcome to OpenCV " + Core.VERSION);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QiSDK.register(this, this);
        OpenCVLoader.initDebug();

        setContentView(R.layout.activity_main);
        //context = getApplicationContext();
        playerStart = MediaPlayer.create(this, R.raw.ready_sound);
        playerFlash = MediaPlayer.create(this, R.raw.automatic_camera);
        playerSuccess = MediaPlayer.create(this, R.raw.success_sound);
        playerStart.start();
        gestureDetectedView = (ImageView)findViewById(R.id.imageView);
        gestureDetectedView.setVisibility(ImageView.VISIBLE);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        backgroundExtractionButton = findViewById(R.id.Background);
        backgroundExtractionButton.setEnabled(true);
        gestureExtractionButton = findViewById(R.id.ROI);
        gestureExtractionButton.setEnabled(false);
      
    }

    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        say = SayBuilder.with(qiContext) // Create the builder with the context.
                .withText("All right!! Now click on '\'Capture Background' button to Start. ") // Set the text to say.
                .build(); // Build the say action.
        say.run();

    }

    public void onRobotFocusLost() {
        this.qiContext = null;
        Log.i("Capture", "Robot focus is lost.." );
    }
    public void onRobotFocusRefused(String reason) {
        Log.i(TAG,reason);
    }

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        Log.d("OpenCV", "OpenCV library found inside package. Using it!");
    }


    public void onCameraViewStopped() {

    }
    /**
     * This method is invoked when delivery of the frame needs to be done.
     * The returned values - is a modified frame which needs to be displayed on the screen.
     * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
     *
     * @param inputFrame
     */

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Imgproc.cvtColor(rgbaFrame,rgbaFrame,Imgproc.COLOR_BGRA2RGB);
        rgbaBilateralFrame = rgbaFrame.clone();
        Imgproc.bilateralFilter(rgbaFrame,rgbaBilateralFrame,smoothingFactor,sigmaColor,sigmaSpace);
        rgba= createRectangleOnFrame(rgbaBilateralFrame);
    
        backgroundExtractionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Masking : ", "Background capture button Click Started.. ");
                getBackgroundCaptured(rgbaFrame);
                //getBackgroundCaptured(rgbaBilateralFrame, isBackGroundCaptured);
                getSavedImage(backgroundSubtractionFrame, "backgroundCaptured.jpg");
                Log.i("Masking : ", "Background capture button Click Finished.. ");
                backgroundExtractionButton.setEnabled(false);
                gestureExtractionButton.setEnabled(true);
                String message = "";
                if (mToastBackground != null && mToastBackground.getView().isShown()) {
                    mToastBackground.show();
                    mToastBackground.cancel(); // Close the toast if it is already open
                    message = "Please capture a plain Background First. Capture Gesture will be enabled after the button click.";
                    int duration = Toast.LENGTH_LONG;
                    mToastBackground = Toast.makeText(getApplicationContext(), message, duration);
                    mToastBackground.show();
                }
            }
        });
        gestureExtractionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message="";
                message = "Please place a hand with a gesture 'L','Peace','Fist','Palm','Okay'";
                int duration = Toast.LENGTH_LONG;
                mToastGesture = Toast.makeText(getApplicationContext(), message, duration);
                mToastGesture.show();
                isGestureButtonClicked=true;
                Log.i("Masking : ", "Gesture capture button Click Started.. ");
                backgroundExtractionButton.setEnabled(true);
                gestureExtractionButton.setEnabled(false);
                getSavedImage(rgbaBilateralFrame, "gesturebutton.jpg");
                Log.i("Masking : ", "Gesture capture button Click Finished.. ");

            }
        });
        try {
            startThread();
            Thread.sleep(10);

        } catch (InterruptedException ex) {
        }
        return rgba;
    }
    private void startThread() {
        new Thread() {
            public void run() {
                String predictedGesture = "Gesture Predicted: ";
                if (isGestureButtonClicked) {
                    extractGesture(backgroundSubtractionFrame);
                    Bitmap maskedBitmap = matToBitmapConversion(rgbaBilateralFrame);
                    actionOnDetectedGesture("Peace");
                 }
            }
        }.start();

    }
    private void putText(Mat imgThreshold,String recognitionValue){
        int width = imgThreshold.width();
        int w_rect = width*3/4;
        int new_width = (width+w_rect)/3;
        Imgproc.putText(imgThreshold, recognitionValue, new Point(new_width+30 ,30), Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255,255,255),1);
    }
    private void getBackgroundCaptured(Mat inputFrame) {
        backgroundSubtractionFrame = clipImageOnROI(inputFrame);
    }

    private void extractGesture(Mat backgroundSubtractionFrame) {
        rgbaBilateralFrame = clipImageOnROI(rgbaBilateralFrame);
        if(!rgbaBilateralFrame.empty()) {
            removeBackgroundFromFrame(backgroundSubtractionFrame, rgbaBilateralFrame);
        }else{
            Log.i("Remove Background" ,"Frame is empty to predict the gesture. Try again");
        }
    }

    private void getSavedImage(Mat clippedMat,String name){
        Bitmap bitmap =matToBitmapConversion(clippedMat);
        try {

            savebitmap(bitmap,name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Bitmap matToBitmapConversion(Mat imageMat) {
        Bitmap bitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, bitmap);
        return bitmap;
    }
    //This creates a rectangle on the camera frame for the hand to get cropped.
    private Mat createRectangleOnFrame(Mat rgbaBilateralFrame){
        int w = rgbaBilateralFrame.width();
        int h = rgbaBilateralFrame.height();
        int w_rect = w*3/4;
        int h_rect = h*3/4;
        int new_width = (w+w_rect)/3;
        int new_height = (h+h_rect)/3;
        Imgproc.rectangle(rgbaBilateralFrame,  new Point(0, 0), new Point( new_width, new_height),new Scalar( 0, 255, 0 ), 5);
        return rgbaBilateralFrame;
    }
    private Mat removeBackgroundFromFrame(Mat backgroundSubtractionFrame,Mat rgbaBilateralFrame){
        Mat mRgb = new Mat();
        Mat fgMask = new Mat();
        Imgproc.GaussianBlur(rgbaBilateralFrame, backgroundSubtractionFrame, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT);
        Imgproc.cvtColor(rgbaBilateralFrame,mRgb,Imgproc.COLOR_RGBA2RGB);
        bgModel.apply(mRgb,fgMask);
        Imgproc.cvtColor(fgMask, rgbaBilateralFrame, Imgproc.COLOR_GRAY2RGBA);
        BackgroundErosion(fgMask);
        return rgbaBilateralFrame;

    }

    private void BackgroundErosion(Mat fgMask) {
        int erosion_size=5;
        final Point anchor = new Point(-1,-1);
        final int iteration=2;
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(2*erosion_size +1,2*erosion_size+1));
        //if doesn't work then change this to bilateral filtermat
        Imgproc.erode(fgMask,fgMask,kernel,anchor,iteration);
        Imgproc.dilate(fgMask,fgMask,kernel,anchor,iteration);
        final Size ksize = new Size(3,3);
        Mat skin = new Mat(fgMask.rows(),fgMask.cols(), CvType.CV_8U,new Scalar(3));
        Imgproc.GaussianBlur(fgMask,fgMask,ksize,0);
        Core.bitwise_and(fgMask,fgMask,skin);

    }

    private Mat clipImageOnROI(Mat rgbaBilateralFrame){
        Bitmap nextBitmap = matToBitmapConversion(rgbaBilateralFrame);
        Bitmap cutBitmap = clipImageMat(nextBitmap);
        rgbaBilateralFrame = bitmapToMatConversion(cutBitmap);
        return rgbaBilateralFrame;
    }

    private Mat bitmapToMatConversion(Bitmap imageBitmap){
        Mat convertedMat = new Mat();
        Bitmap bmp32 = imageBitmap.copy(Bitmap.Config.ARGB_8888,true);
        Utils.bitmapToMat(bmp32,convertedMat);
        return convertedMat;
    }

    private Bitmap clipImageMat(Bitmap origialBitmap) {
        int height = origialBitmap.getHeight();
        int width = origialBitmap.getWidth();
        int w_rect = width*3/4;
        int h_rect = height*3/4;
        int new_width = (width+w_rect)/3;
        int new_height = (height+h_rect)/3;
        //TODO; get the BOX height and width here and send it in the srcRect and dest Rect
        Bitmap cutBitmap = Bitmap.createBitmap(new_width,
                new_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cutBitmap);
        android.graphics.Rect desRect = new android.graphics.Rect(0, 0, new_width, new_height);
        android.graphics.Rect srcRect = new Rect(0, 0, new_width,new_height);
        canvas.drawBitmap(origialBitmap, srcRect, desRect, null);
        return cutBitmap;
    }
    public void onDestroy() {
        super.onDestroy();
        QiSDK.unregister(this, this);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    private  void savebitmap(Bitmap bmp , String name) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        verifyStoragePermissions(this);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + name);
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();

    }
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    public String classifyImage(Bitmap bitmap) {
        Classifier classifier =
                ImageClassifier.create(
                        getAssets(),
                        Constant.MODEL_FILE,
                        Constant.LABEL_FILE,
                        Constant.INPUT_SIZE,
                        Constant.IMAGE_MEAN,
                        Constant.IMAGE_STD,
                        Constant.INPUT_NAME,
                        Constant.OUTPUT_NAME);

        Bitmap resizedBitmap = UtilsClassify.getResizedBitmap(bitmap, Constant.INPUT_SIZE, Constant.INPUT_SIZE, false);
        Mat resizedMat = bitmapToMatConversion(resizedBitmap);
        runOnUiThread(() -> gestureDetectedView.setImageBitmap(bitmap));
        getSavedImage(resizedMat,"Classify.jpg");
        List<Classifier.Recognition> results = classifier.recognizeImage(resizedBitmap);
        bestRecognition = new Classifier.Recognition("None", "NoGesture", 0.0f);
        int count =0;
        for (Classifier.Recognition recognition :
                results) {
            count++;
            if (recognition.getConfidence() > bestRecognition.getConfidence())
                bestRecognition = recognition;

        }
        playerSuccess.start();
        //  displayGestureOnScreen();
        return bestRecognition.getTitle();
    }

    private void displayGestureOnScreen() {
        float confidence = bestRecognition.getConfidence() * 100;

        String message="";
        if(confidence < 99.0) {
            message = "Gesture is not properly detected.Try again!!";
        }
        else{
            if (mToast != null && mToast.getView().isShown())
                mToast.cancel(); // Close the toast if it is already open
            message = "Detected Gesture is :" + bestRecognition.getTitle() + "(" + confidence + ")";

            new AlertDialog.Builder(this)
                    .setTitle("Information")
                    .setMessage("Detected Gesture is :" + bestRecognition.getTitle()+"("+confidence+")%")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                        }
                    }).show();

        }
        int duration = Toast.LENGTH_LONG;
        mToast = Toast.makeText(this, message, duration);
        mToast.show();
    }

    private void actionOnDetectedGesture(String recognitionName){
        if(recognitionName.equalsIgnoreCase("L")){
            goToActionOnL(qiContext,new Vector3(0, 0.3, 0));
            Say sayName = SayBuilder.with(qiContext).withText("This Location is Rostock.").build();
            sayName.run();
            //goToActionOnL(qiContext,);
        }
        else if(recognitionName.equalsIgnoreCase("Peace")){
            String locationName = " university of Rostock";
            String locationDescription = "Department of Computer Science. Building AE22";
            Say sayName = SayBuilder.with(qiContext).withText("This Location is "+locationName).build();
            Say sayDescription = SayBuilder.with(qiContext)
                    .withText(locationDescription)
                    .build();
            sayName.run();
            sayDescription.run();

        }else if(recognitionName.equalsIgnoreCase("Palm")){
            Say sayName = SayBuilder.with(qiContext).withText("This Location is Rostock.").build();
            sayName.run();
        }else if(recognitionName.equalsIgnoreCase("Fist")){
            Say sayName = SayBuilder.with(qiContext).withText("This Location is Rostock.").build();
            sayName.run();
        }else if(recognitionName.equalsIgnoreCase("Okay")){
            Say sayName = SayBuilder.with(qiContext).withText("This Location is Rostock.").build();
            sayName.run();
        }else{
            Log.d(TAG,"No task has been assigned to me!!");
        }


    }

    private void goToActionOnL(QiContext qiContext, Vector3 vec){
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Actuation actuation = qiContext.getActuation();
                    Frame robotFrame = actuation.robotFrame();
                    Transform transform = TransformBuilder.create().fromTranslation(vec);
                    Mapping mapping = qiContext.getMapping();
                    FreeFrame targetFrame = mapping.makeFreeFrame();
                    // Pass a 0-timestamp to publish target relatively to the
                    // last known location of robotFrame.
                    targetFrame.update(robotFrame, transform, 0L);
                    GoTo goTo = GoToBuilder.with(qiContext)
                            .withFrame(targetFrame.frame())
                            .build();
                    goTo.run();
                    Log.d(TAG, "I moved!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

}
