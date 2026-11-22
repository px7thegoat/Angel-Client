package io.github.angelclient.client.mod.impl.autotext;

/**
 * Representa um atalho de texto: nome, tecla e mensagem a enviar.
 */
public class Atalho {

    private String  nome;
    private int     keyCode;
    private String  mensagem;
    private boolean ativo;

    public Atalho(String nome, int keyCode, String mensagem) {
        this.nome     = nome;
        this.keyCode  = keyCode;
        this.mensagem = mensagem;
        this.ativo    = true;
    }

    public String  getNome()                   { return nome; }
    public void    setNome(String nome)         { this.nome = nome; }

    public int     getKeyCode()                { return keyCode; }
    public void    setKeyCode(int keyCode)      { this.keyCode = keyCode; }

    public String  getMensagem()               { return mensagem; }
    public void    setMensagem(String mensagem) { this.mensagem = mensagem; }

    public boolean isAtivo()                   { return ativo; }
    public void    setAtivo(boolean ativo)      { this.ativo = ativo; }
}
