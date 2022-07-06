package uk.ac.warwick.cim.unheardmidi;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;

public class BleDeviceService extends MidiDeviceService {
    private static final String TAG = "MidiSynthDeviceService";
    private BleMidiReceiver bleMidiReceiver = new BleMidiReceiver();
    private boolean synthStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        bleMidiReceiver.stop();
        super.onDestroy();
    }

    @Override
    // Declare the receivers associated with your input ports.
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { bleMidiReceiver };
    }

    /**
     * This will get called when clients connect or disconnect.
     * You can use it to turn on your synth only when needed.
     */
    @Override
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
        if (status.isInputPortOpen(0) && !synthStarted) {
            bleMidiReceiver.start();
            synthStarted = true;
        } else if (!status.isInputPortOpen(0) && synthStarted){
            bleMidiReceiver.stop();
            synthStarted = false;
        }
    }
}

