package io.github.angelclient.client.tweak;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class Tweaker implements ITweaker {

	public static boolean optiFine;
	private List<String> args = new ArrayList<>();
	private LaunchClassLoader classLoader;
	private Set<String> exceptions;

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		try {
			Class.forName("optifine.Patcher");
			optiFine = true;
		}
		catch(ClassNotFoundException ignored) {
		}

		this.args.addAll(args);

		if(gameDir != null) {
			this.args.add("--gameDir");
			this.args.add(gameDir.getAbsolutePath());
		}

		if(assetsDir != null) {
			this.args.add("--assetsDir");
			this.args.add(assetsDir.getAbsolutePath());
		}

		if(profile != null) {
			this.args.add("--version");
			this.args.add(profile);
		}
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		this.classLoader = classLoader;

		classLoader.registerTransformer("io.github.angelclient.client.tweak.transformer.ClassTransformer");

		MixinBootstrap.init();

		Mixins.addConfiguration("mixins.angelclient.json");


		MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();

		env.setObfuscationContext("searge");
		env.setSide(MixinEnvironment.Side.CLIENT);

		try {
			removeClassLoaderException("org.lwjgl.");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void removeClassLoaderException(String exception) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		if(exceptions == null) {
			getExceptions();
		}

		exceptions.remove(exception);
	}

	@SuppressWarnings("unchecked")
	private void getExceptions() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field exceptionsField = classLoader.getClass().getDeclaredField("classLoaderExceptions");
		exceptionsField.setAccessible(true);
		exceptions = (Set<String>) exceptionsField.get(classLoader);
	}

	@Override
	public String getLaunchTarget() {
		return "io.github.angelclient.client.PreMain";
	}

	@Override
	public String[] getLaunchArguments() {
		return args.toArray(new String[0]);
	}

}