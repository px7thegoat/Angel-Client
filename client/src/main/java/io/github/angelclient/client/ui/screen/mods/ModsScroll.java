package io.github.angelclient.client.ui.screen.mods;

import org.lwjgl.opengl.GL11;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModOption;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.impl.ScrollListComponent;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class ModsScroll extends ScrollListComponent {

    private ModsScreen.ModsScreenComponent screen;

    private static final int COLUMN_COUNT = 4;
    private static final int GAP_X        = 6;
    private static final int GAP_Y        = 6;

    public ModsScroll(ModsScreen.ModsScreenComponent screen) {
        this.screen = screen;
    }

    @Override
    public int getSpacing() { return 0; }

    @Override
    protected int getScrollStep() { return 22; }

    /**
     * Sobrescreve shouldScissor para desativar o scissor automático do framework,
     * que usa getScaleFactor() real e quebraria com a normalização para escala 2.
     * O scissor correto é aplicado manualmente em render() com escala 2.
     */
    @Override
    protected boolean shouldScissor() { return false; }

    /**
     * Aplica manualmente o GL_SCISSOR_TEST com escala 2 antes de renderizar,
     * garantindo que o conteúdo seja corretamente clipado independente da GUI Scale.
     */
    @Override
    public void render(ComponentRenderInfo info) {
        Rectangle bounds = getBounds();

        // Calcula posição absoluta percorrendo a hierarquia de pais
        int absX = bounds.getX();
        int absY = bounds.getY();
        io.github.angelclient.client.ui.component.Component p = parent;
        while (p != null && p.getParent() != null) {
            Rectangle pb = p.getBounds();
            absX += pb.getX();
            absY += pb.getY();
            p = p.getParent();
        }

        Minecraft mc = Minecraft.getMinecraft();
        int displayH = mc.displayHeight;
        // Com glScalef(TARGET_SCALE/realSf) ativo, coords de escala-2 × 2 = pixels físicos
        int sf = ModsScreen.TARGET_SCALE;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            absX * sf,
            displayH - (absY + bounds.getHeight()) * sf,
            bounds.getWidth() * sf,
            bounds.getHeight() * sf
        );

        super.render(info);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Recarrega o conteúdo do scroll de acordo com o estado atual:
     * - Se nenhum mod selecionado: grade de ModListing (cards de mods)
     * - Se mod selecionado: lista de ModOptionComponent (widgets de opção)
     */
    public void load() {
        clear();

        if (screen.getMod() == null) {
            // ── Grade de mods (4 colunas) ──────────────────────────────────────
            int scrollW = getBounds().getWidth();
            if (scrollW <= 0) scrollW = 300;

            int cardW = (scrollW - GAP_X * (COLUMN_COUNT - 1)) / COLUMN_COUNT;
            int cardH = (int)(cardW * 0.85f);

            int col = 0, row = 0;

            for (Mod mod : Client.INSTANCE.getMods()) {
                if (!mod.getName().toLowerCase()
                        .contains(screen.getQuery().toLowerCase())) continue;

                final int fc = col, fr = row;
                final int cw = cardW, ch = cardH;

                add(new ModListing(mod, screen),
                        (c, b) -> new Rectangle(
                                fc * (cw + GAP_X),
                                fr * (ch + GAP_Y),
                                cw, ch));

                col++;
                if (col >= COLUMN_COUNT) { col = 0; row++; }
            }

         } else {
            // ── Lista de opções com widgets reais ──────────────────────────────
            // A opção "Enabled" é omitida aqui: ela é controlada pelo botão
            // grande no rodapé do painel, não por um checkbox na lista.
            String enabledKey = net.minecraft.client.resources.I18n.format(
                    "angel_client.mod.generic.option.enabled");

            java.util.List<io.github.angelclient.client.mod.ModOption> all =
                    screen.getMod().getOptions();
            java.util.List<io.github.angelclient.client.mod.ModOption> options =
                    new java.util.ArrayList<>();
            if (all != null) {
                for (io.github.angelclient.client.mod.ModOption o : all) {
                    if (o != null && !enabledKey.equals(o.getName())) options.add(o);
                }
            }

            int total = options.size();

            for (int i = 0; i < total; i++) {
                ModOptionComponent comp = new ModOptionComponent(options.get(i));
                comp.setFirst(i == 0);
                comp.setLast(i == total - 1);

                final int fi = i;
                add(comp, (c, b) -> new Rectangle(0, fi * 23, b.getWidth(), 22));
            }
        }
    }
}
