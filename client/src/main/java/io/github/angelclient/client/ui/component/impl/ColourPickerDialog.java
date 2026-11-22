package io.github.angelclient.client.ui.component.impl;

import java.awt.Color;
import java.util.function.Consumer;

import io.github.angelclient.client.mod.ModOption;
import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.AlignedBoundsController;
import io.github.angelclient.client.ui.component.controller.AnimatedColourController;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Alignment;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * ColourPickerDialog — redesenhado.
 *
 * Layout (280 × 210):
 *   ┌─────────────────────────────────────┐
 *   │  [título centralizado]              │
 *   │                                     │
 *   │  ┌──────────────────────┐  ┌──────┐ │
 *   │  │  SV square (144×144) │  │  Hue  │ │
 *   │  │                      │  │ strip │ │
 *   │  └──────────────────────┘  └──────┘ │
 *   │                                     │
 *   │  A ████████████████████████  255    │
 *   │                                     │
 *   │  [preview]  #RRGGBBAA   [Done]      │
 *   └─────────────────────────────────────┘
 *
 * Interações:
 *   • Arrastar no quadrado SV → ajusta Saturation + Value
 *   • Arrastar na faixa Hue  → ajusta Hue
 *   • Arrastar no slider A   → ajusta Alpha
 *   • Campo hex editável
 */
public class ColourPickerDialog extends Component {

    // ── Dimensões ─────────────────────────────────────────────────────────────
    private static final int W          = 280;
    private static final int H          = 218;

    // SV square
    private static final int SV_X       = 12;
    private static final int SV_Y       = 26;
    private static final int SV_SIZE    = 140;

    // Hue strip (direita do SV)
    private static final int HUE_X      = SV_X + SV_SIZE + 8;
    private static final int HUE_Y      = SV_Y;
    private static final int HUE_W      = 16;
    private static final int HUE_H      = SV_SIZE;

    // Alpha slider
    private static final int A_X        = SV_X;
    private static final int A_Y        = SV_Y + SV_SIZE + 10;
    private static final int A_W        = HUE_X + HUE_W - SV_X;
    private static final int A_H        = 10;

    // Bottom bar
    private static final int BOT_Y      = A_Y + A_H + 10;

    // ── Cores UI ─────────────────────────────────────────────────────────────
    private static final int BG         = 0xFF111111;
    private static final int BORDER     = 0xFF2A2A2A;
    private static final int LABEL_COL  = 0xFFAAAAAA;
    private static final int CURSOR_COL = 0xFFFFFFFF;

    // ── Estado ────────────────────────────────────────────────────────────────
    private float hue, sat, val;
    private int   alpha;
    private Colour colour;
    private final Consumer<Colour> callback;

    // drag state: 0=none 1=sv 2=hue 3=alpha
    private int drag = 0;

    private final TextFieldComponent hex;

    public ColourPickerDialog(ModOption colourOption, Colour colour, Consumer<Colour> callback) {
        // Dimensões fixas do dialog

        this.colour   = colour;
        this.callback = callback;

        // decompose initial colour to HSB + alpha
        float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
        hue = hsb[0];
        sat = hsb[1];
        val = hsb[2];
        alpha = colour.getAlpha();

        // ── Title ─────────────────────────────────────────────────────────────
        add(new LabelComponent(
                        (comp, def) -> colourOption.getName(),
                        (comp, def) -> new Colour(LABEL_COL)),
                new AlignedBoundsController(Alignment.CENTRE, Alignment.START,
                        (c, b) -> new Rectangle(b.getX(), b.getY() + 9, b.getWidth(), b.getHeight())));

        // ── Hex field ─────────────────────────────────────────────────────────
        add(hex = new TextFieldComponent(76, true),
                (c, b) -> new Rectangle(
                        SV_X + 20,
                        BOT_Y + 2,
                        76, 12));

        // ── Done button ───────────────────────────────────────────────────────
        add(ButtonComponent.done(() -> {
            hex.flush();
            parent.setDialog(null);
        }), (c, b) -> new Rectangle(SV_X + 20 + 76 + 8, BOT_Y - 2,
                b.getWidth(), b.getHeight()));

        // ── Apply-to-all (se aplicável) ───────────────────────────────────────
        if (colourOption.canApplyToAll()) {
            add(new ButtonComponent("angel_client.mod.screen.apply_to_all",
                            new AnimatedColourController(
                                    (component, defaultColour) -> component.isHovered() ? Colour.BLUE_HOVER : Colour.BLUE))
                            .withIcon("angel_client_new")
                            .onClick((info, button) -> {
                                if (button == 0) {
                                    hex.flush();
                                    parent.setDialog(null);
                                    Utils.playClickSound(true);
                                    colourOption.applyToAll();
                                    return true;
                                }
                                return false;
                            }),
                    new AlignedBoundsController(Alignment.CENTRE, Alignment.END,
                            (c, b) -> new Rectangle(b.getX() + 60, b.getY() - 5, b.getWidth(), b.getHeight())));
        }

        // ── Preview box ───────────────────────────────────────────────────────
        add(new ColourBoxComponent((c, def) -> this.colour, null),
                (c, b) -> new Rectangle(SV_X, BOT_Y, 14, 14));

        // ── Hex callbacks ─────────────────────────────────────────────────────
        updateHex();
        hex.onUpdate(text -> {
            Colour parsed = Colour.fromHexString(text);
            if (parsed != null) {
                applyColour(parsed);
                updateHex();
            }
            return parsed != null;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyColour(Colour c) {
        this.colour = c;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue   = hsb[0];
        sat   = hsb[1];
        val   = hsb[2];
        alpha = c.getAlpha();
        callback.accept(this.colour);
    }

    private void rebuildColour() {
        int rgb   = Color.HSBtoRGB(hue, sat, val);
        int r     = (rgb >> 16) & 0xFF;
        int g     = (rgb >> 8)  & 0xFF;
        int b     = rgb & 0xFF;
        colour    = new Colour(r, g, b, alpha);
        callback.accept(colour);
        updateHex();
    }

    private void updateHex() {
        hex.setText(colour.toHexString());
    }

    // ── Render ────────────────────────────────────────────────────────────────


    private void renderFallback(ComponentRenderInfo info) {
        // background + border
        fillRounded(0, 0, W, H, 8, BG);
        outlineRounded(0, 0, W, H, 8, BORDER);
    }

    @Override
    public void render(ComponentRenderInfo info) {
        // Desenha fundo sempre, antes de tudo (evita o path de textura do ScaledIconComponent)
        renderFallback(info);
        super.render(info);

        int mx = info.getRelativeMouseX();
        int my = info.getRelativeMouseY();

        // ── Handle drag ───────────────────────────────────────────────────────
        if (drag == 1) {
            sat = MathHelper.clamp_float((float)(mx - SV_X) / SV_SIZE, 0f, 1f);
            val = 1f - MathHelper.clamp_float((float)(my - SV_Y) / SV_SIZE, 0f, 1f);
            rebuildColour();
        } else if (drag == 2) {
            hue = MathHelper.clamp_float((float)(my - HUE_Y) / HUE_H, 0f, 1f);
            rebuildColour();
        } else if (drag == 3) {
            alpha = MathHelper.clamp_int(Math.round((float)(mx - A_X) / A_W * 255), 0, 255);
            rebuildColour();
        }

        // ── SV square ─────────────────────────────────────────────────────────
        drawSVSquare();

        // cursor SV
        int cx = SV_X + Math.round(sat * SV_SIZE);
        int cy = SV_Y + Math.round((1f - val) * SV_SIZE);
        drawCircleCursor(cx, cy, 5);

        // ── Hue strip ─────────────────────────────────────────────────────────
        drawHueStrip();

        // cursor hue
        int hy = HUE_Y + Math.round(hue * HUE_H);
        fillRect(HUE_X - 2, hy - 1, HUE_W + 4, 2, CURSOR_COL);

        // ── Alpha slider ──────────────────────────────────────────────────────
        // tabuleiro de xadrez
        for (int tx = A_X; tx < A_X + A_W; tx += 6) {
            boolean evenCol = ((tx - A_X) / 6) % 2 == 0;
            fillRect(tx, A_Y, Math.min(6, A_X + A_W - tx), A_H / 2,
                    evenCol ? 0xFF606060 : 0xFF404040);
            fillRect(tx, A_Y + A_H / 2, Math.min(6, A_X + A_W - tx), A_H / 2,
                    evenCol ? 0xFF404040 : 0xFF606060);
        }
        // gradient alpha
        drawAlphaGradient(A_X, A_Y, A_W, A_H);
        // cursor alpha
        int ax = A_X + Math.round((float) alpha / 255 * A_W);
        fillRect(ax - 1, A_Y - 2, 2, A_H + 4, CURSOR_COL);

        // ── Labels ────────────────────────────────────────────────────────────
        renderLabel("S", SV_X - 10, SV_Y - 1);
        renderLabel("V", SV_X - 1,  SV_Y + SV_SIZE + 2);
        renderLabel("H",  HUE_X + HUE_W / 2 - 2, HUE_Y - 9);
        renderLabel("A",  A_X - 10, A_Y + 1);
        renderLabel(Integer.toString(alpha), A_X + A_W + 3, A_Y + 1);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawSVSquare() {
        int pureHue = Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        // Usa blend normal — GL_ZERO/GL_SRC_COLOR (multiply) escurecia tudo no fundo preto
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        // Passo 1: branco (esquerda) -> hue puro (direita) — representa saturacao
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addVertex(wr, SV_X,           SV_Y,           0xFFFFFFFF);
        addVertex(wr, SV_X + SV_SIZE, SV_Y,           pureHue);
        addVertex(wr, SV_X + SV_SIZE, SV_Y + SV_SIZE, pureHue);
        addVertex(wr, SV_X,           SV_Y + SV_SIZE, 0xFFFFFFFF);
        tess.draw();

        // Passo 2: transparente (topo) -> preto opaco (baixo) — representa value
        // Com alpha blend normal isso funciona corretamente sobre qualquer fundo
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addVertex(wr, SV_X,           SV_Y,           0x00000000);
        addVertex(wr, SV_X + SV_SIZE, SV_Y,           0x00000000);
        addVertex(wr, SV_X + SV_SIZE, SV_Y + SV_SIZE, 0xFF000000);
        addVertex(wr, SV_X,           SV_Y + SV_SIZE, 0xFF000000);
        tess.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private void drawHueStrip() {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        int steps = HUE_H;
        for (int i = 0; i < steps; i++) {
            int c0 = Color.HSBtoRGB((float) i       / steps, 1f, 1f) | 0xFF000000;
            int c1 = Color.HSBtoRGB((float)(i + 1)  / steps, 1f, 1f) | 0xFF000000;
            addVertex(wr, HUE_X,          HUE_Y + i,     c0);
            addVertex(wr, HUE_X + HUE_W,  HUE_Y + i,     c0);
            addVertex(wr, HUE_X + HUE_W,  HUE_Y + i + 1, c1);
            addVertex(wr, HUE_X,          HUE_Y + i + 1, c1);
        }
        tess.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private void drawAlphaGradient(int x, int y, int w, int h) {
        int rgb = Color.HSBtoRGB(hue, sat, val);
        int opaque = (rgb & 0x00FFFFFF) | 0xFF000000;
        int transp = rgb & 0x00FFFFFF;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addVertex(wr, x,     y,     transp);
        addVertex(wr, x + w, y,     opaque);
        addVertex(wr, x + w, y + h, opaque);
        addVertex(wr, x,     y + h, transp);
        tess.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private void addVertex(WorldRenderer wr, int x, int y, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        wr.pos(x, y, 0).color(r, g, b, a).endVertex();
    }

    private void fillRect(int x, int y, int w, int h, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int bv =  argb       & 0xFF;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x,     y,     0).color(r, g, bv, a).endVertex();
        wr.pos(x + w, y,     0).color(r, g, bv, a).endVertex();
        wr.pos(x + w, y + h, 0).color(r, g, bv, a).endVertex();
        wr.pos(x,     y + h, 0).color(r, g, bv, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    /** Círculo cursor desenhado com linha de pixels. */
    private void drawCircleCursor(int cx, int cy, int r) {
        // outer dark ring
        drawRing(cx, cy, r + 1, 0xFF000000);
        // white ring
        drawRing(cx, cy, r, CURSOR_COL);
    }

    private void drawRing(int cx, int cy, int r, int argb) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        int a = (argb >> 24) & 0xFF;
        int rv = (argb >> 16) & 0xFF;
        int g  = (argb >>  8) & 0xFF;
        int b  =  argb & 0xFF;
        int segs = 24;
        for (int i = 0; i < segs; i++) {
            double angle = 2 * Math.PI * i / segs;
            wr.pos(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, 0)
              .color(rv, g, b, a).endVertex();
        }
        tess.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
    }

    private void fillRounded(int x, int y, int w, int h, int r, int argb) {
        int a = (argb >> 24) & 0xFF; int rv = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF; int b  =  argb & 0xFF;
        net.minecraft.client.gui.Gui.drawRect(x + r, y,     x + w - r, y + h,     argb);
        net.minecraft.client.gui.Gui.drawRect(x,     y + r, x + r,     y + h - r, argb);
        net.minecraft.client.gui.Gui.drawRect(x + w - r, y + r, x + w, y + h - r, argb);
    }

    private void outlineRounded(int x, int y, int w, int h, int r, int argb) {
        net.minecraft.client.gui.Gui.drawRect(x,         y,         x + w, y + 1,     argb);
        net.minecraft.client.gui.Gui.drawRect(x,         y + h - 1, x + w, y + h,     argb);
        net.minecraft.client.gui.Gui.drawRect(x,         y,         x + 1, y + h,     argb);
        net.minecraft.client.gui.Gui.drawRect(x + w - 1, y,         x + w, y + h,     argb);
    }

    private void renderLabel(String s, int x, int y) {
        font.renderString(s, x, y, LABEL_COL);
    }

    @Override
    protected Rectangle getDefaultBounds() {
        return new Rectangle(0, 0, W, H);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(ComponentRenderInfo info, int button) {
        if (button != 0) return super.mouseClicked(info, button);

        int mx = info.getRelativeMouseX();
        int my = info.getRelativeMouseY();

        if (mx >= SV_X && mx <= SV_X + SV_SIZE && my >= SV_Y && my <= SV_Y + SV_SIZE) {
            drag = 1; Utils.playClickSound(true); return true;
        }
        if (mx >= HUE_X && mx <= HUE_X + HUE_W && my >= HUE_Y && my <= HUE_Y + HUE_H) {
            drag = 2; Utils.playClickSound(true); return true;
        }
        if (mx >= A_X && mx <= A_X + A_W && my >= A_Y && my <= A_Y + A_H) {
            drag = 3; Utils.playClickSound(true); return true;
        }

        return super.mouseClicked(info, button);
    }

    @Override
    public boolean mouseReleasedAnywhere(ComponentRenderInfo info, int button, boolean inside) {
        if (button == 0 && drag != 0) { drag = 0; return true; }
        return super.mouseReleasedAnywhere(info, button, inside);
    }
}