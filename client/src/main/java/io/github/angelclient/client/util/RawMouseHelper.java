package io.github.angelclient.client.util;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.util.MouseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom MouseHelper that uses JInput to bypass OS mouse acceleration.
 * Aggregates all detected mice to ensure it works on any system configuration.
 *
 * BUG FIX: when the cursor is not grabbed (e.g. during replay), raw JInput polling
 * is skipped entirely and falls back to vanilla LWJGL mouse handling. This prevents
 * JInput from consuming movement events and interfering with the free cursor.
 */
public class RawMouseHelper extends MouseHelper {

    private final List<MouseDevice> mice = new ArrayList<>();
    private float partialX, partialY;
    private boolean grabbed = false;

    private static class MouseDevice {
        Controller controller;
        Component x, y;
    }

    public RawMouseHelper() {
        try {
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            for (Controller controller : controllers) {
                if (controller.getType() == Controller.Type.MOUSE) {
                    Component tx = null, ty = null;

                    for (Component component : controller.getComponents()) {
                        if (component.getIdentifier() == Component.Identifier.Axis.X) {
                            tx = component;
                        } else if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                            ty = component;
                        }
                    }

                    if (tx != null && ty != null) {
                        MouseDevice device = new MouseDevice();
                        device.controller = controller;
                        device.x = tx;
                        device.y = ty;
                        mice.add(device);
                        System.out.println("[AngelClient] Raw Input: Mouse detectado - " + controller.getName());
                    }
                }
            }

            if (mice.isEmpty()) {
                System.out.println("[AngelClient] Raw Input: Nenhum mouse compatível encontrado via JInput.");
            }
        } catch (Exception e) {
            System.err.println("[AngelClient] Erro ao inicializar Raw Input:");
            e.printStackTrace();
        }
    }

    @Override
    public void grabMouseCursor() {
        grabbed = true;
        // Poll and discard to reset JInput state after display regain
        for (MouseDevice device : mice) {
            device.controller.poll();
        }
        partialX = 0;
        partialY = 0;
        super.grabMouseCursor();
    }

    @Override
    public void ungrabMouseCursor() {
        grabbed = false;
        super.ungrabMouseCursor();
    }

    @Override
    public void mouseXYChange() {
        // Durante o replay (e qualquer situação com cursor solto), o JInput não deve
        // ser usado — ele continua consumindo eventos de movimento e interfere no cursor livre.
        if (!grabbed || mice.isEmpty()) {
            super.mouseXYChange();
            return;
        }

        float totalDX = 0;
        float totalDY = 0;
        boolean polledAny = false;

        for (MouseDevice device : mice) {
            if (device.controller.poll()) {
                totalDX += device.x.getPollData();
                totalDY += device.y.getPollData();
                polledAny = true;
            }
        }

        if (polledAny) {
            float dx = totalDX + partialX;
            float dy = totalDY + partialY;

            this.deltaX = (int) dx;
            this.deltaY = -(int) dy;

            partialX = dx - (float) this.deltaX;
            partialY = dy + (float) this.deltaY;
            return;
        }

        // Fallback para o mouse padrão se não houver dispositivos JInput ativos
        super.mouseXYChange();
    }
}
