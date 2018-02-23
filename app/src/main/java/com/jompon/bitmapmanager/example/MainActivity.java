package com.jompon.bitmapmanager.example;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.jompon.bitmapmanager.BitmapManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private BitmapManager bitmapManager;
    private Uri mPhotoURI;
    private Button btnPhoto;
    private ImageView imgPhoto;
    private FloatingActionButton fab;
    private float rotate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(((Toolbar)findViewById(R.id.toolbar)));

        btnPhoto = (Button) findViewById(R.id.btnPhoto);
        imgPhoto = (ImageView) findViewById(R.id.imgPhoto);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        bitmapManager = BitmapManager.getInstance(this);
        bitmapManager.setRootExt(Constant.rootExt);
        bitmapManager.setRootInt(Constant.rootInt);
        btnPhoto.setOnClickListener(this);
        fab.setOnClickListener(this);
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

                if( data != null && data.getData() != null ) {
                    mPhotoURI = data.getData();   //reference to image selected path
                    try{
                        File sdImageMainDirectory = bitmapManager.getDestinationImageFilename();

                        //It cannot modify original image so need to copy file to new path
                        bitmapManager.copyFile(new File(bitmapManager.getRealPathFromUri(mPhotoURI)), sdImageMainDirectory);
                        mPhotoURI = Uri.fromFile(sdImageMainDirectory);   //reference to new image path

                        save(mPhotoURI);
                        imgPhoto.setImageURI(mPhotoURI);
                    }catch (Exception e){
                        if( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){
                            showPermissionAlertDialog(getString(R.string.dialog_warning_permission_write_external));
                        }else{
                            Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        if( v == btnPhoto ){
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
                    }catch (Exception e){
                        if( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){
                            showPermissionAlertDialog(getString(R.string.dialog_warning_permission_write_external));
                        }else{
                            Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
            myAlertDialog.setCancelable(false);
            myAlertDialog.show();
        }

        if( v == fab ){

            float rotate = (this.rotate+90)%360;
            imgPhoto.setRotation(rotate);
            this.rotate = rotate;
        }
    }

    private void save(Uri uri){

        try{
            int minSize = 600;
            Bitmap oriBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Bitmap bitmap = bitmapManager.getRealRotate(oriBitmap, uri);
            double ratio = (double)bitmap.getWidth()/bitmap.getHeight();
            int width = (bitmap.getWidth() <= bitmap.getHeight())? minSize: (int)(minSize*ratio);
            int height = (bitmap.getWidth() <= bitmap.getHeight())? (int)(minSize/ratio): minSize;
            bitmapManager.storeImage(bitmapManager.rotateWithScale(bitmap, rotate, width, height), uri, 100);
            bitmapManager.scanMediaFile(new File(bitmapManager.getRealPathFromUri(uri)));
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage()+"", Toast.LENGTH_LONG).show();
        }
    }

    private void showPermissionAlertDialog(String message)
    {
        AlertDialog.Builder abPermission = new AlertDialog.Builder(this);
        abPermission.setIcon(android.R.drawable.ic_dialog_alert);
        abPermission.setTitle(getString(R.string.dialog_warning));
        abPermission.setMessage(message);
        abPermission.setCancelable(false);
        abPermission.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivity(myAppSettings);
                dialog.dismiss();
            }
        });
        AlertDialog dialog = abPermission.create();
        dialog.show();
    }
}
