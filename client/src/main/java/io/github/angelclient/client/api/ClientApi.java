package io.github.angelclient.client.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.mod.Mod;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

public class ClientApi {

	private static final Logger LOGGER = LogManager.getLogger();

	@EventHandler
	public void onCustomPayload(S3FPacketCustomPayload payload) {
		if(payload.getChannelName().startsWith("angelclient:")) {
			String message = payload.getBufferData().readStringFromBuffer(32767);

			if(payload.getChannelName().equals("angelclient:block_mods")) {
				JsonArray array = JsonParser.parseString(message).getAsJsonArray();

				Client.INSTANCE.getMods().forEach(Mod::unblock);

				for(JsonElement element : array) {
					String modId = element.getAsString();

					Mod mod = Client.INSTANCE.getModById(modId);

					if(mod != null) {
						mod.block();
					}
					else {
						LOGGER.warn("Cannot block mod " + modId + ": not found");
					}
				}
			}
			else if(payload.getChannelName().equals("angelclient:popup")) {
				JsonObject data = JsonParser.parseString(message).getAsJsonObject();

				String text = data.get("text").getAsString();
				String command = data.get("command").getAsString();

				Client.INSTANCE.getPopupManager().add(new Popup(text, command));
			}
		}
	}

}
