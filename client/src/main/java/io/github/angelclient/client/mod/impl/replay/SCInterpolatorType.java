package io.github.angelclient.client.mod.impl.replay;

import com.replaymod.replaystudio.util.I18n;


public enum SCInterpolatorType {
	CATMULL,
	CUBIC,
	LINEAR;

	@Override
	public String toString() {
		return I18n.format("angel_client.mod.replay.interpolator." + name().toLowerCase());
	}

}
