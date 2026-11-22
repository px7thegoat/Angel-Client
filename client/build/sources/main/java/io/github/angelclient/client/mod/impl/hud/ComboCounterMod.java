package io.github.angelclient.client.mod.impl.hud;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.EntityAttackEvent;
import io.github.angelclient.client.event.impl.EntityDamageEvent;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import io.github.angelclient.client.util.data.Position;
import net.minecraft.client.resources.I18n;

public class ComboCounterMod extends SimpleHudMod {

    private long hitTime = -1;
    private int combo;
    private int possibleTarget;

    @Override
    public String getId() {
        return "combo_counter";
    }

    @Override
    public void render(Position position, boolean editMode) {
        super.render(position, editMode);
        // Reseta o combo se passar 2 segundos sem hit
        if((System.currentTimeMillis() - hitTime) > 2000) {
            combo = 0;
        }
    }

    @Override
    public String getText(boolean editMode) {
        // Se estiver editando ou o combo for 0, mostra "No hits" (ou tradução correspondente)
        if(editMode || combo == 0) {
            return I18n.format("angel_client.mod.combo_counter.no_hits");
        } 
        // Se o combo for 1, mostra "1 hit"
        else if(combo == 1) {
            return I18n.format("angel_client.mod.combo_counter.one_hit");
        } 
        // Para mais de 1 hit, mostra "X hits" usando o formatador
        else {
            return I18n.format("angel_client.mod.combo_counter.n_hits", combo);
        }
    }

    public void dealHit() {
        combo++;
        possibleTarget = -1;
        hitTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onEntityAttack(EntityAttackEvent event) {
        possibleTarget = event.victim.getEntityId();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.entity.getEntityId() == possibleTarget) {
            dealHit();
        }
        else if(event.entity == mc.thePlayer) {
            takeHit();
        }
    }

    public void takeHit() {
        combo = 0;
    }
}