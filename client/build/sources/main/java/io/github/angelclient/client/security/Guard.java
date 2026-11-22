package io.github.angelclient.client.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

/**
 * Runtime guard.
 *
 * Verifies that this build was started by the official Angel Client launcher,
 * exchanges a signed session with it and refuses to run when the environment
 * is being inspected or tampered with.
 */
public final class Guard {

	/* Shared launcher/bootstrap key (kept as bytes so it does not show up as a string). */
	private static final byte[] KEY = {
			0x4e, 0x09, (byte) 0xe4, (byte) 0xff, (byte) 0x94, (byte) 0x8e, 0x55, 0x24,
			0x1a, 0x7c, (byte) 0xdb, 0x55, (byte) 0xaa, 0x64, 0x73, (byte) 0xc5,
			(byte) 0xe8, (byte) 0x96, (byte) 0xf9, (byte) 0xea, (byte) 0x92, 0x2b, (byte) 0x80,
			(byte) 0xc4, (byte) 0xe6, 0x34, 0x79, (byte) 0xad, 0x42, (byte) 0xd8, 0x45, 0x37
	};

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String PROTOCOL = "ANGEL/1";

	private static volatile boolean verified;
	private static volatile boolean failed;

	private Guard() {
	}

	public static boolean isVerified() {
		return verified;
	}

	/**
	 * Entry point called by {@code PreMain} before Minecraft boots.
	 */
	public static void enforce() {
		if(verified || failed) {
			return;
		}

		try {
			if(isExplodedBuild() && Boolean.getBoolean("angel.dev.session")) {
				verified = true;
				System.out.println("[Guard] development session accepted");
				return;
			}

			if(!isVmClean()) {
				reject("Depurador ou agente de instrumentacao detectado.");
				return;
			}

			String token = System.getProperty("angel.session.token");
			String nonce = System.getProperty("angel.session.nonce");
			String user = System.getProperty("angel.session.user", "");
			String port = System.getProperty("angel.session.port");

			if(isEmpty(token) || isEmpty(nonce) || isEmpty(port)) {
				reject("Sessao ausente. Inicie o jogo pelo launcher oficial do Angel Client.");
				return;
			}

			if(!verifyToken(token, nonce, user)) {
				reject("Assinatura de sessao invalida. Reinicie o launcher.");
				return;
			}

			if(!handshake(port, token)) {
				reject("O launcher nao confirmou a sessao. Inicie o jogo pelo launcher oficial.");
				return;
			}

			String expectedHash = System.getProperty("angel.jar.hash");

			if(isEmpty(expectedHash)) {
				// Running from a JAR always requires a launcher-provided hash; only
				// exploded development builds may skip the integrity check.
				if(!isExplodedBuild()) {
					reject("Integridade do client nao verificada. Inicie pelo launcher oficial.");
					return;
				}
			}
			else {
				String localHash = ownHash();

				if(localHash == null || !constantTimeEquals(localHash, expectedHash)) {
					reject("Integridade do client violada.");
					return;
				}
			}

			verified = true;
			System.out.println("[Guard] session verified");
		}
		catch(Throwable error) {
			reject("Falha de verificacao: " + error.getClass().getSimpleName());
		}
	}

	/* ------------------------------------------------------------------ */
	/* Token                                                                 */
	/* ------------------------------------------------------------------ */

	private static boolean verifyToken(String token, String nonce, String user) {
		try {
			int split = token.indexOf(':');

			if(split <= 0 || split >= token.length() - 1) {
				return false;
			}

			String stampHex = token.substring(0, split);
			String signature = token.substring(split + 1);
			long stamp = Long.parseLong(stampHex, 16);
			long now = System.currentTimeMillis();

			if(Math.abs(now - stamp) > 10L * 60L * 1000L) {
				return false;
			}

			String expected = hmacHex("angel.session.v1|" + user + "|" + nonce + "|" + stampHex);
			return constantTimeEquals(signature, expected);
		}
		catch(Throwable ignored) {
			return false;
		}
	}

	private static boolean handshake(String portText, String token) {
		Socket socket = null;

		try {
			int port = Integer.parseInt(portText.trim());

			if(port <= 0 || port > 65535) {
				return false;
			}

			String clientNonce = randomHex(16);

			socket = new Socket();
			socket.connect(new InetSocketAddress("127.0.0.1", port), 2500);
			socket.setSoTimeout(2500);

			Writer out = new OutputStreamWriter(socket.getOutputStream(), UTF8);
			out.write(PROTOCOL + " " + token + " " + clientNonce + "\n");
			out.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF8));
			String answer = in.readLine();

			if(answer == null || !answer.startsWith("OK ")) {
				return false;
			}

			String signature = answer.substring(3).trim();
			String expected = hmacHex("angel.ok.v1|" + clientNonce + "|" + token);
			return constantTimeEquals(signature, expected);
		}
		catch(Throwable ignored) {
			return false;
		}
		finally {
			if(socket != null) {
				try {
					socket.close();
				}
				catch(Throwable ignored) {
				}
			}
		}
	}

	/* ------------------------------------------------------------------ */
	/* Anti inspection                                                       */
	/* ------------------------------------------------------------------ */

	private static boolean isVmClean() {
		try {
			List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

			for(String argument : arguments) {
				String lower = argument.toLowerCase();

				if(lower.contains("jdwp")
						|| lower.startsWith("-javaagent:")
						|| lower.contains("-agentlib:")
						|| lower.equals("-noverify")
						|| lower.contains("-xverify:none")) {
					return false;
				}
			}

			for(Thread thread : Thread.getAllStackTraces().keySet()) {
				String name = thread.getName().toLowerCase();

				if(name.contains("jdwp") || name.contains("debugger") || name.contains("jdi ")) {
					return false;
				}
			}
		}
		catch(Throwable ignored) {
			// If the JVM refuses to expose this information we simply continue.
		}

		return true;
	}

	/* ------------------------------------------------------------------ */
	/* Helpers                                                               */
	/* ------------------------------------------------------------------ */

	private static boolean isExplodedBuild() {
		try {
			File file = ownFile();
			return file != null && !file.getName().toLowerCase().endsWith(".jar");
		}
		catch(Throwable ignored) {
			return false;
		}
	}

	/**
	 * Resolves the file (JAR or class directory) that contains this class.
	 *
	 * <p>The obvious {@code getProtectionDomain().getCodeSource()} does not work
	 * under LaunchWrapper, which defines classes without a code source. Looking
	 * the class up as a resource works with any class loader.</p>
	 */
	private static File ownFile() {
		try {
			java.net.URL url = Guard.class.getResource(Guard.class.getSimpleName() + ".class");

			if(url != null) {
				String path = url.getPath();

				if("jar".equals(url.getProtocol())) {
					int split = path.indexOf("!/");

					if(split > 0) {
						return new File(new java.net.URI(path.substring(0, split)));
					}
				}
				else if("file".equals(url.getProtocol())) {
					return new File(url.toURI());
				}
			}

			java.security.CodeSource source = Guard.class.getProtectionDomain().getCodeSource();

			if(source != null && source.getLocation() != null) {
				return new File(source.getLocation().toURI());
			}
		}
		catch(Throwable ignored) {
		}

		return null;
	}

	private static String ownHash() {
		try {
			File file = ownFile();

			if(file == null || !file.isFile()) {
				return null;
			}

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] data = Files.readAllBytes(file.toPath());
			return hex(digest.digest(data));
		}
		catch(Throwable ignored) {
			return null;
		}
	}

	private static String hmacHex(String data) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
			return hex(mac.doFinal(data.getBytes(UTF8)));
		}
		catch(Throwable error) {
			throw new IllegalStateException(error);
		}
	}

	private static String randomHex(int length) {
		byte[] bytes = new byte[length];
		new SecureRandom().nextBytes(bytes);
		return hex(bytes);
	}

	private static String hex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];

		for(int i = 0; i < bytes.length; i++) {
			out[i * 2] = Character.forDigit((bytes[i] >> 4) & 0xf, 16);
			out[i * 2 + 1] = Character.forDigit(bytes[i] & 0xf, 16);
		}

		return new String(out);
	}

	private static boolean constantTimeEquals(String a, String b) {
		if(a == null || b == null || a.length() != b.length()) {
			return false;
		}

		int result = 0;

		for(int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}

		return result == 0;
	}

	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static void reject(String message) {
		failed = true;
		System.err.println("[Guard] " + message);

		try {
			JOptionPane.showMessageDialog(null, message, "Angel Client", JOptionPane.ERROR_MESSAGE);
		}
		catch(Throwable ignored) {
		}

		Runtime.getRuntime().halt(0x5a);
	}

}
