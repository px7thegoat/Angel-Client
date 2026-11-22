package io.github.angelclient.client.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderManager;
import io.github.angelclient.client.mod.impl.FreelookMod;
import io.github.angelclient.client.util.access.AccessRenderManager;
import io.github.angelclient.client.nametag.Nametag;
import io.github.angelclient.client.nametag.NametagManager;
import io.github.angelclient.client.mod.impl.NametagsMod;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.cosmetic.LocalCosmeticManager;
import io.github.angelclient.client.cosmetic.render.Cosmetic3DRenderer;
@Mixin(RendererLivingEntity.class)
public abstract class MixinNametagRenderer<T extends EntityLivingBase> {

    @Shadow protected abstract boolean canRenderName(T entity);

    private static final ResourceLocation ANGEL_DEFAULT_ICON =
            new ResourceLocation("angel_client", "textures/angelclient/logo.png");

    // ── Toggle 1: Show Nametags (global) ─────────────────────────────────────

    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void angelclient_cancelNativeNametag(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityPlayer)) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || entity == null) return;

        if (Cosmetic3DRenderer.skipNametagRendering) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // Mod desabilitado: não interferir no vanilla (sem tags customizadas).
        NametagsMod nametagsMod = NametagsMod.getInstance();
        if (nametagsMod == null || !nametagsMod.isEnabled()) return;

        // Toggle 1 — Se showNametags estiver OFF, bloqueia qualquer renderização.
        if (!nametagsMod.isShowNametags()) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;
        if (entity == mc.thePlayer && !nametagsMod.isShowOwnNametag()) return;

        FreelookMod freelook = FreelookMod.INSTANCE;
        boolean freelookActive = (freelook != null && freelook.hasStarted());

        String playerName = entity.getName();
        NametagManager manager = NametagManager.getInstance();
        if (manager == null) return;

        if (freelookActive || manager.hasAngelClient(playerName) || manager.getPlayerNametag(playerName) != null) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    // ── Show Own Nametag (substitui TweaksMod.showOwnTag) ───────────────────────

    /**
     * O vanilla usa RenderManager.livingPlayer para suprimir a própria nametag
     * em 1ª pessoa. Quando showOwnNametag=ON, retornamos null para enganar o
     * vanilla e fazê-lo renderizar o nome mesmo assim — mesmo comportamento
     * do @Redirect que existia no MixinTweaksMod, agora centralizado aqui.
     */
    @Redirect(
        method = "canRenderName",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/entity/RenderManager;livingPlayer:Lnet/minecraft/entity/Entity;"
        )
    )
    private net.minecraft.entity.Entity angelclient_showOwnNametag(RenderManager manager) {
        NametagsMod mod = NametagsMod.getInstance();
        if (mod != null && mod.isEnabled() && mod.isShowOwnNametag()) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiInventory) {
                return manager.livingPlayer;
            }
            return null; // engana o vanilla: ele acha que nao ha livingPlayer, renderiza o nome
        }
        return manager.livingPlayer;
    }

    // ── Renderização principal ────────────────────────────────────────────────

    @Inject(method = "renderName", at = @At("HEAD"), cancellable = true)
    private void angelclient_renderCustomNametag(T entity, double x, double y, double z, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer) || entity == null) return;
        if (!this.canRenderName(entity)) return;
        if (Cosmetic3DRenderer.skipNametagRendering) { ci.cancel(); return; }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        if (mc.currentScreen instanceof GuiInventory) {
            ci.cancel();
            return;
        }

        NametagManager manager = NametagManager.getInstance();
        if (manager == null) return;

        String playerName = entity.getName();
        Nametag customTag = manager.getPlayerNametag(playerName);
        boolean isAngelUser = manager.hasAngelClient(playerName);

        if (customTag == null && !isAngelUser) return;

        // Lê configurações do NametagsMod uma única vez para toda a frame.
        NametagsMod nametagsMod = NametagsMod.getInstance();

        // Mod desabilitado: deixa o vanilla renderizar normalmente (sem ícone, sem tags customizadas).
        if (nametagsMod == null || !nametagsMod.isEnabled()) return;

        boolean drawBackground = nametagsMod.isNametagBackground();
        boolean drawShadow     = nametagsMod.isTextShadow();
        boolean drawIcon       = nametagsMod.isShowIcon();

        // ── Resolução da cor de borda ─────────────────────────────────────────
        // Para o próprio jogador: usa a cor configurada localmente (NametagsMod)
        //   e faz upload automático para o Firebase se mudou desde o último tick.
        // Para outros jogadores Angel Client: usa a cor recebida do Firebase.
        boolean isSelf = entity == mc.thePlayer;
        int resolvedBorderArgb;
        if (isSelf) {
            resolvedBorderArgb = nametagsMod.getNametagBorderColour().getValue();
            // Upload automático: sincroniza cor local → Firebase em background
            boolean grad = nametagsMod.isNametagBorderGradient();
            int color2   = nametagsMod.getNametagBorderGradientColour().getValue();
            angelclient_syncBorderColor(manager, resolvedBorderArgb, grad, color2);
        } else {
            int remoteBorder = manager.getPlayerBorderColor(playerName);
            // -1 = jogador não tem borda customizada no Firebase → sem borda
            resolvedBorderArgb = remoteBorder;
        }

        FontRenderer font = mc.fontRendererObj;
        if (font == null) return;

        String fullText = entity.getDisplayName().getFormattedText();
        int halfWidth = font.getStringWidth(fullText) / 2;
        int prefixWidth = 0;

        int nameStartFormatted = findNameStartInFormattedText(fullText, playerName);
        if (nameStartFormatted > 0) {
            String formattedPrefix = fullText.substring(0, nameStartFormatted);
            prefixWidth = font.getStringWidth(formattedPrefix);
        }

        // --- Lógica de múltiplas linhas ---
        String specialTagType = manager.getSpecialTagType(playerName);
        boolean hasSpecialTag = (specialTagType != null && !specialTagType.isEmpty());

        // ── Nametag Message (texto personalizado flutuante acima do nick) ──────
        String nametagMsg = manager.getPlayerNametagMessage(playerName);
        // Para o próprio jogador: usa o valor local (prioridade sobre Firebase)
        if (isSelf && nametagsMod.isEnabled()) {
            String localMsg = nametagsMod.getNametagMessage();
            if (localMsg != null && !localMsg.trim().isEmpty()) {
                nametagMsg = localMsg.trim();
                // Sincroniza com Firebase se mudou
                nametagsMod.syncNametagMessage();
            }
        }
        boolean hasNametagMsg = (nametagMsg != null && !nametagMsg.trim().isEmpty());

        EntityPlayer player = (EntityPlayer) entity;
        Scoreboard scoreboard = player.getWorldScoreboard();
        ScoreObjective scoreobjective = (scoreboard != null) ? scoreboard.getObjectiveInDisplaySlot(2) : null;

        int extraLines = 0;
        if (hasSpecialTag) extraLines++;
        if (scoreobjective != null) extraLines++;

        double yOffset = extraLines * ((double) ((float) font.FONT_HEIGHT * 1.15F * 0.02666667F));
        if (entity.isSneaking()) {
            yOffset -= 0.25D; // Baixa a tag ligeiramente ao agachar, como no vanilla
        }

        GlStateManager.pushMatrix();
        float f_scale = 1.6F;
        float f1 = 0.016666668F * f_scale;
        float childOffset = entity.isChild() ? (entity.height / 2.0F) : 0.0F;

        float hasLargeCosmetic = LocalCosmeticManager.getInstance().hasLargeCosmetic(playerName) ? 0.35F : 0.0F;
        GlStateManager.translate((float) x, (float) (y + yOffset) + entity.height + 0.6F + hasLargeCosmetic - childOffset, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        
        float viewY = ((AccessRenderManager) mc.getRenderManager()).getPlayerViewY();
        float viewX = ((AccessRenderManager) mc.getRenderManager()).getPlayerViewX();
        
        // A rotação já é tratada globalmente no MixinRenderManager (F5 e Freelook)
        GlStateManager.rotate(-viewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(viewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.translate(0.0F, 0.0F, -0.1F);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // ── Ícone / Largura dinâmica ──────────────────────────────────────────
        // Quando showIcon=OFF, iconSpace=0 → fundo e texto ficam colados sem lacuna.
        ResourceLocation texture = (customTag != null) ? customTag.getTexture() : ANGEL_DEFAULT_ICON;
        int iconSpace = (texture != null && drawIcon) ? 12 : 0;
        int totalWidth = font.getStringWidth(fullText) + iconSpace;
        int halfTotalWidth = totalWidth / 2;

        // ── Toggle 2: Nametag Background (fundo translúcido do nome) ─────────
        if (drawBackground) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableFog();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0F, 1.0F);

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

            float bgAlpha = 0.4f;
            worldrenderer.pos(-halfTotalWidth - 1, -1, 0).color(0f, 0f, 0f, bgAlpha).endVertex();
            worldrenderer.pos(-halfTotalWidth - 1,  8, 0).color(0f, 0f, 0f, bgAlpha).endVertex();
            worldrenderer.pos( halfTotalWidth + 1,  8, 0).color(0f, 0f, 0f, bgAlpha).endVertex();
            worldrenderer.pos( halfTotalWidth + 1, -1, 0).color(0f, 0f, 0f, bgAlpha).endVertex();
            tessellator.draw();

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GlStateManager.enableFog();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        // ── Toggle: Nametag Border (borda colorida ao redor do fundo) ─────────
        // Para o próprio player: respeita o toggle nametagBorder do NametagsMod.
        // Para outros players: renderiza se o Firebase retornou uma cor válida (!= -1).
        boolean selfGradient = isSelf && nametagsMod.isNametagBorderGradient();
        boolean remoteGradient = !isSelf && manager.isPlayerBorderGradient(playerName);
        boolean useGradient = selfGradient || remoteGradient;

        boolean drawBorder = isSelf ? nametagsMod.isNametagBorder() : (resolvedBorderArgb != -1 || remoteGradient);
        if (drawBorder && (resolvedBorderArgb != -1 || useGradient)) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableFog();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(2.0F, 2.0F);

            // Margem: 2px além do fundo
            float bLeft  = -halfTotalWidth - 2;
            float bRight =  halfTotalWidth + 2;
            float bTop   = -2;
            float bBot   =  9;

            Tessellator bTess = Tessellator.getInstance();
            WorldRenderer bWr = bTess.getWorldRenderer();

            if (useGradient) {
                // ── Gradiente girando estilo Badlion ─────────────────────────
                // Um ponto de cor C2 ("highlight") gira ao redor da borda
                // sobre um fundo de cor C1. A transição é suave via cosseno.
                // O ponto oposto (C2 também) fica do lado contrário.
                // Período: 2 segundos por volta completa.

                float phase = (System.currentTimeMillis() % 2000L) / 2000.0F;

                int c1argb = isSelf
                        ? nametagsMod.getNametagBorderColour().getValue()
                        : resolvedBorderArgb;
                int c2argb = isSelf
                        ? nametagsMod.getNametagBorderGradientColour().getValue()
                        : manager.getPlayerBorderGradientColor2(playerName);
                if (c2argb == -1) c2argb = c1argb;

                float bA = 1.0F;

                // Comprimento real de cada segmento do perímetro
                float segTop   = bRight - bLeft;  // topo:    esq → dir
                float segRight = bBot   - bTop;   // direita: cima → baixo
                float segBot   = segTop;           // baixo:   dir → esq
                float segLeft  = segRight;         // esquerda: baixo → cima
                float perimeter = 2.0F * (segTop + segRight);

                // Posição no perímetro de cada canto (normalizado 0..1, horário)
                // TL=0, TR=segTop/P, BR=(segTop+segRight)/P, BL=(2*segTop+segRight)/P
                float pTL = 0.0F;
                float pTR = segTop / perimeter;
                float pBR = (segTop + segRight) / perimeter;
                float pBL = (2.0F * segTop + segRight) / perimeter;

                // Cada canto recebe a cor interpolada entre C1 e C2
                // com base em sua distância ao ponto "phase" no perímetro.
                // gradientSpot usa janela estreita (raio=0.15) → highlight visível
                float[] cTL = lerpArgb(c1argb, c2argb, gradientSpot(pTL, phase, 0.18f));
                float[] cTR = lerpArgb(c1argb, c2argb, gradientSpot(pTR, phase, 0.18f));
                float[] cBR = lerpArgb(c1argb, c2argb, gradientSpot(pBR, phase, 0.18f));
                float[] cBL = lerpArgb(c1argb, c2argb, gradientSpot(pBL, phase, 0.18f));

                // Linha superior — TL → TR
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,  bTop,     0).color(cTL[0], cTL[1], cTL[2], bA).endVertex();
                bWr.pos(bLeft,  bTop + 1, 0).color(cTL[0], cTL[1], cTL[2], bA).endVertex();
                bWr.pos(bRight, bTop + 1, 0).color(cTR[0], cTR[1], cTR[2], bA).endVertex();
                bWr.pos(bRight, bTop,     0).color(cTR[0], cTR[1], cTR[2], bA).endVertex();
                bTess.draw();

                // Linha direita — TR → BR
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bRight - 1, bTop, 0).color(cTR[0], cTR[1], cTR[2], bA).endVertex();
                bWr.pos(bRight - 1, bBot, 0).color(cBR[0], cBR[1], cBR[2], bA).endVertex();
                bWr.pos(bRight,     bBot, 0).color(cBR[0], cBR[1], cBR[2], bA).endVertex();
                bWr.pos(bRight,     bTop, 0).color(cTR[0], cTR[1], cTR[2], bA).endVertex();
                bTess.draw();

                // Linha inferior — BR → BL (dir→esq)
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,  bBot - 1, 0).color(cBL[0], cBL[1], cBL[2], bA).endVertex();
                bWr.pos(bLeft,  bBot,     0).color(cBL[0], cBL[1], cBL[2], bA).endVertex();
                bWr.pos(bRight, bBot,     0).color(cBR[0], cBR[1], cBR[2], bA).endVertex();
                bWr.pos(bRight, bBot - 1, 0).color(cBR[0], cBR[1], cBR[2], bA).endVertex();
                bTess.draw();

                // Linha esquerda — BL → TL
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,     bTop, 0).color(cTL[0], cTL[1], cTL[2], bA).endVertex();
                bWr.pos(bLeft,     bBot, 0).color(cBL[0], cBL[1], cBL[2], bA).endVertex();
                bWr.pos(bLeft + 1, bBot, 0).color(cBL[0], cBL[1], cBL[2], bA).endVertex();
                bWr.pos(bLeft + 1, bTop, 0).color(cTL[0], cTL[1], cTL[2], bA).endVertex();
                bTess.draw();

            } else {
                // ── Cor sólida (comportamento original) ─────────────────────
                Colour borderColour = nametagsMod.getNametagBorderColour();
                float bA = isSelf ? borderColour.getAlphaFloat() : ((resolvedBorderArgb >> 24) & 0xFF) / 255.0F;
                float bR = isSelf ? borderColour.getRedFloat()   : ((resolvedBorderArgb >> 16) & 0xFF) / 255.0F;
                float bG = isSelf ? borderColour.getGreenFloat() : ((resolvedBorderArgb >>  8) & 0xFF) / 255.0F;
                float bB = isSelf ? borderColour.getBlueFloat()  : ( resolvedBorderArgb        & 0xFF) / 255.0F;

                // Linha superior
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,  bTop,     0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bLeft,  bTop + 1, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight, bTop + 1, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight, bTop,     0).color(bR, bG, bB, bA).endVertex();
                bTess.draw();

                // Linha inferior
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,  bBot - 1, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bLeft,  bBot,     0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight, bBot,     0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight, bBot - 1, 0).color(bR, bG, bB, bA).endVertex();
                bTess.draw();

                // Linha esquerda
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bLeft,      bTop, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bLeft,      bBot, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bLeft + 1,  bBot, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bLeft + 1,  bTop, 0).color(bR, bG, bB, bA).endVertex();
                bTess.draw();

                // Linha direita
                bWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bWr.pos(bRight - 1, bTop, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight - 1, bBot, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight,     bBot, 0).color(bR, bG, bB, bA).endVertex();
                bWr.pos(bRight,     bTop, 0).color(bR, bG, bB, bA).endVertex();
                bTess.draw();
            }

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GlStateManager.enableFog();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        // ── Ícone ─────────────────────────────────────────────────────────────
        if (texture != null && drawIcon) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, -0.01F);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(texture);
            drawIcon(-halfTotalWidth, -0.5F, 8, 8);
            GlStateManager.popMatrix();
        }

        // ── Toggle 3: Text Shadow — nome principal ────────────────────────────
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, -0.02F);
        float textX = -halfTotalWidth + iconSpace;

        // Passagem "fantasma" (sem depth, para ver através das paredes)
        GlStateManager.disableDepth();
        font.drawString(fullText, (int) textX, 0, (0xFFFFFF & 0x00FFFFFF) | 0x20000000, drawShadow);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        // Passagem final com cor sólida
        font.drawString(fullText, (int) textX, 0, -1, drawShadow);
        GlStateManager.popMatrix();

        // ── Nametag Message — mesmo comportamento de depth do nickname ────────
        if (hasNametagMsg) {
            GlStateManager.pushMatrix();
            float msgLineH = (float) font.FONT_HEIGHT + 2.0F;
            GlStateManager.translate(0.0F, -msgLineH, -0.05F);

            int msgHalfW = font.getStringWidth(nametagMsg) / 2;

            // ── Borda da NametagMessage (mesma lógica da borda principal) ─────
            // Só desenha a borda se o próprio player tem borda habilitada (isSelf)
            // ou se o Firebase retornou cor válida para outros jogadores.
            boolean drawMsgBorder = isSelf ? nametagsMod.isNametagBorder() : (resolvedBorderArgb != -1 || remoteGradient);
            if (drawMsgBorder && (resolvedBorderArgb != -1 || useGradient)) {
                GlStateManager.pushMatrix();
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableFog();
                GlStateManager.enableDepth();
                GlStateManager.depthMask(false);

                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(2.0F, 2.0F);

                float mbLeft  = -msgHalfW - 2;
                float mbRight =  msgHalfW + 2;
                float mbTop   = -2;
                float mbBot   =  font.FONT_HEIGHT; // 1px além do fundo (igual à borda principal: bBot=9)

                Tessellator mbTess = Tessellator.getInstance();
                WorldRenderer mbWr = mbTess.getWorldRenderer();

                if (useGradient) {
                    float phase = (System.currentTimeMillis() % 2000L) / 2000.0F;

                    int mc1argb = isSelf
                            ? nametagsMod.getNametagBorderColour().getValue()
                            : resolvedBorderArgb;
                    int mc2argb = isSelf
                            ? nametagsMod.getNametagBorderGradientColour().getValue()
                            : manager.getPlayerBorderGradientColor2(playerName);
                    if (mc2argb == -1) mc2argb = mc1argb;

                    float mbA = 1.0F;

                    float msegTop   = mbRight - mbLeft;
                    float msegRight = mbBot   - mbTop;
                    float mperimeter = 2.0F * (msegTop + msegRight);

                    float mpTL = 0.0F;
                    float mpTR = msegTop / mperimeter;
                    float mpBR = (msegTop + msegRight) / mperimeter;
                    float mpBL = (2.0F * msegTop + msegRight) / mperimeter;

                    float[] mcTL = lerpArgb(mc1argb, mc2argb, gradientSpot(mpTL, phase, 0.18f));
                    float[] mcTR = lerpArgb(mc1argb, mc2argb, gradientSpot(mpTR, phase, 0.18f));
                    float[] mcBR = lerpArgb(mc1argb, mc2argb, gradientSpot(mpBR, phase, 0.18f));
                    float[] mcBL = lerpArgb(mc1argb, mc2argb, gradientSpot(mpBL, phase, 0.18f));

                    // Linha superior
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,  mbTop,     0).color(mcTL[0], mcTL[1], mcTL[2], mbA).endVertex();
                    mbWr.pos(mbLeft,  mbTop + 1, 0).color(mcTL[0], mcTL[1], mcTL[2], mbA).endVertex();
                    mbWr.pos(mbRight, mbTop + 1, 0).color(mcTR[0], mcTR[1], mcTR[2], mbA).endVertex();
                    mbWr.pos(mbRight, mbTop,     0).color(mcTR[0], mcTR[1], mcTR[2], mbA).endVertex();
                    mbTess.draw();

                    // Linha direita
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbRight - 1, mbTop, 0).color(mcTR[0], mcTR[1], mcTR[2], mbA).endVertex();
                    mbWr.pos(mbRight - 1, mbBot, 0).color(mcBR[0], mcBR[1], mcBR[2], mbA).endVertex();
                    mbWr.pos(mbRight,     mbBot, 0).color(mcBR[0], mcBR[1], mcBR[2], mbA).endVertex();
                    mbWr.pos(mbRight,     mbTop, 0).color(mcTR[0], mcTR[1], mcTR[2], mbA).endVertex();
                    mbTess.draw();

                    // Linha inferior
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,  mbBot - 1, 0).color(mcBL[0], mcBL[1], mcBL[2], mbA).endVertex();
                    mbWr.pos(mbLeft,  mbBot,     0).color(mcBL[0], mcBL[1], mcBL[2], mbA).endVertex();
                    mbWr.pos(mbRight, mbBot,     0).color(mcBR[0], mcBR[1], mcBR[2], mbA).endVertex();
                    mbWr.pos(mbRight, mbBot - 1, 0).color(mcBR[0], mcBR[1], mcBR[2], mbA).endVertex();
                    mbTess.draw();

                    // Linha esquerda
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,     mbTop, 0).color(mcTL[0], mcTL[1], mcTL[2], mbA).endVertex();
                    mbWr.pos(mbLeft,     mbBot, 0).color(mcBL[0], mcBL[1], mcBL[2], mbA).endVertex();
                    mbWr.pos(mbLeft + 1, mbBot, 0).color(mcBL[0], mcBL[1], mcBL[2], mbA).endVertex();
                    mbWr.pos(mbLeft + 1, mbTop, 0).color(mcTL[0], mcTL[1], mcTL[2], mbA).endVertex();
                    mbTess.draw();

                } else {
                    Colour mbColour = nametagsMod.getNametagBorderColour();
                    float mbA = isSelf ? mbColour.getAlphaFloat() : ((resolvedBorderArgb >> 24) & 0xFF) / 255.0F;
                    float mbR = isSelf ? mbColour.getRedFloat()   : ((resolvedBorderArgb >> 16) & 0xFF) / 255.0F;
                    float mbG = isSelf ? mbColour.getGreenFloat() : ((resolvedBorderArgb >>  8) & 0xFF) / 255.0F;
                    float mbB = isSelf ? mbColour.getBlueFloat()  : ( resolvedBorderArgb        & 0xFF) / 255.0F;

                    // Linha superior
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,  mbTop,     0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbLeft,  mbTop + 1, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight, mbTop + 1, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight, mbTop,     0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbTess.draw();

                    // Linha inferior
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,  mbBot - 1, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbLeft,  mbBot,     0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight, mbBot,     0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight, mbBot - 1, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbTess.draw();

                    // Linha esquerda
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbLeft,     mbTop, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbLeft,     mbBot, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbLeft + 1, mbBot, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbLeft + 1, mbTop, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbTess.draw();

                    // Linha direita
                    mbWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                    mbWr.pos(mbRight - 1, mbTop, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight - 1, mbBot, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight,     mbBot, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbWr.pos(mbRight,     mbTop, 0).color(mbR, mbG, mbB, mbA).endVertex();
                    mbTess.draw();
                }

                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                GlStateManager.enableFog();
                GlStateManager.enableTexture2D();
                GlStateManager.popMatrix();
            }

            // Fundo: enableDepth + depthMask(false) — igual ao fundo do nickname principal.
            if (drawBackground) {
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.enableDepth();
                GlStateManager.depthMask(false);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(1.0F, 1.0F);
                Tessellator msgTess = Tessellator.getInstance();
                WorldRenderer msgWr = msgTess.getWorldRenderer();
                msgWr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                msgWr.pos(-msgHalfW - 1, -1,                    0).color(0f, 0f, 0f, 0.4f).endVertex();
                msgWr.pos(-msgHalfW - 1, font.FONT_HEIGHT - 1,  0).color(0f, 0f, 0f, 0.4f).endVertex();
                msgWr.pos( msgHalfW + 1, font.FONT_HEIGHT - 1,  0).color(0f, 0f, 0f, 0.4f).endVertex();
                msgWr.pos( msgHalfW + 1, -1,                    0).color(0f, 0f, 0f, 0.4f).endVertex();
                msgTess.draw();
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                GlStateManager.enableTexture2D();
            }

            // ── Cor do texto da NametagMessage ────────────────────────────────
            // Aplica messageEffect: STATIC (branco), RAINBOW ou GRADIENT.
            // Para outros jogadores (não isSelf), a cor sempre vem do Firebase
            // como branco puro (o efeito é local do próprio player).
            int msgColorFinal;
            if (!isSelf) {
                msgColorFinal = 0xFFFFFF;
            } else {
                NametagsMod.MessageEffect effect = nametagsMod.getMessageEffect();
                if (effect == NametagsMod.MessageEffect.RAINBOW) {
                    // Arco-íris animado: ciclo de 3 segundos, posição baseada no tempo
                    float hue = (System.currentTimeMillis() % 3000L) / 3000.0F;
                    msgColorFinal = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0x00FFFFFF;
                } else if (effect == NametagsMod.MessageEffect.GRADIENT) {
                    // Gradiente pulsante entre messageColour1 e messageColour2
                    float t = (float)(0.5 - 0.5 * Math.cos(
                            (System.currentTimeMillis() % 2000L) / 2000.0F * 2.0 * Math.PI));
                    int c1 = nametagsMod.getMessageColour1().getValue();
                    int c2 = nametagsMod.getMessageColour2().getValue();
                    float[] lerped = lerpArgb(c1, c2, t);
                    int ri = (int)(lerped[0] * 255) & 0xFF;
                    int gi = (int)(lerped[1] * 255) & 0xFF;
                    int bi = (int)(lerped[2] * 255) & 0xFF;
                    msgColorFinal = (ri << 16) | (gi << 8) | bi;
                } else {
                    // STATIC: usa messageColour1
                    msgColorFinal = nametagsMod.getMessageColour1().getValue() & 0x00FFFFFF;
                }
            }

            // Passagem 1 — fantasma (alpha baixo, visível através de blocos).
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            font.drawString(nametagMsg, -msgHalfW, 0,
                    (msgColorFinal & 0x00FFFFFF) | 0x20000000, drawShadow);

            // Passagem 2 — sólida (respeita depth buffer).
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            font.drawString(nametagMsg, -msgHalfW, 0,
                    (msgColorFinal & 0x00FFFFFF) | 0xFF000000, drawShadow);

            GlStateManager.popMatrix();
        }

        // --- Controle de altura das linhas extras ---
        int currentLineY = font.FONT_HEIGHT + 1;

        // ── Tag Especial (removida — só o nametag ao lado do nick) ────────────

        // ── Scoreboard (abaixo do nome ou da Tag Especial) ───────────────────
        if (scoreobjective != null) {
            net.minecraft.scoreboard.Score score = scoreboard.getValueFromObjective(playerName, scoreobjective);
            String scoreString = score.getScorePoints() + " " + scoreobjective.getDisplayName();
            int scoreHalfW = font.getStringWidth(scoreString) / 2;

            renderBackgroundRect(scoreHalfW, currentLineY, 0x000000, 0.25f, font, drawBackground);

            int scoreColor = 0xFFFFAA;
            font.drawString(scoreString, -scoreHalfW, currentLineY, (scoreColor & 0x00FFFFFF) | 0x20000000, drawShadow);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            font.drawString(scoreString, -scoreHalfW, currentLineY, scoreColor | 0xFF000000, drawShadow);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        ci.cancel();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sincroniza a cor de borda local com o Firebase.
     * Só faz o upload se a cor mudou desde o último envio,
     * evitando uma requisição HTTP a cada frame.
     */
    private static int lastUploadedBorderArgb  = Integer.MIN_VALUE;
    private static int lastUploadedBorderArgb2 = Integer.MIN_VALUE;
    private static boolean lastUploadedGradient = false;

    private static void angelclient_syncBorderColor(NametagManager manager, int currentArgb,
                                                     boolean gradient, int currentArgb2) {
        if (currentArgb  != lastUploadedBorderArgb
         || currentArgb2 != lastUploadedBorderArgb2
         || gradient     != lastUploadedGradient) {
            lastUploadedBorderArgb  = currentArgb;
            lastUploadedBorderArgb2 = currentArgb2;
            lastUploadedGradient    = gradient;
            manager.uploadBorderColor(currentArgb, gradient, currentArgb2);
        }
    }

    /**
     * Retorna fator 0..1 de quanto a cor C2 aparece num ponto do perímetro.
     * O "spot" (highlight) está em `phase` e tem largura `radius` (0..0.5).
     * Fora do raio retorna 0 (cor C1 pura). Dentro, sobe suavemente até 1.
     * Dois spots opostos giram juntos (phase e phase+0.5).
     */
    private static float gradientSpot(float perimPos, float phase, float radius) {
        // Distância ao spot principal (phase), wrap [0,1)
        float d1 = Math.abs(perimPos - phase);
        if (d1 > 0.5F) d1 = 1.0F - d1;
        // Distância ao spot oposto (phase + 0.5)
        float d2 = Math.abs(perimPos - ((phase + 0.5F) % 1.0F));
        if (d2 > 0.5F) d2 = 1.0F - d2;
        // Pega a menor distância (qualquer um dos dois spots)
        float d = Math.min(d1, d2);
        if (d >= radius) return 0.0F;
        // Suaviza com cosseno dentro do raio
        return (float)(0.5 - 0.5 * Math.cos(Math.PI * (1.0 - d / radius)));
    }

    /**
     * dado a posição normalizada no perímetro [0,1) e a fase atual.
     *
     * As duas cores ficam em posições opostas no perímetro e giram juntas.
     * O ponto de cor C1 está em `phase`, o de C2 está em `(phase + 0.5) mod 1`.
     * Entre eles o fator interpola suavemente 0→1→0 ao longo do perímetro.
     */
    private static float gradientColorAt(float perimPos, float phase) {
        // Distância angular entre a posição e o ponto C1 (phase), wrap [0,1)
        float dist = perimPos - phase;
        if (dist < 0) dist += 1.0F;
        // dist: 0 = no ponto C1, 0.5 = no ponto C2, wrap suave
        // Converte para t: 0 em C1, 1 em C2, 0 de volta em C1
        // Seno faz a transição suave
        return (float)(0.5 - 0.5 * Math.cos(dist * 2.0 * Math.PI));
    }

    /**
     * Interpola linearmente entre dois ARGB e devolve float[]{r,g,b} em 0..1.
     * @param t fator 0.0 = cor1, 1.0 = cor2
     */
    private static float[] lerpArgb(int argb1, int argb2, float t) {
        float r1 = ((argb1 >> 16) & 0xFF) / 255.0F;
        float g1 = ((argb1 >>  8) & 0xFF) / 255.0F;
        float b1 = ( argb1        & 0xFF) / 255.0F;
        float r2 = ((argb2 >> 16) & 0xFF) / 255.0F;
        float g2 = ((argb2 >>  8) & 0xFF) / 255.0F;
        float b2 = ( argb2        & 0xFF) / 255.0F;
        return new float[]{ r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t };
    }

    /**
     * Desenha o retângulo de fundo de uma linha secundária (tag / scoreboard).
     *
     * @param drawBackground se {@code false}, o método retorna imediatamente
     *                       sem desenhar nada (Toggle 2 OFF).
     */
    private void renderBackgroundRect(int halfWidth, int y, int color, float alpha,
                                      FontRenderer font, boolean drawBackground) {
        if (!drawBackground) return;

        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableFog();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >>  8 & 255) / 255.0F;
        float b = (float)(color       & 255) / 255.0F;

        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-halfWidth - 1, y - 1,                  0.0D).color(r, g, b, alpha).endVertex();
        wr.pos(-halfWidth - 1, y + font.FONT_HEIGHT - 1, 0.0D).color(r, g, b, alpha).endVertex();
        wr.pos( halfWidth + 1, y + font.FONT_HEIGHT - 1, 0.0D).color(r, g, b, alpha).endVertex();
        wr.pos( halfWidth + 1, y - 1,                  0.0D).color(r, g, b, alpha).endVertex();
        tess.draw();

        GlStateManager.enableFog();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
    }

    private String formatSpecialTag(String tagType) {
        if (tagType == null) return "";
        switch (tagType.toLowerCase()) {
            case "dono":     return "\u2756 Dono";
            case "dev":      return "\u2699 Dev";
            case "tiktoker": return "\u266B TikToker";
            case "youtuber": return "\u25BA YouTuber";
            case "staff":    return "\u2605 Staff";
            case "admin":    return "\u26A1 Admin";
            case "vip":      return "\u2764 VIP";
            case "beta":     return "\u03B2 Beta Tester";
            default:
                return tagType.substring(0, 1).toUpperCase() + tagType.substring(1);
        }
    }

    private int getSpecialTagColor(String tagType) {
        if (tagType == null) return 0xFFFFFF;
        switch (tagType.toLowerCase()) {
            case "dono":     return 0xFFD700;
            case "dev":      return 0x00CFFF;
            case "tiktoker": return 0xFF69B4;
            case "youtuber": return 0xFF4444;
            case "staff":    return 0x44FF88;
            case "admin":    return 0xFF8800;
            case "vip":      return 0xCC44FF;
            case "beta":     return 0xAABBCC;
            default:         return 0xFFFFFF;
        }
    }

    private int findNameStartInFormattedText(String fullText, String playerName) {
        if (fullText == null || playerName == null) return -1;
        StringBuilder clean = new StringBuilder();
        int[] indexMap = new int[fullText.length()];
        int cleanLen = 0;
        for (int i = 0; i < fullText.length(); i++) {
            if (fullText.charAt(i) == '\u00A7') {
                i++;
                continue;
            }
            indexMap[cleanLen] = i;
            clean.append(fullText.charAt(i));
            cleanLen++;
        }
        String cleanStr = clean.toString();
        int nameIndex = cleanStr.toLowerCase().indexOf(playerName.toLowerCase());
        if (nameIndex < 0) return -1;
        return indexMap[nameIndex];
    }

    private void drawIcon(float x, float y, float w, float h) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x,     y + h, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + w, y + h, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + w, y,     0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x,     y,     0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
    }
}