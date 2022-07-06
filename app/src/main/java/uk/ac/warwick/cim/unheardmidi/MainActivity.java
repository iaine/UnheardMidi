package uk.ac.warwick.cim.unheardmidi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * todo: think about removing the location part for sonification?
 *
 * If we remove, then we need to work out how to use service to run every
 * few seconds (5?)
 */

public class MainActivity extends AppCompatActivity {

    private File signalFile;

    private String CHANNEL_ID = "UM01";

    private File wifiFile;

    private File devices;

    private static final String TAG = "MAINACC";

    private final Handler handler = new Handler();

    private BroadcastReceiver receiver;

    private MidiManager midiManager;

    private MidiConnection midiConnection = new MidiConnection(this);


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //this is new in Android 31
        //if (Build.VERSION.SDK_INT >= 31) {
        String[] permissions = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION};
        checkPermissions(permissions, "Bluetooth Permissions error");
        //checkPermissions(Manifest.permission.BLUETOOTH_CONNECT, "Bluetooth Permissions error");
        //}
        //checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION, "Location Permissions error");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);


        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Log.i("MIDI", "midi not supported ");
            //@todo: needs to be a toast message
            Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
        }

        midiManager = (MidiManager) this.getSystemService(Context.MIDI_SERVICE);

        //let's set up the connections first.
        this.setUpMidiUSB();
        this.setUpMidiBle();

        // Set up for Android T.
        /*Collection<MidiDeviceInfo> universalDeviceInfos = midiManager.getDevicesForTransport(
                MidiManager.TRANSPORT_UNIVERSAL_MIDI_PACKETS);*/


        /*MidiOutputPort outputPort = device.openOutputPort(index);
        outputPort.connect(new MySynthEngine());*/

        // in case we do find any new organisations
        String currentName = "ble_" + System.currentTimeMillis() + ".txt";
        signalFile = this.createDataFile(currentName);

        wifiFile = this.createDataFile("wifi_" + System.currentTimeMillis() + ".txt");

        devices = this.createDataFile("devices_" + System.currentTimeMillis() + ".txt");
        Log.i(TAG, "Created file");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel();
        }

        //this.setUpWifiScan(this);
        this.setUpBleScan();

    }

    /**
     * Function to identify the connected MIDI devices over USB
     */
    private void setUpMidiUSB() {
        midiConnection.getUSBConnections(midiManager);
        MidiDeviceInfo[] connections = midiConnection.getUSBDevices();
        if (connections.length < 1) {
            Toast.makeText(this, "No USB Connections", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, connections.length + " USB Devices Connected", Toast.LENGTH_SHORT).show();
        }

        midiConnection.extractConnectionData(connections, devices);
    }

    /**
     * Function to identify the connected MIDI devices
     * on user action through a button
     *
     * @param view - takes a view argument from the Layout
     */
    public void setUpMidiUSB(View view) {
        midiConnection.getUSBConnections(midiManager);
        MidiDeviceInfo[] connections = midiConnection.getUSBDevices();
        if (connections.length < 1) {
            Toast.makeText(this, "No USB Connections", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, connections.length + " USB Devices Connected", Toast.LENGTH_SHORT).show();
        }
        midiConnection.extractConnectionData(connections, devices);
    }

    /**
     * Function to allow for a manual re-scan of Bluetooth devices that support Midi.
     * This is linked to the button.
     *
     * @param view - Android View
     */
    public void setUpMidiBle (View view){
        new ScanBluetoothMidi(this, midiManager);
    }

    public void setUpMidiBle (){
        new ScanBluetoothMidi(this, midiManager);
    }


    /**
     * Set up the Bluetooth scanning
     */
    @SuppressLint("MissingPermission")
    private void setUpBluetoothScan() {
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    System.out.println("Found device " + deviceName + " with addy " + deviceHardwareAddress);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    /**
     * Runnable to set up the Wifi scan
     */
    private void setUpWifiScan (Context context) {
        Runnable runnable = new Runnable() {
            //put the handler on a timer - every few seconds & make configurable?
            public void run() {
                Log.i(TAG, "In the runner");
                /*
                 * Open the companies file here and pass through.
                 * Use to sonify and to get any new data.
                 */
                new WifiDetails(wifiFile, midiManager, context);
                handler.postDelayed(this, 30000);
            }
        };
        //scan wifi every 30 seconds. OS throttles the call.
        handler.postDelayed(runnable, 30000);
    }

    /**
     * Set up the Bluetooth Scan
     */
    private void setUpBleScan ()  {
        this.setNotification("BLE Scan", "Scanning for Bluetooth LE");
        Runnable runnable = new Runnable() {
            //put the handler on a timer - every few seconds & make configurable?
            public void run() {
                Log.i(TAG, "In the runner");
                /*
                 * Open the companies file here and pass through.
                 * Use to sonify and to get any new data.
                 */
                new BluetoothLE(getBaseContext(), signalFile, midiManager);
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(runnable, 5000);
    }

    private void checkPermissions(String[] permissions, String s) {
        String[] requestPermissions;
        //Get permissions to find location
        for (String accessFineLocation: permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, accessFineLocation)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i("PERMISSIONS", s);
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        accessFineLocation);
            }
        }
        ActivityCompat.requestPermissions(MainActivity.this,
                permissions, 1);
    }

    /*private void checkPermissions(String accessFineLocation, String s) {
        //Get permissions to find location
        if (ContextCompat.checkSelfPermission(MainActivity.this, accessFineLocation)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("PERMISSIONS", s);
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    accessFineLocation)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{accessFineLocation}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{accessFineLocation}, 1);
            }
        }
    }*/

   @Override
    protected void onResume() {
        super.onResume();
       handler.removeCallbacks(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.setUpBleScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalFile.length() == 0) {
            try {
                signalFile.delete();
            } catch (Exception e) {
                Log.i(TAG, "Could not delete file");
            }
        }
        unregisterReceiver(receiver);
    }

    private File createDataFile(String fileName) {
        File fName = new File(this.getExternalFilesDir(null), fileName);

        if (!fName.exists()) {
            try {
                final boolean newFile = fName.createNewFile();
                if (!newFile) Log.i("FILE", fileName + " not created");
            } catch (IOException e) {
                Log.i("FILE",e.toString());
            }
        }

        return fName;
    }

    /**
     * Set up a notification for system states. Cancel on tap.
     * @param notificationTitle
     * @param notificationMsg
     */
    public void setNotification (String notificationTitle, String notificationMsg) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationMsg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //CharSequence name = getString(R.string.channel_name);
            //String description = getString(R.string.channel_description);
            CharSequence name = "Unheard Midi";
            String description = "States for Unheard Midi";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



    /*@SuppressLint("MissingPermission")
    private void startBluetoothScan () {
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    System.out.println("Found device " + deviceName + " with addy " + deviceHardwareAddress);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    private void stopBluetoothScan () {
        unregisterReceiver(receiver);
    }*/
}
