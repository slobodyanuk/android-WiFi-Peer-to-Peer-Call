package com.android.wificall.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by mpodolsky on 23.05.2016.
 */
public class PermissionsUtil {

    public static boolean needReadWritePermissions(Context context) {
        int permissionCheck1 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean needRecordAudioPermissions(Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        return permissionCheck != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestReadWritePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                Globals.REQUEST_READWRITE_STORAGE);
    }

    public static void requestRecordAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                Globals.REQUEST_RECORD_AUDIO);
    }

}
