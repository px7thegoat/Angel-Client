package io.github.angelclient.client.util;

import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Lwjgl2FunctionProvider implements FunctionProvider {

    /**
     * Resolve uma função OpenGL pelo nome (String).
     * Sem @Override — esta assinatura pode não existir na interface
     * dependendo da versão do LWJGL 3 usada.
     */
    public long getFunctionAddress(String functionName) {
        try {
            Method method = org.lwjgl.opengl.GLContext.class.getMethod("getFunctionAddress", String.class);
            return (Long) method.invoke(null, functionName);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * LWJGL 3 chama este método internamente (ByteBuffer null-terminated UTF-8).
     * Sem @Override para compatibilidade com diferentes versões do LWJGL 3.
     */
    public long getFunctionAddress(ByteBuffer functionName) {
        ByteBuffer copy = functionName.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);

        // Remove o null terminator se presente
        int len = bytes.length;
        while (len > 0 && bytes[len - 1] == 0) {
            len--;
        }

        String name = new String(bytes, 0, len, StandardCharsets.UTF_8);
        return getFunctionAddress(name);
    }

    public void close() {
        // Não precisa fazer nada
    }
}