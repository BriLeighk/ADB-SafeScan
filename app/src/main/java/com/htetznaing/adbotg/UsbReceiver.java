package com.htetznaing.adbotg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Parcelable;
import android.util.Log;

import static com.htetznaing.adbotg.Message.USB_PERMISSION;

/**
 * Listens for USB device attachment events and broadcasts a permission request.
 */
public class UsbReceiver extends BroadcastReceiver {
    /**
     * Called when a broadcast is received.
     * @param context The context in which the receiver is running.
     * @param intent The intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action;
        Log.d("UsbReceiver", "Broadcasting USB_CONNECTED");
        if (intent != null && (action = intent.getAction()) != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            Intent intent1 = new Intent(USB_PERMISSION);
            intent1.putExtra(UsbManager.EXTRA_DEVICE, (Parcelable) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
            context.sendBroadcast(intent1);
        }
    }
}
