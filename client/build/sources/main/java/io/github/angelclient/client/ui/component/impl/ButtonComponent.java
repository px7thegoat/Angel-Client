package io.github.angelclient.client.ui.component.impl;

import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.AlignedBoundsController;
import io.github.angelclient.client.ui.component.controller.AnimatedColourController;
import io.github.angelclient.client.ui.component.controller.Controller;
import io.github.angelclient.client.ui.component.handler.ClickHandler;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Alignment;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import io.github.angelclient.client.util.render.GLRectUtils;
import net.minecraft.client.resources.I18n;

/**
 * Botão do Angel Client com renderizador de retângulos arredondados
 * e animação de hover portados do Melon Client (ElementModule/ElementButtonIcon).
 *
 * Hover fade: bgFade sobe de 0→70 em incrementos de 5 por tick (igual ao Melon).
 * Outline pulsa entre alpha normal e alpha+hover.
 */
public class ButtonComponent extends ColouredComponent {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final int COL_BG_IDLE    = 0xFF161616;
    private static final int COL_BG_HOV     = 0xFF222222;
    private static final int COL_OUTLINE     = 0xFF3C3C3C;
    private static final int COL_OUTLINE_HOV = 0xFFCCCCCC;
    private static final int COL_TEXT        = 0xFFEEEEEE;
    private static final int COL_TEXT_DIM    = 0xFF666666;

    private static final float RADIUS     = 4f;
    private static final float LINE_WIDTH = 1.5f;

    // ── Estado de animação (estilo Melon: bgFade por tick) ───────────────────
    private int   bgFade      = 0;    // 0..70  — opacidade do hover fill
    private float outlineFade = 0f;   // 0..255 — interpolação do outline

    private final Controller<String> text;
    private ButtonType type = ButtonType.NORMAL;
    private boolean enabled = true;

    public ButtonComponent(String text, Controller<Colour> colour) {
        this((component, defaultText) -> I18n.format(text), colour);
    }

    public ButtonComponent(Controller<String> text, Controller<Colour> colour) {
        super(colour);
        this.text = text;
        add(new LabelComponent(text), new AlignedBoundsController(Alignment.CENTRE, Alignment.CENTRE));
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(ComponentRenderInfo info) {
        Rectangle b = getRelativeBounds();
        int x  = b.getX(),  y  = b.getY();
        int x2 = b.getEndX(), y2 = b.getEndY();

        // 1. Fill de fundo (sempre presente, cor varia com hover)
        int bgCol = blendColor(COL_BG_IDLE, COL_BG_HOV, bgFade / 70f);
        GLRectUtils.drawRoundedRect(x, y, x2, y2, RADIUS, bgCol);

        // 2. Overlay de hover (igual ao Melon: preto com alpha animado)
        if (bgFade > 0) {
            GLRectUtils.drawRoundedRect(x, y, x2, y2, RADIUS,
                    (bgFade << 24) | 0x00FFFFFF & COL_BG_HOV);
        }

        // 3. Outline arredondado animado
        int outlineAlpha = (int)(80 + outlineFade * (255 - 80) / 255f);
        int outlineCol   = enabled
                ? ((outlineAlpha << 24) | (COL_OUTLINE_HOV & 0x00FFFFFF))
                : (COL_OUTLINE);
        GLRectUtils.drawRoundedOutline(x, y, x2, y2, RADIUS, LINE_WIDTH, outlineCol);

        // 4. Filhos (label)
        super.render(info);
    }

    // ── Tick: animação de fade (igual ao Melon — incremento fixo por tick) ───
    @Override
    public void tick() {
        super.tick();
        boolean hov = isHovered() && enabled;

        // bgFade: 0..70, passo 5 por tick
        if (hov  && bgFade + 5 <= 70)  bgFade += 5;
        if (!hov && bgFade - 5 >= 0)   bgFade -= 5;

        // outlineFade: 0..255, passo 12 por tick
        if (hov  && outlineFade + 12 <= 255) outlineFade += 12;
        if (!hov && outlineFade - 12 >= 0)   outlineFade -= 12;
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    @Override
    protected Rectangle getDefaultBounds() {
        return new Rectangle(0, 0, type.getWidth(), 20);
    }

    @Override
    public ButtonComponent onClick(ClickHandler onClick) {
        super.onClick(onClick);
        return this;
    }

    public ButtonComponent withIcon(String name) {
        add(new ScaledIconComponent(name, 16, 16),
                new AlignedBoundsController(Alignment.START, Alignment.CENTRE,
                        (component, defaultBounds) -> new Rectangle(
                                defaultBounds.getY(), defaultBounds.getY(),
                                defaultBounds.getWidth(), defaultBounds.getHeight())));
        return this;
    }

    public ButtonComponent type(ButtonType type) {
        this.type = type;
        return this;
    }

    public ButtonComponent setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // ── Factory (igual ao original) ───────────────────────────────────────────
    public static ButtonComponent done(Runnable onClick) {
        return new ButtonComponent("gui.done", new AnimatedColourController(
                (component, defaultColour) -> component.isHovered()
                        ? new Colour(20, 120, 20) : new Colour(0, 100, 0)))
                .onClick((info, button) -> {
                    if (button == 0) {
                        Utils.playClickSound(true);
                        onClick.run();
                        return true;
                    }
                    return false;
                }).withIcon("angel_client_tick");
    }

    // ── Helper: interpolação linear de dois ARGB ──────────────────────────────
    private static int blendColor(int from, int to, float t) {
        int aF = (from >> 24) & 0xFF, rF = (from >> 16) & 0xFF,
            gF = (from >>  8) & 0xFF, bF =  from        & 0xFF;
        int aT = (to   >> 24) & 0xFF, rT = (to   >> 16) & 0xFF,
            gT = (to   >>  8) & 0xFF, bT =  to          & 0xFF;
        int a = (int)(aF + (aT - aF) * t);
        int r = (int)(rF + (rT - rF) * t);
        int g = (int)(gF + (gT - gF) * t);
        int b = (int)(bF + (bT - bF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}