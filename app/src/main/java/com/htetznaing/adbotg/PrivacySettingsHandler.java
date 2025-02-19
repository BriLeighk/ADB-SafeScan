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

    // List of known social media and dual-use apps
    private static final Map<String, String[]> MONITORED_APPS = new HashMap<String, String[]>() {{
        // Social Media Apps
        put("com.instagram.android", new String[]{"Instagram", "Photo sharing, messaging"});
        put("com.snapchat.android", new String[]{"Snapchat", "Ephemeral messaging, location sharing"});
        put("com.whatsapp", new String[]{"WhatsApp", 
            "Popular messaging app that can be used to monitor communications",
            "Remove if not actively used",
            "Check app permissions",
            "Review privacy settings"});
        put("com.facebook.katana", new String[]{"Facebook", "Social networking, messaging"});
        put("com.facebook.orca", new String[]{"Messenger", "Messaging, location sharing"});
        put("com.zhiliaoapp.musically", new String[]{"TikTok", "Video sharing, messaging"});
        put("com.twitter.android", new String[]{"Twitter", "Social networking, messaging"});
        
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

    private static PrivacySettingsHandler instance;

    public static void setup(BinaryMessenger messenger) {
        if (instance == null) {
            instance = new PrivacySettingsHandler(
                ApplicationController.getAppContext(),
                SpywareDetector.getInstance()
            );
        }
        new MethodChannel(messenger, CHANNEL).setMethodCallHandler((call, result) -> {
            handleMethodCall(call, result);
        });
    }

    public PrivacySettingsHandler(Context context, SpywareDetector detector) {
        this.context = context;
        this.spywareDetector = detector;
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
        List<Map<String, Object>> apps = new ArrayList<>();
        
        if (fromTarget) {
            if (spywareDetector == null || !spywareDetector.isConnected()) {
                Log.e(TAG, "ADB not connected for target device");
                return apps;
            }

            try {
                List<String> detectedPackages = executeCommand("/system/bin/pm list packages");
                
                for (String packageName : detectedPackages) {
                    if (MONITORED_APPS.containsKey(packageName)) {
                        try {
                            Map<String, Object> appData = new HashMap<>();
                            appData.put("packageName", packageName);
                            appData.put("name", MONITORED_APPS.get(packageName)[0]);
                            appData.put("description", getAppDescription(packageName));
                            // Use a placeholder icon string that's valid base64
                            appData.put("icon", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
                            appData.put("recommendations", Arrays.asList(MONITORED_APPS.get(packageName)));
                            
                            apps.add(appData);
                            Log.d(TAG, "Successfully added target app: " + packageName);
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting target app info for " + packageName, e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting target apps", e);
            }
        } else {
            // Source device code
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            Set<String> installedPackages = new HashSet<>();
            for (ApplicationInfo app : installedApps) {
                installedPackages.add(app.packageName);
            }

            for (Map.Entry<String, String[]> entry : MONITORED_APPS.entrySet()) {
                String packageName = entry.getKey();
                if (!installedPackages.contains(packageName)) {
                    Log.d(TAG, "Skipping " + packageName + " - not installed");
                    continue;
                }

                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    Map<String, Object> appData = new HashMap<>();
                    appData.put("packageName", packageName);
                    appData.put("name", entry.getValue()[0]); // Get first element of array
                    appData.put("description", getAppDescription(packageName));
                    appData.put("icon", getAppIconBase64(appInfo, pm));
                    appData.put("recommendations", Arrays.asList(entry.getValue())); // Convert array to List
                    apps.add(appData);
                    Log.d(TAG, "Successfully added source app: " + packageName);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting source app info for " + packageName, e);
                }
            }
        }
        
        return apps;
    }

    private List<String> executeCommand(String command) {
        List<String> detectedPackages = new ArrayList<>();
        final StringBuilder[] outputHolder = new StringBuilder[]{ new StringBuilder() };
        Set<String> processedPackages = new HashSet<>();
        AtomicBoolean isComplete = new AtomicBoolean(false);
        
        Thread commandThread = new Thread(() -> {
            AdbStream stream = null;
            try {
                // Open a single stream for the command
                stream = spywareDetector.getAdbConnection().open("shell:" + command);
                
                // Read data until the stream is closed or we have all packages
                while (!stream.isClosed()) {
                    byte[] data = stream.read(); // This blocks until data is available
                    if (data == null) {
                        // End of stream reached
                        Log.d(TAG, "End of stream reached");
                        break;
                    }

                    // Append new data to output buffer
                    outputHolder[0].append(new String(data, StandardCharsets.US_ASCII));
                    
                    // Process complete lines
                    String[] lines = outputHolder[0].toString().split("\n");
                    int lastCompleteLine = lines.length - 1;
                    
                    // Don't process the last line if it doesn't end with newline
                    // (it might be incomplete)
                    if (!outputHolder[0].toString().endsWith("\n")) {
                        lastCompleteLine--;
                    }
                    
                    // Process complete lines
                    for (int i = 0; i <= lastCompleteLine; i++) {
                        String line = lines[i].trim();
                        if (line.startsWith("package:")) {
                            String packageName = line.replace("package:", "").trim();
                            if (!packageName.isEmpty() && !processedPackages.contains(packageName)) {
                                detectedPackages.add(packageName);
                                processedPackages.add(packageName);
                            }
                        }
                    }
                    
                    // Keep any incomplete line for next iteration
                    if (lastCompleteLine < lines.length - 1) {
                        outputHolder[0] = new StringBuilder(lines[lines.length - 1]);
                    } else {
                        outputHolder[0] = new StringBuilder();
                    }

                    // Check if we've reached the end of the package list
                    // This assumes the last line after packages will be a shell prompt
                    String lastLine = lines[lines.length - 1].trim();
                    if (!lastLine.isEmpty() && !lastLine.contains("package:") && 
                        (lastLine.endsWith("$") || lastLine.endsWith("#"))) {
                        Log.d(TAG, "Found shell prompt, command complete");
                        break;
                    }
                }
                
                isComplete.set(true);
                
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error in command execution", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }
        });

        commandThread.start();
        
        try {
            // Wait for the command thread with timeout
            commandThread.join(30000);
            if (commandThread.isAlive()) {
                commandThread.interrupt();
                Log.e(TAG, "Command thread timed out");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Command thread interrupted");
            commandThread.interrupt();
        }

        // Add debug log
        Log.d(TAG, "Detected packages: " + detectedPackages.toString());

        return detectedPackages;
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
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error converting app icon to base64", e);
            return "";
        }
    }

    private String getAppDescription(String packageName) {
        String[] appInfo = MONITORED_APPS.get(packageName);
        return appInfo != null ? appInfo[1] : "Social media application";
    }

    private List<String> getPrivacyRecommendations(String packageName) {
        // Base recommendations for all apps
        List<String> recommendations = new ArrayList<>(Arrays.asList(
            "Review app permissions regularly",
            "Enable two-factor authentication if available",
            "Check privacy settings after each app update",
            "Be cautious when sharing location data"
        ));

        // Add app-specific recommendations
        if (packageName.equals("com.instagram.android")) {
            recommendations.addAll(Arrays.asList(
                "Set account to private",
                "Disable activity status",
                "Review tagged photos before they appear",
                "Limit story visibility"
            ));
        } else if (packageName.equals("com.snapchat.android")) {
            recommendations.addAll(Arrays.asList(
                "Enable Ghost Mode for location",
                "Set story visibility to 'Friends Only'",
                "Review who can contact you",
                "Disable Quick Add"
            ));
        }
        // Add more app-specific recommendations as needed

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

    private String getDefaultAppIconBase64() {
        try {
            // Create a simple default icon - a colored square with app initials
            Bitmap defaultIcon = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(defaultIcon);
            
            // Fill with a default color
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            canvas.drawRect(0, 0, 144, 144, paint);

            // Convert to Base64
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            defaultIcon.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            byte[] byteArray = byteStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error creating default icon", e);
            return "";
        }
    }
} 