package org.autojs.autojs.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipApkUtil {

    public static void packageApk(File dir, OutputStream out) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(out);
        zos.putNextEntry(new ZipEntry("META-INF/"));
        zos.closeEntry();
        Manifest manifest = new Manifest();
        writeMF(dir, manifest, zos);
        IOUtils.closeQuietly(zos);
    }

    private static void writeMF(File dir, Manifest manifest, ZipOutputStream zos) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        DigestOutputStream dos = new DigestOutputStream(zos, md);
        zipFiles(dir, zos, dos, manifest);
        Attributes main = manifest.getMainAttributes();
        main.putValue("Manifest-Version", "1.0");
        main.putValue("Created-By", "PKG BUILDER");
        zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        manifest.write(dos);
        zos.closeEntry();
    }

    private static void zipFiles(File dir, ZipOutputStream zos, DigestOutputStream dos, Manifest m) throws NoSuchAlgorithmException, IOException {
        File[] files;
        if ((files = dir.listFiles()) == null) {
            return;
        }
        for (File f : files) {
            if (!f.getName().startsWith("META-INF")) {
                if (f.isFile()) {
                    doFile(f.getName(), f, zos, dos, m);
                } else {
                    doDir(f.getName() + "/", f, zos, dos, m);
                }
            }
        }
    }

    private static void doDir(String prefix, File dir, ZipOutputStream zos, DigestOutputStream dos, Manifest m) throws IOException {
        zos.putNextEntry(new ZipEntry(prefix));
        zos.closeEntry();
        File[] files;
        if ((files = dir.listFiles()) == null) {
            return;
        }
        for (File f : files) {
            if (f.isFile()) {
                doFile(prefix + f.getName(), f, zos, dos, m);
            } else {
                doDir(prefix + f.getName() + "/", f, zos, dos, m);
            }
        }

    }

    private static void doFile(String name, File f, ZipOutputStream zos, DigestOutputStream dos, Manifest m) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        FileInputStream fis = FileUtils.openInputStream(f);
        IOUtils.copy(fis, dos);
        IOUtils.closeQuietly(fis);
        zos.closeEntry();
    }
}
