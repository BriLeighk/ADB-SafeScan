package com.htetznaing.adbotg;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.content.Context;
import android.app.PendingIntent;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;
import com.cgutman.adblib.TcpChannel;
import com.cgutman.adblib.UsbChannel;
import com.cgutman.adblib.AdbBase64;

    public class SpywareDetector {

        // Variables
    private static final String TAG = "SpywareDetector";
        private UsbManager mManager; // USB manager
        private static AdbConnection adbConnection; // ADB connection
        private UsbDevice mDevice; // USB device
        private AdbCrypto adbCrypto; // ADB crypto
        private boolean isAdbConnected = false; // Flag to check if the ADB connection is established
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final Object adbLock = new Object(); // Lock for ADB operations
    private volatile boolean isScanning = false; // Flag to track scanning state
    private static final Object connectionLock = new Object();
    private static SpywareDetector instance;
    private Context applicationContext;  // Add this
    private static final String ACTION_USB_PERMISSION = "com.htetznaing.adbotg.USB_PERMISSION";

    // Singleton pattern to maintain connection state
    public static SpywareDetector getInstance() {
        if (instance == null) {
            synchronized (connectionLock) {
                if (instance == null) {
                    instance = new SpywareDetector();
                }
            }
        }
        return instance;
    }

    public void initialize(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

        /**
         * Get the detected spyware apps from the CSV data
         * @param csvData - The CSV data
         * @param isTargetDevice - Flag to check if the target device is the current device
         * @return The list of detected spyware apps
         **/
        public static List<Map<String, Object>> getDetectedSpywareApps(List<List<String>> csvData, boolean isTargetDevice) {
            Log.d("SpywareDetector", "Starting spyware app scan...");

        List<String> ids = new ArrayList<>();
        Map<String, String> types = new HashMap<>();
        List<String> installedApps;
        PackageManager packageManager = null;
        List<Map<String, Object>> detectedSpywareApps = new ArrayList<>();  // Initialize the list here

        // Parse CSV data
        for (int i = 1; i < csvData.size(); i++) {
                List<String> line = csvData.get(i);
                if (!line.isEmpty()) {
                    String appId = line.get(0).trim();
                ids.add(appId);
                    if (line.size() > 2) {
                    types.put(appId, line.get(2).trim());
                    }
                }
            }
            Log.d("SpywareDetector", "Successfully added all csv app ids to ids list."); 

        SpywareDetector detector = getInstance();

        if (isTargetDevice) {
            installedApps = detector.fetchAppsFromTargetDevice(detector.applicationContext);
                Log.d("SpywareDetector", "Installed Apps on Target:" + installedApps.toString());
        } else {
            packageManager = detector.applicationContext.getPackageManager();
            List<ApplicationInfo> appInfoList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                installedApps = new ArrayList<>(); 
                for (ApplicationInfo appInfo : appInfoList) {
                    installedApps.add(appInfo.packageName); 
                }
                Log.d("SpywareDetector", "Installed Apps on Source:" + installedApps.toString());
            }

            for (String appID : installedApps) {
                if (ids.contains(appID)) {
                    try { 
                        Map<String, String> appMetadata;
                        String iconBase64;
                    if (isTargetDevice) {
                        appMetadata = detector.fetchAppMetadataFromTarget(detector.applicationContext, appID, csvData);
                        Drawable placeholderIcon = detector.applicationContext.getDrawable(R.drawable.placeholder_icon);
                            iconBase64 = getBase64IconFromDrawable(placeholderIcon);
                    } else {
                            String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(appID, 0)).toString();
                            iconBase64 = getBase64IconFromDrawable(packageManager.getApplicationIcon(appID));
                            String installer = getInstallerPackageName(packageManager, appID);
                            
                            if (installer == null) {
                                installer = "Unknown Installer";
                            }

                            appMetadata = new HashMap<>();
                            appMetadata.put("name", appName);
                            appMetadata.put("installer", installer);
                        }

                        String storeLink = getStoreLink(appID, appMetadata.get("installer"));
                        String appType = types.getOrDefault(appID, "Unknown");
                        List<Map<String, String>> permissions = AppDetailsFetcher.getAppPermissions(appID);

                        Map<String, Object> appInfo = new HashMap<>();
                        appInfo.put("id", appID);
                        appInfo.put("name", appMetadata.get("name"));
                        appInfo.put("icon", iconBase64);
                        appInfo.put("installer", appMetadata.get("installer"));
                        appInfo.put("storeLink", storeLink);
                        appInfo.put("type", appType);
                        appInfo.put("permissions", permissions);

                        detectedSpywareApps.add(appInfo);
                    } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Package not found: " + appID, e);
                    }
                }
            }
            
            /*
            // Log the detected spyware apps (for debugging purposes)
            for (int i = 0; i < detectedSpywareApps.size(); i++) {
                Log.d("SpywareDetector", "Detected spyware apps: " + detectedSpywareApps.get(i));
            }
             
             */
            
            return detectedSpywareApps;
        }

        
        /**
         * Get the base64 encoded icon from the drawable
         * @param drawable - The drawable
         * @return The base64 encoded icon
         **/
        private static String getBase64IconFromDrawable(Drawable drawable) {
            Bitmap bitmap; // Bitmap to store the icon
            if (drawable instanceof BitmapDrawable) { // Check if the drawable is a BitmapDrawable
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if (bitmapDrawable.getBitmap() != null) { // Check if the bitmap is not null & set it to the bitmap
                    bitmap = bitmapDrawable.getBitmap(); 
                } else { // If the bitmap is null, create a new bitmap with the drawable's intrinsic width and height
                    bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                }
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            // Ensure the bitmap is mutable
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Create a canvas to draw the drawable on the bitmap
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);

            // Create a ByteArrayOutputStream to store the bitmap as a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Compress the bitmap as a PNG image & store it in the ByteArrayOutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            // Return the base64 encoded icon
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        }


        /**
         * Get the installer package name of the app
         * @param packageManager - The package manager
         * @param appID - The package ID of the app
         * @return The installer package name of the app
         **/
        private static String getInstallerPackageName(PackageManager packageManager, String appID) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Check if the device is running Android 12 or higher
                try {
                    return packageManager.getInstallSourceInfo(appID).getInstallingPackageName(); // Get the installer package name
                } catch (PackageManager.NameNotFoundException e) {
                    return "Unknown"; // Return "Unknown" if the installer package name is not found
                }
            } else { // If the device is running Android 11 or lower, get the installer package name using the deprecated method
                return packageManager.getInstallerPackageName(appID) != null ? packageManager.getInstallerPackageName(appID) : "Unknown";
            }
        }


        /**
         * Get link to the store where the app was dowloaded from 
         * @param packageName - App ID
         * @param installer - name of the installer app was downloaded from
         * @return The link to the 
         */
        private static String getStoreLink(String packageName, String installer) {
            if (installer == null) {
                return "Unknown Installer"; // or handle it as needed
            }
            
            switch (installer) {
                case "com.android.vending": // google play store
                    return "https://play.google.com/store/apps/details?id=" + packageName;
                case "com.amazon.venezia": // amazon store
                    return "https://www.amazon.com/gp/mas/dl/android?p=" + packageName;
                default: // unknown (unsecure download)
                    return "Unknown Installer"; 
            }
        }

    

        /**
         * Fetch the list of all installed app Ids on the target device
         * @param context - The context of the application
         * @return The list of installed app Ids
         **/
        public List<String> fetchAppsFromTargetDevice(Context context) {
        List<String> packageNames = new ArrayList<>();
        isScanning = true;
        AdbStream stream = null;
        
        try {
            if (!maintainConnection(context)) {
                Log.e(TAG, "Failed to establish/maintain ADB connection");
                return packageNames;
            }
    
            synchronized(adbLock) {
                try {
                    // Open shell stream
                    stream = adbConnection.open("shell:");
                    Log.d(TAG, "ADB shell stream opened successfully");
                    
                    // Get package list
                    String output = executeAdbCommand(stream, "/system/bin/pm list packages");
                    
                    // Parse package names from output
                    if (output != null && !output.isEmpty()) {
                        String[] lines = output.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.startsWith("package:")) {
                                String packageName = line.substring(8).trim(); // Remove "package:" prefix
                                packageNames.add(packageName);
                            }
                        }
                        Log.d(TAG, "Found " + packageNames.size() + " packages");
                    } else {
                        Log.e(TAG, "No output from pm list packages command");
                    }

                    // Send exit command to close shell
                    stream.write("exit\n".getBytes(StandardCharsets.UTF_8));
                    Thread.sleep(100);

                } finally {
                    // Always close the stream
                    if (stream != null) {
                        try {
                            stream.close();
                            Log.d(TAG, "ADB shell stream closed");
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing stream", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching apps", e);
        } finally {
            isScanning = false;
        }
        
        Log.d(TAG, "Returning " + packageNames.size() + " package names");
        return packageNames;
        }
    

        /**
         * Find the ADB interface on the device
         * @param device - The USB device
         * @return The ADB interface
         **/
        private UsbInterface findAdbInterface(UsbDevice device) {
            int count = device.getInterfaceCount(); // Get the number of interfaces on the device
            for (int i = 0; i < count; i++) { // Iterate through all the interfaces
                UsbInterface intf = device.getInterface(i); // Get the interface
                // Check if the interface is the ADB interface
                if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 && intf.getInterfaceProtocol() == 1) {
                    return intf; 
                }
            }
            return null;
        }
    

        /**
         * Set the ADB interface
         * @param device - The USB device
         * @param intf - The ADB interface
         * @return True if the interface is set successfully, false otherwise
         * @throws IOException
         * @throws InterruptedException
         **/
        private synchronized boolean setAdbInterface(UsbDevice device, UsbInterface intf) throws IOException, InterruptedException {
            // Close the existing ADB connection if it exists
            if (adbConnection != null) {
                adbConnection.close();
                adbConnection = null;
                mDevice = null;
            }
    
            // Set the ADB interface if the device and interface are not null
            if (device != null && intf != null) {
                UsbDeviceConnection connection = mManager.openDevice(device);
                if (connection != null) { // Check if the connection is not null
                    if (connection.claimInterface(intf, false)) { // Claim the interface
                        adbConnection = AdbConnection.create(new UsbChannel(connection, intf), adbCrypto); // Create the ADB connection
                        adbConnection.connect(); // Connect to the ADB server
                        mDevice = device; // Set the device
                        return true; // Return true if the interface is set successfully
                    } else { // Log an error if the interface is not claimed
                        connection.close();
                    }
                }
            }
            return false;
        }
    

        /**
         * Execute the ADB command and return the list of all installed app Ids on the target device
         * @param command - The ADB command to execute
         * @return The list of detected packages
         **/
        private List<String> executeCommand(String command) {
            List<String> detectedPackages = new ArrayList<>();
            final StringBuilder[] outputHolder = new StringBuilder[]{ new StringBuilder() };
            Set<String> processedPackages = new HashSet<>();
            AtomicBoolean isComplete = new AtomicBoolean(false);
            
            Thread commandThread = new Thread(() -> {
                AdbStream stream = null;
                try {
                    // Establish connection if needed
                    if (adbConnection == null || !isAdbConnected) {
                    if (!establishAdbConnection(applicationContext)) {
                            Log.e("SpywareDetector", "Failed to establish ADB connection");
                            return;
                        }
                    }

                    // Open a single stream for the command
                    stream = adbConnection.open("shell:" + command);
                    
                    // Read data until the stream is closed or we have all packages
                    while (!stream.isClosed()) {
                        byte[] data = stream.read(); // This blocks until data is available
                        if (data == null) {
                            // End of stream reached
                            Log.d("SpywareDetector", "End of stream reached");
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
                            Log.d("SpywareDetector", "Found shell prompt, command complete");
                            break;
                        }
                    }
                    
                    isComplete.set(true);
                    
                } catch (IOException | InterruptedException e) {
                    Log.e("SpywareDetector", "Error in command execution", e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.e("SpywareDetector", "Error closing stream", e);
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
                    Log.e("SpywareDetector", "Command thread timed out");
                }
            } catch (InterruptedException e) {
                Log.e("SpywareDetector", "Command thread interrupted");
                commandThread.interrupt();
            }

            return detectedPackages;
        }


        /**
         * Fetch the metadata of the app from the target device
         * @param context - The context of the application
         * @param packageName - The package name of the app
         * @return The metadata of the app
         **/                    
        private Map<String, String> fetchAppMetadataFromTarget(Context context, String packageName, List<List<String>> csvData) {
            Map<String, String> appMetadata = new HashMap<>();
            
            try {
                // Get app info from CSV first
                String[] csvInfo = getAppInfoFromCsv(packageName, csvData);
                String csvFlag = (csvInfo != null) ? csvInfo[1] : null;  // Get flag from CSV
                
                if (csvInfo != null) {
                    appMetadata.put("store", csvInfo[0]);
                    appMetadata.put("flag", csvFlag);
                    appMetadata.put("name", csvInfo[2]);
                }

                // Get installer info from a new stream
                AdbStream stream = null;
                try {
                    stream = adbConnection.open("shell:");
                    String installerCommand = "pm list packages -i " + packageName;
                    String installerOutput = executeAdbCommand(stream, installerCommand);
                    
                    // Parse installer info
                    String installer = "Unknown";
                    if (installerOutput != null && installerOutput.contains("installer=")) {
                        installer = installerOutput.substring(installerOutput.indexOf("installer=") + 10).trim();
                        if (installer.equals("null") || installer.isEmpty()) {
                            installer = "Unknown";
                        }
                    }
                    appMetadata.put("installer", installer);

                    // Determine color based on CSV flag only for now
                    String color = "red"; // Default color
                    if (csvFlag != null) {
                        switch (csvFlag) {
                            case "dual-use":
                                color = "blue";
                                break;
                            case "spyware":
                                color = "yellow";
                                break;
                        }
                    }
                    appMetadata.put("color", color);

                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing stream", e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching metadata for " + packageName, e);
                return getDefaultMetadata(packageName, csvData);
            }
            
            return appMetadata;
        }

    private String[] getAppInfoFromCsv(String packageName, List<List<String>> csvData) {
        if (csvData != null) {
            for (List<String> row : csvData) {
                if (row.size() >= 4 && row.get(0).equals(packageName)) {
                    return new String[]{
                        row.get(1),  // store
                        row.get(2),  // flag
                        row.get(3)   // title
                    };
                }
            }
        }
        return null;
    }

    // Add helper method to get default metadata
    private Map<String, String> getDefaultMetadata(String packageName, List<List<String>> csvData) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", getAppNameFromCsv(packageName, csvData));
        metadata.put("installer", "Unknown");
        return metadata;
    }

    // Add helper method to get app name from CSV
    private String getAppNameFromCsv(String packageName, List<List<String>> csvData) {
                for (List<String> row : csvData) {
            if (row.size() > 3 && row.get(0).equals(packageName)) {
                return row.get(3); // Return the title from CSV
            }
        }
        return packageName; // Fallback to package name if not found in CSV
    }

    // Update executeAdbCommand to handle InterruptedException
    private String executeAdbCommand(AdbStream stream, String command) throws IOException, InterruptedException {
        StringBuilder response = new StringBuilder();
        
        try {
            Log.d(TAG, "Executing ADB command: " + command);
            
            // Send command with explicit newline and exit
            stream.write((command + "\nexit\n").getBytes(StandardCharsets.UTF_8));
            
            // Read with timeout
            long startTime = System.currentTimeMillis();
            boolean foundPrompt = false;
            int timeoutMs = 10000;
            boolean isFirstChunk = true;
            int emptyReads = 0;
            
            while (System.currentTimeMillis() - startTime < timeoutMs && !foundPrompt) {
                byte[] data = stream.read();
                if (data == null || data.length == 0) {
                    Thread.sleep(100);
                    emptyReads++;
                    if (emptyReads > 50) { // 5 seconds of empty reads
                                break;
                            }
                    continue;
                }
                emptyReads = 0;
                
                String chunk = new String(data, StandardCharsets.UTF_8);
                Log.d(TAG, "Raw chunk: " + chunk);
                
                // Skip command echo and prompt in first chunk
                if (isFirstChunk) {
                    int promptIndex = chunk.indexOf("pnangn:/ $");
                    if (promptIndex >= 0) {
                        chunk = chunk.substring(promptIndex + "pnangn:/ $".length());
                    }
                    isFirstChunk = false;
                }
                
                // Skip shell prompt lines
                if (chunk.trim().equals("pnangn:/ $")) {
                    foundPrompt = true;
                    continue;
                }
                
                // Add the chunk to response if it contains package data
                if (chunk.contains("package:") || !command.contains("list packages")) {
                    response.append(chunk);
                }
                
                // Check if we've reached the shell prompt
                if (chunk.contains("\npnangn:/ $")) {
                    foundPrompt = true;
                }
            }
            
            // Clean up the response
            String result = response.toString()
                .replaceAll("\0", "")           // Remove null bytes
                .replaceAll("\r", "")           // Remove CR
                .replaceAll("pnangn:/ \\$.*$", "") // Remove shell prompt
                .replaceAll("^\\s*" + command + "\\s*", "") // Remove command echo
                .trim();                        // Remove extra whitespace
                
            Log.d(TAG, "Clean result: " + result);
            
            return result;
            
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error executing command: " + command, e);
            throw e;
        }
    }
    
        /**
         * Establish the ADB connection to the target device
         * @param context - The context of the application
         * @return True if the connection is established successfully, false otherwise
         **/
    private synchronized boolean establishAdbConnection(Context context) {
        try {
            if (adbConnection != null && isAdbConnected) {
                return true;
            }

            // Get USB manager
            mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (mManager == null) {
                Log.e(TAG, "Failed to get USB manager");
                return false;
            }

            // Find ADB device
            HashMap<String, UsbDevice> deviceList = mManager.getDeviceList();
            for (UsbDevice device : deviceList.values()) {
                if (isAdbDevice(device)) {
                    mDevice = device;
                    break;
                }
            }

            if (mDevice == null) {
                Log.e(TAG, "No ADB device found");
                return false;
            }

            // Check/request permission
            if (!mManager.hasPermission(mDevice)) {
                Log.d(TAG, "Requesting USB device permission");
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(ACTION_USB_PERMISSION), 
                    PendingIntent.FLAG_IMMUTABLE);
                mManager.requestPermission(mDevice, permissionIntent);
                return false;
            }

            // Open device connection
            UsbDeviceConnection connection = mManager.openDevice(mDevice);
            if (connection == null) {
                Log.e(TAG, "Failed to open USB device connection");
                return false;
            }

            // Find and claim ADB interface
            UsbInterface adbInterface = null;
            for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                UsbInterface intf = mDevice.getInterface(i);
                if (intf.getInterfaceClass() == 255 && 
                    intf.getInterfaceSubclass() == 66 &&
                    intf.getInterfaceProtocol() == 1) {
                    adbInterface = intf;
                    break;
                }
            }

            if (adbInterface == null) {
                Log.e(TAG, "ADB interface not found");
                connection.close();
                return false;
            }

            if (!connection.claimInterface(adbInterface, true)) {
                Log.e(TAG, "Failed to claim ADB interface");
                connection.close();
                return false;
            }

            // Initialize ADB connection
            UsbChannel usbChannel = new UsbChannel(connection, adbInterface);
            adbConnection = AdbConnection.create(usbChannel, getAdbCrypto(context));
            adbConnection.connect();

            Log.d(TAG, "ADB interface set successfully");
            isAdbConnected = true;
            return true;

                } catch (Exception e) {
            Log.e(TAG, "Error establishing ADB connection", e);
            isAdbConnected = false;
            adbConnection = null;
            return false;
        }
    }

    /**
     * Checks if the device is an ADB device by checking its interface properties
     */
    private static boolean isAdbDevice(UsbDevice device) {
        // Check if this is an ADB interface
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 255 && 
                intf.getInterfaceSubclass() == 66 &&
                intf.getInterfaceProtocol() == 1) {
                return true;
            }
        }
        return false;
    }

    // Add connection management methods
    public synchronized boolean maintainConnection(Context context) {
        synchronized(adbLock) {
            if (!isAdbConnected || adbConnection == null) {
                for (int retry = 0; retry < MAX_RETRIES; retry++) {
                    if (establishAdbConnection(context)) {
                        Log.d(TAG, "ADB connection established on retry " + retry);
                        return true;
                    }
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                Log.e(TAG, "Failed to establish ADB connection after " + MAX_RETRIES + " retries");
                return false;
            }
            return true;
        }
    }

    public void closeConnection() {
        synchronized(adbLock) {
            if (adbConnection != null) {
                try {
                    adbConnection.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing ADB connection", e);
                }
                adbConnection = null;
            }
            isAdbConnected = false;
        }
    }

    public boolean isConnected() {
        synchronized(adbLock) {
            return isAdbConnected && adbConnection != null;
        }
    }

    public AdbConnection getAdbConnection() {
        synchronized(adbLock) {
            return adbConnection;
        }
    }

    /**
     * Get or generate the ADB crypto keys
         * @param context - The context of the application
     * @return The ADB crypto keys
     */
    private AdbCrypto getAdbCrypto(Context context) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        if (adbCrypto == null) {
            File keyFile = new File(context.getFilesDir(), "adbkey");
            File pubKeyFile = new File(context.getFilesDir(), "adbkey.pub");

            if (keyFile.exists() && pubKeyFile.exists()) {
                // Load existing keys
                try {
                    adbCrypto = AdbCrypto.loadAdbKeyPair(new AdbBase64() {
                        @Override
                        public String encodeToString(byte[] data) {
                            return Base64.encodeToString(data, Base64.NO_WRAP);
                        }
                    }, keyFile, pubKeyFile);
                } catch (InvalidKeySpecException e) {
                    Log.e(TAG, "Invalid key format, generating new keys", e);
                    // Delete invalid keys
                    keyFile.delete();
                    pubKeyFile.delete();
                    // Fall through to generate new keys
                }
            }

            // Generate new keys if loading failed or files don't exist
                if (adbCrypto == null) {
                    adbCrypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
                    @Override
                    public String encodeToString(byte[] data) {
                        return Base64.encodeToString(data, Base64.NO_WRAP);
                    }
                });
                // Save the keys
                adbCrypto.saveAdbKeyPair(keyFile, pubKeyFile);
            }
        }
        return adbCrypto;
    }

    /**
     * Requests USB permission for ADB connection.
     * @param context The application context
     */
    public static void requestUsbPermission(Context context) {
        Log.d(TAG, "Requesting USB device permission");
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        
        if (usbManager == null) {
            Log.e(TAG, "Failed to get USB manager");
            broadcastConnectionStatus(context, false);
            return;
        }

        // Get all available devices and find ADB device
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        UsbDevice adbDevice = null;

        for (UsbDevice device : deviceList.values()) {
            if (isAdbDevice(device)) {
                adbDevice = device;
                            break;
                        }
                    }

        if (adbDevice == null) {
            Log.e(TAG, "No ADB device found");
            broadcastConnectionStatus(context, false);
            return;
        }

        // Create permission intent
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            new Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        );

        // Request permission
        usbManager.requestPermission(adbDevice, permissionIntent);
    }

    public void resetConnection() {
        synchronized (connectionLock) {
            isAdbConnected = false;
            if (adbConnection != null) {
                try {
                    adbConnection.close();
                        } catch (IOException e) {
                    Log.e(TAG, "Error closing ADB connection", e);
                }
                adbConnection = null;
            }
            mDevice = null;
        }
    }

    public void connect(Context context) {
        synchronized (connectionLock) {
            try {
                if (establishAdbConnection(context)) {
                    Log.d(TAG, "ADB connection established successfully");
                    broadcastConnectionStatus(context, true);
                } else {
                    Log.d(TAG, "Failed to establish ADB connection");
                    broadcastConnectionStatus(context, false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to establish ADB connection", e);
                broadcastConnectionStatus(context, false);
            }
        }
    }

    private static void broadcastConnectionStatus(Context context, boolean isConnected) {
        Intent intent = new Intent("com.htetznaing.adbotg.USB_STATUS");
        intent.putExtra("isConnected", isConnected);
        context.sendBroadcast(intent);
    }

    public String executeShellCommand(String command) throws Exception {
        synchronized(adbLock) {
            if (!isConnected()) {
                throw new Exception("ADB not connected");
            }

                AdbStream stream = null;
                try {
                // Open shell without the command
                stream = adbConnection.open("shell:");
                Log.d(TAG, "Shell opened successfully");
                
                // Wait for shell prompt
                Thread.sleep(100);
                
                // Write command and wait for completion marker
                String fullCommand = command + "; echo $?; echo '---END---'\n";
                stream.write(fullCommand.getBytes(StandardCharsets.UTF_8));
                Log.d(TAG, "Command written: " + fullCommand);
                
                StringBuilder output = new StringBuilder();
                byte[] buffer;
                
                // Read until we see our marker
                while ((buffer = stream.read()) != null) {
                    String data = new String(buffer, StandardCharsets.UTF_8);
                    Log.d(TAG, "Received data chunk: [" + data + "]");
                    if (data.contains("---END---")) {
                                break;
                            }
                    output.append(data);
                }

                String result = output.toString()
                    .replace("---END---", "")
                    .replaceAll("\\r\\n$", "")
                    .replaceAll("\\$", "")
                    .trim();
                    
                Log.d(TAG, "Shell command executed: " + command);
                Log.d(TAG, "Raw output: [" + result + "]");
                
                // Check if we got an error code
                if (result.endsWith("1") || result.endsWith("127")) {
                    throw new Exception("Command failed with error: " + result);
                }
                
                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error executing shell command: " + command, e);
                throw e;
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }
        }
    }

    // Add this getter method
    public Object getAdbLock() {
        return adbLock;
    }

    // Add AppInfo as inner class
    private static class AppInfo {
        private final String packageName;
        private final String type;

        public AppInfo(String packageName, String type) {
            this.packageName = packageName;
            this.type = type;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getType() {
            return type;
        }
    }

    private Map<String, AppInfo> compareWithCSV(String csvData, Map<String, String> installerMap) {
        Map<String, AppInfo> detectedApps = new HashMap<>();
        String[] lines = csvData.split("\n");

        for (Map.Entry<String, String> entry : installerMap.entrySet()) {
            String packageName = entry.getKey();
            String installer = entry.getValue();
            boolean isSecureInstaller = isSecureInstaller(installer);

            // Check each line in CSV
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].trim().equals(packageName)) {
                    String type = parts[1].trim().toUpperCase();
                    String flags = parts[2].trim().toUpperCase();

                    if (isSecureInstaller) {
                        // For secure installers, use the type from CSV
                        detectedApps.put(packageName, new AppInfo(packageName, type));
                    } else {
                        // For unsecure/unknown installers, mark as OFFSTORE
                        detectedApps.put(packageName, new AppInfo(packageName, "OFFSTORE"));
                    }
                    break;
                }
            }
        }
        
        // Add debug logging
        for (Map.Entry<String, AppInfo> entry : detectedApps.entrySet()) {
            Log.d(TAG, "App: " + entry.getKey() + " Type: " + entry.getValue().getType() + 
                  " Installer: " + installerMap.get(entry.getKey()));
        }
        
        return detectedApps;
    }

    private boolean isSecureInstaller(String installer) {
        return installer != null && !installer.isEmpty() && !installer.equals("null") && !installer.equals("Unknown") &&
            (installer.contains("com.android.vending") || // Google Play
             installer.contains("com.google.android.packageinstaller") || // System installer
             installer.contains("com.samsung.android.app.store")); // Samsung Store
    }
}

