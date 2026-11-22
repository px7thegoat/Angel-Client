package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.util.data.Colour;

// MessageEffect é usado pelo MixinNametagRenderer via NametagsMod.MessageEffect
/**
 * NametagsMod — controle visual centralizado de etiquetas de nome.
 *
 * Substitui as opções esparsas de nametag que viviam em TweaksMod,
 * expondo três toggles independentes acessíveis via mixin.
 *
 * Uso nos mixins:
 *   NametagsMod mod = NametagsMod.getInstance();
 *   if (mod == null || !mod.isEnabled()) { ... }   // módulo desligado
 *   mod.isShowNametags()                            // toggle global
 *   mod.isNametagBackground()                       // fundo translúcido
 *   mod.isTextShadow()                              // sombra no drawString
 *   mod.isShowIcon()                                // ícone Angel/custom
 */
public class NametagsMod extends Mod {

    /**
     * Efeito de cor aplicado à CustomMessage da nametag.
     */
    public enum MessageEffect {
        STATIC,
        RAINBOW,
        GRADIENT;

        @Override
        public String toString() {
            switch (this) {
                case STATIC:   return "Static";
                case RAINBOW:  return "Rainbow";
                case GRADIENT: return "Gradient";
                default:       return name();
            }
        }
    }


    // ── Singleton ────────────────────────────────────────────────────────────

    private static NametagsMod instance;

    public static NametagsMod getInstance() {
        return instance;
    }

    // ── Options ───────────────────────────────────────────────────────────────

    /**
     * Ativa ou desativa globalmente a renderização de todas as nametags
     * (jogadores, entidades, scoreboard, servidores).
     * Quando OFF: o mixin cancela renderName antes de qualquer desenho.
     */
    @Expose
    @Option
    private boolean showNametags = true;

    /**
     * Controla o retângulo preto translúcido atrás do texto.
     * Quando OFF: o bloco WorldRenderer com color(0,0,0,0.25) é suprimido.
     */
    @Expose
    @Option
    private boolean nametagBackground = true;

    /**
     * Passa {@code true/false} como parâmetro de sombra em
     * {@code fontRenderer.drawString(text, x, y, color, shadow)}.
     * Quando ON:  drawString com shadow=true  → texto tem sombra.
     * Quando OFF: drawString com shadow=false → texto sem sombra.
     */
    @Expose
    @Option
    private boolean textShadow = false;

    /**
     * Exibe a própria nametag do jogador quando em 3ª pessoa ou com Freelook.
     * Equivalente ao antigo 'Show Own Nametag' do TweaksMod, agora centralizado aqui.
     */
    @Expose
    @Option
    private boolean showOwnNametag = true;

    /**
     * Exibe ou oculta o ícone (Angel Client logo ou ícone customizado da tag)
     * ao lado esquerdo do nome do jogador.
     * Quando OFF: o bloco de bind/draw de textura é inteiramente suprimido
     * e o espaço reservado (iconSpace) é zerado, colando o fundo ao texto.
     */
    @Expose
    @Option
    private boolean showIcon = true;

    /**
     * Exibe uma borda ao redor da nametag.
     */
    @Expose
    @Option
    private boolean nametagBorder = true;

    /**
     * Cor da borda da nametag.
     * Por ser do tipo {@link Colour}, o ModOptionComponent renderiza
     * automaticamente o ColourBoxComponent (quadradinho colorido) e
     * abre o ColourPickerDialog ao clicar — igual ao CpsMod.
     * Padrão: branco opaco.
     */
    @Expose
    @Option
    private Colour nametagBorderColour = new Colour(255, 255, 255);

    /**
     * Ativa gradiente animado na borda da nametag.
     * Quando ON: a borda anima entre nametagBorderColour e nametagBorderGradientColour.
     * Outros usuários Angel Client verão o mesmo efeito via Firebase.
     */
    @Expose
    @Option
    private boolean nametagBorderGradient = false;

    /**
     * Segunda cor do gradiente da borda.
     * Usada apenas quando nametagBorderGradient=true.
     * O gradiente anima ciclicamente entre nametagBorderColour e esta cor.
     * Padrão: ciano.
     */
    @Expose
    @Option
    private Colour nametagBorderGradientColour = new Colour(0, 255, 255);

    /**
     * Mensagem personalizada exibida acima da nametag do jogador.
     * Visível para todos os usuários Angel Client via Firebase.
     * Deixe vazio para não exibir nada.
     * Máximo: 32 caracteres.
     */
    @Expose
    @Option
    private String nametagMessage = "";


    /**
     * Efeito de cor da CustomMessage.
     * STATIC = cor sólida branca, RAINBOW = arco-íris animado, GRADIENT = entre duas cores.
     */
    @Expose
    @Option
    private MessageEffect messageEffect = MessageEffect.STATIC;

    /**
     * Primeira cor do gradiente da CustomMessage (usada em GRADIENT).
     */
    @Expose
    @Option
    private Colour messageColour1 = new Colour(255, 255, 255);

    /**
     * Segunda cor do gradiente da CustomMessage (usada em GRADIENT).
     */
    @Expose
    @Option
    private Colour messageColour2 = new Colour(0, 200, 255);

    // ── Mod identity ─────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return "nametags";
    }

    @Override
    public String getName() {
        return "Nametags";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onRegister() {
        super.onRegister();
        instance = this;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    // ── Accessors (leitura pelos mixins) ─────────────────────────────────────

    /** @return {@code true} se nametags devem ser renderizadas globalmente. */
    public boolean isShowNametags() {
        return showNametags;
    }

    /** @return {@code true} se o fundo translúcido deve ser desenhado. */
    public boolean isNametagBackground() {
        return nametagBackground;
    }

    /**
     * @return {@code true} para passar {@code shadow=true} em drawString,
     *         {@code false} para passar {@code shadow=false}.
     */
    public boolean isTextShadow() {
        return textShadow;
    }

    /** @return {@code true} se a própria nametag do jogador deve ser exibida. */
    public boolean isShowOwnNametag() {
        return showOwnNametag;
    }

    /**
     * @return {@code true} se o ícone Angel/custom deve ser renderizado
     *         ao lado do nome; {@code false} para suprimir completamente.
     */
    public boolean isShowIcon() {
        return showIcon;
    }

    /**
     * @return {@code true} se a borda ao redor da nametag deve ser desenhada.
     */
    public boolean isNametagBorder() {
        return nametagBorder;
    }

    /**
     * @return A cor da borda como {@link Colour}.
     *         Use {@code getNametagBorderColour().getValue()} para obter
     *         o int ARGB, ou os floats via {@code getRedFloat()} etc.
     */
    public Colour getNametagBorderColour() {
        return nametagBorderColour;
    }

    /** @return {@code true} se a borda deve usar gradiente animado entre duas cores. */
    public boolean isNametagBorderGradient() {
        return nametagBorderGradient;
    }

    /** @return A segunda cor do gradiente da borda. */
    public Colour getNametagBorderGradientColour() {
        return nametagBorderGradientColour;
    }


    /** @return O efeito de cor da CustomMessage. */
    public MessageEffect getMessageEffect() {
        return messageEffect;
    }

    /** @return Primeira cor do gradiente da CustomMessage. */
    public Colour getMessageColour1() {
        return messageColour1;
    }

    /** @return Segunda cor do gradiente da CustomMessage. */
    public Colour getMessageColour2() {
        return messageColour2;
    }

    // ── Nametag Message ───────────────────────────────────────────────────────

    private String lastUploadedMessage = null;

    /** @return O texto personalizado acima da nametag (vazio = sem mensagem). */
    public String getNametagMessage() {
        return nametagMessage;
    }

    /**
     * Define o texto e faz upload automático para o Firebase se mudou.
     * Chamado pelo ModOption ao editar o campo de texto nas configs.
     */
    public void setNametagMessage(String msg) {
        if (msg == null) msg = "";
        this.nametagMessage = msg;
        syncNametagMessage();
    }

    /**
     * Sincroniza a mensagem local → Firebase.
     * Só faz upload se o valor mudou desde o último envio.
     * Deve ser chamado uma vez por tick (ou ao salvar).
     */
    public void syncNametagMessage() {
        String current = nametagMessage == null ? "" : nametagMessage.trim();
        if (current.equals(lastUploadedMessage)) return;
        lastUploadedMessage = current;
        io.github.angelclient.client.nametag.NametagManager mgr =
                io.github.angelclient.client.nametag.NametagManager.getInstance();
        if (mgr != null) mgr.uploadNametagMessage(current.isEmpty() ? null : current);
    }
}