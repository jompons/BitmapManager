package com.jompon.bitmapmanager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Jompon.
 */
public class BitmapManager extends FileManager{

    private static final String TAG = BitmapManager.class.getSimpleName();
    public static BitmapManager getInstance(Context context)
    {
        if( fileManager == null )      fileManager = new BitmapManager(context);
        return (BitmapManager) fileManager;
    }

    private BitmapManager(Context context)
    {
        super(context);
    }

    public Bitmap resizeBitmap(Uri uri, int baseSampleSize, int width, int height) throws FileNotFoundException
    {
        // First we get the the dimensions of the file on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);

        int outHeight = options.outHeight;
        int outWidth = options.outWidth;
        int inSampleSize = baseSampleSize;

        if (outHeight > height || outWidth > width)
        {
            inSampleSize *= outWidth > outHeight
                    ? outHeight / height
                    : outWidth / width;
        }

        // Now we will load the image and have BitmapFactory resize it for us.
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap resizedBitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        if( bitmap != null && !bitmap.isRecycled() )
            bitmap.recycle();

        return resizedBitmap;
    }

    /**
     * Get bitmap with MediaStore class
     * @param uri of image file path
     * @return bitmap
     * @throws IOException if file not exist
     */
    public Bitmap getBitmap(Uri uri) throws IOException
    {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    /**
     * Get bitmap from uri
     * @param uri of image file path
     * @param maxSize of Pixel
     * @return bitmap
     */
    public Bitmap getBitmap(Uri uri, int maxSize) {

        InputStream in = null;
        Bitmap b = null;
        try {
            in = context.getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) > maxSize) {
                scale++;
            }
            //Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            in = context.getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                //Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(maxSize / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x, (int) y, true);
                if( !b.isRecycled() )
                    b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();
            //Log.d(TAG, "bitmap size - width: " +b.getWidth() + ", height: " + b.getHeight());
            return b;
        } catch (Exception e) {
            //Log.e(TAG, e.getMessage(), e);
            if( b != null )
                return b;
            return null;
        }
    }

    /**
     * check bitmap that rotated by exif
     * @param bitmap that need to check
     * @param uri of file
     * @return bitmap that real angle
     */
    public Bitmap getRealRotate(Bitmap bitmap, Uri uri)
    {
        int rotate = 0;
        try {
            String path = getRealPathFromUri(uri);
            File imageFile = new File(path);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //bitmap.recycle();
            return rotateBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * Get bitmap which define pixel size and get real rotate
     * @param uri of image file path
     * @param maxSize of Pixel
     * @return bitmap which reduce size and real rotate
     */
    public Bitmap getRealRotate(Uri uri, int maxSize)
    {
        Bitmap bitmap = getBitmap(uri, maxSize);
        return getRealRotate(bitmap, uri);
    }

    public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        } catch (Exception e){
            return contentUri.getPath();
        } finally{
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * save image by replaced to uri path
     * @param uri
     */
    public void storeImage(Uri uri, int quality) {

        try {
            Bitmap bitmap = getBitmap(uri);
            Bitmap image = getRealRotate(bitmap, uri);
            String path = getRealPathFromUri(uri);
            File pictureFile = new File(path);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            image.recycle();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * save image with bitmap by replaced to uri path
     * @param image
     * @param uri
     */
    public void storeImage(Bitmap image, Uri uri, int quality) {

        try {
            String path = getRealPathFromUri(uri);
            File pictureFile = new File(path);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            if( !image.isRecycled() ){
                image.recycle();
            }
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public Bitmap rotateWithScale(Bitmap source, float angle, int width, int height)
    {
        try{
            Bitmap image = getResizedBitmap(source, width, height);
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        }catch (OutOfMemoryError e){
            return source;
        }catch (Exception e){
            return source;
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
//        if (resizedBitmap != bm) {
//            if( !bm.isRecycled() )
//                bm.recycle();
//        }
        return resizedBitmap;
    }

    // Decodes image and scales it to reduce memory consumption
    public Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE=600;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale > REQUIRED_SIZE &&
                    o.outHeight / scale > REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }

    /**
     * reduces the size of the image
     * @param image
     * @param maxSize
     * @return
     */
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * reduces the size of the image
     * @param image
     * @param minSize
     * @return
     */
    public Bitmap getResizedBitmap2(Bitmap image, int minSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            height = minSize;
            width = (int) (height * bitmapRatio);
        } else {
            width = minSize;
            height = (int) (width / bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
