package io.github.angelclient.client.cosmetic;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gerencia cosméticos dos jogadores.
 *
 * CORREÇÃO DE TRAVAMENTOS:
 *  - availableCosmetics e playerEquippedCosmetics são ConcurrentHashMap:
 *    leitura na render thread (getPlayerCosmetic) é sempre lock-free e segura.
 *  - Todo acesso à rede está em um ScheduledExecutorService de thread única.
 *  - AtomicBoolean impede que dois refreshes simultâneos rodem ao mesmo tempo.
 *  - Threads marcadas como daemon: não impedem o JVM de encerrar.
 */
public class CosmeticManager {

    private static CosmeticManager INSTANCE;

    private final Map<String, Cosmetic> availableCosmetics     = new ConcurrentHashMap<>();
    private final Map<String, String>   playerEquippedCosmetics = new ConcurrentHashMap<>();

    private final CosmeticAPI api = new CosmeticAPI();

    // Intervalo de sincronização com o Firebase (ms)
    private static final long REFRESH_INTERVAL_SECONDS = 20L;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Cosmetic-Refresh");
                t.setDaemon(true); // encerra junto com o jogo
                return t;
            });

    // Evita overlap: se um refresh ainda está rodando, o próximo é ignorado
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    // ── Singleton ─────────────────────────────────────────────────────────────
    private CosmeticManager() {
        System.out.println("[Angel Client] CosmeticManager iniciado.");

        // Carga inicial em background
        scheduler.execute(this::doFullRefresh);

        // Refresh periódico
        scheduler.scheduleAtFixedRate(
                this::doSyncFirebase,
                REFRESH_INTERVAL_SECONDS,
                REFRESH_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static CosmeticManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CosmeticManager();
        }
        return INSTANCE;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // API pública — chamada da render thread — NUNCA faz rede aqui
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retorna o cosmético equipado por um jogador.
     * Leitura de ConcurrentHashMap: seguro na render thread, sem bloqueio.
     */
    public Cosmetic getPlayerCosmetic(String playerName) {
        String cosmeticId = playerEquippedCosmetics.get(playerName);
        if (cosmeticId == null) return null;
        return availableCosmetics.get(cosmeticId);
    }

    /** Lista todos os cosméticos que o jogador atual pode usar. */
    public List<Cosmetic> getCurrentPlayerCosmetics() {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        String uuid   = Minecraft.getMinecraft().getSession().getPlayerID();

        List<Cosmetic> allowed = new ArrayList<>();
        for (Cosmetic cosmetic : availableCosmetics.values()) {
            if (cosmetic.canPlayerUse(player, uuid)) {
                allowed.add(cosmetic);
            }
        }
        return allowed;
    }

    public Collection<Cosmetic> getAllCosmetics() {
        return availableCosmetics.values();
    }

    public String getEquippedCosmeticId() {
        String playerName = Minecraft.getMinecraft().getSession().getUsername();
        return playerEquippedCosmetics.get(playerName);
    }

    public boolean canCurrentPlayerUseMenu() {
        return true;
    }

    public boolean hasCurrentPlayerPremium() {
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operações de rede — sempre agendadas no scheduler
    // ══════════════════════════════════════════════════════════════════════════

    /** Força recarga completa (cosméticos + firebase). */
    public void refresh() {
        scheduler.execute(this::doFullRefresh);
    }

    /**
     * Alias mantido para compatibilidade com Client.java.
     * Equivalente a {@link #refresh()}.
     */
    public void loadCosmetics() {
        refresh();
    }

    public void equipCosmetic(String cosmeticId) {
        String playerName = Minecraft.getMinecraft().getSession().getUsername();

        if (cosmeticId == null) {
            unequipCosmetic();
            return;
        }

        if (!availableCosmetics.containsKey(cosmeticId)) return;

        Cosmetic cosmetic = availableCosmetics.get(cosmeticId);
        if (!cosmetic.canPlayerUse(playerName, null)) {
            System.out.println("[Angel Client] Jogador não pode usar este cosmético.");
            return;
        }

        playerEquippedCosmetics.put(playerName, cosmeticId);

        // Persiste no Firebase em background
        scheduler.execute(() -> {
            api.updatePlayerCosmetic(playerName, cosmeticId);
            // Aguarda 2s e re-sincroniza para confirmar
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            doSyncFirebase();
        });
    }

    public void unequipCosmetic() {
        String playerName = Minecraft.getMinecraft().getSession().getUsername();
        playerEquippedCosmetics.remove(playerName);
        scheduler.execute(() -> api.updatePlayerCosmetic(playerName, null));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Internos (executados apenas pelo scheduler)
    // ══════════════════════════════════════════════════════════════════════════

    /** Carrega lista de cosméticos disponíveis + sincroniza firebase. */
    private void doFullRefresh() {
        if (!refreshing.compareAndSet(false, true)) return;
        try {
            System.out.println("[Angel Client] Carregando cosméticos...");

            api.fetchCosmetics(cosmeticData -> {
                System.out.println("[Angel Client] " + cosmeticData.size() + " cosméticos recebidos.");

                for (Map<String, Object> cape : cosmeticData) {
                    try {
                        String id      = (String) cape.get("id");
                        String name    = (String) cape.get("name");
                        String file    = (String) cape.get("file");
                        boolean premium = (boolean) cape.getOrDefault("premium", false);

                        CapeCosmetic cosmetic = new CapeCosmetic(id, name, file, premium, api.getGithubBaseUrl());

                        @SuppressWarnings("unchecked")
                        List<String> allowedPlayers = (List<String>) cape.get("allowedPlayers");
                        if (allowedPlayers != null) cosmetic.setAllowedPlayers(allowedPlayers);

                        String requiredTag = (String) cape.get("requiredTag");
                        if (requiredTag != null) cosmetic.setRequiredTag(requiredTag);

                        availableCosmetics.put(id, cosmetic);
                    } catch (Exception e) {
                        System.err.println("[Angel Client] Erro ao parsear cosmético: " + e.getMessage());
                    }
                }
            });

            doSyncFirebase();

        } catch (Exception e) {
            System.err.println("[Angel Client] Erro no refresh de cosméticos: " + e.getMessage());
        } finally {
            refreshing.set(false);
        }
    }

    /** Sincroniza apenas os dados de jogadores com o Firebase. */
    private void doSyncFirebase() {
        try {
            String localPlayer = Minecraft.getMinecraft().getSession().getUsername();
            String localEquipped = playerEquippedCosmetics.get(localPlayer);
            api.fetchPlayerCosmetics(playerCapes -> {
                playerEquippedCosmetics.clear();
                playerEquippedCosmetics.putAll(playerCapes);
                if (localEquipped != null) {
                    playerEquippedCosmetics.put(localPlayer, localEquipped);
                }
            });
        } catch (Exception e) {
            System.err.println("[Angel Client] Erro ao sincronizar Firebase: " + e.getMessage());
        }
    }
}