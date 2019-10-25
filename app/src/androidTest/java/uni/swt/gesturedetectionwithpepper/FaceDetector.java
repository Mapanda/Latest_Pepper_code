package uni.swt.gesturedetectionwithpepper;

import android.graphics.Bitmap;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.util.Vector;

// This is to detect the face and remove the face from the image:
public class FaceDetector  {

    private String faceClassifierFileName =  "../res/haarcascade_frontalface_alt.xml" ;
    private CascadeClassifier faceCascadeClassifier;
    private Bitmap sourceBitmap;
    Mat sourceMat;
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(255, 0, 0, 255);
    public FaceDetector(Mat sourceMat)
    {
       this.sourceMat = sourceMat;
       if(!faceCascadeClassifier.load(faceClassifierFileName)){
            throw new RuntimeException("can't load file " + faceClassifierFileName);
        }
        getFaceRect(sourceMat);
    }
    //conversion of the source image from bitmap to 'Mat'
    private Mat inputImageToMatConversion(){
        sourceMat = new Mat(sourceBitmap.getWidth(), sourceBitmap.getHeight(), CvType.CV_8UC3);
        return sourceMat;
    }

    public void removeFaces(Mat input , Mat output)
    {
        MatOfRect faces = new MatOfRect();
        Rect[] facesArray = faces.toArray();
        Mat frameGray = new Mat();
        Imgproc.cvtColor(input,frameGray,Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(frameGray,frameGray);
        faceCascadeClassifier.detectMultiScale(frameGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                new Size(120, 120));
        //copied from other source so check this logic while error:
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(output, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    }

    private Rect getFaceRect(Mat input){
        MatOfRect faceRectangles = new MatOfRect();
        Rect[] facesArray = faceRectangles.toArray();
        Mat inputGray = new Mat();
        Imgproc.cvtColor(inputGray,faceRectangles,Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(inputGray,inputGray);
        faceCascadeClassifier.detectMultiScale(inputGray, faceRectangles, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                new Size(120, 120));
        if (facesArray.length > 0)
            return facesArray[0];
        else
            return new Rect(0, 0, 1, 1);
    }
}
