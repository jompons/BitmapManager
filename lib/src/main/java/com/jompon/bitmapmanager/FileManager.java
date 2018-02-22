package com.jompon.bitmapmanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by Jompon.
 */
public class FileManager {

    private static final String TAG = FileManager.class.getSimpleName();
    private static File rootExt = new File(Environment.getExternalStorageDirectory() + File.separator + "Lib" + File.separator + "BitmapManager" + File.separator);
    private static File rootInt = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Lib" + File.separator + "BitmapManager" + File.separator);
    protected static FileManager fileManager;
    protected Context context;
    private static File getRoot( )
    {
        File root = (isExternalStorageAvailable())? rootExt: rootInt;
        root.mkdirs();
        return root;
    }

    // Returns true if external storage for photos is available
    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    public static FileManager getInstance(Context context)
    {
        if( fileManager == null )      fileManager = new FileManager(context);
        return fileManager;
    }

    protected FileManager(Context context)
    {
        this.context = context;
    }

    private String getUniqueImageFilename( )
    {
        return "img_" + System.currentTimeMillis() + ".jpg";
    }

    public File getDestinationImageFilename( )
    {
        File root = FileManager.getRoot( );
        String imgName = getUniqueImageFilename();
        return new File(root, imgName);
    }

    public void setRootExt(String path)
    {
        rootExt = new File(path);
    }

    public void setRootInt(String path)
    {
        rootInt = new File(path);
    }

    public void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    public void scanMediaFile(File photo) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photo);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }
}
