package io.github.angelclient.client.util.nanovg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.lwjgl.nanovg.NanoVGGL2;

import lombok.Getter;

public class NanoVGManager {

    @Getter
    protected static long nvg = 0L;

    /**
     * DEVE ser chamado no PostGameStartEvent (após GL ativo), não no init().
     * Tenta criar o contexto NanoVG em 3 estratégias progressivas.
     * Em caso de falha, nvg permanece 0L e o fallback GL cuida da renderização.
     */
    public static void createContext() throws IOException {
        // Estratégia 1: tenta sem nada — se o launcher já extraiu os nativos
        // para java.library.path, nvgCreate funciona direto.
        try {
            long handle = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS | NanoVGGL2.NVG_STENCIL_STROKES);
            if (handle != 0L) {
                nvg = handle;
                System.out.println("[AngelClient] NanoVG criado via library.path existente.");
                return;
            }
        } catch (Throwable ignored) {}

        // Estratégia 2: extrai os nativos dos JARs do classpath/Gradle cache e
        // carrega manualmente antes de tentar novamente.
        try {
            io.github.angelclient.client.util.Lwjgl3NativesExtractor.extract();
            long handle = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS | NanoVGGL2.NVG_STENCIL_STROKES);
            if (handle != 0L) {
                nvg = handle;
                System.out.println("[AngelClient] NanoVG criado após extração de nativos.");
                return;
            }
        } catch (Throwable ignored) {}

        // Estratégia 3: tenta extrair o nativo do próprio JAR do mod (recursos embutidos).
        try {
            loadFromResources();
            long handle = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS | NanoVGGL2.NVG_STENCIL_STROKES);
            if (handle != 0L) {
                nvg = handle;
                System.out.println("[AngelClient] NanoVG criado após extração do JAR do mod.");
                return;
            }
        } catch (Throwable ignored) {}

        // Todas as estratégias falharam.
        System.err.println("[AngelClient] AVISO: NanoVG não pôde ser inicializado. " +
                "O menu usará renderização GL de fallback (sem bordas arredondadas).");
        nvg = 0L;
        // Não lança exceção — o fallback no render cuida do resto.
    }

    /**
     * Inicia um frame NanoVG. DEVE ser chamado antes de qualquer nvgBeginPath/Fill/Stroke
     * e DEVE ser pareado com endFrame(). Coordenadas em pixels lógicos (escala 2).
     *
     * @param displayW largura do display em pixels lógicos
     * @param displayH altura do display em pixels lógicos
     */
    public static void beginFrame(int displayW, int displayH) {
        if (nvg == 0L) return;
        org.lwjgl.nanovg.NanoVG.nvgBeginFrame(nvg, displayW, displayH, 1.0f);
    }

    /**
     * Finaliza o frame NanoVG e envia os comandos para a GPU.
     * Restaura o estado GL alterado pelo NanoVG (blending, stencil, etc.).
     */
    public static void endFrame() {
        if (nvg == 0L) return;
        org.lwjgl.nanovg.NanoVG.nvgEndFrame(nvg);
        // NanoVG altera vários estados GL — restaura o mínimo necessário para o MC
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Tenta carregar lwjgl_nanovg nativo a partir de recursos embutidos no JAR do mod.
     * Espera encontrar o arquivo em /natives/<os>/<arch>/lwjgl_nanovg.dll (ou .so/.dylib).
     */
    private static void loadFromResources() throws Exception {
        String os   = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch",  "").toLowerCase();

        String osDir   = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";
        String archDir = arch.contains("64") ? "x64" : "x86";
        String suffix  = os.contains("win") ? ".dll" : os.contains("mac") ? ".dylib" : ".so";
        String prefix  = os.contains("win") ? "" : "lib";

        // Tenta primeiro lwjgl (core) depois lwjgl_nanovg
        for (String base : new String[]{"lwjgl", "lwjgl_nanovg"}) {
            String resource = "/natives/" + osDir + "/" + archDir + "/" + prefix + base + suffix;
            InputStream in  = NanoVGManager.class.getResourceAsStream(resource);
            if (in == null) continue;

            File tmp = File.createTempFile("angel_nvg_" + base + "_", suffix);
            tmp.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            } finally {
                in.close();
            }

            try {
                System.load(tmp.getAbsolutePath());
            } catch (UnsatisfiedLinkError e) {
                // Pode já estar carregado — não é fatal
            }
        }
    }

    public static void closeContext() {
        if (nvg != 0L) {
            NanoVGGL2.nvgDelete(nvg);
            nvg = 0L;
        }
    }
}