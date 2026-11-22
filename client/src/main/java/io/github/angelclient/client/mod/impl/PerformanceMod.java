package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;

public class PerformanceMod extends Mod {

    public static PerformanceMod instance;

    // ─── Opção 1 ──────────────────────────────────────────────────────────────
    // Anti-Hologram: pula entidades ArmorStand invisíveis a >5 blocos.
    // Elimina draw-calls de holograms de NPC em servidores como Hypixel.
    @Expose
    @Option
    public boolean antiHologram = false;

    // ─── Opção 2 ──────────────────────────────────────────────────────────────
    // No Foliage: remove grama/fern do dispatcher de blocos.
    // Elimina centenas de polígonos alpha-tested por chunk em biomas densos.
    @Expose
    @Option
    public boolean noFoliage = false;

    // ─── Opção 3 ──────────────────────────────────────────────────────────────
    // Limit Particles: cap de 500 partículas simultâneas.
    // Evita spike de FPS em explosões e batalhas PvP.
    @Expose
    @Option
    public boolean limitParticles = true;

    // ─── Opção 4 ──────────────────────────────────────────────────────────────
    // No Clouds: desativa a geometria de nuvens por frame.
    // Ganho grátis em PvP sem impacto visual relevante.
    @Expose
    @Option
    public boolean noClouds = false;

    // ─── Opção 5 ──────────────────────────────────────────────────────────────
    // Limit Entity Render: cap de 150 entidades por frame.
    // Em lobbies com 200+ jogadores melhora FPS significativamente.
    @Expose
    @Option
    public boolean limitEntityRender = false;

    // ─── Opção 6 (NOVO — Badlion BetterFrames) ───────────────────────────────
    // No Weather: cancela renderRainSnow() completamente.
    // Chuva/neve usa quads alpha-tested extras — custo de 5-15% FPS em PvP.
    @Expose
    @Option
    public boolean noWeather = false;

    // ─── Opção 7 (NOVO — Badlion BetterFrames) ───────────────────────────────
    // No Fog: cancela setupFog() e setupFogColor() por frame.
    // Elimina ~6 chamadas GL_FOG extras por frame sem fog visível.
    @Expose
    @Option
    public boolean noFog = false;

    @Expose
    @Option
    public boolean reducedChunkUpdates = false;

    // ─── Visuals (Badlion Opts) ──────────────────────────────────────────────
    @Expose @Option public boolean hideTallGrass = false;
    @Expose @Option public boolean hideDoubleTallGrass = false;
    @Expose @Option public boolean hideFlowers = false;
    @Expose @Option public boolean hideDoubleTallFlowers = false;
    @Expose @Option public boolean hideFences = false;
    @Expose @Option public boolean hideFenceGates = false;
    @Expose @Option public boolean hideSkulls = false;
    @Expose @Option public boolean hideArmorStands = false;
    @Expose @Option public boolean hideItemFrames = false;
    @Expose @Option public boolean hideMapsInItemFrames = false;
    @Expose @Option public boolean hideStuckArrows = false;
    @Expose @Option public boolean hideGroundArrows = false;
    @Expose @Option public boolean hideParticleLighting = false;
    @Expose @Option public boolean hideLavaParticles = false;
    @Expose @Option public boolean hideMobInSpawner = false;
    @Expose @Option public boolean hideSpawnerParticles = false;
    @Expose @Option public boolean minimalPortalTextures = false;

    public PerformanceMod() {
        instance = this;
    }

    @Override
    public String getId() {
        return "performance";
    }

    @Override
    public String getName() {
        return "Performance";
    }

    @Override
    public String getDescription() {
        return "Opções avançadas de FPS Boost";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.UTILITY;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public boolean isLocked() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }
}