package uni.swt.gesturedetectionwithpepper;

import android.graphics.Bitmap;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.charset.Charset;

// This is to remove the background of the real image and transform that to black in color
public class BackgroundRemover {

    private Bitmap sourceBitmap;
    private Mat background;
    private boolean isCalibrated = false;
    private Mat sourceMat;

    public BackgroundRemover(Mat sourceMat){
        //just declaring empty skin color detection constructor.
        this.sourceMat = sourceMat;
        background  = new Mat();

    }

    public void calibrate(Mat sourceMat){
        Imgproc.cvtColor(sourceMat,background,Imgproc.COLOR_RGB2HSV);
        isCalibrated = true;
    }
    public Mat getForeground(Mat sourceMat)
    {

        Mat foregroundMask = getForegroundMask();
        Mat foreground = new Mat();
        sourceMat.copyTo(foreground, foregroundMask);
        return foreground;

    }
    private Mat getForegroundMask()
    {
        Mat foregroundMask = new Mat();
        if(!isCalibrated) {
            foregroundMask = Mat.zeros(sourceMat.rows(), sourceMat.cols(), CvType.CV_8UC1);
            return foregroundMask;
        }
        Imgproc.cvtColor(sourceMat,foregroundMask,Imgproc.COLOR_RGB2HSV);
        removeBackground(foregroundMask, background);
        return foregroundMask;
    }
    private void removeBackground(Mat foregroundMask , Mat background ) {
        int thresholdOffset = 10;
        for (int i = 0; i < sourceMat.rows(); i++) {
            for (int j = 0; j < sourceMat.cols(); j++) { //at < Charset > (i, j);
                double framePixel = sourceMat.get(i,j)[0];
                double bgPixel = background.get(i,j)[0];
                //Check the logic here: might have some issue here while putting the value back to the 'Mat' check this : https://github.com/opencv/opencv/blob/master/samples/java/tutorial_code/core/mat_mask_operations/MatMaskOperations.java
                double[] sourceMatPixel =  sourceMat.get(i,j);
                if (framePixel >= bgPixel - thresholdOffset && framePixel <= bgPixel + thresholdOffset){

                   sourceMat.put(i,j,0);
                }
			else {
			       sourceMat.put(i,j,255);
                }
            }
        }
    }

}
