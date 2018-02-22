package com.jompon.bitmapmanager.example;

import android.os.Environment;
import java.io.File;

public class Constant {

    public static final int REQUEST_CAMERA = 665;
    public static final int REQUEST_SELECT_PICTURE = 666;

    public static String rootExt = Environment.getExternalStorageDirectory() + File.separator + "Lib" + File.separator + "BitmapManagerExample" + File.separator;
    public static String rootInt = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Lib" + File.separator + "BitmapManagerExample" + File.separator;
}
