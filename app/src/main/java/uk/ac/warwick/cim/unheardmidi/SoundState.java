package uk.ac.warwick.cim.unheardmidi;

public class SoundState {

    public enum SoundStates {
        TONES, MIDI
    }

    SoundStates soundState;

    public SoundStates getState() {
        return soundState;
    }

    public void setState(SoundStates state){
        soundState = state;
    }
}
