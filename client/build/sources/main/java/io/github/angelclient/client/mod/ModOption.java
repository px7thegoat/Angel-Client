package io.github.angelclient.client.mod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.annotation.AbstractTranslationKey;
import io.github.angelclient.client.mod.annotation.FileOption;
import io.github.angelclient.client.mod.annotation.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

public class ModOption {

	private static final Logger LOGGER = LogManager.getLogger();

	private final Mod mod;
	private final Field field;
	private String translationKey;
	private final int priority;
	private FileOption configFile;
	private File file;

	public File getFile() { return file; }
	private String applyToAllClass;

	public ModOption(Mod mod, Option option, Field field) throws IOException {
		this.mod = mod;
		this.field = field;

		if (field != null) {
			field.setAccessible(true);

			if (field.isAnnotationPresent(FileOption.class)) {
				configFile = field.getAnnotation(FileOption.class);
				file = new File(Minecraft.getMinecraft().mcDataDir, configFile.file());
				readFile();
			}

			String name = field.getDeclaringClass() + "." + field.getName();

			if (Modifier.isFinal(field.getModifiers()) && !(field.getType() == KeyBinding.class)) {
				LOGGER.warn("Mod option {} is final", name);
			}

			if (getValue() == null) {
				LOGGER.warn("Mod option {} has no default value. This may cause a crash.", name);
			}
		}

		priority = option.priority();

		if (!option.translationKey().isEmpty()) {
			translationKey = option.translationKey();
		}

		if (translationKey == null) {
			if (field != null && field.getDeclaringClass().isAnnotationPresent(AbstractTranslationKey.class)) {
				translationKey = field.getDeclaringClass().getAnnotation(AbstractTranslationKey.class).value();
			} else {
				translationKey = mod.getTranslationKey();
			}
		}

		applyToAllClass = option.applyToAllClass();

		if (applyToAllClass.isEmpty()) {
			applyToAllClass = null;
		}
	}

	public boolean isFile() {
		return file != null;
	}

	public String getEditText() {
		return I18n.format(configFile.text());
	}

	public void readFile() throws IOException {
		if (!file.exists()) {
			FileUtils.writeStringToFile(file, configFile.header(), StandardCharsets.UTF_8);
		}

		String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		setValue(contents);
		mod.onFileUpdate(field.getName());
	}

	public String getName() {
		return I18n.format(translationKey + ".option." + field.getName());
	}

	public Class<?> getType() {
		return field.getType();
	}

	public Object getValue() {
		try {
			return field.get(mod);
		} catch (IllegalArgumentException | IllegalAccessException error) {
			throw new IllegalStateException(error);
		}
	}

	public void setValue(Object value) {
		try {
			if (!value.equals(field.get(mod))) {
				if (mod.onOptionChange(field.getName(), value)) {
					field.set(mod, value);
					mod.postOptionChange(field.getName(), value);
					// BUG FIX: mudanças de opções (slider, enum, cor, boolean)
					// nunca eram salvas. optionChanged() existia mas nenhum
					// código o chamava após setValue(). Agora toda alteração
					// de opção agenda um save.
					Client.INSTANCE.optionChanged();
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException error) {
			throw new IllegalStateException(error);
		}
	}

	public static List<ModOption> get(Mod mod) throws IOException {
		List<ModOption> result = new ArrayList<>();
		add(mod, mod.getClass(), result);

		if (mod instanceof ConfigOnlyMod) {
			result.remove(0);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<ModOption> add(Mod mod, Class<? extends Mod> clazz, List<ModOption> list) throws IOException {
		List<Field> fields = new ArrayList<>();

		for (Field field : clazz.getDeclaredFields()) {
			if (!fields.contains(field)) {
				fields.add(field);
			}
		}

		for (Field field : clazz.getFields()) {
			if (!fields.contains(field)) {
				fields.add(field);
			}
		}

		for (Field field : fields) {
			if (field.isAnnotationPresent(Option.class)) {
				if (list.stream().noneMatch((cached) -> cached.field.equals(field))) {
					list.add(new ModOption(mod, field.getAnnotation(Option.class), field));
				}
			}
		}

		// FIX: uso seguro de asSubclass() em vez de cast direto (elimina unchecked warning)
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != null && Mod.class.isAssignableFrom(superClass)) {
			add(mod, superClass.asSubclass(Mod.class), list);
		}

		list.sort(Comparator.comparingInt(c -> -c.priority));

		return list;
	}

	public boolean canApplyToAll() {
		return applyToAllClass != null;
	}

	public void applyToAll() {
		String key = applyToAllClass;

		if (key == null) {
			return;
		}

		for (Mod mod : Client.INSTANCE.getMods()) {
			for (ModOption option : mod.getOptions()) {
				if (option.canApplyToAll() && option.applyToAllClass.equals(key)
						&& option.getType() == getType()) {
					option.setValue(getValue());
				}
			}
		}
	}

	public Field getField() { return field; }

}