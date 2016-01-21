package rlib.classpath.impl;

import rlib.util.Util;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Реализация сканера, который еще умеет из манифеста classpath доставать.
 *
 * @author Ronn
 */
public class ManifestClassPathScannerImpl extends ClassPathScanerImpl {

    /**
     * Рутовый класс для приложения.
     */
    private final Class<?> rootClass;

    /**
     * Ключ, по которому будет извлекаться дополнительный classpath.
     */
    private final String classPathKey;

    public ManifestClassPathScannerImpl(final Class<?> rootClass, final String classPathKey) {
        this.rootClass = rootClass;
        this.classPathKey = classPathKey;
    }

    public String[] getManifestClassPath() {

        final Path root = Util.getRootFolderFromClass(rootClass);

        if (root == null) {
            return new String[0];
        }

        final Array<String> result = ArrayFactory.newArray(String.class);

        final Thread currentThread = Thread.currentThread();
        final ClassLoader loader = currentThread.getContextClassLoader();

        Enumeration<URL> urls;

        try {

            urls = loader.getResources(JarFile.MANIFEST_NAME);

            while (urls.hasMoreElements()) {

                try {

                    final URL url = urls.nextElement();

                    final InputStream is = url.openStream();

                    if (is != null) {

                        final Manifest manifest = new Manifest(is);
                        final Attributes attributes = manifest.getMainAttributes();

                        final String value = attributes.getValue(classPathKey);

                        if (value == null) {
                            continue;
                        }

                        final String[] classpath = value.split(" ");

                        for (final String path : classpath) {

                            final Path file = root.resolve(path);

                            if (Files.exists(file)) {
                                result.add(file.toString());
                            }
                        }
                    }

                } catch (final Exception e) {
                    LOGGER.warning(e);
                }
            }

        } catch (final IOException e1) {
            LOGGER.warning(e1);
        }

        result.trimToSize();

        return result.array();
    }

    @Override
    protected String[] getPaths() {

        final Array<String> result = ArrayFactory.newArraySet(String.class);
        result.addAll(super.getPaths());
        result.addAll(getManifestClassPath());
        result.trimToSize();

        return result.array();
    }
}
