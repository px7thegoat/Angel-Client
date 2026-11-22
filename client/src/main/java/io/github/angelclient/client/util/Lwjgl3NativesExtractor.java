package io.github.angelclient.client.util;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extrai os nativos do LWJGL 3 (lwjgl.dll + lwjgl_nanovg.dll) e os carrega
 * via System.load() antes que o LibNanoVG tente fazer Library.loadSystem().
 *
 * Usa org.lwjgl.nanovg.explicitInit=true para suprimir o loading automático
 * do LWJGL 3, permitindo que nós carreghemos as DLLs manualmente.
 *
 * Compatível com o URLClassLoader do Forge/LaunchWrapper — não depende só
 * de java.class.path.
 */
public final class Lwjgl3NativesExtractor {

    private static boolean done = false;

    // Nomes dos nativos a extrair, NA ORDEM correta de dependência.
    // lwjgl (core) deve ser carregado ANTES de lwjgl_nanovg.
    private static final String[] NATIVE_BASENAMES = { "lwjgl", "lwjgl_nanovg" };

    private Lwjgl3NativesExtractor() {}

    /**
     * Deve ser chamado UMA VEZ, ANTES de qualquer classe org.lwjgl.nanovg.* ser carregada.
     * Idempotente.
     */
    public static synchronized void extract() {
        if (done) return;
        done = true;

        // Suprime o loading automático do LibNanoVG para que possamos
        // carregar as DLLs manualmente com System.load().
        System.setProperty("org.lwjgl.nanovg.explicitInit", "true");
        System.setProperty("org.lwjgl.explicitInit",        "true");

        String os   = System.getProperty("os.name", "").toLowerCase();
        String prefix = os.contains("win") ? "" : "lib";
        String suffix = os.contains("win") ? ".dll" : os.contains("mac") ? ".dylib" : ".so";

        try {
            File outDir = Files.createTempDirectory("lwjgl3-nvg-").toFile();
            outDir.deleteOnExit();

            // Coleta todos os JARs acessíveis — sistema + URLClassLoaders da cadeia
            Set<String> jarPaths = collectAllJarPaths();

            // Também busca no cache do Gradle (caso o natives-JAR não esteja no classpath)
            jarPaths.addAll(findInGradleCache(os));

            // Para cada nativo, encontra no conjunto de JARs e extrai
            Map<String, File> extracted = new LinkedHashMap<>();
            for (String base : NATIVE_BASENAMES) {
                String fileName = prefix + base + suffix;
                File dest = new File(outDir, fileName);
                if (tryExtract(jarPaths, fileName, dest)) {
                    extracted.put(base, dest);
                } else {
                    System.err.println("[AngelClient] Nativo não encontrado nos JARs: " + fileName);
                }
            }

            // Carrega na ordem: core primeiro, depois nanovg
            for (String base : NATIVE_BASENAMES) {
                File f = extracted.get(base);
                if (f != null && f.exists()) {
                    try {
                        System.load(f.getAbsolutePath());
                        System.out.println("[AngelClient] Native carregada: " + f.getName());
                    } catch (UnsatisfiedLinkError e) {
                        // Pode já estar carregada — não é fatal
                        System.out.println("[AngelClient] " + f.getName() + " já carregada ou erro: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[AngelClient] Falha ao extrair nativos LWJGL 3: " + e);
        }
    }

    // ── Coleta caminhos de JARs ──────────────────────────────────────────────

    private static Set<String> collectAllJarPaths() {
        Set<String> paths = new LinkedHashSet<>();

        // 1. java.class.path (funciona em alguns launchers)
        for (String p : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (p.endsWith(".jar")) paths.add(p);
        }

        // 2. Sobe a cadeia de ClassLoaders coletando URLs de URLClassLoaders
        ClassLoader cl = Lwjgl3NativesExtractor.class.getClassLoader();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) cl).getURLs()) {
                    if (url.getProtocol().equals("file")) {
                        String p = url.getFile();
                        if (p.endsWith(".jar")) paths.add(p);
                    }
                }
            }
            cl = cl.getParent();
        }

        // 3. Context class loader (Forge/LaunchWrapper usa este)
        cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                if (url.getProtocol().equals("file")) {
                    String p = url.getFile();
                    if (p.endsWith(".jar")) paths.add(p);
                }
            }
        }

        return paths;
    }

    private static List<String> findInGradleCache(String os) {
        List<String> found = new ArrayList<>();
        String nativeClassifier = os.contains("win") ? "natives-windows"
                : os.contains("mac") ? "natives-macos" : "natives-linux";

        // Cache padrão do Gradle: ~/.gradle/caches/modules-2/files-2.1/org.lwjgl/lwjgl-nanovg/
        String home  = System.getProperty("user.home", "");
        File modules = new File(home, ".gradle/caches/modules-2/files-2.1/org.lwjgl");
        if (!modules.isDirectory()) return found;

        // Artefatos que nos interessam
        String[] artifacts = { "lwjgl", "lwjgl-nanovg" };
        for (String artifact : artifacts) {
            File artDir = new File(modules, artifact);
            if (!artDir.isDirectory()) continue;
            // Itera versões
            for (File ver : listFiles(artDir)) {
                for (File hash : listFiles(ver)) {
                    for (File jar : listFiles(hash)) {
                        if (jar.getName().endsWith(".jar")
                                && jar.getName().contains(nativeClassifier)) {
                            found.add(jar.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return found;
    }

    private static File[] listFiles(File dir) {
        File[] f = dir.listFiles();
        return f != null ? f : new File[0];
    }

    // ── Extração ─────────────────────────────────────────────────────────────

    private static boolean tryExtract(Set<String> jarPaths, String fileName, File dest) {
        if (dest.exists()) return true;
        for (String jarPath : jarPaths) {
            File jarFile = new File(jarPath);
            if (!jarFile.isFile()) continue;
            try (ZipFile zip = new ZipFile(jarFile)) {
                ZipEntry entry = findEntry(zip, fileName);
                if (entry == null) continue;
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }
                dest.deleteOnExit();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /** Busca uma entrada pelo nome do arquivo, independente do caminho dentro do JAR. */
    private static ZipEntry findEntry(ZipFile zip, String fileName) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (new File(e.getName()).getName().equals(fileName)) return e;
        }
        return null;
    }
}