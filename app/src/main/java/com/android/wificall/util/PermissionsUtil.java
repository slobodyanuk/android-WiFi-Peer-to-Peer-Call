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

    public static boolean needRecordAudioPermissions(Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        return permissionCheck != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRecordAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                Globals.REQUEST_RECORD_AUDIO);
    }

}
