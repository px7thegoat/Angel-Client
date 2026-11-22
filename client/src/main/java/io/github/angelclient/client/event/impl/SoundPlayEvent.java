package io.github.angelclient.client.event.impl;


public class SoundPlayEvent {

	public String soundName;
	public float volume;
	public float pitch;
	public float originalVolume;
	public float originalPitch;

	/**
	 * Sets volume to <code>0</code>.
	 */
	public void cancel() {
		volume = 0;
	}


	public SoundPlayEvent(String soundName, float volume, float pitch, float originalVolume, float originalPitch) {
		this.soundName = soundName;
		this.volume = volume;
		this.pitch = pitch;
		this.originalVolume = originalVolume;
		this.originalPitch = originalPitch;
	}

}