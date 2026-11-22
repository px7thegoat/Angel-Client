package io.github.angelclient.client.nametag;

import net.minecraft.util.ResourceLocation;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Representa uma nametag do Angel Client.
 *
 * CORREÇÃO DE TRAVAMENTOS:
 *  - getTexture() retorna null enquanto o download ainda não terminou.
 *  - Só retorna a ResourceLocation depois que a textura foi registrada
 *    no TextureManager (via callback em NametagAPI.downloadTexture).
 *  - AtomicBoolean garante que o download só é disparado uma vez,
 *    mesmo que getTexture() seja chamado várias vezes antes do término.
 */
public class Nametag {

    private static final String BASE_URL = io.github.angelclient.client.cosmetic.CosmeticAPI.GITHUB_BASE_URL;

    private final String id;
    private final String displayName;
    private final String file;
    private final int color;
    private final boolean premium;

    /** ResourceLocation alocada antecipadamente (só o "nome", sem dados de pixel ainda). */
    private final ResourceLocation location;

    /** true apenas depois que NametagAPI registrou a textura no TextureManager. */
    private volatile boolean textureReady = false;

    /** Impede disparar dois downloads simultâneos. */
    private final AtomicBoolean downloadStarted = new AtomicBoolean(false);

    public Nametag(String id, String displayName, String file, int color, boolean premium) {
        this.id          = id;
        this.displayName = displayName;
        this.file        = file;
        this.color       = color;
        this.premium     = premium;
        this.location    = new ResourceLocation("angelclient", "nametags/" + id);
    }

    // ── Getters básicos ───────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getFile()        { return file; }
    public int    getColor()       { return color; }
    public boolean isPremium()     { return premium; }

    // ── Textura ───────────────────────────────────────────────────────────────

    /**
     * Retorna a ResourceLocation somente depois que a textura estiver registrada.
     * Retorna null enquanto o download ainda está em andamento.
     * O download é disparado automaticamente na primeira chamada.
     */
    public ResourceLocation getTexture() {
        if (textureReady) {
            return location;
        }

        // Dispara o download apenas uma vez
        if (file != null && downloadStarted.compareAndSet(false, true)) {
            String fullUrl = BASE_URL + file;
            NametagAPI.downloadTexture(fullUrl, location, () -> textureReady = true);
        }

        return null; // ainda não está pronta — o caller deve usar o fallback
    }

    /**
     * Permite verificar se a textura já foi carregada sem disparar download.
     */
    public boolean isTextureReady() {
        return textureReady;
    }
}
