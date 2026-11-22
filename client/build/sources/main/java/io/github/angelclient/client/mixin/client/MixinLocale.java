package io.github.angelclient.client.mixin.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;
import net.minecraft.util.ResourceLocation;

@Mixin(Locale.class)
public abstract class MixinLocale {

	@Overwrite
	public synchronized void loadLocaleDataFiles(IResourceManager resourceManager, List<String> languageCodes) {
		properties.clear();

		for(String language : languageCodes) {
			String vanillaLang = String.format("lang/%s.lang", language);
			String ofLang = String.format("optifine/lang/%s.lang", language);

			for(String domain : resourceManager.getResourceDomains()) {
				try {
					loadLocaleData(resourceManager.getAllResources(new ResourceLocation(domain, vanillaLang)));
					loadLocaleData(resourceManager.getAllResources(new ResourceLocation(domain, ofLang)));
				}
				catch(IOException ignored) {
				}
			}
		}

		checkUnicode();
	}

	/**
	 * Sobrescreve formatMessage para evitar NullPointerException quando a chave
	 * de tradução não existe no .lang. O Minecraft passa o valor do mapa como
	 * format string — se a chave não existir, o valor é null e String.format
	 * lança NPE. Retornamos a própria chave como fallback (comportamento do Forge).
	 */
	@Overwrite
	public String formatMessage(String key, Object... args) {
		String value = properties.get(key);
		if (value == null) {
			// Chave não encontrada — retorna a própria chave como fallback
			return key;
		}
		try {
			return String.format(value, args);
		} catch (Exception e) {
			// Format string inválida — retorna o valor bruto sem formatar
			return value;
		}
	}

	@Shadow
	protected abstract void loadLocaleData(List<IResource> resourcesList) throws IOException;

	@Shadow
	private Map<String, String> properties;

	@Shadow
	protected abstract void checkUnicode();

}