package com.jompon.bitmapmanager.example;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.jompon.bitmapmanager.BitmapManager;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private BitmapManager bitmapManager;
    private Uri mPhotoURI;
    private Button btnPhoto;
    private ImageView imgPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPhoto = (Button) findViewById(R.id.btnPhoto);
        imgPhoto = (ImageView) findViewById(R.id.imgPhoto);
        bitmapManager = BitmapManager.getInstance(this);
        btnPhoto.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constant.REQUEST_CAMERA) {

                if( mPhotoURI == null && data.getExtras() != null )
                    mPhotoURI = data.getData();

                save(mPhotoURI);
                imgPhoto.setImageURI(mPhotoURI);
            }
            if (requestCode == Constant.REQUEST_SELECT_PICTURE) {
                if (data == null || (data.getData() == null && data.getClipData() == null)) {

                } else {
                    mPhotoURI = data.getData();   //reference to image selected path
                    try{
                        File sdImageMainDirectory = bitmapManager.getDestinationImageFilename();

                        //It cannot modify original image so need to copy file to new path
                        bitmapManager.copyFile(new File(bitmapManager.getRealPathFromUri(mPhotoURI)), sdImageMainDirectory);
                        mPhotoURI = Uri.fromFile(sdImageMainDirectory);   //reference to new image path

                        save(mPhotoURI);
                        imgPhoto.setImageURI(mPhotoURI);
                    }catch (IOException e){
                        Log.e(TAG, e.getMessage()+"");
                    }catch (SecurityException e){
                        Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnPhoto) {
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
            myAlertDialog.setTitle(R.string.dialog_title_picture_option);
            myAlertDialog.setItems(R.array.option_pick_picture, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try{
                        switch (which) {
                            case 0:
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ){
                                        mPhotoURI = Uri.fromFile(bitmapManager.getDestinationImageFilename());
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI);
                                        startActivityForResult(intent, Constant.REQUEST_CAMERA);
                                    }else{
                                        ContentValues values = new ContentValues(1);
                                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                                        mPhotoURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI);
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                        startActivityForResult(intent, Constant.REQUEST_CAMERA);
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), getString(R.string.alert_no_camera), Toast.LENGTH_LONG).show();
                                }
                                break;

                            case 1:
                                Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pictureActionIntent, Constant.REQUEST_SELECT_PICTURE);
                                break;
                        }
                    }catch (SecurityException e){
                        Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
                    }
                }
            });
            myAlertDialog.setCancelable(false);
            myAlertDialog.show();
        }
    }

    private void save(Uri uri){

        try{
            int minSize = 600;
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            double ratio = (double)bitmap.getWidth()/bitmap.getHeight();
            int width = (bitmap.getWidth() <= bitmap.getHeight())? minSize: (int)(minSize*ratio);
            int height = (bitmap.getWidth() <= bitmap.getHeight())? (int)(minSize/ratio): minSize;
            bitmapManager.storeImage(bitmapManager.rotateWithScale(bitmap, 0, width, height), uri);
            bitmapManager.scanMediaFile(new File(bitmapManager.getRealPathFromUri(uri)));
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
        }
    }
}
