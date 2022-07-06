package uk.ac.warwick.cim.unheardmidi;

import android.media.midi.MidiInputPort;
import android.util.Log;

/**
 * Class to set up some simple Bluetooth notes
 */
public class MidiNotes {

    public void bleMidiNote (MidiInputPort inputPort) {
        try {
            final long NANOS_PER_SECOND = 1000000000L;
            long now = System.nanoTime();
            long future = now + (2 * NANOS_PER_SECOND);

            byte[] buffer = new byte[32];
            int numBytes = 0;
            int channel = 1; // MIDI channels 1-16 are encoded as 0-15.
            buffer[numBytes++] = (byte) (0x90 + (channel - 1)); // note on
            buffer[numBytes++] = (byte) 60; // pitch is middle C
            buffer[numBytes++] = (byte) 32; // play half velocity
            int offset = 0;
            // post is non-blocking
            inputPort.send(buffer, offset, numBytes, future);
        } catch (Exception e) {
            Log.e("MIDI", e.getMessage());
        }
    }

    public void wifiMidiNote (MidiInputPort inputPort) {
        try {
            final long NANOS_PER_SECOND = 1000000000L;
            long now = System.nanoTime();
            long future = now + (2 * NANOS_PER_SECOND);

            byte[] buffer = new byte[32];
            int numBytes = 0;
            int channel = 3; // MIDI channels 1-16 are encoded as 0-15.
            buffer[numBytes++] = (byte) (0x90 + (channel - 1)); // note on
            buffer[numBytes++] = (byte) 20;
            buffer[numBytes++] = (byte) 16;
            int offset = 0;
            // post is non-blocking
            inputPort.send(buffer, offset, numBytes, future);
        } catch (Exception e) {
            Log.e("MIDI", e.getMessage());
        }
    }
}
