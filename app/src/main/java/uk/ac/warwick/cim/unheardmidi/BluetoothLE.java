package uk.ac.warwick.cim.unheardmidi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.media.midi.MidiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Check the API levels in the App?
 */


public class BluetoothLE {

    private static final String TAG = "BLUETOOTH";

    private static final int REQUEST_ENABLE_BT = 104;

    private final File fName;

    private Context mContext;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner bluetoothLeScanner;

    private boolean scanning;

    private Handler handler = new Handler();

    private MidiConnection midiConnection ;

    private MidiManager midiManager;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 2500;

    public BluetoothLE(Context ctx, File fileName, MidiManager mManager) {
        mContext = ctx;
        fName = fileName;
        midiManager = mManager;
        midiConnection = new MidiConnection(ctx);
        this.initBluetoothDetails();
    }

    @SuppressLint("MissingPermission")
    private void initBluetoothDetails() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        this.scanLeDevice();

    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if(bluetoothLeScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                }, SCAN_PERIOD);

                scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);
            } else {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }
    }

    // Device scan callback.
    /*
     * @todo: Add in NoteON and NoteOff for each scan
     */
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Toast.makeText(mContext, "Scanning for BLE", Toast.LENGTH_SHORT).show();
                    super.onScanResult(callbackType, result);
                    ScanRecord details = result.getScanRecord();
                    String data;
                    data = System.currentTimeMillis()
                            + ", " + result.getDevice()
                            + ", " + result.getRssi()
                            + ", " + details.getDeviceName()
                            + ", " + details.getManufacturerSpecificData().toString()
                            + ", " + details.getTxPowerLevel()
                            + ", " + details.getAdvertiseFlags();
                    /* test for APIs. If greater than 26, we add some fields. */
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        data += ", " + result.getPrimaryPhy()
                            + ", " + result.getSecondaryPhy()
                            + ", " + result.getPeriodicAdvertisingInterval()
                            + ", " + details.getManufacturerSpecificData().toString();
                    }
                    //@todo: check other distance metric results
                    Log.i(TAG, "RSSI :" + result.getRssi() + " tX " + details.getTxPowerLevel());
                    //Log.i(TAG, "details :" + signalTone.getDistance(result.getRssi()));
                    //midiNote.playNote(signalTone.getDistance(result.getRssi()));
                    data += " \n";
                    writeData(data);
                    midiConnection.sendNoteToDevice(midiManager,"ble");
                }
            };

    private void writeData (String data) {
        try {
            new FileConnection(fName).writeFile(data);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

}

