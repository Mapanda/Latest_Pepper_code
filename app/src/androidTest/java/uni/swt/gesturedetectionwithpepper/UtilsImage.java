package uni.swt.gesturedetectionwithpepper;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class UtilsImage {

    public static Bitmap getResizedBitmap(Bitmap bitmap , int newWidth , int newHeight , boolean recycle){

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float)newWidth/width ;
        float scaleHeight = (float)newHeight/height ;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth,scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap,0,0,width,height,matrix,false);

        if(recycle)
            bitmap.recycle();

        return resizedBitmap;

    }
}
