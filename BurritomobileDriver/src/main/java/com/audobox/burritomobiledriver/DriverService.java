package com.audobox.burritomobiledriver;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by will on 10/6/13.
 */
public class DriverService extends Service {

    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket;
    BluetoothHandler btHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread driverThread = new Thread() {

            @Override
            public void run() {
                Log.d("Burritomobile", "Running new thread.");
                drive();
            }
        };

        driverThread.start();

        return START_STICKY;
    }

    public void drive() {
        Log.d("Burritomobile", "Thread started.");
        btHandler = new BluetoothHandler();
        btHandler.setup();
        String serverAddress = "ec2-54-215-239-150.us-west-1.compute.amazonaws.com";
//        String serverAddress = "ngrok.com";
        Socket s = null;
        try {
            s = new Socket(serverAddress, 3000);
            BufferedReader instructions =
                    new BufferedReader(new InputStreamReader(s.getInputStream()));
            while (true) {
                int message = instructions.read();
//                Log.d("Burritomobile", "Message received: " + message);
                switch (message) {
                    case 'w':
                        btHandler.writeData(new byte[]{(byte) 1});
                        break;
                    case 'q':
                        btHandler.writeData(new byte[]{(byte) 2});
                        break;
                    case 'e':
                        btHandler.writeData(new byte[]{(byte) 3});
                        break;
                    case 's':
                        btHandler.writeData(new byte[]{(byte) 4});
                        break;
                    case 'a':
                        btHandler.writeData(new byte[]{(byte) 5});
                        break;
                    case 'd':
                        btHandler.writeData(new byte[]{(byte) 6});
                        break;
                }
//                    btHandler.writeData(message);
//                Log.d("Burritomobile", "Message received: " + message);
            }

//                Log.d("Burritomobile", "Message received: " + instructions.read());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class BluetoothHandler {

        public void setup() {
            btAdapter = BluetoothAdapter.getDefaultAdapter();

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(bluetoothFoundReceiver, filter); // Don't forget to unregister during onDestroy
            BluetoothDevice device = btAdapter.getRemoteDevice("00:06:66:08:60:0E");
            connect(device);

        }

        public void connect(BluetoothDevice device) {
            btAdapter.cancelDiscovery();
            Log.d("Burritomobile", device.toString());

            try {
                btSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Log.d("Burritomobile", "made socket");
//            btSocket = device.createRfcommSocketToServiceRecord(new DeviceUuidFactory(this).getDeviceUuid());
                btSocket.connect();
                Log.d("Burritomobile", "Connection made.");
            } catch (IOException e) {
//            Log.d("Burritomobile", "Socket creation failed");
                Log.e("Burritomobile", "Socket creation failed", e);
            }
        }

        private void writeData(byte[] bytes) {
            try {
                OutputStream outStream = btSocket.getOutputStream();
                Log.d("Burritomobile", "About to send bytes: " );
                for (byte b : bytes) {
                    Log.d("Burritomobile", "Byte: " + (int) b);
                }

                try {
                    outStream.write(bytes);
                } catch (IOException e) {
                    Log.e("Burritomobile", "Bug while sending stuff", e);
                }
            } catch (IOException e) {
                Log.e("Burritomobile", "Bug BEFORE Sending stuff", e);
                stopSelf();
            }
        }

        private void writeData(String data) {
            writeData(data.getBytes());
            Log.d("Burritomobile", "Sent message on BT: " + data);
        }

        public void onDestroy() {
            unregisterReceiver(bluetoothFoundReceiver);
        }

        // Create a BroadcastReceiver for ACTION_FOUND
        private final BroadcastReceiver bluetoothFoundReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d("Burritomobile", "Got a BT result");

                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String name = device.getName();

//                if (device.getAddress().equals("00:06:66:08:60:0E")) {
//                    connect(device);
//                    return;
//                }
                    if (name != null && !name.equals("")) {
                        if (name.equals("FireFly-600E")) {
                            connect(device);
                            return;
                        }
                        Log.d("Burritomobile", device.getName());
                    } else {
                        Log.d("Burritomobile", device.getAddress());
                    }

                }
            }
        };
    }





    @Override
    public void onDestroy() {
        btHandler.onDestroy();
    }
}
