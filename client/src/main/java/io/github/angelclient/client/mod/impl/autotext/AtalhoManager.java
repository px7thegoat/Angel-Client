package io.github.angelclient.client.mod.impl.autotext;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia os atalhos de texto: CRUD, disparo por tecla e persistência em arquivo.
 */
public class AtalhoManager {

    private final File        configFile;
    private final List<Atalho> atalhos = new ArrayList<>();

    /** Cooldown entre disparos para evitar spam acidental. */
    private static final long COOLDOWN_MS = 250;
    private long lastTrigger = 0;

    public AtalhoManager(File mcDataDir) {
        this.configFile = new File(new File(mcDataDir, "config"), "angel_autotext.cfg");
        this.configFile.getParentFile().mkdirs();
        carregar();
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    public List<Atalho> getAtalhos() { return atalhos; }

    /** @return false se já existe um atalho com o mesmo nome. */
    public boolean adicionar(String nome, int keyCode, String mensagem) {
        if (buscarPorNome(nome) != null) return false;
        atalhos.add(new Atalho(nome, keyCode, mensagem));
        salvar();
        return true;
    }

    public boolean remover(int index) {
        if (index < 0 || index >= atalhos.size()) return false;
        atalhos.remove(index);
        salvar();
        return true;
    }

    public boolean editar(int index, String novoNome, int novoKeyCode, String novaMensagem) {
        if (index < 0 || index >= atalhos.size()) return false;
        Atalho a = atalhos.get(index);
        a.setNome(novoNome);
        a.setKeyCode(novoKeyCode);
        a.setMensagem(novaMensagem);
        salvar();
        return true;
    }

    public boolean toggleAtivo(int index) {
        if (index < 0 || index >= atalhos.size()) return false;
        Atalho a = atalhos.get(index);
        a.setAtivo(!a.isAtivo());
        salvar();
        return true;
    }

    public Atalho buscarPorNome(String nome) {
        for (Atalho a : atalhos)
            if (a.getNome().equalsIgnoreCase(nome)) return a;
        return null;
    }

    // -------------------------------------------------------------------------
    // Disparo por tecla
    // -------------------------------------------------------------------------

    /**
     * Verifica se algum atalho ativo corresponde ao keyCode pressionado e,
     * se sim, envia a mensagem no chat. Deve ser chamado apenas quando nenhuma
     * GUI de chat estiver aberta.
     */
    public void verificarTeclas(Minecraft mc, int keyCode) {
        long now = System.currentTimeMillis();
        if (now - lastTrigger < COOLDOWN_MS) return;

        for (Atalho a : atalhos) {
            if (!a.isAtivo() || a.getKeyCode() <= 0) continue;
            if (a.getKeyCode() == keyCode) {
                enviar(mc, a.getMensagem());
                lastTrigger = now;
                break;
            }
        }
    }

    private void enviar(Minecraft mc, String mensagem) {
        if (mc.thePlayer == null) return;
        String msg = mensagem
            .replace("{player}", mc.thePlayer.getName())
            .replace("{x}", String.valueOf((int) mc.thePlayer.posX))
            .replace("{y}", String.valueOf((int) mc.thePlayer.posY))
            .replace("{z}", String.valueOf((int) mc.thePlayer.posZ));
        mc.thePlayer.sendChatMessage(msg);
    }

    // -------------------------------------------------------------------------
    // Persistência
    // -------------------------------------------------------------------------

    public void salvar() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(configFile))) {
            pw.println("# Angel Client — AutoText atalhos");
            pw.println("# formato: nome|keyCode|mensagem|ativo");
            for (Atalho a : atalhos) {
                pw.println(esc(a.getNome()) + "|"
                        + a.getKeyCode()   + "|"
                        + esc(a.getMensagem()) + "|"
                        + a.isAtivo());
            }
        } catch (IOException e) {
            // O logger do mod vai capturar isso via AutoTextMod
            e.printStackTrace();
        }
    }

    public void carregar() {
        atalhos.clear();
        if (!configFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", 4);
                if (p.length < 4) continue;
                Atalho a = new Atalho(unesc(p[0]), Integer.parseInt(p[1]), unesc(p[2]));
                a.setAtivo(Boolean.parseBoolean(p[3]));
                atalhos.add(a);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // Escapa/desescapa o separador "|" dentro dos campos
    private String esc(String s)   { return s.replace("|", "\\|"); }
    private String unesc(String s) { return s.replace("\\|", "|"); }
}
