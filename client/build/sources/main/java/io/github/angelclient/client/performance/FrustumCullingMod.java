package io.github.angelclient.client.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;

public class FrustumCullingMod {

    private static final FrustumCullingMod INSTANCE = new FrustumCullingMod();

    public static FrustumCullingMod getInstance() {
        return INSTANCE;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private net.minecraft.client.renderer.culling.Frustum frustum = new net.minecraft.client.renderer.culling.Frustum();

    private boolean enabled = true;



    public boolean isEnabled() {
        return enabled;
    }



    /**
     * Chamado uma vez por render pass (MixinEntityRenderer.onRenderWorldPass).
     * Atualiza o frustum apenas quando a câmera mudou — evita recalcular
     * a cada frame quando o jogador está parado.
     */
    public void onRenderTick(float partialTicks) {
        Entity view = mc.getRenderViewEntity();
        if (mc.theWorld == null || view == null) return;

        double px = view.lastTickPosX + (view.posX - view.lastTickPosX) * (double)partialTicks;
        double py = view.lastTickPosY + (view.posY - view.lastTickPosY) * (double)partialTicks;
        double pz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double)partialTicks;

        // Re-cria o frustum para capturar as matrizes GL atuais (Projection/ModelView)
        this.frustum = new net.minecraft.client.renderer.culling.Frustum();
        this.frustum.setPosition(px, py, pz);
    }

    // ── Chunk Culling ─────────────────────────────────────────────────────────

    public boolean isChunkVisible(BlockPos pos) {
        if (!enabled || mc.thePlayer == null) return true;

        double x = pos.getX();
        double z = pos.getZ();

        double dx = mc.thePlayer.posX - (x + 8);
        double dz = mc.thePlayer.posZ - (z + 8);
        double distSq = dx * dx + dz * dz;

        // Além de 400 blocos nunca está no render distance — corta cedo
        if (distSq > 400 * 400) return false;

        // Perto o suficiente para sempre renderizar (evita flicker)
        if (distSq < 48 * 48) return true;

        return frustum.isBoxInFrustum(x, 0, z, x + 16, 256, z + 16);
    }

    // ── Entity Culling ────────────────────────────────────────────────────────

    public boolean shouldRenderEntity(Entity entity) {
        if (!enabled || mc.thePlayer == null) return true;
        if (entity.ignoreFrustumCheck) return true;

        // Jogador local sempre renderiza (câmera em 3ª pessoa)
        if (entity == mc.thePlayer) return true;

        double dx = mc.thePlayer.posX - entity.posX;
        double dy = mc.thePlayer.posY - entity.posY;
        double dz = mc.thePlayer.posZ - entity.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Além de 200 blocos não precisa renderizar
        if (distSq > 200 * 200) return false;

        // Dentro de 16 blocos sempre renderiza (evita corte próximo)
        if (distSq < 16 * 16) return true;

        return frustum.isBoundingBoxInFrustum(entity.getEntityBoundingBox());
    }
}