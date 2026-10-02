package com.mojang.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NativeLoader {

    private NativeLoader() {
    }

    public static void load() {
        try {
            File nativesDir = new File(System.getProperty("java.io.tmpdir"),
                    "rd-132211-natives-" + UUID.randomUUID());
            if (!nativesDir.mkdirs() && !nativesDir.isDirectory()) {
                throw new IOException("Could not create natives directory: " + nativesDir);
            }
            nativesDir.deleteOnExit();

            File jarFile = findJarFile();
            if (jarFile != null && jarFile.isFile()) {
                extractFromJar(jarFile, nativesDir);
            }

            String path = nativesDir.getAbsolutePath();
            System.setProperty("org.lwjgl.librarypath", path);
            System.setProperty("net.java.games.input.librarypath", path);

            String existing = System.getProperty("java.library.path");
            if (existing == null || existing.isEmpty()) {
                System.setProperty("java.library.path", path);
            } else {
                System.setProperty("java.library.path", path + File.pathSeparator + existing);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract natives; relying on system library path.");
            e.printStackTrace();
        }
    }

    private static File findJarFile() {
        try {
            return new File(NativeLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }

    private static void extractFromJar(File jarFile, File destDir) throws IOException {
        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = name.toLowerCase();
                boolean isNative = lower.endsWith(".dll")
                        || lower.endsWith(".so")
                        || lower.endsWith(".dylib")
                        || lower.endsWith(".jnilib");
                if (!isNative) {
                    continue;
                }

                String fileName = name.substring(name.lastIndexOf('/') + 1);
                File out = new File(destDir, fileName);
                out.deleteOnExit();

                try (InputStream in = zip.getInputStream(entry);
                     OutputStream outStream = new FileOutputStream(out)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        outStream.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
