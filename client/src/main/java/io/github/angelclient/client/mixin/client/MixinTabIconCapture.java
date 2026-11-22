package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.nametag.Nametag;
import io.github.angelclient.client.nametag.NametagManager;
import io.github.angelclient.client.nametag.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;

/**
 * PARTE 1 do sistema de ícones no TAB — captura informações e registra ícones.
 *
 * CORREÇÃO DE TRAVAMENTOS:
 *  - Os getters de NametagManager (getPlayerNametag / hasAngelClient) agora
 *    apenas lêem ConcurrentHashMaps — sem I/O, sem bloqueio.
 *  - currentPlayer / currentX são campos de instância usados somente
 *    na render thread: sem race condition.
 */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinTabIconCapture extends Gui {

    @Shadow
    protected Minecraft mc;

    // Textura padrão Angel Client
    private static final ResourceLocation ANGEL_ICON =
            new ResourceLocation("angelclient", "textures/logo.png");

    // Estado local da iteração de renderização (render thread only)
    private NetworkPlayerInfo currentPlayer;
    private int currentX;

    // ── PASSO 1: Limpa a lista de ícones no início de cada frame ──────────────
    @Inject(method = "renderPlayerlist", at = @At("HEAD"))
    private void clearIconList(int width, Scoreboard scoreboardIn,
                               ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        TabIconRenderer.getInstance().clear();
    }

    // ── PASSO 2: Captura o jogador atual e reserva espaço para o(s) ícone(s) ──
    @ModifyVariable(
        method   = "renderPlayerlist",
        at       = @At(value = "STORE", ordinal = 0),
        ordinal  = 2
    )
    private String capturePlayerAndAddSpace(String displayName, int width, int x,
                                            NetworkPlayerInfo playerInfo) {
        try {
            if (playerInfo == null) {
                currentPlayer = null;
                return displayName;
            }

            currentPlayer = playerInfo;
            String playerName = playerInfo.getGameProfile().getName();
            NametagManager mgr = NametagManager.getInstance();

            String  specialTag   = mgr.getSpecialTagType(playerName);
            Nametag personal     = mgr.getPersonalNametag(playerName);
            boolean hasAngCl     = mgr.hasAngelClient(playerName);

            boolean hasTag   = specialTag != null;
            boolean hasNametag = personal != null || hasAngCl;

            if (!hasTag && !hasNametag) {
                currentPlayer = null;
                return displayName;
            }

            int slots = (hasTag ? 1 : 0) + (hasNametag ? 1 : 0);
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < slots; i++) spaces.append("    ");
            return spaces.toString() + displayName;
        } catch (Exception e) {
            currentPlayer = null;
            return displayName;
        }
    }

    // ── PASSO 3: Captura posição X ────────────────────────────────────────────
    @ModifyVariable(
        method   = "renderPlayerlist",
        at       = @At(value = "STORE", ordinal = 1),
        ordinal  = 1
    )
    private int captureX(int x) {
        currentX = x;
        return x;
    }

    // ── PASSO 4: Captura posição Y e registra o(s) ícone(s) para renderização ─
    @ModifyVariable(
        method   = "renderPlayerlist",
        at       = @At(value = "STORE", ordinal = 1),
        ordinal  = 2
    )
    private int captureYAndRegisterIcon(int y) {
        try {
            if (currentPlayer == null) return y;

            String playerName = currentPlayer.getGameProfile().getName();
            NametagManager mgr = NametagManager.getInstance();

            String  specialTag   = mgr.getSpecialTagType(playerName);
            Nametag tagNametag   = mgr.getTagNametag(playerName);
            Nametag personal     = mgr.getPersonalNametag(playerName);
            boolean hasAngCl     = mgr.hasAngelClient(playerName);

            boolean hasTag   = specialTag != null;
            boolean hasNametag = personal != null || hasAngCl;

            if (!hasTag && !hasNametag) return y;

            int offsetX = currentX;

            // 1) Tag especial (owner, member, mod, etc.)
            if (hasTag) {
                ResourceLocation tex = null;

                // Tenta baixar do GitHub via Nametag (se o ID coincidir)
                if (tagNametag != null) {
                    tex = tagNametag.getTexture();
                    if (tex == null) return y; // aguarda download
                }

                // Fallback: carrega do assets local (ex: special_tag="dono" → /angel/dono.png)
                if (tex == null) {
                    tex = new ResourceLocation("angel_client",
                        "textures/angelclient/angel/" + specialTag + ".png");
                }

                TabIconRenderer.getInstance().addIcon(tex, offsetX, y);
                offsetX += 12;
            }

            // 2) Nametag pessoal / Angel Client
            if (hasNametag) {
                ResourceLocation texture;
                if (personal != null) {
                    texture = personal.getTexture();
                    if (texture == null) return y;
                } else {
                    texture = ANGEL_ICON;
                }
                TabIconRenderer.getInstance().addIcon(texture, offsetX, y);
            }
        } catch (Exception e) {
            // Silencioso — não pode crashar a render thread
        }
        return y;
    }
}
