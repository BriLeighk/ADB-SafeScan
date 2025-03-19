package com.htetznaing.adbotg;

import android.content.Context;

/**
 * Holds the application context for global access.
 */
public class AppContextHolder {
    private static Context applicationContext;

    /**
     * Set the application context.
     * @param context The application context.
     */
    public static void setContext(Context context) {
        applicationContext = context.getApplicationContext();
    }

    /**
     * Get the application context.
     * @return The application context.
     */
    public static Context getContext() {
        return applicationContext;
    }
}
