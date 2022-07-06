package uk.ac.warwick.cim.unheardmidi;

import org.junit.Test;
import uk.ac.warwick.cim.unheardmidi.Tone;

import static org.junit.Assert.assertEquals;

public class ToneTest {

    @Test
    public void midiToFreq_isCorrect() {
        Tone tone = new Tone();
        double frequency = tone.midiToFrequency(60);
        assertEquals(261.3, frequency, 0.5);
    }

    @Test
    public void toneCreatesObjects() {
        Tone tone = new Tone();
        double frequency = tone.midiToFrequency(60);
        assertEquals(261.3, frequency, 0.5);
    }

    @Test
    public void frequencyFromKey() {
        Tone tone = new Tone();
        double frequency = tone.frequencyfromKey(440.0);
        assertEquals(261.3, frequency, 0.5);
    }
}
