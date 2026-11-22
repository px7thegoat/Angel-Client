package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.mod.hud.HudMod;
import io.github.angelclient.client.util.data.Position;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

import io.github.angelclient.client.mod.hud.HudPosition;

public class TitlePositionMod extends HudMod {

    @Expose @Option
    @Slider(min = 0.5f, max = 5, step = 0.1f, format = "angel_client.slider.factor")
    public float titleScale = 2.0f;

    @Expose @Option
    @Slider(min = 0.5f, max = 3, step = 0.1f, format = "angel_client.slider.factor")
    public float subtitleScale = 1.2f;

    @Expose @Option
    public boolean shadow = true;

    // Timer e textos
    private long displayUntil = 0;
    private String currentTitle = "";
    private String currentSubtitle = "";

    @Override
    public String getId() {
        return "titleposition"; // ID exato que o Mixin procura
    }

    /**
     * Chamado pelo Mixin quando um título é recebido
     */
    public void setTitle(String title, String subtitle, int ticks) {
        // Se ambos vierem nulos e ticks == 0, é um comando de limpar
        if (title == null && subtitle == null && ticks == 0) {
            this.currentTitle = "";
            this.currentSubtitle = "";
            this.displayUntil = 0;
            return;
        }

        // Atualiza apenas os textos que foram recebidos
        if (title != null) this.currentTitle = title;
        if (subtitle != null) this.currentSubtitle = subtitle;
        
        // Converte os ticks do Minecraft (20 ticks = 1s) para milissegundos
        if (ticks > 0) {
            this.displayUntil = System.currentTimeMillis() + (ticks * 50L);
        } else if (title != null) {
            // Fallback caso o tempo venha zerado apenas se for um novo título
            this.displayUntil = System.currentTimeMillis() + 3000L;
        }
    }

    @Override
    public void render(Position position, boolean editMode) {
        // 1. Se estiver no modo de edição, ignoramos o cronômetro
        boolean isExpired = System.currentTimeMillis() > displayUntil;
        if (isExpired && !editMode) return;

        // 2. DEFINIÇÃO DO TEXTO FIXO:
        // Se editMode for true, ele usa o texto de exemplo. Se for false, usa o que veio do servidor.
        String title = editMode ? "Título Exemplo" : currentTitle;
        String subtitle = editMode ? "Subtítulo Exemplo" : currentSubtitle;

        // 3. Se não houver nada para mostrar e não estiver editando, paramos aqui
        if (title.isEmpty() && subtitle.isEmpty() && !editMode) return;

        FontRenderer fr = mc.fontRendererObj;
        GlStateManager.pushMatrix();

        float globalScale = getScale();
        GlStateManager.translate(position.getX(), position.getY(), 0);
        GlStateManager.scale(globalScale, globalScale, 1);

        // Render do Título
        if (!title.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(titleScale, titleScale, 1);
            int tw = fr.getStringWidth(title);
            if (shadow) fr.drawStringWithShadow(title, -tw / 2f, 0, -1);
            else fr.drawString(title, (int)(-tw / 2f), 0, -1);
            GlStateManager.popMatrix();
        }

        // Render do Subtítulo
        if (!subtitle.isEmpty()) {
            GlStateManager.pushMatrix();
            float spacing = 10 * titleScale;
            GlStateManager.translate(0, spacing, 0);
            GlStateManager.scale(subtitleScale, subtitleScale, 1);
            int sw = fr.getStringWidth(subtitle);
            if (shadow) fr.drawStringWithShadow(subtitle, -sw / 2f, 0, -1);
            else fr.drawString(subtitle, (int)(-sw / 2f), 0, -1);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    @Override
    public Rectangle getBounds(Position position) {
        FontRenderer fr = mc.fontRendererObj;
        
        // Importante: O retângulo de seleção também precisa saber o tamanho do texto fixo!
        String title = currentTitle.isEmpty() ? "Título Exemplo" : currentTitle;
        String subtitle = currentSubtitle.isEmpty() ? "Subtítulo Exemplo" : currentSubtitle;

        float globalScale = getScale();
        int titleWidth = (int)(fr.getStringWidth(title) * titleScale * globalScale);
        int subtitleWidth = (int)(fr.getStringWidth(subtitle) * subtitleScale * globalScale);
        int width = Math.max(titleWidth, subtitleWidth);

        int titleHeight = (int)(fr.FONT_HEIGHT * titleScale * globalScale);
        int subtitleHeight = (int)(fr.FONT_HEIGHT * subtitleScale * globalScale);
        int spacing = (int)(10 * titleScale * globalScale);
        return new Rectangle((int)(position.getX() - width / 2f), (int)position.getY(), width, titleHeight + spacing + subtitleHeight);
    }

    @Override
    public HudPosition getDefaultPosition() {
        return new HudPosition(0.5f, 0.3f);
    }
}