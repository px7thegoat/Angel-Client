/*
 * Includes modified decompiled Replay Mod class files.
 * License: GPL v3 — see https://www.gnu.org/licenses/
 */

package io.github.angelclient.client.mod.impl.replay.fix;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.MatrixStack;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.callbacks.PostRenderScreenCallback;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import com.replaymod.lib.de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;

import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.GameOverlayElement;
import io.github.angelclient.client.event.impl.OpenGuiEvent;
import io.github.angelclient.client.event.impl.PostGuiInitEvent;
import io.github.angelclient.client.event.impl.PostGuiRenderEvent;
import io.github.angelclient.client.event.impl.PostRenderTickEvent;
import io.github.angelclient.client.event.impl.PreGameOverlayRenderEvent;
import io.github.angelclient.client.event.impl.PreGuiInitEvent;
import io.github.angelclient.client.event.impl.PreRenderTickEvent;
import io.github.angelclient.client.event.impl.PreTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class SCReplayModBackend extends SCEventRegistrations {

	private ReplayMod mod;

	public void init() {
		new File(Minecraft.getMinecraft().mcDataDir, "config").mkdirs();
		register();
		try {
			// Instancia o ReplayMod — o construtor define ReplayMod.instance internamente
			mod = new ReplayMod(this);

			// Garante sincronização com ReplayMod.instance caso seja campo estático separado
			try {
				Field instanceField = ReplayMod.class.getDeclaredField("instance");
				instanceField.setAccessible(true);
				Object existing = instanceField.get(null);
				if (existing == null) {
					instanceField.set(null, mod);
				} else {
					mod = (ReplayMod) existing;
				}
			} catch (NoSuchFieldException ignored) {
				// Sem campo instance estático — ok
			}

			Method initModulesMethod = mod.getClass().getDeclaredMethod("initModules");
			initModulesMethod.setAccessible(true);
			initModulesMethod.invoke(mod);
		}
		catch(NoSuchMethodException | IllegalAccessException error) {
			throw new Error(error);
		}
		catch (InvocationTargetException error) {
			if(error.getCause() instanceof Error) {
				throw (Error) error.getCause();
			}
			else if(error.getCause() instanceof RuntimeException) {
				throw (RuntimeException) error.getCause();
			}
		}
		KeyBinding.resetKeyBindingArrayAndHash();
	}

	public String getVersion() { return "2.6.1"; }
	public String getMinecraftVersion() { return "1.8.9"; }
	public boolean isModLoaded(String id) { return false; }

	@EventHandler
	public void onRenderHud(PreGameOverlayRenderEvent event) {
		if(!event.cancelled) {
			if(event.type == GameOverlayElement.ALL) {
				RenderHudCallback.EVENT.invoker().renderHud(new MatrixStack(), event.partialTicks);
			} else if(event.type == GameOverlayElement.CROSSHAIRS) {
				event.cancelled = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair() == Boolean.FALSE;
			}
		}
	}

	@EventHandler public void preGuiInit(PreGuiInitEvent event) { InitScreenCallback.Pre.EVENT.invoker().preInitScreen(event.screen); }
	@EventHandler public void onGuiInit(PostGuiInitEvent event) { InitScreenCallback.EVENT.invoker().initScreen(event.screen, event.buttonList); }
	@EventHandler public void onGuiOpen(OpenGuiEvent event) { OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(event.screen); }
	@EventHandler public void onGuiRender(PostGuiRenderEvent event) { PostRenderScreenCallback.EVENT.invoker().postRenderScreen(new MatrixStack(), event.partialTicks); }
	@EventHandler public void tickOverlay(PreTickEvent event) { PreTickCallback.EVENT.invoker().preTick(); }
	@EventHandler public void onPreRender(PreRenderTickEvent event) { PreRenderCallback.EVENT.invoker().preRender(); }
	@EventHandler public void onPostRender(PostRenderTickEvent event) { PostRenderCallback.EVENT.invoker().postRender(); }
}
