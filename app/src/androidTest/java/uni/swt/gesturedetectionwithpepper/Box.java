package uni.swt.gesturedetectionwithpepper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class Box extends View {
    private Paint paint = new Paint();
    Box(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);

        //center
        int x0 = canvas.getWidth()/2;
        int y0 = canvas.getHeight()/2;
        int dx = canvas.getHeight()/4;
        int dy = canvas.getHeight()/4;
        //draw guide box
        canvas.drawRect(x0-dx, y0-dy, x0+dx, y0+dy, paint);
        System.out.println("Size:"+x0);
        System.out.println("Size:"+y0);
        System.out.println("Size:"+dx);
        System.out.println("Size:"+dy);
    }
    //Programmatically photos for screenshots:
    //https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-on-android
}
