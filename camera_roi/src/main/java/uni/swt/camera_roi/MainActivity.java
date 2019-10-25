
package uni.swt.cameraroi;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import uni.swt.camera_roi.R;

public class MainActivity extends AppCompatActivity {

    Button mCapture ;
    ImageView mView;
    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mView = findViewById(R.id.imageView);
       // mCapture = findViewById(R.id.button);
        mCapture.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                                if(checkSelfPermission(Manifest.permission.CAMERA) ==PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                                                {
                                                    String[] permission = {Manifest.permission.CAMERA , Manifest.permission.WRITE_EXTERNAL_STORAGE};
                                                    requestPermissions(permission , PERMISSION_CODE);

                                                }else{
                                                    openCamera();
                                                }

                                            }else{
                                                openCamera();
                                            }

                                        }
                                    }
        );
    }
    private void openCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case PERMISSION_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openCamera();
                }else{
                    Toast.makeText(this , "Permission Denied.." ,Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            mView.setImageURI(image_uri);
        }
    }
}