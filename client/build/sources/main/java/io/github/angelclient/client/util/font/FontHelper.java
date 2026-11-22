package io.github.angelclient.client.util.font;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import net.minecraft.client.Minecraft;

public class FontHelper {

    public static void drawString(String text, float x, float y, int color) {
        if (isHD()) {
            SlickFontRenderer.DEFAULT.renderString(text, x, y, color);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int) x, (int) y, color);
        }
    }

    public static void drawCentered(String text, float cx, float cy, int color) {
        if (isHD()) {
            SlickFontRenderer.DEFAULT.renderCenteredString(text, cx, cy, color);
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            int w = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawString(text, (int)(cx - w / 2f), (int) cy, color);
        }
    }

    public static float getWidth(String text) {
        if (isHD()) return SlickFontRenderer.DEFAULT.getWidth(text);
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
    }

    public static float getHeight() {
        if (isHD()) return SlickFontRenderer.DEFAULT.getHeight();
        return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }

    private static boolean isHD() {
        return AngelClientMod.instance != null && AngelClientMod.instance.fancyFont;
    }
}