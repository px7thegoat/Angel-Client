package io.github.angelclient.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Release hardening pipeline:
 *
 * <ol>
 * <li>extracts the client classes from the ForgeGradle output JAR,</li>
 * <li>runs ProGuard (renaming, member obfuscation, debug info removal),</li>
 * <li>rewrites every string constant into an encrypted, lazily decoded call,</li>
 * <li>merges the hardened classes back into the distributable JAR,</li>
 * <li>signs the final JAR hash with the launcher key.</li>
 * </ol>
 */
public final class Protector {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String CLIENT_PACKAGE = "io/github/angelclient/";
	private static final String HELPER_PACKAGE = "io/github/angelclient/client/security/";

	private Protector() {
	}

	/**
	 * @param fullJar        the (already reobfuscated) client JAR
	 * @param proguardConfig template with the {@code @IN@} / {@code @OUT@} placeholders
	 * @param outJar         destination
	 * @param sigFile        destination of the signed checksum
	 * @param keyHex         shared launcher key
	 */
	public static void protect(File fullJar, File proguardConfig, File outJar, File sigFile, String keyHex,
			File dictionary, File minecraftJar) throws Exception {
		File work = Files.createTempDirectory("angel-protect").toFile();

		try {
			File thin = new File(work, "angel-thin.jar");
			File libraries = new File(work, "angel-libs.jar");
			File obfuscated = new File(work, "angel-obf.jar");
			File encrypted = new File(work, "angel-sec.jar");

			System.out.println("[Protector] extracting client classes...");
			extract(fullJar, thin, libraries);

			System.out.println("[Protector] running ProGuard...");
			runProGuard(thin, obfuscated, proguardConfig, dictionary, work, libraries, minecraftJar);

			File mapping = new File(work, "mapping.txt");

			if(mapping.isFile()) {
				Files.copy(mapping.toPath(),
						new File(outJar.getParentFile(), "proguard-mapping.txt").toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}

			System.out.println("[Protector] encrypting string constants...");
			encryptStrings(obfuscated, encrypted, keyHex, work);

			System.out.println("[Protector] merging hardened classes...");
			merge(fullJar, encrypted, outJar);

			System.out.println("[Protector] signing build...");
			sign(outJar, sigFile, keyHex);

			System.out.println("[Protector] done: " + outJar.getName() + " (" + outJar.length() + " bytes)");
		}
		finally {
			delete(work);
		}
	}

	/* ------------------------------------------------------------------ */

	private static void extract(File input, File thin, File libraries) throws Exception {
		try (ZipFile zip = new ZipFile(input);
				ZipOutputStream client = new ZipOutputStream(new FileOutputStream(thin));
				ZipOutputStream libs = new ZipOutputStream(new FileOutputStream(libraries))) {
			Enumeration<? extends ZipEntry> entries = zip.entries();

			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				boolean mine = entry.getName().startsWith(CLIENT_PACKAGE);
				ZipOutputStream out = mine ? client : libs;

				out.putNextEntry(new ZipEntry(entry.getName()));

				if(!entry.isDirectory()) {
					copy(zip.getInputStream(entry), out);
				}

				out.closeEntry();
			}
		}
	}

	private static void runProGuard(File input, File output, File template, File dictionary, File work,
			File libraries, File minecraftJar) throws Exception {
		String config = new String(Files.readAllBytes(template.toPath()), UTF8);
		config = config.replace("@IN@", input.getAbsolutePath().replace('\\', '/'));
		config = config.replace("@OUT@", output.getAbsolutePath().replace('\\', '/'));
		config = config.replace("@LIB@", libraries.getAbsolutePath().replace('\\', '/'));
		if(minecraftJar != null && minecraftJar.isFile()) {
			config = config.replace("@MCLIB@", minecraftJar.getAbsolutePath().replace('\\', '/'));
		}
		else {
			config = config.replace("-libraryjars @MCLIB@\n", "");
		}
		config = config.replace("@MAP@", new File(work, "mapping.txt").getAbsolutePath().replace('\\', '/'));
		config = config.replace("@DICT@", dictionary.getAbsolutePath().replace('\\', '/'));

		File configFile = new File(work, "proguard.pro");
		Files.write(configFile.toPath(), config.getBytes(UTF8));

		File proguardJar = new File(proguard.ProGuard.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

		ProcessBuilder builder = new ProcessBuilder(java, "-cp", proguardJar.getAbsolutePath(),
				"proguard.ProGuard", "@" + configFile.getAbsolutePath());
		builder.redirectErrorStream(true);

		Process process = builder.start();

		try (InputStream in = process.getInputStream()) {
			byte[] buffer = new byte[4096];
			int read;

			while((read = in.read(buffer)) > 0) {
				System.out.write(buffer, 0, read);
			}
		}

		int code = process.waitFor();

		if(code != 0) {
			throw new IllegalStateException("ProGuard failed with exit code " + code);
		}

		if(!output.isFile() || output.length() == 0) {
			throw new IllegalStateException("ProGuard did not produce an output JAR");
		}
	}

	private static void encryptStrings(File input, File output, String keyHex, File work) throws Exception {
		byte[] key = hexToBytes(keyHex);
		String suffix = Long.toHexString(Double.doubleToLongBits(Math.random()) ^ System.nanoTime());
		String helperName = HELPER_PACKAGE + "s" + suffix;
		byte[] helper = buildHelper(helperName, key, work);

		Set<String> skip = new HashSet<String>();
		skip.add(helperName + ".class");

		try (ZipFile zip = new ZipFile(input);
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output))) {
			Enumeration<? extends ZipEntry> entries = zip.entries();

			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				if(entry.isDirectory()) {
					continue;
				}

				byte[] data = readAll(zip.getInputStream(entry));

				if(entry.getName().endsWith(".class") && !skip.contains(entry.getName())) {
					byte[] hardened = hardenClass(data, helperName, key);
					data = hardened != null ? hardened : data;
				}

				write(out, entry.getName(), data);
			}

			write(out, helperName + ".class", helper);
		}
	}

	private static byte[] hardenClass(byte[] data, String helperName, byte[] key) {
		try {
			ClassNode node = new ClassNode();
			new ClassReader(data).accept(node, 0);

			boolean changed = false;

			for(MethodNode method : node.methods) {
				if(method.instructions == null) {
					continue;
				}

				AbstractInsnNode[] instructions = method.instructions.toArray();

				for(AbstractInsnNode instruction : instructions) {
					if(instruction.getOpcode() != Opcodes.LDC) {
						continue;
					}

					Object constant = ((LdcInsnNode) instruction).cst;

					if(!(constant instanceof String)) {
						continue;
					}

					String encrypted = encrypt((String) constant, key);
					MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC,
							helperName, "d", "(Ljava/lang/String;)Ljava/lang/String;", false);

					method.instructions.set(instruction, call);
					method.instructions.insertBefore(call, new LdcInsnNode(encrypted));
					changed = true;
				}
			}

			if(!changed) {
				return null;
			}

			ClassWriter writer = new ClassWriter(0);
			node.accept(writer);
			return writer.toByteArray();
		}
		catch(Throwable error) {
			System.err.println("[Protector] could not harden class: " + error);
			return null;
		}
	}

	private static String encrypt(String value, byte[] key) {
		byte[] bytes = value.getBytes(UTF8);
		char[] out = new char[bytes.length * 2];

		for(int i = 0; i < bytes.length; i++) {
			int encrypted = (bytes[i] ^ key[(i * 7 + 3) % key.length]) & 0xff;
			out[i * 2] = Character.forDigit(encrypted >> 4, 16);
			out[i * 2 + 1] = Character.forDigit(encrypted & 0xf, 16);
		}

		return new String(out);
	}

	private static byte[] buildHelper(String className, byte[] key, File work) throws Exception {
		String simpleName = className.substring(className.lastIndexOf('/') + 1);
		String packageName = className.substring(0, className.lastIndexOf('/')).replace('/', '.');

		StringBuilder source = new StringBuilder();
		source.append("package ").append(packageName).append(";\n");
		source.append("import java.util.HashMap;\n");
		source.append("public final class ").append(simpleName).append(" {\n");
		source.append("  private static final byte[] K = new byte[]{");
		for(int i = 0; i < key.length; i++) {
			if(i > 0) {
				source.append(',');
			}
			source.append("(byte)").append(key[i]);
		}
		source.append("};\n");
		source.append("  private static final HashMap<String,String> C = new HashMap<String,String>();\n");
		source.append("  public static String d(String s) {\n");
		source.append("    if(s == null) return null;\n");
		source.append("    String c = C.get(s);\n");
		source.append("    if(c != null) return c;\n");
		source.append("    int n = s.length() / 2;\n");
		source.append("    byte[] b = new byte[n];\n");
		source.append("    for(int i = 0; i < n; i++) {\n");
		source.append("      int hi = Character.digit(s.charAt(i * 2), 16);\n");
		source.append("      int lo = Character.digit(s.charAt(i * 2 + 1), 16);\n");
		source.append("      b[i] = (byte)(((hi << 4) | lo) ^ K[(i * 7 + 3) % K.length]);\n");
		source.append("    }\n");
		source.append("    try { c = new String(b, \"UTF-8\"); }\n");
		source.append("    catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }\n");
		source.append("    C.put(s, c);\n");
		source.append("    return c;\n");
		source.append("  }\n");
		source.append("}\n");

		File sourceRoot = new File(work, "helper-src");
		File classRoot = new File(work, "helper-classes");
		File sourceFile = new File(sourceRoot, className + ".java");
		sourceFile.getParentFile().mkdirs();
		classRoot.mkdirs();
		Files.write(sourceFile.toPath(), source.toString().getBytes(UTF8));

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		if(compiler == null) {
			throw new IllegalStateException("no Java compiler available for the string decoder");
		}

		int result = compiler.run(null, null, null,
				"-source", "1.8", "-target", "1.8", "-encoding", "UTF-8",
				"-d", classRoot.getAbsolutePath(), sourceFile.getAbsolutePath());

		if(result != 0) {
			throw new IllegalStateException("could not compile the string decoder");
		}

		File compiled = new File(classRoot, className + ".class");
		return Files.readAllBytes(compiled.toPath());
	}

	private static void merge(File fullJar, File hardened, File output) throws Exception {
		try (ZipFile base = new ZipFile(fullJar);
				ZipFile extra = new ZipFile(hardened);
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output))) {
			Enumeration<? extends ZipEntry> entries = base.entries();

			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				if(entry.getName().startsWith(CLIENT_PACKAGE)) {
					continue;
				}

				out.putNextEntry(new ZipEntry(entry.getName()));

				if(!entry.isDirectory()) {
					copy(base.getInputStream(entry), out);
				}

				out.closeEntry();
			}

			Enumeration<? extends ZipEntry> added = extra.entries();

			while(added.hasMoreElements()) {
				ZipEntry entry = added.nextElement();

				if(entry.isDirectory()) {
					continue;
				}

				out.putNextEntry(new ZipEntry(entry.getName()));
				copy(extra.getInputStream(entry), out);
				out.closeEntry();
			}
		}
	}

	private static void sign(File jar, File sigFile, String keyHex) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(Files.readAllBytes(jar.toPath()));
		String hashHex = hex(hash);

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(hexToBytes(keyHex), "HmacSHA256"));
		String signature = hex(mac.doFinal(("angel.jar.v1|" + hashHex).getBytes(UTF8)));

		Files.write(sigFile.toPath(), (signature + "\n").getBytes(UTF8));
	}

	/* ------------------------------------------------------------------ */

	private static byte[] readAll(InputStream in) throws Exception {
		try (InputStream stream = in) {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;

			while((read = stream.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}

			return out.toByteArray();
		}
	}

	private static void copy(InputStream in, OutputStream out) throws Exception {
		try (InputStream stream = in) {
			byte[] buffer = new byte[8192];
			int read;

			while((read = stream.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}
		}
	}

	private static void write(ZipOutputStream out, String name, byte[] data) throws Exception {
		out.putNextEntry(new ZipEntry(name));
		out.write(data);
		out.closeEntry();
	}

	private static byte[] hexToBytes(String hex) {
		byte[] bytes = new byte[hex.length() / 2];

		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}

		return bytes;
	}

	private static String hex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];

		for(int i = 0; i < bytes.length; i++) {
			out[i * 2] = Character.forDigit((bytes[i] >> 4) & 0xf, 16);
			out[i * 2 + 1] = Character.forDigit(bytes[i] & 0xf, 16);
		}

		return new String(out);
	}

	private static void delete(File file) {
		if(file == null || !file.exists()) {
			return;
		}

		File[] children = file.listFiles();

		if(children != null) {
			for(File child : children) {
				delete(child);
			}
		}

		file.delete();
	}

}
