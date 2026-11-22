package io.github.angelclient.client.mod.impl.replay.fix;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.annotation.ForgeCompat;
import net.minecraft.client.settings.KeyBinding;

@Deprecated
@ForgeCompat
public class SCClientRegistry {

	public static void registerKeyBinding(KeyBinding keyBinding) {
		Client.INSTANCE.registerKeyBinding(keyBinding);
	}

}
