package io.github.angelclient.client.mod.impl.autotext;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PreTickEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;

import net.minecraft.client.settings.KeyBinding;

/**
 * AutoText — Mod do Angel Client
 *
 * Permite cadastrar atalhos de teclado que enviam mensagens no chat
 * automaticamente. A tecla de abertura do menu aparece em Options → Controls.
 *
 * Placeholders suportados nas mensagens:
 *   {player}  nome do jogador
 *   {x} {y} {z}  posição atual
 */
public class AutoTextMod extends Mod {

    /** Instância singleton para que outros lugares possam acessar o manager. */
    public static AutoTextMod instance;

    /** Manager que guarda os atalhos e faz a persistência. */
    public AtalhoManager atalhoManager;

    /** Keybind para abrir o menu — aparece em Options → Controls. */
    @Option
    public KeyBinding keyAbrirMenu = null;

    /** Controla que o menu só abre no key-down, não fica abrindo em loop. */
    private boolean menuKeyWasDown = false;

    // -------------------------------------------------------------------------
    // Ciclo de vida do Mod
    // -------------------------------------------------------------------------

    @Override
    public void onRegister() {
        super.onRegister();
        instance = this;
        atalhoManager = new AtalhoManager(mc.mcDataDir);
        if (keyAbrirMenu == null)
            keyAbrirMenu = new KeyBinding(getTranslationKey() + ".key", Keyboard.KEY_NONE, Client.KEY_CATEGORY);
        Client.INSTANCE.registerKeyBinding(keyAbrirMenu);
    }

    @Override
    public String getId() {
        return "auto_text";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.UTILITY;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    // -------------------------------------------------------------------------
    // Lógica de teclas — escutamos o tick para abrir menu e disparar atalhos
    // -------------------------------------------------------------------------

    @EventHandler
    public void onTick(PreTickEvent event) {
        // Abrir menu (detecta borda de descida para não abrir em loop)
        boolean menuKeyDown = keyAbrirMenu.isKeyDown();
        if (menuKeyDown && !menuKeyWasDown) {
            mc.displayGuiScreen(new GuiAutoText());
        }
        menuKeyWasDown = menuKeyDown;

        // Disparar atalhos apenas quando nenhuma GUI estiver aberta
        if (mc.currentScreen != null) return;
        if (mc.theWorld == null)      return;

        int keyCode = Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() && keyCode > 0) {
            atalhoManager.verificarTeclas(mc, keyCode);
        }
    }
}