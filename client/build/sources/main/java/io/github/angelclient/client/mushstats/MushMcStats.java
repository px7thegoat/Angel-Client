package io.github.angelclient.client.mushstats;


public class MushMcStats {
    private static final MushMcStats INSTANCE = new MushMcStats();

    public static MushMcStats getInstance() {
        return INSTANCE;
    }

    public void init() {
        try {

            
            System.out.println("[MushMcStats] Mod inicializado com sucesso!");
            System.out.println("[MushMcStats] Pressione M no MushMC para abrir stats!");
        } catch (Exception e) {
            System.err.println("[MushMcStats] Erro na inicialização:");
            e.printStackTrace();
        }
    }

    /**
     * Tick do jogo
     * CHAMADO PELO MixinMushMcStatsTick
     */
    public void onTick() {
        try {
            PlayerMonitor.getInstance().onClientTick();
        } catch (Exception e) {
            // Silencioso
        }
    }

    /**
     * Evento de teclado
     * CHAMADO PELO MixinMushMcStatsKeyboard
     */
    public void onKeyInput() {
        try {
            KeyHandler.getInstance().handleKey();
        } catch (Exception e) {
            // Silencioso
        }
    }
}