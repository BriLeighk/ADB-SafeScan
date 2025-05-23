package com.cgutman.adblib;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

/**
 * Created by xudong on 2/21/14.
 */
public class UsbChannel implements AdbChannel {

    private final UsbDeviceConnection mDeviceConnection;
    private final UsbEndpoint mEndpointOut;
    private final UsbEndpoint mEndpointIn;
    private final UsbInterface mInterface;

    private final int defaultTimeout = 1000;

    private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<UsbRequest>();

    // return an IN request to the pool
    public void releaseInRequest(UsbRequest request) {
        synchronized (mInRequestPool) {
            mInRequestPool.add(request);
        }
    }


    // get an IN request from the pool
    public UsbRequest getInRequest() {
        synchronized (mInRequestPool) {
            if (mInRequestPool.isEmpty()) {
                UsbRequest request = new UsbRequest();
                request.initialize(mDeviceConnection, mEndpointIn);
                return request;
            } else {
                return mInRequestPool.removeFirst();
            }
        }
    }


    @Override
    public void readx(byte[] buffer, int length) throws IOException {
        int totalRead = 0;
        int retryCount = 0;
        final int MAX_RETRIES = 3;
        boolean wasInterrupted = false;
        
        while (totalRead < length && !wasInterrupted) {
            try {
                ByteBuffer buf = ByteBuffer.allocate(length - totalRead);
                UsbRequest request = getInRequest();
                if (request == null) {
                    throw new IOException("Failed to obtain USB request object");
                }
                
                request.queue(buf, length - totalRead);
                
                // Wait for the request with timeout
                UsbRequest response = mDeviceConnection.requestWait();
                if (response == null || response != request) {
                    releaseInRequest(request);
                    if (++retryCount > MAX_RETRIES) {
                        throw new IOException("USB read failed after " + MAX_RETRIES + " retries");
                    }
                    Thread.sleep(100); // Increased delay between retries
                    continue;
                }
                
                int bytesRead = buf.position();
                if (bytesRead > 0) {
                    buf.rewind();
                    buf.get(buffer, totalRead, bytesRead);
                    totalRead += bytesRead;
                    retryCount = 0; // Reset retry count on successful read
                } else if (++retryCount > MAX_RETRIES) {
                    throw new IOException("No data read from USB after " + MAX_RETRIES + " attempts");
                }
                
                releaseInRequest(request);
                
            } catch (InterruptedException e) {
                wasInterrupted = true;
                Thread.currentThread().interrupt(); // Preserve interrupt status
                throw new IOException("USB read interrupted", e);
            } finally {
                if (wasInterrupted) {
                    // Clean up resources if interrupted
                    closeConnection();
                }
            }
        }
        
        if (totalRead < length && !wasInterrupted) {
            throw new IOException("Incomplete read: expected " + length + " bytes, got " + totalRead);
        }
    }

    private void closeConnection() {
        try {
            if (mDeviceConnection != null) {
                mDeviceConnection.close();
            }
        } catch (Exception e) {
            // Log but don't throw as this is cleanup code
            Log.w("UsbChannel", "Error closing device connection", e);
        }
    }

    // API LEVEL 18 is needed to invoke bulkTransfer(mEndpointOut, buffer, offset, buffer.length - offset, defaultTimeout)
//    @Override
//    public void writex(byte[] buffer) throws IOException{
//
//        int offset = 0;
//        int transferred = 0;
//
//        while ((transferred = mDeviceConnection.bulkTransfer(mEndpointOut, buffer, offset, buffer.length - offset, defaultTimeout)) >= 0) {
//            offset += transferred;
//            if (offset >= buffer.length) {
//                break;
//            }
//        }
//        if (transferred < 0) {
//            throw new IOException("bulk transfer fail");
//        }
//    }

    // A dirty solution, only API level 12 is needed, not 18
    private void writex(byte[] buffer) throws IOException{

        int offset = 0;
        int transferred = 0;

        byte[] tmp = new byte[buffer.length];
        System.arraycopy(buffer, 0, tmp, 0, buffer.length);

        while ((transferred = mDeviceConnection.bulkTransfer(mEndpointOut, tmp, buffer.length - offset, defaultTimeout)) >= 0) {
            offset += transferred;
            if (offset >= buffer.length) {
                break;
            } else {
                System.arraycopy(buffer, offset, tmp, 0, buffer.length - offset);
            }
        }
        if (transferred < 0) {
            throw new IOException("bulk transfer fail");
        }
    }

    @Override
    public void writex(AdbMessage message) throws IOException {
        // TODO: here is the weirdest thing
        // write (message.head + message.payload) is totally different with write(message.head) + write(head.payload)
        writex(message.getMessage());
        if (message.getPayload() != null) {
            writex(message.getPayload());
        }
    }


    @Override
    public void close() throws IOException {
        mDeviceConnection.releaseInterface(mInterface);
        mDeviceConnection.close();
    }

    public UsbChannel(UsbDeviceConnection connection, UsbInterface intf) {
        mDeviceConnection = connection;
        mInterface = intf;

        UsbEndpoint epOut = null;
        UsbEndpoint epIn = null;
        // look for our bulk endpoints
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;
                }
            }
        }
        if (epOut == null || epIn == null) {
            throw new IllegalArgumentException("not all endpoints found");
        }
        mEndpointOut = epOut;
        mEndpointIn = epIn;
    }

}