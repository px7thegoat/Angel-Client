package io.github.angelclient.client.ui.screen.mods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.text.DecimalFormat;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.resources.I18n;

import io.github.angelclient.client.mod.ModOption;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.AlignedBoundsController;
import io.github.angelclient.client.ui.component.controller.AnimatedColourController;
import io.github.angelclient.client.ui.component.controller.Controller;
import io.github.angelclient.client.ui.component.impl.ColourBoxComponent;
import io.github.angelclient.client.ui.component.impl.ColourPickerDialog;
import io.github.angelclient.client.ui.component.impl.LabelComponent;
import io.github.angelclient.client.ui.component.impl.ScaledIconComponent;
import io.github.angelclient.client.ui.component.impl.SliderComponent;
import io.github.angelclient.client.ui.component.impl.TextFieldComponent;
import io.github.angelclient.client.ui.component.impl.TickboxComponent;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Alignment;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class ModOptionComponent extends ScaledIconComponent {

    private static final Colour ROW_BG       = new Colour(13,  13,  13,  220);
    private static final Colour ROW_BG_HOV   = new Colour(25,  25,  25,  220);
    private static final Colour ROW_BORDER   = new Colour(35,  35,  35,  255);
    private static final Colour LABEL_COL    = new Colour(200, 200, 200, 255);
    private static final Colour ACCENT_COL   = new Colour(255, 255, 255, 255);
    private static final Colour BTN_NORMAL   = new Colour(180, 180, 180, 255);
    private static final Colour BTN_HOVER    = new Colour(255, 255, 255, 255);

    private static final int ROW_RADIUS = 6;

    private ModOption option;
    private boolean   listening;
    private int       enumWidth;

    private boolean isFirst = false;
    private boolean isLast  = false;

    public void setFirst(boolean first) { this.isFirst = first; }
    public void setLast(boolean last)   { this.isLast  = last;  }

    public ModOptionComponent(ModOption option) {
        super("angel_client_mod_option", 300, 22,
                (component, defaultColour) -> new Colour(13, 13, 13, 220));

        this.option = option;

        add(new LabelComponent(
                        (comp, def) -> option.getName(),
                        (comp, def) -> LABEL_COL),
                new AlignedBoundsController(Alignment.CENTRE, Alignment.CENTRE,
                        (component, defaultBounds) -> new Rectangle(
                                defaultBounds.getY(),
                                defaultBounds.getY(),
                                defaultBounds.getWidth(),
                                defaultBounds.getHeight())));

        Controller<Rectangle> rightAlign = new AlignedBoundsController(Alignment.END, Alignment.CENTRE,
                (component, defaultBounds) -> new Rectangle(
                        defaultBounds.getX() - defaultBounds.getY(),
                        defaultBounds.getY(),
                        defaultBounds.getWidth(),
                        defaultBounds.getHeight()));

        if (option.getType() == boolean.class) {
            add(new TickboxComponent((boolean) option.getValue(), option::setValue, this), rightAlign);
        }
        else if (option.getType() == Colour.class) {
            add(new ColourBoxComponent(
                            (component, defaultColour) -> (Colour) option.getValue(), this),
                    rightAlign);
            onClick((info, button) -> {
                if (button == 0) {
                    Utils.playClickSound(true);
                    screen.getRoot().setDialog(new ColourPickerDialog(
                            option,
                            (Colour) option.getValue(),
                            colour -> option.setValue(colour)));
                    return true;
                }
                return false;
            });
        }
        else if (option.getType() == String.class) {
            // Campo de texto inline — editável direto na linha da opção
            TextFieldComponent textField = new TextFieldComponent(110, false);
            String current = option.getValue() != null ? (String) option.getValue() : "";
            textField.setText(current);
            textField.onUpdate(text -> {
                option.setValue(text);
                return true;
            }).autoFlush();
            add(textField,
                (component, defaultBounds) -> {
                    Rectangle base = rightAlign.get(component, defaultBounds);
                    // base.getX() é a borda direita (alinhado pelo END).
                    // Posiciona o campo com margem de 4px da borda direita,
                    // e centraliza verticalmente dentro dos bounds do pai.
                    int fieldW = 110;
                    int fieldH = 12;
                    int x = base.getX() - fieldW - 4;
                    int y = (defaultBounds.getHeight() - fieldH) / 2;
                    return new Rectangle(x, y, fieldW, fieldH);
                });
        }
        else if (option.getType() == KeyBinding.class) {
            KeyBinding binding = (KeyBinding) option.getValue();
            add(new LabelComponent(
                    (component, defaultText) -> GameSettings.getKeyDisplayString(binding.getKeyCode()),
                    new AnimatedColourController((component, defaultColour) -> {
                        if (listening)             return new Colour(255, 220, 50);
                        if (isConflicting(binding)) return new Colour(255, 80, 80);
                        return isHovered() ? BTN_HOVER : BTN_NORMAL;
                    })), rightAlign);

            onClick((info, button) -> {
                if (button == 0) {
                    Utils.playClickSound(true);
                    listening = true;
                    screen.getRoot().onClickAnwhere((ignoredInfo, pressedButton) -> {
                        mc.gameSettings.setOptionKeyBinding(binding, pressedButton - 100);
                        listening = false;
                        mc.gameSettings.saveOptions();
                        KeyBinding.resetKeyBindingArrayAndHash();
                        screen.getRoot().onKeyPressed(null);
                        screen.getRoot().onClickAnwhere(null);
                        return true;
                    });
                    screen.getRoot().onKeyPressed((ignoredInfo, key, character) -> {
                        if      (key == 1)      mc.gameSettings.setOptionKeyBinding(binding, 0);
                        else if (key != 0)      mc.gameSettings.setOptionKeyBinding(binding, key);
                        else if (character > 0) mc.gameSettings.setOptionKeyBinding(binding, character + 256);
                        listening = false;
                        mc.gameSettings.saveOptions();
                        KeyBinding.resetKeyBindingArrayAndHash();
                        screen.getRoot().onKeyPressed(null);
                        screen.getRoot().onClickAnwhere(null);
                        return true;
                    });
                    return true;
                }
                return false;
            });
        }
        else if (option.getType().isEnum()) {
            try {
                Method valuesMethod = option.getType().getMethod("values");
                valuesMethod.setAccessible(true);
                Enum<?>[] fields = (Enum<?>[]) valuesMethod.invoke(null);

                add(new LabelComponent(
                        (component, defaultText) -> option.getValue().toString(),
                        new AnimatedColourController((component, defaultColour) ->
                                isHovered() ? BTN_HOVER : BTN_NORMAL)),
                        (component, defaultBounds) -> {
                            if (enumWidth == 0) {
                                for (Enum<?> field : fields) {
                                    int nw = (int) font.getWidth(field.toString());
                                    if (nw > enumWidth) enumWidth = nw;
                                }
                            }
                            Rectangle base = rightAlign.get(component, defaultBounds);
                            return new Rectangle(
                                    getBounds().getWidth() - enumWidth - 16 + (enumWidth / 2)
                                            - ((int) font.getWidth(((LabelComponent) component).getText()) / 2),
                                    base.getY(),
                                    base.getWidth(),
                                    base.getHeight());
                        });

                Component previous;
                add(new ScaledIconComponent("angel_client_next", 8, 8,
                        new AnimatedColourController((component, defaultColour) ->
                                component.isHovered() ? BTN_HOVER : BTN_NORMAL)),
                        rightAlign);

                add(previous = new ScaledIconComponent("angel_client_previous", 8, 8,
                        new AnimatedColourController((component, defaultColour) ->
                                component.isHovered() ? BTN_HOVER : BTN_NORMAL)),
                        (component, defaultBounds) -> {
                            Rectangle base = rightAlign.get(component, defaultBounds);
                            return new Rectangle(
                                    base.getX() - enumWidth - 12,
                                    base.getY(),
                                    base.getWidth(),
                                    base.getHeight());
                        });

                onClick((info, button) -> {
                    if (button == 0) {
                        Utils.playClickSound(true);
                        int current   = ((Enum<?>) option.getValue()).ordinal();
                        boolean back  = GuiScreen.isShiftKeyDown();
                        if (previous.isHovered()) back = !back;
                        if (back) current = (current - 1 < 0) ? fields.length - 1 : current - 1;
                        else      current = (current + 1 > fields.length - 1) ? 0 : current + 1;
                        option.setValue(fields[current]);
                        return true;
                    }
                    return false;
                });

            } catch (NoSuchMethodException | IllegalArgumentException
                     | IllegalAccessException | InvocationTargetException error) {
                throw new IllegalStateException(error);
            }
        }
        else if (option.getType() == float.class
                && option.getField().isAnnotationPresent(Slider.class)) {

            Slider sliderAnnotation = option.getField().getAnnotation(Slider.class);

            if (sliderAnnotation.showValue()) {
                add(new LabelComponent(
                        (component, defaultText) -> I18n.format(
                                sliderAnnotation.format(),
                                new DecimalFormat("0.##").format(option.getValue())),
                        (comp, def) -> ACCENT_COL),
                        (component, defaultBounds) -> {
                            Rectangle base = rightAlign.get(component, defaultBounds);
                            return new Rectangle(
                                    (int)(getBounds().getWidth()
                                            - font.getWidth(((LabelComponent) component).getText()) - 117),
                                    base.getY(),
                                    defaultBounds.getWidth(),
                                    defaultBounds.getHeight());
                        });
            }

            add(new SliderComponent(
                    sliderAnnotation.min(),
                    sliderAnnotation.max(),
                    sliderAnnotation.step(),
                    (float) option.getValue(),
                    value -> option.setValue(value),
                    this),
                    (component, defaultBounds) -> {
                        defaultBounds = rightAlign.get(component, defaultBounds);
                        return new Rectangle(
                                defaultBounds.getX() - 5,
                                defaultBounds.getY(),
                                defaultBounds.getWidth(),
                                defaultBounds.getHeight());
                    });
        }
        else if (option.isFile()) {
            String text = option.getEditText();
            add(new LabelComponent(
                    (component, defaultText) -> text,
                    new AnimatedColourController((component, defaultColour) ->
                            isHovered() ? BTN_HOVER : BTN_NORMAL)),
                    rightAlign);

            onClick((info, button) -> {
                if (button == 0) {
                    Utils.playClickSound(true);
                    try {
                        Utils.openUrl(option.getFile().toURI().toURL().toString());
                    } catch (MalformedURLException error) {
                        throw new IllegalStateException(error);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void renderFallback(ComponentRenderInfo info) {
        Rectangle b = getRelativeBounds();
        int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
        boolean hov = isHovered();
        Colour  bg  = hov ? ROW_BG_HOV : ROW_BG;

       if (isFirst && isLast) {
            // CORREÇÃO APLICADA AQUI
            ModListing.fillRounded(x, y, w, h, ROW_RADIUS, ModListing.toInt(bg));
            ModListing.outlineRounded(x, y, w, h, ROW_RADIUS, ModListing.toInt(ROW_BORDER));
        } else {
            Utils.drawRectangle(b, bg);
            Utils.drawRectangle(new Rectangle(x, y + h - 1, w, 1), ROW_BORDER);
            Utils.drawRectangle(new Rectangle(x, y, 1, h), ROW_BORDER);
            Utils.drawRectangle(new Rectangle(x + w - 1, y, 1, h), ROW_BORDER);
        }

        if (hov) {
            int barH = isFirst ? h - ROW_RADIUS : (isLast ? h - ROW_RADIUS : h);
            int barY = isFirst ? y + ROW_RADIUS : y;
            Utils.drawRectangle(new Rectangle(x, barY, 2, barH), ACCENT_COL);
        }
    }

    @Override
    public boolean useFallback() { return true; }

    private boolean isConflicting(KeyBinding binding) {
        if (binding.getKeyCode() != 0) {
            for (KeyBinding testKey : mc.gameSettings.keyBindings) {
                if (testKey != binding && testKey.getKeyCode() == binding.getKeyCode())
                    return true;
            }
        }
        return false;
    }
}