package uk.ac.warwick.cim.unheardmidi;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;


public class ScanBluetoothMidi {

    public final String TAG = "BleConnection";

    public ArrayList<BluetoothDevice> bleDevices;

    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner bluetoothLeScanner;

    private Handler handler = new Handler();

    private MidiManager midiManager;

    private Context ctx;

    private ArrayList<ScanFilter> scanFilterList = new ArrayList();

    //set up the service UUID for MIDI over Bluetooth
    private final ParcelUuid parcelUuid = new ParcelUuid(
            UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700"));

    protected ScanBluetoothMidi (Context context, MidiManager midiManager) {

        this.midiManager = midiManager;
        ctx = context;

        // Filters and Settings
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(parcelUuid).build();
        scanFilterList.add(scanFilter);
        //@todo: fix the filter to use a scan filter
        bleDevices = new ArrayList<>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.startScanningLeDevices();
        this.connectBluetoothToMidi();
    }

    public ArrayList<BluetoothDevice> getDevices () {
        return bleDevices;
    }

    public void connectBluetoothToMidi () {
        for (BluetoothDevice bleDevice: bleDevices) {
            this.midiManager.openBluetoothDevice(bleDevice,
                    new MidiManager.OnDeviceOpenedListener() {
                        @Override
                        public void onDeviceOpened(MidiDevice device) {
                            if (device == null) {
                                Log.e(TAG, "could not connect device " + bleDevice);
                                Toast.makeText(ctx, "Could not connect device ", Toast.LENGTH_SHORT).show();
                            } else {
                                MidiDeviceInfo.PortInfo[] ports = device.getInfo().getPorts();
                                int idx = 0;
                                for (MidiDeviceInfo.PortInfo portInfo : ports) {
                                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                                        idx = portInfo.getPortNumber();
                                    }
                                }
                                MidiOutputPort outputPort = device.openOutputPort(idx);
                                outputPort.connect(new BleMidiReceiver());
                            }
                        }
                    }, null);
        }
    }


    public void sendNoteToDevice (MidiManager midiManager, String type) {

        for (BluetoothDevice bleDevice: bleDevices) {
            this.midiManager.openBluetoothDevice(bleDevice,
                    new MidiManager.OnDeviceOpenedListener() {
                        @Override
                        public void onDeviceOpened(MidiDevice device) {
                            if (device == null) {
                                Log.e(TAG, "could not open ble device " + bleDevice);
                                Toast.makeText(ctx, "Could not open ble device ", Toast.LENGTH_SHORT).show();
                            } else {
                                MidiDeviceInfo.PortInfo[] ports = device.getInfo().getPorts();
                                int idx = 0;
                                for (MidiDeviceInfo.PortInfo portInfo : ports) {
                                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                                        idx = portInfo.getPortNumber();
                                    }
                                }
                                MidiInputPort inputPort = device.openInputPort(idx);
                                sendMidiNote(inputPort, type);
                            }
                        }
                    }, null);
        }

    }

    private void sendMidiNote (MidiInputPort inputPort, String noteType) {
        MidiNotes midiNotes = new MidiNotes();
        if (noteType.equals("ble")) {
            midiNotes.bleMidiNote(inputPort);
        } else if (noteType.equals("wifi")) {
            midiNotes.wifiMidiNote(inputPort);
        } else {
            Log.i(TAG, "Type is " + noteType);
        }
    }

    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD = 2000;

    @SuppressLint("MissingPermission")
    public void startScanningLeDevices() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                bluetoothLeScanner.startScan(leScanCallback);
            }
        }, SCAN_PERIOD);

        bluetoothLeScanner.startScan(leScanCallback);
    }

    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    ScanRecord details = result.getScanRecord();

                    Log.i("SCANBEL", "Parcel " + parcelUuid);
                    Log.i("SCANBEL", "Detail " + details);

                    //filter on the UUIDs for now.
                    if (details.getServiceUuids() != null &&
                            details.getServiceUuids().contains(parcelUuid)) {
                        bleDevices.add(result.getDevice());
                    }

                }
            };
}
