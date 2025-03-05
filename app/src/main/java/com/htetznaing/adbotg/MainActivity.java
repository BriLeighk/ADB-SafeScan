package com.htetznaing.adbotg;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;
import com.cgutman.adblib.UsbChannel;
import com.htetznaing.adbotg.UI.SpinnerDialog;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static com.htetznaing.adbotg.Message.CONNECTING;
import static com.htetznaing.adbotg.Message.DEVICE_FOUND;
import static com.htetznaing.adbotg.Message.DEVICE_NOT_FOUND;
import static com.htetznaing.adbotg.Message.FLASHING;
import static com.htetznaing.adbotg.Message.INSTALLING_PROGRESS;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.BinaryMessenger;

/**
 * Main activity of the application, handling USB connections, ADB commands, and user interactions.
 */
public class MainActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnKeyListener {
    private Handler handler;
    private UsbDevice mDevice;
    private TextView tvStatus, logs;
    private ImageView usb_icon;
    private AdbCrypto adbCrypto;
    private AdbConnection adbConnection;
    private UsbManager mManager;
    private RelativeLayout terminalView;
    private LinearLayout checkContainer;
    private EditText edCommand;
    private Button btnRun;
    private ScrollView scrollView;
    private String user = null;
    private boolean doubleBackToExitPressedOnce = false;
    private AdbStream stream;
    private SpinnerDialog waitingDialog;
    private static final String CHANNEL = "com.htetznaing.adbotg/usb_receiver";
    private FlutterEngine flutterEngine;
    private boolean usbConnectionDetected = false;
    private MethodChannel methodChannel;
    private SpywareDetector detector;
    private ApplicationController appController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appController = ApplicationController.getInstance();
        detector = SpywareDetector.getInstance();
        
        // Keep screen on during scanning
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // Initialize MethodChannel
        FlutterEngine flutterEngine = new FlutterEngine(this);
        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);

        // Initialize additional method channels
        AppDetailsChannelHandler.setup(flutterEngine.getDartExecutor().getBinaryMessenger());
        SpywareChannelHandler.setup(flutterEngine.getDartExecutor().getBinaryMessenger());

        tvStatus = findViewById(R.id.tv_status);
        usb_icon = findViewById(R.id.usb_icon);
        logs = findViewById(R.id.logs);
        terminalView = findViewById(R.id.terminalView);
        checkContainer = findViewById(R.id.checkContainer);
        edCommand = findViewById(R.id.edCommand);
        btnRun = findViewById(R.id.btnRun);
        scrollView = findViewById(R.id.scrollView);
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull android.os.Message msg) {
                switch (msg.what) {
                    case DEVICE_FOUND:
                        closeWaiting();
                        tvStatus.setText(getString(R.string.adb_device_connected));
                        usb_icon.setImageResource(R.drawable.ic_usb);
                        usb_icon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                        terminalView.setVisibility(View.VISIBLE);
                        checkContainer.setVisibility(View.GONE);
                        // Initialize command after ensuring connection
                        if (detector.isConnected()) {
                            initCommand();
                        }
                        break;

                    case CONNECTING:
                        waitingDialog();
                        tvStatus.setText(getString(R.string.waiting_device));
                        usb_icon.setColorFilter(Color.BLUE);
                        checkContainer.setVisibility(View.VISIBLE);
                        break;

                    case DEVICE_NOT_FOUND:
                        closeWaiting();
                        tvStatus.setText(getString(R.string.adb_device_not_connected));
                        tvStatus.setTextColor(Color.RED);
                        usb_icon.setColorFilter(Color.RED);
                        checkContainer.setVisibility(View.VISIBLE);
                        sendConnectionStatusBroadcast(false); // Notify Flutter
                        break;

                    case FLASHING:
                        Toast.makeText(MainActivity.this, "Flashing", Toast.LENGTH_SHORT).show();
                        break;

                    case INSTALLING_PROGRESS:
                        Toast.makeText(MainActivity.this, "Progress", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        AdbBase64 base64 = new MyAdbBase64();
        try {
            adbCrypto = AdbCrypto.loadAdbKeyPair(base64, new File(getFilesDir(), "private_key"), new File(getFilesDir(), "public_key"));
        } catch (Exception e) {
            Log.e(Const.TAG, "Failed to load ADB key pair", e);
        }

        if (adbCrypto == null) {
            try {
                adbCrypto = AdbCrypto.generateAdbKeyPair(base64);
                adbCrypto.saveAdbKeyPair(new File(getFilesDir(), "private_key"), new File(getFilesDir(), "public_key"));
            } catch (Exception e) {
                Log.w(Const.TAG, "Failed to generate and save key-pair", e);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Message.USB_PERMISSION);

        ContextCompat.registerReceiver(this, mUsbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Check USB
        UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            System.out.println("From Intent!");
            asyncRefreshAdbConnection(device);
        } else {
            System.out.println("From onCreate!");
            for (String k : mManager.getDeviceList().keySet()) {
                UsbDevice usbDevice = mManager.getDeviceList().get(k);
                handler.sendEmptyMessage(CONNECTING);
                if (mManager.hasPermission(usbDevice)) {
                    asyncRefreshAdbConnection(usbDevice);
                } else {
                    mManager.requestPermission(
                        usbDevice,
                        PendingIntent.getBroadcast(getApplicationContext(),
                            0,
                            new Intent(Message.USB_PERMISSION),
                            PendingIntent.FLAG_IMMUTABLE));
                }
            }
        }

        // Initialize connection
        if (appController.initializeConnection()) {
            handler.sendEmptyMessage(DEVICE_FOUND);
        }

        edCommand.setImeActionLabel("Run", EditorInfo.IME_ACTION_DONE);
        edCommand.setOnEditorActionListener(this);
        edCommand.setOnKeyListener(this);
    }

    /**
     * Closes the waiting dialog if it is open.
     */
    private void closeWaiting() {
        if (waitingDialog != null)
            waitingDialog.dismiss();
    }

    /**
     * Displays a waiting dialog with a message.
     */
    private void waitingDialog() {
        closeWaiting();
        waitingDialog = SpinnerDialog.displayDialog(this, "IMPORTANT ⚡",
                "You may need to accept a prompt on the target device if you are connecting " +
                        "to it for the first time from this device.", false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.go_to_github) {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/KhunHtetzNaing/ADB-OTG")));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("From onNewIntent");
        asyncRefreshAdbConnection((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
    }

    /**
     * Asynchronously refreshes the ADB connection for the given USB device.
     * @param device The USB device to connect to.
     */
    public void asyncRefreshAdbConnection(final UsbDevice device) {
        if (device != null) {
            new Thread() {
                @Override
                public void run() {
                    final UsbInterface intf = findAdbInterface(device);
                    try {
                        setAdbInterface(device, intf);
                    } catch (Exception e) {
                        Log.w(Const.TAG, "setAdbInterface(device, intf) fail", e);
                    }
                }
            }.start();
        }
    }

    /**
     * Broadcast receiver for USB events.
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(Const.TAG, "mUsbReceiver onReceive => " + action);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.getDeviceName().equals(deviceName)) {
                    try {
                        Log.d(Const.TAG, "setAdbInterface(null, null)");
                        setAdbInterface(null, null);
                    } catch (Exception e) {
                        Log.w(Const.TAG, "setAdbInterface(null,null) failed", e);
                    }
                }
            } else if (Message.USB_PERMISSION.equals(action)){
                System.out.println("From receiver!");
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                handler.sendEmptyMessage(CONNECTING);
                if (mManager.hasPermission(usbDevice))
                    asyncRefreshAdbConnection(usbDevice);
                else {
                    mManager.requestPermission(usbDevice,PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(Message.USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE));
                }
            }
        }
    };

    /**
     * Searches for an ADB interface on the given USB device.
     * @param device The USB device to search.
     * @return The ADB interface if found, null otherwise.
     */
    private UsbInterface findAdbInterface(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 &&
                    intf.getInterfaceProtocol() == 1) {
                return intf;
            }
        }
        return null;
    }

    /**
     * Sets the current USB device and interface.
     * @param device The USB device to set.
     * @param intf The USB interface to set.
     * @return True if the interface was set successfully, false otherwise.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    private synchronized boolean setAdbInterface(UsbDevice device, UsbInterface intf) throws IOException, InterruptedException {
        if (adbConnection != null) {
            adbConnection.close();
            adbConnection = null;
            mDevice = null;
        }

         if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                if (connection.claimInterface(intf, false)) {
                    handler.sendEmptyMessage(CONNECTING);
                    adbConnection = AdbConnection.create(new UsbChannel(connection, intf), adbCrypto);
                    adbConnection.connect();
                    //TODO: DO NOT DELETE IT, I CAN'T EXPLAIN WHY
                    adbConnection.open("shell:exec date");

                    mDevice = device;
                    handler.sendEmptyMessage(DEVICE_FOUND);
                    return true;
                } else {
                    connection.close();
                }
            }
        }

        handler.sendEmptyMessage(DEVICE_NOT_FOUND);
        mDevice = null;
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        detector.maintainConnection(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        detector.closeConnection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        try {
            if (adbConnection != null) {
                adbConnection.close();
                adbConnection = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Only close connection if app is actually being destroyed
        if (isFinishing()) {
            appController.closeConnection();
        }
    }

    /**
     * Initializes the command input and output.
     */
    private void initCommand() {
        logs.setText("");
        try {
            if (!detector.isConnected()) {
                Log.e(Const.TAG, "ADB connection not available");
                return;
            }
            
            AdbConnection connection = detector.getAdbConnection();
            if (connection == null) {
                Log.e(Const.TAG, "ADB connection is null");
                return;
            }

            stream = connection.open("shell:");
            
            // Start the receiving thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!stream.isClosed()) {
                        try {
                            // Print each thing we read from the shell stream
                            final String[] output = {new String(stream.read(), "US-ASCII")};
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (user == null) {
                                        user = output[0].substring(0,output[0].lastIndexOf("/")+1);
                                    }else if (output[0].contains(user)){
                                        System.out.println("End => "+user);
                                    }

                                    logs.append(output[0]);

                                    scrollView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                            edCommand.requestFocus();
                                        }
                                    });
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            return;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }).start();

            btnRun.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    putCommand();
                }
            });
        } catch (Exception e) {
            Log.e(Const.TAG, "Error initializing command", e);
        }
    }

    /**
     * Sends a command to the ADB shell.
     */
    private void putCommand() {
        if (!edCommand.getText().toString().isEmpty()){
            // We become the sending thread
            try {
                String cmd = edCommand.getText().toString();
                if (cmd.equalsIgnoreCase("clear")) {
                    String log = logs.getText().toString();
                    String[] logSplit = log.split("\n");
                    logs.setText(logSplit[logSplit.length-1]);
                }else if (cmd.equalsIgnoreCase("exit")) {
                    finish();
                }else {
                    stream.write((cmd+"\n").getBytes("UTF-8"));
                }
                edCommand.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else Toast.makeText(MainActivity.this, "No command", Toast.LENGTH_SHORT).show();
    }

    public void open(View view) {

    }

    public void showKeyboard() {
        edCommand.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        // Simply call the super method to handle the back press
        super.onBackPressed();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        /* We always return false because we want to dismiss the keyboard */
        if (adbConnection != null && actionId == EditorInfo.IME_ACTION_DONE) {
            putCommand();
        }

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            /* Just call the onEditorAction function to handle this for us */
            return onEditorAction((TextView)v, EditorInfo.IME_ACTION_DONE, event);
        } else {
            return false;
        }
    }

    /**
     * Sends a broadcast to notify about the USB connection status.
     * @param isConnected True if the device is connected, false otherwise.
     */
    private void sendConnectionStatusBroadcast(boolean isConnected) {
        Intent intent = new Intent("com.htetznaing.adbotg.USB_STATUS");
        intent.putExtra("isConnected", isConnected);
        sendBroadcast(intent);
    }
}
