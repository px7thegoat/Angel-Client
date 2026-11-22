package keystrokesmod;

import keystrokesmod.event.TickEvent;
import keystrokesmod.manager.ModuleManager;
import keystrokesmod.module.Module;
import keystrokesmod.ui.AngelRavenGui;
import keystrokesmod.util.GeneralUtils;
import net.lenni0451.asmevents.EventManager;
import net.lenni0451.asmevents.event.EventTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class Raven {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static ModuleManager moduleManager;

    public Raven() {
        moduleManager = new ModuleManager();
    }

    public void init() {
        EventManager.register(this);
        moduleManager.register();
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (GeneralUtils.nullCheck()) {
            if (mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_INSERT)
                    && mc.getSession().getUsername().equalsIgnoreCase("bwrcelos")) {
                if (!(mc.currentScreen instanceof AngelRavenGui)) {
                    mc.displayGuiScreen(new AngelRavenGui());
                }
            }
            for (Module module : ModuleManager.getModules()) {
                if (mc.currentScreen == null && module.canBeEnabled()) {
                    module.keybind();
                }
                if (module.isEnabled()) {
                    module.onUpdate();
                }
            }
        }
    }
}
