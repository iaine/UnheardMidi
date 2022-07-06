package uk.ac.warwick.cim.unheardmidi;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class MidiConnection {

    public final String TAG = "MIDIConnection";

    public MidiDeviceInfo[] infos;

    private Context context;

    protected MidiConnection ( Context ctx) {
        context = ctx;
    }


    public void getUSBConnections (MidiManager midiManager) {
        try {
            infos = midiManager.getDevices();
        } catch (Exception e) {
            Log.i(TAG, "Connection error: " + e);
        }
    }

    public MidiDeviceInfo[] getUSBDevices () {
        return infos;
    }

    /**
     * Extracts the data. Idea is to store it at some moment to support
     * a replay or to identify how the phone senses connections and device networks.
     * @param infos - List of all the connected devices
     * @param fName
     */
    public void extractConnectionData (MidiDeviceInfo[] infos, File fName) {
        String deviceData = "";
        for (MidiDeviceInfo info: infos) {
            Bundle properties = info.getProperties();
            deviceData = System.currentTimeMillis()
            + ", inputs: " + info.getInputPortCount()
            + ", " +  info.getOutputPortCount()
            + ", " + properties
                    .getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
            + ", " + properties
                    .getString(MidiDeviceInfo.PROPERTY_NAME) + "\n";
        }
        new FileConnection(fName).writeFile(deviceData);
    }

    public void connectBluetoothMidi () {

    }

    public void sendNoteToDevice (MidiManager midiManager, String type) {

        this.getUSBConnections(midiManager);
        MidiDeviceInfo[] devices = this.getUSBDevices();

        if (devices == null) {
            Log.i(TAG, "No MIDI devices connected");
            Toast.makeText(context, "No MIDI devices connected", Toast.LENGTH_SHORT).show();
        } else {
            for (MidiDeviceInfo info : devices) {
                midiManager.openDevice(info, device -> {
                    if (device == null) {
                        Log.e(TAG, "could not open device " + info);
                        Toast.makeText(context, "Could not open device ", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Opening Device " + device.getInfo().getId(), Toast.LENGTH_SHORT).show();
                        MidiDeviceInfo.PortInfo[] ports = device.getInfo().getPorts();
                        int idx = 0;
                        for (MidiDeviceInfo.PortInfo portInfo : ports) {
                            if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                                idx = portInfo.getPortNumber();
                            }
                        }
                        Toast.makeText(context, "Sending Data " + idx, Toast.LENGTH_SHORT).show();
                        MidiInputPort inputPort = device.openInputPort(idx);
                        sendMidiNote(inputPort, type);
                    }

                }, new Handler(Looper.getMainLooper()));
            }
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
}
