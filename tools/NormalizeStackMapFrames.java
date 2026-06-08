import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Rewrites third-party Java 7 class files with explicit StackMapTable frames.
 *
 * Amazon Appstore SDK 3.0.9 ships valid but legacy-shaped bytecode that AGP 9's
 * D8 warns about while dexing. This tool keeps class names and bytecode behavior
 * intact while asking ASM to recompute max stack/local metadata and stack-map
 * frames so D8 no longer has to infer them from missing class-file attributes.
 */
public final class NormalizeStackMapFrames {
    private NormalizeStackMapFrames() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: NormalizeStackMapFrames <input.jar> <output.jar>");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());

        try (
            JarInputStream jarIn = new JarInputStream(Files.newInputStream(input));
            JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(output))
        ) {
            JarEntry entry;
            while ((entry = jarIn.getNextJarEntry()) != null) {
                JarEntry outEntry = new JarEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                jarOut.putNextEntry(outEntry);

                byte[] bytes = readAllBytes(jarIn);
                if (entry.getName().endsWith(".class")) {
                    bytes = normalizeClass(bytes);
                }
                jarOut.write(bytes);
                jarOut.closeEntry();
            }
        }
    }

    private static byte[] normalizeClass(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(writer, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }
}
