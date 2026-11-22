package io.github.angelclient.client.util;

import com.google.gson.*;
import net.minecraft.client.settings.KeyBinding;
import java.lang.reflect.Type;

public class KeyBindingGsonAdapter implements JsonSerializer<KeyBinding>, JsonDeserializer<KeyBinding> {

    @Override
    public JsonElement serialize(KeyBinding src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getKeyCode());
    }

    @Override
    public KeyBinding deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Não criamos um novo KeyBinding aqui.
        // Retornamos um "marcador" que será usado pelo postLoad abaixo.
        // Veja nota sobre o padrão alternativo.
        return null; // Tratado via postLoad no onRegister()
    }
}