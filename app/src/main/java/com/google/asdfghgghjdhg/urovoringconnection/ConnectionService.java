package com.google.asdfghgghjdhg.urovoringconnection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class ConnectionService extends Service {
    public static final String TAG = "UrovoRingConnection";

    public static final String CHANNEL_ID = "UrovoConnectionServiceChannel";
    public static final int NOTIFICATION_ID = 1;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private static final int STATE_BLUETOOTH_DISABLED = 0;
    private static final int STATE_BLUETOOTH_OFF = 1;
    private static final int STATE_DISCONNECTED = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_DEVICE_NOT_SELECTED = 4;

    private static final int MESSAGE_SEND_BARCODE = 1;
    private static final String MESSAGE_SEND_BARCODE_DATA = "barcode";

    private class BarcodeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_SEND_BARCODE) {
                String barcode = msg.getData().getString(MESSAGE_SEND_BARCODE_DATA);
                showBarcode(barcode);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectionService.this);

                if (preferences.getBoolean(getString(R.string.broadcast_messages_key), false)) {
                    String action = preferences.getString(getString(R.string.intent_action_key), null);
                    String data = preferences.getString(getString(R.string.intent_barcode_data_key), null);

                    if (action != null && !action.isEmpty() && data != null && !data.isEmpty()) {
                        Intent intent = new Intent();
                        intent.setAction(action);
                        intent.putExtra(data, barcode);
                        sendBroadcast(intent);
                    }
                }

                if (preferences.getBoolean(getString(R.string.clipboard_paste_key), false)) {
                    ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboardManager != null) {
                        ClipData text = ClipData.newPlainText(MESSAGE_SEND_BARCODE_DATA, barcode);
                        clipboardManager.setPrimaryClip(text);
                    }
                }

            }
        }
    }

    private class ConnectThread extends Thread {
        public ConnectThread() {
        }

        public void run() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectionService.this.getApplicationContext());
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                //Log.e(TAG, "Bluetooth not available");
                changeState(STATE_BLUETOOTH_DISABLED);
                return;
            }

            BluetoothSocket socket = null;

            while (true) {
                if (socket == null) {
                    if (!adapter.isEnabled()) {
                        //Log.w(TAG, "Bluetooth disabled, waiting...");
                        changeState(STATE_BLUETOOTH_OFF);

                        if (preferences.getBoolean(getString(R.string.bluetooth_on_key), false)) {
                            adapter.enable();
                        }

                        try {
                            Thread.sleep(500);
                            continue;
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    String deviceAddress = preferences.getString(getString(R.string.connect_to_device_key), "not selected");
                    if (deviceAddress.isEmpty() || deviceAddress.equals("not selected") || deviceAddress.length() != 17) {
                        changeState(STATE_DEVICE_NOT_SELECTED);
                        try {
                            Thread.sleep(500);
                            continue;
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
                    if (device == null) {
                        //Log.e(TAG, "Cannot find desired device");
                        changeState(STATE_DISCONNECTED);
                        try {
                            Thread.sleep(500);
                            continue;
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    try {
                        socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    } catch (IOException e) {
                        //Log.w(TAG, "Cannot initiate connection to device, retrying...");
                        changeState(STATE_DISCONNECTED);
                        try {
                            Thread.sleep(100);
                            continue;
                        } catch (InterruptedException interruptedException) {
                            return;
                        }
                    }

                    adapter.cancelDiscovery();
                }

                try {
                    socket.connect();
                } catch (IOException e) {
                    //Log.w(TAG, "Cannot connect to device, retrying...");
                    changeState(STATE_DISCONNECTED);
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        closeException.printStackTrace();
                    }
                    socket = null;
                    try {
                        Thread.sleep(100);
                        continue;
                    } catch (InterruptedException interruptedException) {
                        return;
                    }
                }

                ConnectionService.this.onConnect(socket);
                break;
            }
        }
    }

    private class BluetoothReaderThread extends Thread {
        private InputStream readerStream = null;
        private byte[] readBuffer = new byte[128];
        private String codeBuffer = "";

        public BluetoothReaderThread(InputStream stream) {
            readerStream = stream;
        }

        public void run() {
            int numBytes;

            while (readerStream != null) {
                try {
                    Arrays.fill(readBuffer, (byte)0);
                    numBytes = readerStream.read(readBuffer);
                    byte[] tmp = Arrays.copyOf(readBuffer, numBytes);

                    boolean done = false;
                    String data = new String(tmp, StandardCharsets.UTF_8);
                    if (data.endsWith("\r") || data.endsWith("\n")) {
                        done = true;
                        data = data.replaceAll("(\\r|\\n)", "");
                    }
                    Log.v(TAG, String.format("Received %d bytes: %s", numBytes, data));

                    codeBuffer = codeBuffer + data;
                    if (done) {
                        Log.i(TAG, String.format("Received barcode: %s", codeBuffer));
                        Message msg = new Message();
                        msg.what = MESSAGE_SEND_BARCODE;
                        Bundle bundle = new Bundle();
                        bundle.putString(MESSAGE_SEND_BARCODE_DATA, codeBuffer);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                        codeBuffer = "";
                    }

                } catch (IOException e) {
                    //Log.w(TAG, "Input stream was disconnected, reconnecting...");
                    break;
                }
            }

            ConnectionService.this.connect();
        }

    }

    private BluetoothSocket currentSocket = null;
    private BarcodeHandler handler = new BarcodeHandler();
    private boolean couldConnect = false;
    private int currentState = STATE_BLUETOOTH_DISABLED;

    public ConnectionService() {
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Urovo Connection Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentSocket != null) {
            return START_STICKY;
        }

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_running_title))
                .setContentText(getString(R.string.service_running_descrition))
                .setSmallIcon(R.drawable.baseline_bluetooth_disabled_24)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        couldConnect = true;
        connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        couldConnect = false;
        disconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void changeState(int newState) {
        if (currentState == newState) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectionService.this.getApplicationContext());
        String deviceAddress = preferences.getString(getString(R.string.connect_to_device_key), "");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device;

        currentState = newState;
        String notificationTitle = getString(R.string.service_running_title);
        String notificationText = getString(R.string.service_running_descrition);
        int notificationIcon = R.drawable.baseline_bluetooth_disabled_24;
        switch (currentState) {
            case STATE_BLUETOOTH_DISABLED:
                notificationTitle = getString(R.string.service_bluetooth_disabled_title);
                notificationText = getString(R.string.service_bluetooth_disabled_description);
                notificationIcon = R.drawable.baseline_bluetooth_disabled_24;
                break;
            case STATE_BLUETOOTH_OFF:
                notificationTitle = getString(R.string.service_bluetooth_off_title);
                notificationText = getString(R.string.service_bluetooth_off_description);
                notificationIcon = R.drawable.baseline_bluetooth_disabled_24;
                break;
            case STATE_DEVICE_NOT_SELECTED:
                notificationTitle = getString(R.string.service_device_not_selected_title);
                notificationText = getString(R.string.service_device_not_selected_description);
                notificationIcon = R.drawable.baseline_bluetooth_searching_24;
                break;
            case STATE_DISCONNECTED:
                notificationTitle = getString(R.string.service_disconnected_title);
                device = adapter.getRemoteDevice(preferences.getString(getString(R.string.connect_to_device_key), ""));
                if (device == null) {
                    notificationText = String.format(getString(R.string.service_disconnected_description), deviceAddress);
                } else {
                    notificationText = String.format(getString(R.string.service_disconnected_description), device.getName());
                }
                notificationIcon = R.drawable.baseline_bluetooth_searching_24;
                break;
            case STATE_CONNECTED:
                notificationTitle = getString(R.string.service_connected_title);
                device = adapter.getRemoteDevice(preferences.getString(getString(R.string.connect_to_device_key), ""));
                if (device == null) {
                    notificationText = String.format(getString(R.string.service_connected_description), deviceAddress);
                } else {
                    notificationText = String.format(getString(R.string.service_connected_description), device.getName());
                }
                notificationIcon = R.drawable.baseline_bluetooth_connected_24;
                break;
        }

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setSmallIcon(notificationIcon)
                    .setContentIntent(pendingIntent)
                    .build();
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    private void showBarcode(String barcode) {
        String notificationTitle = getString(R.string.service_connected_title);
        String notificationText = String.format(getString(R.string.service_connected_last_barcode), barcode);
        int notificationIcon = R.drawable.baseline_bluetooth_connected_24;

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setSmallIcon(notificationIcon)
                    .setContentIntent(pendingIntent)
                    .build();
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void connect() {
        if (currentSocket != null) {
            disconnect();
        }

        if (couldConnect) {
            new ConnectThread().start();
        }
    }
    public void disconnect() {
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onConnect(BluetoothSocket socket) {
        currentSocket = socket;

        if (currentSocket.isConnected()) {
            changeState(STATE_CONNECTED);
            //Log.v(TAG, "Socket connected");

            try {
                InputStream stream = currentSocket.getInputStream();
                new BluetoothReaderThread(stream).start();
            } catch (IOException e) {
                //Log.v(TAG, "Cant get input stream from socket", e);
                connect();
            }
        } else {
            connect();
        }
    }
}
