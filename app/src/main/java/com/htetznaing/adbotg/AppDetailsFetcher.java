package com.htetznaing.adbotg;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches details about applications such as name, icon, and permissions.
 */
public class AppDetailsFetcher {
    /**
     * Get the details of an app given its package name.
     * @param packageName The package name of the app.
     * @return A map containing app details like name, icon, and package name.
     */
    public static Map<String, String> getAppDetails(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }

        try {
            PackageManager packageManager = AppContextHolder.getContext().getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            String iconBase64 = getBase64IconFromDrawable(packageManager.getApplicationIcon(packageName));
            Map<String, String> appDetails = new HashMap<>();
            appDetails.put("name", appName);
            appDetails.put("icon", iconBase64);
            appDetails.put("package", packageName);
            return appDetails;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Convert a drawable to a base64 encoded string.
     * @param drawable The drawable to convert.
     * @return The base64 encoded string of the drawable.
     */
    public static String getBase64IconFromDrawable(Drawable drawable) {
        Bitmap bitmap = (drawable instanceof BitmapDrawable) ? 
            ((BitmapDrawable) drawable).getBitmap() : null;
            
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(), 
                drawable.getIntrinsicHeight(), 
                Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    /**
     * Get the permissions of an app given its package name.
     * @param packageName The package name of the app.
     * @return A list of maps containing permission details.
     */
    public static List<Map<String, String>> getAppPermissions(String packageName) {
        Map<String, String> permissionGroups = new HashMap<>();
        permissionGroups.put("android.permission.ACCESS_FINE_LOCATION", "location");
        permissionGroups.put("android.permission.ACCESS_COARSE_LOCATION", "location");
        permissionGroups.put("android.permission.ACCESS_BACKGROUND_LOCATION", "location");
        permissionGroups.put("android.permission.CAMERA", "camera");
        permissionGroups.put("android.permission.RECORD_AUDIO", "microphone");
        permissionGroups.put("android.permission.READ_EXTERNAL_STORAGE", "storage");
        permissionGroups.put("android.permission.WRITE_EXTERNAL_STORAGE", "storage");
        permissionGroups.put("android.permission.MANAGE_EXTERNAL_STORAGE", "storage");
        permissionGroups.put("android.permission.READ_MEDIA_IMAGES", "storage");
        permissionGroups.put("android.permission.READ_MEDIA_VIDEO", "storage");

        List<Map<String, String>> grantedPermissions = new ArrayList<>();
        try {
            PackageInfo packageInfo = AppContextHolder.getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = packageInfo.requestedPermissions;
            int[] requestedPermissionsFlags = packageInfo.requestedPermissionsFlags;

            if (requestedPermissions != null) {
                for (int i = 0; i < requestedPermissions.length; i++) {
                    String permission = requestedPermissions[i];
                    String group = permissionGroups.get(permission);
                    if (group != null && (requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        String iconName = getPermissionIconName(permission);
                        Map<String, String> permissionInfo = new HashMap<>();
                        permissionInfo.put("permission", permission);
                        permissionInfo.put("icon", iconName);
                        grantedPermissions.add(permissionInfo);
                    }
                }
            }
        } catch (Exception e) {
            //Log.e("PermissionsError", "Error fetching permissions for " + packageName, e);
        }

        return grantedPermissions;
    }

    /**
     * Get the icon name for a given permission.
     * @param permission The permission string.
     * @return The icon name associated with the permission.
     */
    private static String getPermissionIconName(String permission) {
        switch (permission) {
            case "android.permission.ACCESS_FINE_LOCATION":
            case "android.permission.ACCESS_COARSE_LOCATION":
            case "android.permission.ACCESS_BACKGROUND_LOCATION":
            case "android.permission.ACCESS_MEDIA_LOCATION":
                return "location";
            case "android.permission.CAMERA":
                return "camera";
            case "android.permission.RECORD_AUDIO":
                return "microphone";
            case "android.permission.READ_EXTERNAL_STORAGE":
            case "android.permission.WRITE_EXTERNAL_STORAGE":
            case "android.permission.MANAGE_EXTERNAL_STORAGE":
            case "android.permission.READ_MEDIA_IMAGES":
            case "android.permission.READ_MEDIA_VIDEO":
                return "storage";
            default:
                return "unknown";
        }
    }
}
