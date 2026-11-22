package io.github.angelclient.client.util.font;

/**
 * LatoFonts — fontes Lato prontas para uso no AngelClient.
 *
 * Portado do MelonClient. Usa o SlickFontRenderer já existente no Angel,
 * carregando os arquivos .ttf das fontes Lato.
 *
 * INSTRUÇÕES DE INSTALAÇÃO:
 *   1. Coloque este arquivo em:
 *      src/main/java/io/github/angelclient/client/util/font/LatoFonts.java
 *
 *   2. Copie os arquivos .ttf para dentro do jar de recursos, em:
 *      src/main/resources/assets/angel_client/fonts/
 *      Arquivos necessários:
 *        Lato-Black.ttf
 *        Lato-Bold.ttf
 *        Lato-Light.ttf
 *        Lato-Hairline.ttf
 *
 *   3. Use as fontes no seu código:
 *      LatoFonts.BOLD.renderString("Texto", x, y, 0xFFFFFFFF);
 *      LatoFonts.LIGHT.renderCenteredString("Texto", cx, cy, 0xFFFFFFFF);
 */
public final class LatoFonts {

    private static final String PATH = "/assets/angel_client/fonts/";

    /** Lato Black — para títulos e cabeçalhos em destaque */
    public static final SlickFontRenderer BLACK    = new SlickFontRenderer(PATH + "Lato-Black.ttf",    9);

    /** Lato Bold — uso geral em botões, labels, etc. */
    public static final SlickFontRenderer BOLD     = new SlickFontRenderer(PATH + "Lato-Bold.ttf",     9);

    /** Lato Light — para textos secundários, subtítulos */
    public static final SlickFontRenderer LIGHT    = new SlickFontRenderer(PATH + "Lato-Light.ttf",    9);

    /** Lato Hairline — para textos pequenos ou decorativos */
    public static final SlickFontRenderer HAIRLINE = new SlickFontRenderer(PATH + "Lato-Hairline.ttf", 9);

    // Versões maiores (tamanho 18) para títulos
    public static final SlickFontRenderer BLACK_18    = new SlickFontRenderer(PATH + "Lato-Black.ttf",    18);
    public static final SlickFontRenderer BOLD_18     = new SlickFontRenderer(PATH + "Lato-Bold.ttf",     18);
    public static final SlickFontRenderer LIGHT_18    = new SlickFontRenderer(PATH + "Lato-Light.ttf",    18);

    private LatoFonts() {}
}