package keystrokesmod.util;

import keystrokesmod.Raven;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class PacketUtils {
    public static void sendPacketNoEvent(Packet packet) {
        if (packet == null || packet.getClass().getSimpleName().startsWith("S")) {
            return;
        }

        Raven.mc.thePlayer.sendQueue.addToSendQueue(packet);
    }

    public static void receivePacketNoEvent(Packet packet) {
        try {
            packet.processPacket(Raven.mc.getNetHandler());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
