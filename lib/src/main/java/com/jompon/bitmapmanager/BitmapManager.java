/*
 * Copyright (C) 2018 jompons.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jompon.bitmapmanager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.IntRange;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
     * Get bitmap from uri with MediaStore class.
     * @param uri of image file path
     * @return bitmap
     * @throws IOException if file not exist
     */
    public Bitmap load(Uri uri) throws IOException
    {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    /**
     * Get bitmap from uri according to size of pixel.
     * @param uri of image file path
     * @param maxSize of pixel
     * @return bitmap
     */
    public Bitmap load(Uri uri, int maxSize) {

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
            Log.e(TAG, e.getMessage(), e);
            return b;
        }
    }

    /**
     * Get bitmap from uri according to baseSampleSize and image size.
     * @param uri of image file path
     * @param baseSampleSize of miniaturize minimum
     * @param width of pixel size
     * @param height of pixel size
     * @return bitmap according to defined size but size is not exactly
     * because it calculate according by multiple
     * for example
     * define width = 1080 but bitmap width = 1200 -> new bitmap width = 1200 because inSampleSize = 1
     * define width = 1080 but bitmap width = 2200 -> new bitmap width = 1100 because inSampleSize = 2
     * define width = 1080 but bitmap width = 2160 -> new bitmap width = 1080 because inSampleSize = 2
     */
    public Bitmap load(Uri uri, int baseSampleSize, int width, int height)
    {
        Bitmap bitmap = null;
        try{
            // First we get the the dimensions of the file on disk
            BitmapFactory.Options options = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);

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

            return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        }catch (FileNotFoundException e){
            Log.e(TAG, e.getMessage(), e);
            return bitmap;
        }
    }

    /**
     * Get bitmap with real rotate by Exif angle.
     * @param uri of image file path
     * @return bitmap rotated
     * @throws IOException if file not exist
     */
    public Bitmap loadRealRotate(Uri uri) throws IOException
    {
        Bitmap bitmap = load(uri);
        return getRealRotate(bitmap, uri);
    }

    /**
     * Get bitmap with real rotate by Exif angle which according to define pixel size.
     * @param uri of image file path
     * @param maxSize of pixel
     * @return bitmap rotated which according to defined size
     */
    public Bitmap loadRealRotate(Uri uri, int maxSize)
    {
        Bitmap bitmap = load(uri, maxSize);
        return getRealRotate(bitmap, uri);
    }

    /**
     * Get bitmap with real rotate by Exif angle which according to define pixel size.
     * @param uri of image file path
     * @param baseSampleSize of miniaturize minimum
     * @param width of pixel size
     * @param height of pixel size
     * @return bitmap rotated which according to defined baseSampleSize and size
     */
    public Bitmap loadRealRotate(Uri uri, int baseSampleSize, int width, int height)
    {
        Bitmap bitmap = load(uri, baseSampleSize, width, height);
        return getRealRotate(bitmap, uri);
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
            String path = getRealPath(uri);
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
     * save image jpeg according to define quality to uri file path.
     * @param uri source
     * @param quality of image
     */
    public void save(Uri uri, @IntRange(from=0, to=100) int quality) throws Exception{

        Bitmap bitmap = load(uri);
        Bitmap image = getRealRotate(bitmap, uri);
        String path = getRealPath(uri);
        File pictureFile = new File(path);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        if( !image.compress(Bitmap.CompressFormat.JPEG, quality, fos) ){
            fos.flush();
            fos.close();
            throw new Exception("Cannot Save");
        }
        fos.flush();
        fos.close();
    }

    /**
     * save image jpeg according to define quality and bitmap to uri file path.
     * @param uri source
     * @param quality of image
     * @param image source of bitmap type
     */
    public void save(Uri uri, @IntRange(from=0, to=100) int quality, Bitmap image) throws Exception{

        String path = getRealPath(uri);
        File pictureFile = new File(path);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        if( !image.compress(Bitmap.CompressFormat.JPEG, quality, fos) ){
            fos.flush();
            fos.close();
            throw new Exception("Cannot Save");
        }
        fos.flush();
        fos.close();
    }

    /**
     * save image according to define quality and compressFormat to uri file path.
     * @param uri source
     * @param quality of image
     * @param compressFormat type
     */
    public void save(Uri uri, @IntRange(from=0, to=100) int quality, Bitmap.CompressFormat compressFormat) throws Exception{

        Bitmap bitmap = load(uri);
        Bitmap image = getRealRotate(bitmap, uri);
        String path = getRealPath(uri);
        File pictureFile = new File(path);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        if( !image.compress(compressFormat, quality, fos) ){
            fos.flush();
            fos.close();
            throw new Exception("Cannot Save");
        }
        fos.flush();
        fos.close();
    }

    /**
     * save image according to define quality, bitmap and compressFormat to uri file path.
     * @param uri source
     * @param quality of image
     * @param image source of bitmap type
     * @param compressFormat type
     */
    public void save(Uri uri, @IntRange(from=0, to=100) int quality, Bitmap image, Bitmap.CompressFormat compressFormat) throws Exception{

        String path = getRealPath(uri);
        File pictureFile = new File(path);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        if( !image.compress(compressFormat, quality, fos) ){
            fos.flush();
            fos.close();
            throw new Exception("Cannot Save");
        }
        fos.flush();
        fos.close();
    }

    /**
     * Scale size of bitmap according to define
     * @param bitmap source
     * @param newWidth according to define
     * @param newHeight according to define
     * @return bitmap according to defined scale sized
     */
    public Bitmap matrixResize(Bitmap bitmap, int newWidth, int newHeight)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * Scale size and angle of bitmap according to define
     * @param bitmap source
     * @param newWidth according to define
     * @param newHeight according to define
     * @param angle according to degree
     * @return bitmap according to defined angle and scale sized
     */
    public Bitmap matrixResize(Bitmap bitmap, int newWidth, int newHeight, float angle)
    {
        Bitmap image = matrixResize(bitmap, newWidth, newHeight);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * set size of one side according to define and another size is defined by ratio
     * @param bitmap source
     * @param maxSize of one side pixel
     * @return bitmap according to defined size
     */
    public Bitmap createScaledBitmapMaxSize(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * set size of one side according to define and another size is defined by ratio
     * @param bitmap source
     * @param minSize of one side pixel
     * @return bitmap according to defined sized
     */
    public Bitmap createScaledBitmapMinSize(Bitmap bitmap, int minSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            height = minSize;
            width = (int) (height * bitmapRatio);
        } else {
            width = minSize;
            height = (int) (width / bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * Get real path of uri
     * @param uri source
     * @return real path of uri
     */
    public String getRealPath(Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        } catch (Exception e){
            return uri.getPath();
        } finally{
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
