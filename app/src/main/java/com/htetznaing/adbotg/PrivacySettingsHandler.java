package com.htetznaing.adbotg;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.util.Base64;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.BinaryMessenger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import java.io.ByteArrayOutputStream;
import io.flutter.plugin.common.MethodCall;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.graphics.Paint;
import android.graphics.Color;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.Thread;
import java.lang.StringBuilder;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrivacySettingsHandler {
    private static final String CHANNEL = "com.htetznaing.adbotg/privacy_settings";
    private static final String TAG = "PrivacySettingsHandler";
    private final SpywareDetector spywareDetector;
    private final Context context;
    private static PrivacySettingsHandler instance;

    // List of known social media and dual-use apps
    private static final Map<String, String[]> MONITORED_APPS = new HashMap<String, String[]>() {{
        // Social Media Apps
        put("com.instagram.android", new String[]{"Instagram", "Photo and video sharing social network"});
        put("com.snapchat.android", new String[]{"Snapchat", "Multimedia messaging app"});
        put("com.whatsapp", new String[]{"WhatsApp", "Messaging and voice/video call app"});
        put("com.facebook.katana", new String[]{"Facebook", "Social networking platform"});
        put("com.facebook.orca", new String[]{"Messenger", "Messaging, location sharing"});
        put("com.zhiliaoapp.musically", new String[]{"TikTok", "Short-form video sharing platform"});
        put("com.twitter.android", new String[]{"Twitter", "Social networking and microblogging service"});
        
        // Cloud Storage (potential data exfiltration)
        put("com.dropbox.android", new String[]{"Dropbox", "File storage, sharing"});
        put("com.google.android.apps.docs", new String[]{"Google Drive", "File storage, sharing"});
        put("com.onedrive.android", new String[]{"OneDrive", "File storage, sharing"});
        
        // Communication Apps
        put("com.google.android.gm", new String[]{"Gmail", "Email, contact sync"});
        put("com.microsoft.office.outlook", new String[]{"Outlook", "Email, calendar sharing"});
        put("com.telegram.messenger", new String[]{"Telegram", "Messaging, file sharing"});
        put("com.viber.voip", new String[]{"Viber", "Messaging, location sharing"});
        
        // Location Sharing Apps
        put("com.google.android.apps.maps", new String[]{"Google Maps", 
            "Location tracking and history",
            "Review location permissions",
            "Check location history settings",
            "Manage location sharing"});
        put("com.life360.android.safetymapd", new String[]{"Life360", "Location tracking, sharing"});
    }};

    public static PrivacySettingsHandler getInstance(Context context, SpywareDetector detector) {
        if (instance == null) {
            instance = new PrivacySettingsHandler(context, detector);
        }
        return instance;
    }

    private PrivacySettingsHandler(Context context, SpywareDetector detector) {
        this.context = context;
        this.spywareDetector = detector;
    }

    public static void setup(BinaryMessenger messenger) {
        new MethodChannel(messenger, CHANNEL);
    }

    private static void handleMethodCall(MethodCall call, MethodChannel.Result result) {
        if (instance == null) {
            result.error("not_initialized", "PrivacySettingsHandler not initialized", null);
            return;
        }

        switch (call.method) {
            case "getSocialMediaApps":
                boolean fromTarget = call.argument("fromTarget");
                result.success(instance.getSocialMediaApps(fromTarget));
                break;
            case "openGooglePrivacySettings":
                openGooglePrivacySettings(result);
                break;
            case "openAppSettings":
                String packageName = call.argument("package");
                openAppSettings(packageName, result);
                break;
            case "getInstalledSocialMediaApps":
                getInstalledSocialMediaApps(result);
                break;
            default:
                result.notImplemented();
        }
    }

    private static void getInstalledSocialMediaApps(MethodChannel.Result result) {
        try {
            PackageManager pm = AppContextHolder.getContext().getPackageManager();
            List<Map<String, Object>> installedApps = new ArrayList<>();
            
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            Log.d(TAG, "Found " + packages.size() + " installed applications");

            for (ApplicationInfo appInfo : packages) {
                String packageName = appInfo.packageName;
                Log.d(TAG, "Checking package: " + packageName);
                
                if (MONITORED_APPS.containsKey(packageName)) {
                    Log.d(TAG, "Found monitored app: " + packageName);
                    try {
                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            Log.d(TAG, "Skipping system app: " + packageName);
                            continue;
                        }

                        HashMap<String, Object> appData = new HashMap<>();
                        String[] appDetails = MONITORED_APPS.get(packageName);
                        
                        // Ensure all values are String or byte[]
                        appData.put("name", String.valueOf(appDetails[0]));
                        appData.put("packageName", String.valueOf(packageName));
                        appData.put("description", String.valueOf(appDetails[1]));
                        
                        try {
                            Log.d(TAG, "Getting icon for: " + packageName);
                            Drawable icon = pm.getApplicationIcon(packageName);
                            String base64Icon = instance.getAppIconBase64(appInfo, pm);
                            byte[] iconBytes = Base64.decode(base64Icon, Base64.NO_WRAP);
                            appData.put("icon", iconBytes);
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting icon for " + packageName, e);
                            continue;
                        }
                        
                        installedApps.add(appData);
                        Log.d(TAG, "Successfully added app: " + appDetails[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing app: " + packageName, e);
                        e.printStackTrace();
                    }
                }
            }
            
            Log.d(TAG, "Found " + installedApps.size() + " monitored apps");
            if (installedApps.isEmpty()) {
                Log.w(TAG, "No monitored apps found!");
            }
            result.success(installedApps);
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching installed apps", e);
            e.printStackTrace();
            result.error("APP_LIST_ERROR", "Could not fetch installed apps: " + e.getMessage(), null);
        }
    }

    private static void openGooglePrivacySettings(MethodChannel.Result result) {
        try {
            if (instance == null || instance.context == null) {
                result.error("SETTINGS_ERROR", "Context not initialized", null);
                return;
            }

            Intent intent = new Intent(Settings.ACTION_PRIVACY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            instance.context.startActivity(intent);
            result.success(null);
        } catch (Exception e) {
            Log.e(TAG, "Error opening privacy settings: " + e.getMessage(), e);
            result.error("SETTINGS_ERROR", "Could not open privacy settings", null);
        }
    }

    private static void openAppSettings(String packageName, MethodChannel.Result result) {
        try {
            if (instance == null || instance.context == null) {
                result.error("SETTINGS_ERROR", "Context not initialized", null);
                return;
            }

            if (packageName == null || packageName.isEmpty()) {
                result.error("SETTINGS_ERROR", "Invalid package name", null);
                return;
            }

            PackageManager pm = instance.context.getPackageManager();
            
            // Verify the app is installed before trying to open settings
            try {
                pm.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "App not installed: " + packageName);
                result.error("SETTINGS_ERROR", "App is not installed", null);
                return;
            }

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            instance.context.startActivity(intent);
            result.success(null);
        } catch (Exception e) {
            Log.e(TAG, "Error opening app settings: " + e.getMessage(), e);
            result.error("SETTINGS_ERROR", "Could not open app settings", null);
        }
    }

    public List<Map<String, Object>> getSocialMediaApps(boolean fromTarget) {
        List<Map<String, Object>> socialMediaApps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        if (fromTarget) {
            // Handle target device apps
            try {
                List<String> targetApps = spywareDetector.fetchAppsFromTargetDevice(context);
                for (String packageName : targetApps) {
                    if (MONITORED_APPS.containsKey(packageName)) {
                        Map<String, Object> appInfo = new HashMap<>();
                        String[] appData = MONITORED_APPS.get(packageName);
                        appInfo.put("name", appData[0]);
                        appInfo.put("packageName", packageName);
                        appInfo.put("description", appData[1]);
                        appInfo.put("icon", getDefaultAppIconBase64());
                        appInfo.put("recommendations", getPrivacyRecommendations(packageName));
                        appInfo.put("permissions", new ArrayList<>()); // Empty list for target device
                        socialMediaApps.add(appInfo);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching target device apps", e);
            }
        } else {
            // Handle source device apps
            try {
                List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo appInfo : installedApps) {
                    String packageName = appInfo.packageName;
                    if (MONITORED_APPS.containsKey(packageName)) {
                        Map<String, Object> app = new HashMap<>();
                        app.put("name", pm.getApplicationLabel(appInfo).toString());
                        app.put("packageName", packageName);
                        app.put("description", MONITORED_APPS.get(packageName)[1]);
                        app.put("icon", getAppIconBase64(appInfo, pm));
                        app.put("recommendations", getPrivacyRecommendations(packageName));
                        
                        // Get permissions using AppDetailsFetcher
                        List<Map<String, String>> permissions = AppDetailsFetcher.getAppPermissions(packageName);
                        app.put("permissions", permissions);
                        
                        socialMediaApps.add(app);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching source device apps", e);
            }
        }

        return socialMediaApps;
    }

    private String getAppIconBase64(ApplicationInfo appInfo, PackageManager pm) {
        try {
            Drawable drawable = pm.getApplicationIcon(appInfo);
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return getDefaultAppIconBase64();
        }
    }

    private String getDefaultAppIconBase64() {
        // Return a base64 string of a default icon
        // This is a placeholder - you should replace with an actual default icon
        return "";
    }

    private List<String> getPrivacyRecommendations(String packageName) {
        List<String> recommendations = new ArrayList<>();
        
        // Add base recommendations
        recommendations.add("Review app permissions regularly");
        recommendations.add("Enable two-factor authentication if available");
        recommendations.add("Check privacy settings after each app update");

        // Add app-specific recommendations
        switch (packageName) {
            case "com.instagram.android":
                recommendations.add("Set account to private");
                recommendations.add("Control who can message you");
                break;

            case "com.snapchat.android":
                recommendations.add("Enable Ghost Mode");
                recommendations.add("Set story visibility to 'Friends Only'");
                break;

            case "com.whatsapp":
                recommendations.add("Review who can see your profile");
                recommendations.add("Control 'Last Seen' visibility");
                break;

            case "com.facebook.katana":
                recommendations.add("Review tagged posts before they appear");
                recommendations.add("Set default post audience to 'Friends'");
                break;

            case "com.zhiliaoapp.musically":
                recommendations.add("Set account to private");
                recommendations.add("Disable 'Allow others to find me'");
                break;

            case "com.google.android.apps.maps":
                recommendations.add("Use incognito mode for sensitive navigation");
                break;
        }
        
        return recommendations;
    }

    // Add some common alternative package names for popular apps
    private static final Map<String, String[]> APP_ALTERNATIVES = new HashMap<String, String[]>() {{
        put("com.twitter.android", new String[]{"com.twitter.android.lite"});
        put("com.facebook.katana", new String[]{"com.facebook.lite"});
        put("com.whatsapp", new String[]{"com.whatsapp.w4b"});
        put("com.instagram.android", new String[]{"com.instagram.lite"});
    }};

    private String findInstalledVariant(String packageName, PackageManager pm) {
        try {
            // First try the original package name
            pm.getPackageInfo(packageName, 0);
            return packageName;
        } catch (PackageManager.NameNotFoundException e) {
            // Check alternatives if they exist
            String[] alternatives = APP_ALTERNATIVES.get(packageName);
            if (alternatives != null) {
                for (String alt : alternatives) {
                    try {
                        pm.getPackageInfo(alt, 0);
                        return alt;
                    } catch (PackageManager.NameNotFoundException ex) {
                        // Continue checking alternatives
                    }
                }
            }
        }
        return null;
    }
} 