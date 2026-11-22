package io.github.angelclient.client.mixin.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.ui.screen.BetterResourcePackList;
import io.github.angelclient.client.util.data.Rectangle;
import io.github.angelclient.client.util.render.GLRectUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiResourcePackAvailable;
import net.minecraft.client.gui.GuiResourcePackSelected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.util.EnumChatFormatting;

@Mixin(GuiScreenResourcePacks.class)
public abstract class MixinGuiScreenResourcePacks extends GuiScreen {

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    private static final int BTN_DONE        = 1001;
    private static final int BTN_OPEN_FOLDER = 1002;
    private static final int BTN_FIND_NEW    = 1003;

    private static final int   COR_PAINEL      = 0xCC141414; // BG3
    private static final int   COR_PAINEL_BORD = 0xFF2A2A2A; // BORDER2
    private static final int   COR_HEADER      = 0xFF1E1E1E; // BG4
    private static final int   COR_SEARCH_BG   = 0xFF141414;
    private static final int   COR_SEARCH_BORD = 0xFF2A2A2A;
    private static final float RAIO            = 3f;

    // -------------------------------------------------------------------------
    // Campos internos
    // -------------------------------------------------------------------------

    private GuiTextField searchField;
    private BetterResourcePackList betterList;

    private int panelL;
    private int panelR;
    private int panelW;
    private int panelTop;
    private int panelBot;
    private int headerH;
    private int btnW;
    private int btnH = 20;
    private Rectangle arrowBounds;

    // -------------------------------------------------------------------------
    // Shadows
    // -------------------------------------------------------------------------

    @Shadow private List<ResourcePackListEntry> availableResourcePacks;
    @Shadow private GuiResourcePackAvailable availableResourcePacksList;
    @Shadow private GuiResourcePackSelected selectedResourcePacksList;

    @Shadow
    protected abstract void actionPerformed(GuiButton button) throws IOException;

    // -------------------------------------------------------------------------
    // Helpers de estado GL
    // -------------------------------------------------------------------------

    /**
     * Salva a matriz atual e aplica identidade LOCAL para os draws GL imediatos.
     * Usar pushMatrix/popMatrix garante que a matriz do mundo NÃO é afetada —
     * apenas isolamos o contexto de renderização deste menu.
     */
    private void prepararGL() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();       // salva matriz atual (pode ter offset de scroll das listas)
        GL11.glLoadIdentity();     // identidade apenas dentro deste push — seguro

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Restaura a matriz e o estado GL padrão do MC. */
    private void restaurarGL() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();        // restaura a matriz que estava antes — mundo intacto
    }

    // -------------------------------------------------------------------------
    // Descarta botões vanilla
    // -------------------------------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "initGui", at = @At(value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    public <E> boolean descartarBotaoAbrir(List list, E e) { return true; }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "initGui", at = @At(value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    public <E> boolean descartarBotaoAplicar(List list, E e) { return true; }

    // -------------------------------------------------------------------------
    // initGui
    // -------------------------------------------------------------------------

    @Inject(method = "initGui", at = @At("RETURN"))
    public void configurarLayout(CallbackInfo ci) {
        panelW   = 200;
        panelL   = width / 2 - 4 - panelW;
        panelR   = width / 2 + 4;
        panelTop = 28;
        panelBot = height - 36;
        headerH  = 22;
        btnH     = 20;
        btnW     = (panelW * 2 + 8) / 3 - 2;

        arrowBounds = new Rectangle(panelL + 4, panelTop + 4, 16, 16);

        int searchX = panelL + 60;
        int searchW = panelW - 64;
        searchField = new GuiTextField(3, mc.fontRendererObj,
                searchX + 4, panelTop + 6, searchW - 8, 12);
        searchField.setMaxStringLength(64);
        searchField.setCanLoseFocus(true);
        searchField.setEnableBackgroundDrawing(false);

        int startX = panelL;
        int btnY   = height - 28;

        buttonList.add(new GuiButton(BTN_FIND_NEW,
                startX, btnY, btnW, btnH,
                I18n.format("angel_client.packs.find_new")));
        buttonList.add(new GuiButton(BTN_OPEN_FOLDER,
                startX + btnW + 2, btnY, btnW, btnH,
                I18n.format("angel_client.packs.open_folder_long")));
        buttonList.add(new GuiButton(BTN_DONE,
                startX + (btnW + 2) * 2, btnY, btnW, btnH,
                I18n.format("angel_client.packs.done")));
    }

    // -------------------------------------------------------------------------
    // Substitui lista vanilla
    // -------------------------------------------------------------------------

    @Redirect(method = "initGui", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/GuiScreenResourcePacks;" +
                    "availableResourcePacksList:Lnet/minecraft/client/gui/GuiResourcePackAvailable;",
            ordinal = 0))
    public void substituirLista(GuiScreenResourcePacks screen,
                                GuiResourcePackAvailable lista) {
        try {
            this.availableResourcePacksList = betterList = new BetterResourcePackList(
                    mc, (GuiScreenResourcePacks)(Object)this,
                    lista.getListWidth(), height,
                    mc.getResourcePackRepository(),
                    () -> searchField != null ? searchField.getText() : "");
        } catch (IOException ignored) {}
    }

    // -------------------------------------------------------------------------
    // Remove textos vanilla
    // -------------------------------------------------------------------------

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiScreenResourcePacks;" +
                    "drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 0))
    public void removerTitulo(GuiScreenResourcePacks s, FontRenderer fr,
                              String t, int x, int y, int c) {}

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiScreenResourcePacks;" +
                    "drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 1))
    public void removerSubtitulo(GuiScreenResourcePacks s, FontRenderer fr,
                                 String t, int x, int y, int c) {}

    // -------------------------------------------------------------------------
    // Suprime mouseClicked vanilla da lista available (impede reabrir menu)
    // -------------------------------------------------------------------------

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiResourcePackAvailable;mouseClicked(III)V"))
    public void suprimirClickListaVanilla(GuiResourcePackAvailable lista,
                                          int mx, int my, int btn) {
        // Bloqueado — repassado manualmente via betterList abaixo
    }

    // -------------------------------------------------------------------------
    // Desenho — fundo e painéis (HEAD = antes das listas)
    // -------------------------------------------------------------------------

    @Inject(method = "drawScreen", at = @At("HEAD"))
    public void desenharFundo(int mx, int my, float pt, CallbackInfo ci) {
        prepararGL();

        drawRect(0, 0, width, height, 0x88000000); // Fundo mais transparente

        // Painel Available
        GLRectUtils.drawRoundedRect(panelL, panelTop,
                panelL + panelW, panelBot, RAIO, COR_PAINEL);
        GLRectUtils.drawRoundedOutline(panelL, panelTop,
                panelL + panelW, panelBot, RAIO, 1f, COR_PAINEL_BORD);
        GLRectUtils.drawRoundedRect(panelL, panelTop,
                panelL + panelW, panelTop + headerH, RAIO, COR_HEADER);

        // Painel Active
        GLRectUtils.drawRoundedRect(panelR, panelTop,
                panelR + panelW, panelBot, RAIO, COR_PAINEL);
        GLRectUtils.drawRoundedOutline(panelR, panelTop,
                panelR + panelW, panelBot, RAIO, 1f, COR_PAINEL_BORD);
        GLRectUtils.drawRoundedRect(panelR, panelTop,
                panelR + panelW, panelTop + headerH, RAIO, COR_HEADER);

        // Fundo do campo de busca
        int sx = panelL + 58;
        int sw = panelW - 62;
        GLRectUtils.drawRoundedRect(sx, panelTop + 4,
                sx + sw, panelTop + headerH - 4, 3f, COR_SEARCH_BG);
        GLRectUtils.drawRoundedOutline(sx, panelTop + 4,
                sx + sw, panelTop + headerH - 4, 3f, 1f, COR_SEARCH_BORD);

        restaurarGL();
    }

    // -------------------------------------------------------------------------
    // Desenho — botões e textos (RETURN = após as listas, ficam na frente)
    // -------------------------------------------------------------------------

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void desenharBotoesETextos(int mx, int my, float pt, CallbackInfo ci) {
        prepararGL();

        restaurarGL(); // popMatrix aqui — volta ao estado correto antes dos textos

        // ── Textos (texture2D reativado pelo restaurarGL) ──────────────────────

        String txtAvailable;
        if (betterList != null && betterList.delegate != null) {
            txtAvailable = betterList.getFolder().getName();
            mc.getTextureManager().bindTexture(
                    new net.minecraft.util.ResourceLocation("textures/gui/resource_pack_up.png"));
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
                    panelL + 4, panelTop + 4, 0,
                    arrowBounds.contains(mx, my) ? 16 : 0, 16, 16, 16, 32);
        } else {
            txtAvailable = EnumChatFormatting.BOLD +
                    I18n.format("angel_client.packs.available");
        }
        mc.fontRendererObj.drawStringWithShadow(txtAvailable,
                panelL + 4, panelTop + 7, 0xFFFFFF);

        String txtActive = EnumChatFormatting.BOLD +
                I18n.format("angel_client.packs.active");
        mc.fontRendererObj.drawStringWithShadow(txtActive,
                panelR + 4, panelTop + 7, 0xFFFFFF);

        if (searchField != null) searchField.drawTextBox();
    }

    // -------------------------------------------------------------------------
    // Ações dos botões
    // -------------------------------------------------------------------------

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    public void aoClicarBotao(GuiButton button, CallbackInfo ci) throws IOException {
        if (button.id == BTN_DONE) {
            // Id 1 = Done vanilla do GuiScreenResourcePacks
            actionPerformed(new GuiButton(1, 0, 0, 1, 1, ""));
            ci.cancel();
        } else if (button.id == BTN_OPEN_FOLDER) {
            try {
                java.awt.Desktop.getDesktop().open(
                        mc.getResourcePackRepository().getDirResourcepacks());
            } catch (Exception ignored) {}
            ci.cancel();
        } else if (button.id == BTN_FIND_NEW) {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new URI("https://www.curseforge.com/minecraft/texture-packs"));
            } catch (Exception ignored) {}
            ci.cancel();
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    public void aoClicarMouse(int mx, int my, int btn, CallbackInfo ci) {
        if (searchField != null) searchField.mouseClicked(mx, my, btn);

        if (betterList != null) {
            betterList.mouseClicked(mx, my, btn);
        }

        if (betterList != null && arrowBounds != null
                && arrowBounds.contains(mx, my) && btn == 0) {
            betterList.up();
        }
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        super.keyTyped(ch, key);
        if (ch > 31 && searchField != null && !searchField.isFocused()) {
            searchField.setText("");
            searchField.setFocused(true);
        }
        if (searchField != null) searchField.textboxKeyTyped(ch, key);
    }
}