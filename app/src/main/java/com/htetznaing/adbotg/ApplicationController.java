package com.htetznaing.adbotg;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import com.cgutman.adblib.AdbConnection;

public class ApplicationController extends Application {
    private static ApplicationController instance;
    private SpywareDetector spywareDetector;
    private boolean isInitialized = false;
    private AdbConnection adbConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        spywareDetector = SpywareDetector.getInstance();
        spywareDetector.initialize(this);
    }

    public static ApplicationController getInstance() {
        return instance;
    }

    public boolean initializeConnection() {
        if (!isInitialized) {
            if (spywareDetector.maintainConnection(this)) {
                // Broadcast successful connection
                Intent intent = new Intent("com.htetznaing.adbotg.USB_STATUS");
                intent.putExtra("isConnected", true);
                sendBroadcast(intent);
                isInitialized = true;
                return true;
            }
        } else if (spywareDetector.isConnected()) {
            // If already initialized and connected, send status update
            Intent intent = new Intent("com.htetznaing.adbotg.USB_STATUS");
            intent.putExtra("isConnected", true);
            sendBroadcast(intent);
            return true;
        }
        return false;
    }

    public void closeConnection() {
        if (isInitialized) {
            spywareDetector.closeConnection();
            isInitialized = false;
            
            // Broadcast disconnection
            Intent intent = new Intent("com.htetznaing.adbotg.USB_STATUS");
            intent.putExtra("isConnected", false);
            sendBroadcast(intent);
        }
        if (adbConnection != null) {
            try {
                adbConnection.close();
            } catch (IOException e) {
                Log.e("ApplicationController", "Error closing connection", e);
            }
            adbConnection = null;
        }
    }

    public SpywareDetector getSpywareDetector() {
        return spywareDetector;
    }

    // Add a method to get application context
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    public boolean isConnected() {
        return isInitialized && spywareDetector.isConnected();
    }

    public void requestConnection() {
        // Close any existing connection
        closeConnection();
        
        // Reset connection state
        if (spywareDetector != null) {
            spywareDetector.resetConnection();
        }
        
        // Request new USB permission and establish connection
        SpywareDetector.requestUsbPermission(this);
        
        // Try to establish connection
        if (spywareDetector != null) {
            spywareDetector.connect(this);
        }
    }
} 