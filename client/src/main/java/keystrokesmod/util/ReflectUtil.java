package keystrokesmod.util;

import net.lenni0451.asmevents.EventManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class ReflectUtil {
    public static Field keystrokesButtonField;
    public static Field keystrokesButtonStateField;
    public static Field buttons;
    public static Field leftClickCounter;
    public static Field jumpTicks;
    public static Field rightClickDelayTimerField;
    public static Field curBlockDamageMP;
    public static Field blockHitDelay;
    public static Method clickMouse;
    public static Method rightClickMouse;
    public static Field shaderResourceLocations;
    public static Field useShader;
    public static Field shaderIndex;
    public static Method loadShader;
    public static Method getPlayerInfo;
    public static Field inGround;
    public static Field itemInUseCount;
    public static Field S08PacketPlayerPosLookYaw;
    public static Field S08PacketPlayerPosLookPitch;
    public static Field C02PacketUseEntityEntityId;
    public static Field bookContents;
    public static Field fallDistance;
    public static Field thirdPersonDistance;
    public static Method mouseClicked;
    public static Field serverSprintState;

    public static boolean sendMessage = false;

    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {}
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String method, String srgName, Class<?>... paramTypes) {
        try {
            Method m = clazz.getDeclaredMethod(method, paramTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            try {
                Method m = clazz.getDeclaredMethod(srgName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e2) {}
        }
        return null;
    }

    public static void getFields() {
        try {
            keystrokesButtonField = keystrokesmod.event.MouseEvent.class.getDeclaredField("button");
            keystrokesButtonStateField = keystrokesmod.event.MouseEvent.class.getDeclaredField("buttonstate");
            keystrokesButtonField.setAccessible(true);
            keystrokesButtonStateField.setAccessible(true);
            buttons = Mouse.class.getDeclaredField("buttons");
            buttons.setAccessible(true);

            leftClickCounter = findField(Minecraft.class, "field_71429_W", "leftClickCounter");

			serverSprintState = findField(EntityPlayerSP.class, "field_175171_bO", "serverSprintState");

            jumpTicks = findField(EntityLivingBase.class, "field_70773_bE", "jumpTicks");

            rightClickDelayTimerField = findField(Minecraft.class, "field_71467_ac", "rightClickDelayTimer");

            curBlockDamageMP = findField(PlayerControllerMP.class, "field_78770_f", "curBlockDamageMP");

            blockHitDelay = findField(PlayerControllerMP.class, "field_78781_i", "blockHitDelay");

            fallDistance = findField(Entity.class, "field_70143_R", "fallDistance");

            shaderResourceLocations = findField(EntityRenderer.class, "field_147712_ad", "shaderResourceLocations");

            thirdPersonDistance = findField(EntityRenderer.class, "field_78490_B", "thirdPersonDistance");

            useShader = findField(EntityRenderer.class, "field_175083_ad", "useShader");

            shaderIndex = findField(EntityRenderer.class, "field_147713_ae", "shaderIndex");

            inGround = findField(EntityArrow.class, "field_70254_i", "inGround");

            itemInUseCount = findField(EntityPlayer.class, "field_71072_f", "itemInUseCount");

            S08PacketPlayerPosLookYaw = findField(S08PacketPlayerPosLook.class, "field_148936_d", "yaw");

            S08PacketPlayerPosLookPitch = findField(S08PacketPlayerPosLook.class, "field_148937_e", "pitch");

            C02PacketUseEntityEntityId = findField(C02PacketUseEntity.class, "field_149567_a", "entityId");

            bookContents = findField(GuiScreenBook.class, "field_175386_A");

        } catch (Exception var2) {
            System.out.println("There was an error, relaunch the game.");
            var2.printStackTrace();
            sendMessage = true;
        }
    }

    public static void getMethods() {
        try {
            try {
                rightClickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("func_147121_ag");
            } catch (NoSuchMethodException var4) {
                try {
                    rightClickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("rightClickMouse");
                } catch (NoSuchMethodException var3) {
                }
            }

            if (rightClickMouse != null) {
                rightClickMouse.setAccessible(true);
            }

            try {
                mouseClicked = GuiScreen.class.getDeclaredMethod("func_73864_a", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            } catch (NoSuchMethodException ex) {
                try {
                    mouseClicked = GuiScreen.class.getDeclaredMethod("mouseClicked", Integer.TYPE, Integer.TYPE, Integer.TYPE);
                } catch (Exception ex2) {}
            }

            if (mouseClicked != null) {
                mouseClicked.setAccessible(true);
            }

            loadShader = findMethod(EntityRenderer.class, "loadShader", "func_175069_a", ResourceLocation.class);

            try {
                clickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("clickMouse");
            } catch (NoSuchMethodException var4) {
                try {
                    clickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("func_147116_af");
                } catch (NoSuchMethodException var3) {
                }
            }

            if (clickMouse != null) {
                clickMouse.setAccessible(true);
            }

            try {
                getPlayerInfo = AbstractClientPlayer.class.getDeclaredMethod("getPlayerInfo");
            } catch (NoSuchMethodException var4) {
                try {
                    getPlayerInfo =

                            AbstractClientPlayer.class.getDeclaredMethod("func_175155_b");
                } catch (NoSuchMethodException var3) {
                }
            }

            if (getPlayerInfo != null) {
                getPlayerInfo.setAccessible(true);
            }
        }
        catch (Exception e) {
            System.out.println("There was an error, relaunch the game.");
            e.printStackTrace();
            sendMessage = true;
        }
    }

    public static void setButton(int t, boolean s) {
        if (keystrokesButtonField != null && keystrokesButtonStateField != null && buttons != null) {
            keystrokesmod.event.MouseEvent event = new keystrokesmod.event.MouseEvent();

            try {
                keystrokesButtonField.set(event, t);
                keystrokesButtonStateField.set(event, s);
                EventManager.call(event);
                ByteBuffer bf = (ByteBuffer) buttons.get(null);
                bf.put(t, (byte) (s ? 1 : 0));
            } catch (Exception ignored) {
            }
        }
    }

    public static void rightClick() {
        try {
            ReflectUtil.rightClickMouse.invoke(Minecraft.getMinecraft());
        }
        catch (InvocationTargetException ex) {}
        catch (IllegalAccessException ex2) {}
    }

    public static void clickMouse() {
        if (clickMouse != null) {
            try {
                clickMouse.invoke(Minecraft.getMinecraft());
            }
            catch (InvocationTargetException ex) {}
            catch (IllegalAccessException ex2) {}
        }
    }

    public static boolean setBlocking(boolean blocking) {
        try {
            itemInUseCount.set(Minecraft.getMinecraft().thePlayer, blocking ? 1 : 0);
        } catch (Exception e) {
            e.printStackTrace();
            GeneralUtils.sendMessage("§cFailed to set block state client-side.");
            return false;
        }
        return blocking;
    }

    public static void clickMouseGui(GuiScreen s, int mouseButton) {
        int x = Mouse.getX() * s.width / Minecraft.getMinecraft().displayWidth;
        int y = s.height - Mouse.getY() * s.height / Minecraft.getMinecraft().displayHeight - 1;

        try {
            mouseClicked.invoke(s, x, y, mouseButton);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
