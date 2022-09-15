package com.ly.permission;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.ActivityUtils;

import java.util.List;


/***
 * Created by fangm on 2019-11-18 14:29
 *
 **/
public class PermissionAnywhere {
    private static PermissionFragment permissionFragment;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestPermission(final Activity context, final String[] permissions, PermissionCallback permissionCallback) {
        if (permissionFragment == null) {
            permissionFragment = new PermissionFragment();
        }
        permissionFragment.setOnAttachCallback(new FragmentAttachCallback() {
            @Override
            public void onAttach() {
                permissionFragment.requestPermission(permissions);
            }
        });
        permissionFragment.setOnPermissionCallback(permissionCallback);
        FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
        fragmentTransaction.add(permissionFragment, "permissionFragment@777").commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestPermission(final String[] permissions, PermissionCallback permissionCallback) throws Exception {
        Activity context = ActivityUtils.getTopActivity();
        if (context == null) {
            throw new NullPointerException("Top Activity is Null!");
        }
        requestPermission(context, permissions, permissionCallback);
    }

}
