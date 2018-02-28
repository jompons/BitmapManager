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

package com.jompon.bitmapmanager.example;

import android.os.Environment;
import java.io.File;

public class Constant {

    public static final int REQUEST_CAMERA = 665;
    public static final int REQUEST_SELECT_PICTURE = 666;

    public static String rootExt = Environment.getExternalStorageDirectory() + File.separator + "Lib" + File.separator + "BitmapManagerExample" + File.separator;
    public static String rootInt = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Lib" + File.separator + "BitmapManagerExample" + File.separator;
}
