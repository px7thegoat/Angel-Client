package io.github.angelclient.client.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.client.settings.KeyBinding;

import java.io.IOException;

public class KeyBindingTypeAdapter extends TypeAdapter<KeyBinding> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!KeyBinding.class.isAssignableFrom(type.getRawType())) return null;
            return (TypeAdapter<T>) new KeyBindingTypeAdapter();
        }
    };

    @Override
    public void write(JsonWriter out, KeyBinding value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getKeyCode());
        }
    }

    @Override
    public KeyBinding read(JsonReader in) throws IOException {
        // Retorna apenas o keyCode — o objeto real é restaurado em onRegister()
        // Esse método não deve ser chamado diretamente; veja abaixo.
        in.skipValue();
        return null;
    }
}