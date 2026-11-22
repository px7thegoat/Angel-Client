package io.github.angelclient.client.mod.impl.hud.crosshair;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class CrosshairEditorScreen extends GuiScreen {

    private static final int GRID = 16;
    private static final int CELL = 16;

    private boolean[][]  pixels;
    private CrosshairMod mod;
    private GuiScreen    parent;

    // ── Cor ───────────────────────────────────────────────────────────────────
    private int     colorR      = 255, colorG = 255, colorB = 255;
    private boolean vanillaColor = false;

    // ── Simetria ──────────────────────────────────────────────────────────────
    private boolean symmetryH = false;
    private boolean symmetryV = false;

    // ── Slider drag ───────────────────────────────────────────────────────────
    private int   dragSlider  = -1; // 0=R,1=G,2=B
    private int   dragSliderX0;
    private int   dragSliderV0;

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final int BG       = 0xFF0A0A0A;
    private static final int PANEL    = 0xFF111111;
    private static final int PANEL2   = 0xFF161616;
    private static final int BORDER   = 0xFF222222;
    private static final int BORDER2  = 0xFF333333;
    private static final int ACCENT   = 0xFFFFFFFF;
    private static final int TEXT     = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF555555;
    private static final int TEXT_MID = 0xFF888888;
    private static final int SEL      = 0x22FFFFFF;

    private static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFCCCCCC, 0xFF888888, 0xFF444444,
        0xFFFF4444, 0xFFFF8844, 0xFFFFFF44, 0xFF44FF44,
        0xFF44FFFF, 0xFF4488FF, 0xFF8844FF, 0xFFFF44FF,
        0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF000000
    };

    public CrosshairEditorScreen(GuiScreen parent, CrosshairMod mod, boolean[][] existing) {
        this.parent = parent;
        this.mod    = mod;
        this.pixels = new boolean[GRID][GRID];
        if (existing != null)
            for (int i = 0; i < GRID; i++)
                for (int j = 0; j < GRID; j++)
                    this.pixels[i][j] = existing[i][j];
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    // Janela ocupa quase a tela toda
    private int winW()   { return Math.min(width  - 20, 800); }
    private int winH()   { return Math.min(height - 20, 520); }
    private int winX()   { return (width  - winW()) / 2; }
    private int winY()   { return (height - winH()) / 2; }

    // Grid fica à esquerda
    private int gridX()  { return winX() + 14; }
    private int gridY()  { return winY() + 36; } // abaixo do título
    private int gridPx() { return GRID * CELL; }

    // Painel direito
    private int rPanX()  { return gridX() + gridPx() + 14; }
    private int rPanW()  { return winX() + winW() - rPanX() - 12; }

    // Footer
    private int footY()  { return winY() + winH() - 26; }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Overlay
        drawRect(0, 0, width, height, 0xBB000000);

        int wx = winX(), wy = winY(), ww = winW(), wh = winH();

        // Janela
        drawRect(wx+3, wy+3, wx+ww+3, wy+wh+3, 0x33000000);
        drawRect(wx, wy, wx+ww, wy+wh, BG);
        drawRect(wx, wy, wx+ww, wy+1,   BORDER2);
        drawRect(wx, wy, wx+1,  wy+wh,  BORDER2);
        drawRect(wx+ww-1, wy, wx+ww, wy+wh, BORDER);
        drawRect(wx, wy+wh-1, wx+ww, wy+wh, BORDER);
        drawRect(wx, wy, wx+2, wy+wh, ACCENT); // acento esquerdo

        // Título
        drawRect(wx, wy, wx+ww, wy+22, PANEL);
        drawRect(wx, wy+22, wx+ww, wy+23, BORDER);
        fontRendererObj.drawString("EDITOR DE MIRA", wx+10, wy+7, TEXT);

        // Botão X
        boolean hX = mouseX >= wx+ww-18 && mouseX <= wx+ww-6 && mouseY >= wy+5 && mouseY <= wy+17;
        fontRendererObj.drawString("X", wx+ww-14, wy+7, hX ? 0xFFFF4444 : TEXT_DIM);

        // ── Grid ──────────────────────────────────────────────────────────────
        drawGrid(mouseX, mouseY);

        // ── Painel direito ────────────────────────────────────────────────────
        drawRightPanel(mouseX, mouseY);

        // ── Footer ────────────────────────────────────────────────────────────
        drawRect(wx, footY(), wx+ww, footY()+26, PANEL);
        drawRect(wx, footY(), wx+ww, footY()+1, BORDER);

        int bw = 80, bh = 16, by2 = footY()+5;
        // Limpar
        drawFlatBtn(mouseX, mouseY, wx+10, by2, bw, bh, "LIMPAR", PANEL2, TEXT_MID);
        // Cancelar
        drawFlatBtn(mouseX, mouseY, wx+ww-bw*2-14, by2, bw, bh, "CANCELAR", PANEL2, TEXT_MID);
        // Salvar
        drawFlatBtn(mouseX, mouseY, wx+ww-bw-8, by2, bw, bh, "SALVAR", 0xFF002200, 0xFF44FF44);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGrid(int mx, int my) {
        int gx = gridX(), gy = gridY();
        int color = makeColor();

        // Fundo do grid
        drawRect(gx-1, gy-1, gx+gridPx()+1, gy+gridPx()+1, BORDER);

        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                int px = gx + x*CELL, py = gy + y*CELL;
                boolean lit = pixels[x][y];
                boolean checker = (x+y)%2 == 0;
                // Fundo xadrez
                drawRect(px, py, px+CELL, py+CELL,
                    lit ? color : (checker ? 0xFF141414 : 0xFF0F0F0F));
                // Grade
                drawRect(px, py, px+CELL, py+1,   BORDER);
                drawRect(px, py, px+1,    py+CELL, BORDER);
            }
        }

        // Linhas guia do centro
        int midX = gx + (GRID/2)*CELL;
        int midY = gy + (GRID/2)*CELL;
        drawRect(gx, midY, gx+gridPx(), midY+1, 0x33FF3333);
        drawRect(midX, gy, midX+1, gy+gridPx(), 0x33FF3333);

        // Pixel central [8][8] marcado
        drawRect(midX,        midY,        midX+CELL,   midY+1,      0x99FF2222);
        drawRect(midX,        midY,        midX+1,      midY+CELL,   0x99FF2222);
        drawRect(midX+CELL-1, midY,        midX+CELL,   midY+CELL,   0x66FF2222);
        drawRect(midX,        midY+CELL-1, midX+CELL,   midY+CELL,   0x66FF2222);
        if (!pixels[GRID/2][GRID/2])
            drawRect(midX+1, midY+1, midX+CELL-1, midY+CELL-1, 0x11FF0000);

        // Hover
        int hgx = (mx-gx)/CELL, hgy = (my-gy)/CELL;
        if (hgx>=0 && hgx<GRID && hgy>=0 && hgy<GRID) {
            int px = gx+hgx*CELL, py = gy+hgy*CELL;
            drawRect(px+1, py+1, px+CELL-1, py+CELL-1, 0x22FFFFFF);
        }
    }

    private void drawRightPanel(int mx, int my) {
        int rx = rPanX(), rw = rPanW();
        int ry = gridY();

        // ── Preview ───────────────────────────────────────────────────────────
        fontRendererObj.drawString("PREVIEW", rx, ry, TEXT_DIM);
        ry += 12;

        int ps = 3; // preview scale
        int pw = GRID*ps, ph = GRID*ps;
        int prx = rx + rw/2 - pw/2;
        int pry = ry;

        // Fundo xadrez do preview
        for (int i=0;i<pw;i+=4) for (int j=0;j<ph;j+=4)
            drawRect(prx+i, pry+j, prx+i+4, pry+j+4, ((i/4+j/4)%2==0)?0xFF181818:0xFF111111);
        drawRect(prx-1, pry-1, prx+pw+1, pry+ph+1, BORDER2);

        int color = makeColor();
        for (int x=0;x<GRID;x++)
            for (int y=0;y<GRID;y++)
                if (pixels[x][y])
                    drawRect(prx+x*ps, pry+y*ps, prx+x*ps+ps, pry+y*ps+ps, color);

        // Centro no preview
        int pcx = prx+(GRID/2)*ps, pcy = pry+(GRID/2)*ps;
        drawRect(pcx, pry, pcx+ps, pry+ph, 0x22FF0000);
        drawRect(prx, pcy, prx+pw, pcy+ps, 0x22FF0000);
        drawRect(pcx, pcy, pcx+ps, pcy+1,  0xAAFF2222);
        drawRect(pcx, pcy, pcx+1,  pcy+ps, 0xAAFF2222);

        ry += ph + 10;

        // ── Cor ───────────────────────────────────────────────────────────────
        fontRendererObj.drawString("COR", rx, ry, TEXT_DIM);
        drawRect(rx+25, ry-1, rx+25+14, ry+9, color);
        drawRect(rx+24, ry-2, rx+25+15, ry+10, BORDER);
        ry += 13;

        // Toggle Vanilla
        boolean hVan = mx>=rx && mx<rx+rw && my>=ry && my<ry+13;
        drawRect(rx, ry, rx+rw, ry+13, vanillaColor ? 0xFF0A1A0A : (hVan ? SEL : 0));
        drawRect(rx, ry, rx+rw, ry+1, vanillaColor ? 0xFF44FF44 : BORDER);
        String vanLbl = (vanillaColor ? "✓ " : "  ") + "Vanilla (branco blend)";
        fontRendererObj.drawString(vanLbl, rx+4, ry+3, vanillaColor ? 0xFF44FF44 : TEXT_MID);
        ry += 17;

        // Sliders RGB
        if (!vanillaColor) {
            int sw = rw;
            String[] lbls  = {"R","G","B"};
            int[]    vals  = {colorR, colorG, colorB};
            int[]    scols = {0xFFFF3333, 0xFF33FF33, 0xFF3333FF};

            for (int i = 0; i < 3; i++) {
                fontRendererObj.drawString(lbls[i], rx, ry+2, scols[i]);
                int sx = rx+10, sy2 = ry, sh = 10;
                float t = vals[i]/255f;

                // Trilho
                drawRect(sx, sy2, sx+sw-10, sy2+sh, 0xFF0A0A0A);
                drawRect(sx, sy2, sx+sw-10, sy2+1,   BORDER);
                drawRect(sx, sy2+sh-1, sx+sw-10, sy2+sh, BORDER);
                drawRect(sx, sy2, sx+1, sy2+sh, BORDER);
                drawRect(sx+sw-11, sy2, sx+sw-10, sy2+sh, BORDER);

                // Fill colorido
                int fillW = (int)(t*(sw-12));
                if (fillW > 0) drawRect(sx+1, sy2+1, sx+1+fillW, sy2+sh-1,
                    (scols[i] & 0x00FFFFFF) | 0x55000000);

                // Thumb
                int tx = sx + (int)(t*(sw-10-8));
                drawRect(tx, sy2-1, tx+8, sy2+sh+1, PANEL2);
                drawRect(tx, sy2-1, tx+8, sy2, BORDER2);
                drawRect(tx, sy2+sh, tx+8, sy2+sh+1, BORDER);
                drawRect(tx, sy2-1, tx+1, sy2+sh+1, BORDER2);
                drawRect(tx+7, sy2-1, tx+8, sy2+sh+1, BORDER);

                // Valor
                String vStr = String.valueOf(vals[i]);
                fontRendererObj.drawString(vStr, sx+sw-8, ry+1,
                    fontRendererObj.getStringWidth(vStr) > 0 ? TEXT_MID : TEXT_DIM);

                ry += 14;
            }
        } else {
            fontRendererObj.drawString("Cor controlada pelo blending", rx, ry+2, TEXT_DIM);
            ry += 14*3;
        }

        ry += 4;

        // ── Paleta rápida ─────────────────────────────────────────────────────
        fontRendererObj.drawString("PALETA", rx, ry, TEXT_DIM);
        ry += 11;
        int palS = 12, palG = 3, palCols = 8;

        // Desativa textura e blend para desenhar cores sólidas
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();

        for (int i = 0; i < PALETTE.length; i++) {
            int px2 = rx + (i % palCols) * (palS + palG);
            int py2 = ry + (i / palCols) * (palS + palG);
            int c   = PALETTE[i];
            float r2 = ((c >> 16) & 0xFF) / 255f;
            float g2 = ((c >>  8) & 0xFF) / 255f;
            float b2 = ( c        & 0xFF) / 255f;

            GL11.glColor3f(r2, g2, b2);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(px2,       py2);
            GL11.glVertex2f(px2,       py2+palS);
            GL11.glVertex2f(px2+palS,  py2+palS);
            GL11.glVertex2f(px2+palS,  py2);
            GL11.glEnd();
        }

        // Restaura estado GL
        GL11.glColor3f(1f, 1f, 1f);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();

        // Borda hover por cima
        for (int i = 0; i < PALETTE.length; i++) {
            int px2 = rx + (i % palCols) * (palS + palG);
            int py2 = ry + (i / palCols) * (palS + palG);
            boolean hov = mx >= px2 && mx < px2+palS && my >= py2 && my < py2+palS;
            drawRect(px2-1, py2-1, px2+palS+1, py2+palS+1, hov ? ACCENT : BORDER);
            if (hov) drawRect(px2, py2, px2+palS, py2+palS, 0x33FFFFFF);
        }
        ry += (PALETTE.length/palCols)*(palS+palG) + 8;

        // ── Simetria ──────────────────────────────────────────────────────────
        fontRendererObj.drawString("SIMETRIA", rx, ry, TEXT_DIM);
        ry += 11;

        int hbw = rw/2 - 3;
        drawToggleBtn(mx, my, rx,        ry, hbw, 14, "H", symmetryH);
        drawToggleBtn(mx, my, rx+hbw+6,  ry, hbw, 14, "V", symmetryV);
        ry += 18;

        // ── Presets ───────────────────────────────────────────────────────────
        fontRendererObj.drawString("PRESET", rx, ry, TEXT_DIM);
        ry += 11;

        String[] pNames = {"Cruz", "Ponto", "Circulo", "Quad"};
        int pbw = rw/4 - 2;
        for (int i=0; i<pNames.length; i++) {
            int px3 = rx + i*(pbw+3);
            boolean hov = mx>=px3 && mx<px3+pbw && my>=ry && my<ry+13;
            drawRect(px3, ry, px3+pbw, ry+13, hov ? SEL : PANEL2);
            drawRect(px3, ry, px3+pbw, ry+1, hov ? BORDER2 : BORDER);
            int lw = fontRendererObj.getStringWidth(pNames[i]);
            fontRendererObj.drawString(pNames[i], px3+pbw/2-lw/2, ry+3,
                hov ? TEXT : TEXT_MID);
        }
    }

    private void drawToggleBtn(int mx, int my, int x, int y, int w, int h, String lbl, boolean on) {
        boolean hov = mx>=x && mx<x+w && my>=y && my<y+h;
        drawRect(x, y, x+w, y+h, on ? 0xFF0A1A0A : (hov ? SEL : PANEL2));
        drawRect(x, y, x+w, y+1, on ? 0xFF44FF44 : BORDER);
        String full = (on ? "✓ SIM-" : "  SIM-") + lbl;
        int fw = fontRendererObj.getStringWidth(full);
        fontRendererObj.drawString(full, x+w/2-fw/2, y+(h-8)/2,
            on ? 0xFF44FF44 : TEXT_MID);
    }

    private void drawFlatBtn(int mx, int my, int x, int y, int w, int h, String lbl, int bg, int tc) {
        boolean hov = mx>=x && mx<x+w && my>=y && my<y+h;
        drawRect(x, y, x+w, y+h, hov ? bg : PANEL2);
        drawRect(x, y, x+w, y+1, hov ? tc : BORDER);
        drawRect(x, y, x+1, y+h, hov ? tc : BORDER);
        drawRect(x+w-1, y, x+w, y+h, BORDER);
        drawRect(x, y+h-1, x+w, y+h, BORDER);
        int lw = fontRendererObj.getStringWidth(lbl);
        fontRendererObj.drawString(lbl, x+w/2-lw/2, y+(h-8)/2, hov ? tc : TEXT_MID);
    }

    private int makeColor() {
        if (vanillaColor) return 0xFFFFFFFF;
        return 0xFF000000 | (colorR<<16) | (colorG<<8) | colorB;
    }

    private void setPixel(int x, int y, boolean v) {
        pixels[x][y] = v;
        if (symmetryH) pixels[GRID-1-x][y] = v;
        if (symmetryV) pixels[x][GRID-1-y] = v;
        if (symmetryH && symmetryV) pixels[GRID-1-x][GRID-1-y] = v;
    }

    private void applyPreset(int id) {
        pixels = new boolean[GRID][GRID];
        switch (id) {
            case 0: // Cruz clássica estilo Badlion
                for (int i=2; i<GRID-2; i++) {
                    if (i < GRID/2-1 || i > GRID/2+1) { // gap central
                        pixels[i][GRID/2] = true;
                        pixels[GRID/2][i] = true;
                    }
                }
                break;
            case 1: // Ponto central
                pixels[GRID/2][GRID/2] = true;
                pixels[GRID/2-1][GRID/2] = true;
                pixels[GRID/2][GRID/2-1] = true;
                pixels[GRID/2+1][GRID/2] = true;
                pixels[GRID/2][GRID/2+1] = true;
                break;
            case 2: // Círculo
                int cx=GRID/2, cy=GRID/2, r=5;
                for (int x=0;x<GRID;x++) for (int y=0;y<GRID;y++) {
                    int d=(x-cx)*(x-cx)+(y-cy)*(y-cy);
                    if (d>=(r-1)*(r-1) && d<=r*r) pixels[x][y]=true;
                }
                break;
            case 3: // Quadrado borda
                for (int i=4;i<12;i++) {
                    pixels[i][4]=true; pixels[i][11]=true;
                    pixels[4][i]=true; pixels[11][i]=true;
                }
                break;
        }
    }

    // ── Mouse click ───────────────────────────────────────────────────────────
    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        int wx = winX(), wy = winY(), ww = winW();

        // X fechar
        if (mx>=wx+ww-18 && mx<=wx+ww-6 && my>=wy+5 && my<=wy+17) {
            mc.displayGuiScreen(parent); return;
        }

        // Footer
        int bw = 80, bh = 16, by2 = footY()+5;
        if (mx>=wx+10 && mx<wx+10+bw && my>=by2 && my<by2+bh) {
            pixels = new boolean[GRID][GRID]; return;
        }
        if (mx>=wx+ww-bw*2-14 && mx<wx+ww-bw-14 && my>=by2 && my<by2+bh) {
            mc.displayGuiScreen(parent); return;
        }
        if (mx>=wx+ww-bw-8 && mx<wx+ww-8 && my>=by2 && my<by2+bh) {
            mod.setCustomPixels(pixels);
            if (!vanillaColor) {
                mod.setCrosshairColour(new io.github.angelclient.client.util.data.Colour(colorR, colorG, colorB, 255));
            } else {
                // Vanilla = branco puro com blend
                mod.setCrosshairColour(new io.github.angelclient.client.util.data.Colour(255, 255, 255, 255));
            }
            mc.displayGuiScreen(parent); return;
        }

        // Grid
        int gx=gridX(), gy=gridY();
        int hgx=(mx-gx)/CELL, hgy=(my-gy)/CELL;
        if (hgx>=0&&hgx<GRID&&hgy>=0&&hgy<GRID) {
            setPixel(hgx, hgy, btn==0); return;
        }

        // Painel direito
        int rx=rPanX(), rw=rPanW();
        int ry=gridY();
        int ps=3, ph=GRID*ps;
        ry += 12 + ph + 10; // após preview

        // Toggle vanilla
        ry += 13; // label "COR"
        if (mx>=rx && mx<rx+rw && my>=ry && my<ry+13) {
            vanillaColor=!vanillaColor; return;
        }
        ry += 17;

        // Sliders
        if (!vanillaColor) {
            int sw=rw;
            for (int i=0;i<3;i++) {
                int sx=rx+10, sy2=ry, sh=10;
                if (mx>=sx && mx<sx+sw-10 && my>=sy2 && my<sy2+sh) {
                    int v=Math.max(0,Math.min(255,(mx-sx)*255/(sw-11)));
                    if(i==0)colorR=v; else if(i==1)colorG=v; else colorB=v;
                    dragSlider=i; dragSliderX0=mx;
                    dragSliderV0=(i==0?colorR:i==1?colorG:colorB);
                    return;
                }
                ry+=14;
            }
        } else ry+=42;
        ry+=4;

        // Paleta
        ry+=11;
        int palS=12,palG=3,palCols=8;
        for (int i=0;i<PALETTE.length;i++) {
            int px2=rx+(i%palCols)*(palS+palG), py2=ry+(i/palCols)*(palS+palG);
            if(mx>=px2&&mx<px2+palS&&my>=py2&&my<py2+palS) {
                int c=PALETTE[i]; colorR=(c>>16)&0xFF; colorG=(c>>8)&0xFF; colorB=c&0xFF;
                vanillaColor=false; return;
            }
        }
        ry+=(PALETTE.length/palCols)*(palS+palG)+8;

        // Simetria
        ry+=11;
        int hbw=rw/2-3;
        if(mx>=rx&&mx<rx+hbw&&my>=ry&&my<ry+14){symmetryH=!symmetryH;return;}
        if(mx>=rx+hbw+6&&mx<rx+rw&&my>=ry&&my<ry+14){symmetryV=!symmetryV;return;}
        ry+=18;

        // Presets
        ry+=11;
        int pbw=rw/4-2;
        for(int i=0;i<4;i++) {
            int px3=rx+i*(pbw+3);
            if(mx>=px3&&mx<px3+pbw&&my>=ry&&my<ry+13){applyPreset(i);return;}
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mx=Mouse.getX()*width/mc.displayWidth;
        int my=height-Mouse.getY()*height/mc.displayHeight-1;

        // Drag pintura no grid
        if (Mouse.isButtonDown(0)||Mouse.isButtonDown(1)) {
            int gx=gridX(),gy=gridY();
            int hgx=(mx-gx)/CELL, hgy=(my-gy)/CELL;
            if(hgx>=0&&hgx<GRID&&hgy>=0&&hgy<GRID)
                setPixel(hgx, hgy, Mouse.isButtonDown(0));
        }

        // Drag sliders
        if (dragSlider>=0) {
            if (Mouse.isButtonDown(0)) {
                int rw=rPanW();
                int delta=mx-dragSliderX0;
                int v=Math.max(0,Math.min(255,dragSliderV0+delta*255/(rw-11)));
                if(dragSlider==0)colorR=v; else if(dragSlider==1)colorG=v; else colorB=v;
            } else dragSlider=-1;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        try{super.mouseReleased(mx,my,state);}catch(Exception e){}
        dragSlider=-1;
    }

    @Override public boolean doesGuiPauseGame(){return false;}
}