package io.github.angelclient.client.cosmetic.wavey;

import io.github.angelclient.client.mod.impl.WaveyCapesMod;
import net.minecraft.entity.player.EntityPlayer;

public class CapeHolder {

    private StickSimulation3d simulation;
    private final Vector3 lastPlayerPos = new Vector3();
    private final Vector3 changeVector = new Vector3();

    public void simTick(MinecraftPlayer player) {
        if (!WaveyCapesMod.enabled) return;

        if (simulation == null) {
            simulation = new StickSimulation3d();
            simulation.init(16);
            lastPlayerPos.copy(new Vector3((float) player.getX(), (float) player.getY(), (float) player.getZ()));
        }

        double d = player.getXCloak() - player.getX();
        double m = player.getZCloak() - player.getZ();
        float n = player.getYBodyRotO() + (player.getYBodyRot() - player.getYBodyRotO());
        double o = Math.sin(n * 0.017453292F);
        double p = -Math.cos(n * 0.017453292F);

        float heightMul = WaveyCapesConfigInstance.get().heightMultiplier;
        float fallHack = 0;

        if (player.isUnderWater()) {
            heightMul *= 2;
        }

        double vertical = player.getY() - lastPlayerPos.y;
        if (vertical < 0 && !player.isUnderWater() && !player.isCrouching()) {
            fallHack = (float) vertical;
        }

        float yawRad = player.getYRot() * 0.017453292F;
        double strafeX = (player.getX() - lastPlayerPos.x);
        double strafeZ = (player.getZ() - lastPlayerPos.z);
        float strafeMul = 2;

        changeVector.x = (float) (d * o + m * p + fallHack);
        changeVector.y = (float) (vertical * heightMul);
        changeVector.z = (float) (-(strafeX * Math.cos(yawRad) + strafeZ * Math.sin(yawRad)) * strafeMul);

        if (player.isCrouching()) {
            changeVector.y += 0.3F;
            changeVector.x += 0.15F;
        }

        if (player.isVisuallySwimming()) {
            float pitchRad = player.getXRot() * 0.017453292F;
            float cosPitch = (float) Math.cos(pitchRad);
            float sinPitch = (float) Math.sin(pitchRad);
            changeVector.y *= cosPitch;
            changeVector.z *= cosPitch;
            changeVector.x -= changeVector.y * sinPitch;
            changeVector.y += changeVector.x * sinPitch;
        }

        simulation.applyMovement(changeVector);
        simulation.simulate();

        lastPlayerPos.x = (float) player.getX();
        lastPlayerPos.y = (float) player.getY();
        lastPlayerPos.z = (float) player.getZ();
    }

    public StickSimulation3d getSimulation() {
        return simulation;
    }

    public void reset() {
        simulation = null;
    }
}
