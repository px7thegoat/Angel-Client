package keystrokesmod.util;

import keystrokesmod.event.MouseEvent;
import net.lenni0451.asmevents.event.EventTarget;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class CPSUtil {
    private static Minecraft mc = Minecraft.getMinecraft();
    private static List<Long> a = new ArrayList<>();
    private static List<Long> b = new ArrayList<>();
    public static long LL = 0L;
    public static long LR = 0L;

    @EventTarget
    public void onMouseUpdate(MouseEvent d) {
        if (d.isButtonState()) {
            if (d.getButton() == 0) {
                aL();
            } else if (d.getButton() == 1) {
                aR();
            }
        }
    }

    public static void aL() {
        a.add(LL = System.currentTimeMillis());
    }

    public static void aR() {
        b.add(LR = System.currentTimeMillis());
    }

    public static int f() {
        a.removeIf(o -> (Long) o < System.currentTimeMillis() - 1000L);
        return a.size();
    }

    public static int i() {
        b.removeIf(o -> (Long) o < System.currentTimeMillis() - 1000L);
        return b.size();
    }
}
