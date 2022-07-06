package uk.ac.warwick.cim.unheardmidi;

import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.util.Log;

import java.io.IOException;

class BleMidiReceiver extends MidiReceiver {

    Tone tone = new Tone();

    public void onSend(byte[] data, int offset,
                       int count, long timestamp) throws IOException {
        // parse MIDI or whatever
        Log.i("BLEReceiver", "bytes " + data);
    }

    private double midiToNote (int midiNote) {
        double freq = tone.frequencyfromKey(midiNote);
        return freq;
    }

    private void playNote() {
        tone.playTone();
    }

    public void start () {}
    public void stop () {}
}


