package com.htetznaing.adbotg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.BinaryMessenger;
import java.util.List;
import java.util.Map;
import android.util.Log;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.net.Uri;

// Import MainActivity
import com.htetznaing.adbotg.MainActivity;

/**
 * Main activity for the Flutter application.
 * Handles method channels for various functionalities.
 */
public class FlutterMainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.htetznaing.adbotg/main_activity";
    private static final String USB_CHANNEL = "com.htetznaing.adbotg/usb_receiver";
    private static final String SPYWARE_CHANNEL = "samples.flutter.dev/spyware";
    private static final String APP_DETAILS_CHANNEL = "com.htetznaing.adbotg/app_details";
    private MethodChannel methodChannel;
    private ApplicationController appController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize AppContextHolder with application context
        AppContextHolder.setContext(getApplicationContext());
        
        appController = ApplicationController.getInstance();

        // Initialize method channels first
        methodChannel = new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), USB_CHANNEL);
        
        // Setup other method channels
        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(
                (call, result) -> {
                    switch (call.method) {
                        case "openMainActivity":
                            openMainActivity();
                            result.success(null);
                            break;
                        case "retryConnection":
                            retryConnection();
                            result.success(null);
                            break;
                        default:
                            result.notImplemented();
                    }
                }
            );

        AppDetailsChannelHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());
        SpywareChannelHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());
        PrivacySettingsHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());

        // Register USB broadcast receiver
        IntentFilter filter = new IntentFilter("com.htetznaing.adbotg.USB_STATUS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbStatusReceiver, filter);
        }

        // Initialize connection after channels are setup
        appController.initializeConnection();

        // Check initial connection status
        if (appController.isConnected()) {
            methodChannel.invokeMethod("usbConnected", null);
        }

        // Setup AppDetailsChannelHandler
        AppDetailsChannelHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());

        // Listen for USB connection status
        methodChannel = new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), USB_CHANNEL);

        // Setup spyware channel
        SpywareChannelHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());

        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), SPYWARE_CHANNEL)
            .setMethodCallHandler(
                (call, result) -> {
                    if ("getSpywareApps".equals(call.method)) {
                        List<List<String>> csvData = call.argument("csvData");
                        if (csvData != null) {
                            try {
                                List<Map<String, Object>> spywareApps = SpywareDetector.getDetectedSpywareApps(csvData, false);
                                result.success(spywareApps);
                            } catch (Exception e) {
                                result.error("SCAN_ERROR", "Error scanning for spyware apps", e.getMessage());
                            }
                        } else {
                            result.error("INVALID_ARGUMENT", "CSV data is required", null);
                        }
                    } else if ("getSpywareAppsFromTarget".equals(call.method)) {
                        List<List<String>> csvData = call.argument("csvData");
                        if (csvData != null) {
                            try {
                                List<Map<String, Object>> spywareApps = SpywareDetector.getDetectedSpywareApps(csvData, true);
                                result.success(spywareApps);
                            } catch (Exception e) {
                                result.error("SCAN_ERROR", "Error scanning target device", e.getMessage());
                            }
                        } else {
                            result.error("INVALID_ARGUMENT", "CSV data is required", null);
                        }
                    } else {
                        result.notImplemented();
                    }
                }
            );

        // Add the privacy settings handler
        PrivacySettingsHandler.setup(getFlutterEngine().getDartExecutor().getBinaryMessenger());
        Log.d("FlutterMainActivity", "PrivacySettingsHandler setup complete");

        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), "com.htetznaing.adbotg/privacy_settings")
            .setMethodCallHandler(
                (call, result) -> {
                    switch (call.method) {
                        case "openAppSettings":
                            String packageName = call.argument("package");
                            if (packageName != null) {
                                try {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + packageName));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    result.success(true);
                                } catch (Exception e) {
                                    result.error("SETTINGS_ERROR", "Could not open app settings", e.getMessage());
                                }
                            } else {
                                result.error("INVALID_PACKAGE", "Package name is null", null);
                            }
                            break;

                        case "getSocialMediaApps":
                            try {
                                Boolean fromTarget = call.argument("fromTarget");
                                if (fromTarget == null) fromTarget = false;
                                
                                List<Map<String, Object>> socialMediaApps = PrivacySettingsHandler.getInstance(this, SpywareDetector.getInstance())
                                    .getSocialMediaApps(fromTarget);
                                result.success(socialMediaApps);
                            } catch (Exception e) {
                                Log.e("FlutterMainActivity", "Error getting social media apps", e);
                                result.error("GET_APPS_ERROR", "Could not get social media apps", e.getMessage());
                            }
                            break;

                        default:
                            result.notImplemented();
                            break;
                    }
                }
            );
    }

    /**
     * Opens the main activity.
     */
    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void retryConnection() {
        // Request USB permission and attempt to establish connection
        appController.requestConnection();
    }

    /**
     * Broadcast receiver for USB connection status.
     */
    private final BroadcastReceiver usbStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = intent.getBooleanExtra("isConnected", false);
            if (isConnected) {
                methodChannel.invokeMethod("usbConnected", null);
            } else {
                methodChannel.invokeMethod("usbDisconnected", null);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbStatusReceiver);
        // Only close connection if app is actually being destroyed
        if (isFinishing()) {
            appController.closeConnection();
        }
    }
}
