package io.github.angelclient.client.util.render;

import io.github.angelclient.client.mod.impl.PerformanceMod;

public class ParticleOptimizer {

    private static int particles = 0;
    private static final int MAX_PARTICLES = 500;

    public static boolean allowParticle() {
        // Só aplica o cap se a opção estiver ligada no PerformanceMod
        PerformanceMod perf = PerformanceMod.instance;
        if (perf == null || !perf.isEnabled() || !perf.limitParticles) return true;

        if (particles > MAX_PARTICLES) return false;
        particles++;
        return true;
    }

    public static void reset() {
        particles = 0;
    }
}