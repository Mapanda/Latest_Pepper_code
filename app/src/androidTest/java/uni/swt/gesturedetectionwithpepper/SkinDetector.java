package uni.swt.gesturedetectionwithpepper;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.max;
import static org.opencv.core.Core.mean;
import static org.opencv.core.Core.min;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.morphologyEx;

public class SkinDetector {
    //This class has the functionality of detecting the human skin color from an image.

    private double hLowThreshold = 0.0;
    private double hHighThreshold  = 0.0;
    private double sLowThreshold  = 0.0;
    private double sHighThreshold  = 0.0;
    private double vLowThreshold  = 0.0;
    private double vHighThreshold  = 0.0;
    private boolean isCalibrated = false;
    private Bitmap sourceBitmap;
    Rect skinColorSamplerRectangle1;
    Rect skinColorSamplerRectangle2;
    Mat sourceMat;


    public SkinDetector(Mat sourceMat){
        //just declaring empty skin color detection constructor.
        this.sourceMat = sourceMat;
    }

    public void drawSkinColorSampler(Mat sourceMat){
        // input is a Mat instead of Bitmap: change it later

        int frameWidth = sourceBitmap.getWidth();
        int frameHeight = sourceBitmap.getHeight();
        int rectangleSize = 30;
        Scalar rectangleColor = new Scalar(255,0,255);
        skinColorSamplerRectangle1 = new Rect(frameWidth / 5, frameHeight / 2, rectangleSize, rectangleSize);
        skinColorSamplerRectangle2 = new Rect(frameWidth / 5, frameHeight / 3, rectangleSize, rectangleSize);
        // function rectangle has to be mentioned here:
        // draw rectangle: Imgproc.rectangle(SrcMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Detect_Color, 5);
        Imgproc.rectangle(sourceMat,skinColorSamplerRectangle1,rectangleColor);
        Imgproc.rectangle(sourceMat,skinColorSamplerRectangle2,rectangleColor);
    }


    public void calibrate(Mat sourceMat){
        Mat hsvInput = new Mat();
        Imgproc.cvtColor(sourceMat,hsvInput,Imgproc.COLOR_RGB2HSV);
        Mat sample1 = new Mat(hsvInput, skinColorSamplerRectangle1);
        Mat sample2 = new Mat(hsvInput, skinColorSamplerRectangle2);
        calculateThresholds(sample1, sample2);

        isCalibrated = true;
    }
    private void calculateThresholds(Mat sample1, Mat sample2){
        int offsetLowThreshold = 80;
        int offsetHighThreshold = 30;
        Scalar hsvMeansSample1 = mean(sample1);
        Scalar hsvMeansSample2 = mean(sample2);
        hLowThreshold = min(hsvMeansSample1.val[0], hsvMeansSample2.val[0]) - offsetLowThreshold;
        hHighThreshold = max(hsvMeansSample1.val[0], hsvMeansSample2.val[0]) + offsetHighThreshold;

        sLowThreshold = min(hsvMeansSample1.val[1], hsvMeansSample2.val[1]) - offsetLowThreshold;
        sHighThreshold = max(hsvMeansSample1.val[1], hsvMeansSample2.val[1]) + offsetHighThreshold;

        // the V channel shouldn't be used. By ignorint it, shadows on the hand wouldn't interfire with segmentation.
        // Unfortunately there's a bug somewhere and not using the V channel causes some problem. This shouldn't be too hard to fix.
        vLowThreshold = min(hsvMeansSample1.val[2], hsvMeansSample2.val[2]) - offsetLowThreshold;
        vHighThreshold = max(hsvMeansSample1.val[2], hsvMeansSample2.val[2]) + offsetHighThreshold;
        //vLowThreshold = 0;
        //vHighThreshold = 255;
    }

    // this is to find the skin Mask for the gesture detection:
    public Mat getSkinMask(Mat sourceMat){
        Mat skinMask = new Mat();
        if(!isCalibrated) {
            skinMask = Mat.zeros(sourceMat.rows(), sourceMat.cols(), CvType.CV_8UC1);
            return skinMask;
        }
       Mat hsvInput = new Mat();
       Imgproc.cvtColor(sourceMat,hsvInput,Imgproc.COLOR_RGB2HSV);
       Core.inRange(hsvInput , new Scalar(hLowThreshold, sLowThreshold, vLowThreshold), new Scalar(hHighThreshold, sHighThreshold, vHighThreshold),skinMask);
       performOpening(skinMask, MORPH_ELLIPSE,  new Size(3, 3));
       Imgproc.dilate(skinMask, skinMask, new Mat(),new Point(-1, -1), 3);
       return skinMask;

    }
    private void performOpening(Mat binaryImage, int kernelShape, Size kernelSize){
        Mat structuringElement = getStructuringElement(kernelShape, kernelSize);
        morphologyEx(binaryImage, binaryImage, MORPH_OPEN, structuringElement);
    }
    // calculation of min and max values for the skin color:
    private double min(double value1 , double value2){
        double minValue = 0.0;
        if(value1 <= value2){
            minValue = value1;
        }else{
            minValue = value2;
        }

        return minValue;
    }
    private double max(double value1 , double value2){
        double maxValue = 0.0;
        if(value1 >= value2){
            maxValue = value1;
        }else{
            maxValue = value2;
        }

        return maxValue;
    }
}
