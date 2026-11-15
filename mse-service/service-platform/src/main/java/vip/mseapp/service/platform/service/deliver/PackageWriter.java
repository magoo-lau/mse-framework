package vip.mseapp.service.platform.service.deliver;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 部署包 zip 写出器（JDK java.util.zip，流式）：逐条目 SHA-256 记账（进 manifest）、
 * 整包 SHA-256 记账（进导出记录）；manifest.json / signature.sig 最后写入（zip 目录序
 * 与条目序无关，导入端按条目名读取）。
 *
 * <pre>
 * try (PackageWriter writer = PackageWriter.create(path)) {
 *     writer.putBytes("license.token", tokenBytes);
 *     try (PackageWriter.HashingOutputStream out = writer.openStream("data/sys_user.jsonl")) {
 *         exporter.writeTableJsonl(conn, "sys_user", false, 0, out);
 *     }
 *     writer.closeEntry();
 *     writer.finishManifest(manifestBytes, signatureBase64);
 * }
 * </pre>
 */
public final class PackageWriter implements Closeable {

    /** 条目校验项（manifest.files / manifest.tables 逐文件行） */
    public record EntryHash(String file, long size, String sha256) {
    }

    private final ZipOutputStream zip;
    private final CountingDigestOutputStream fileOut;
    private final List<EntryHash> entries = new ArrayList<>();

    private PackageWriter(ZipOutputStream zip, CountingDigestOutputStream fileOut) {
        this.zip = zip;
        this.fileOut = fileOut;
    }

    public static PackageWriter create(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        CountingDigestOutputStream out =
                new CountingDigestOutputStream(new FileOutputStream(file.toFile()), sha256Digest());
        return new PackageWriter(new ZipOutputStream(out), out);
    }

    /** 写字节条目（写入即记账） */
    public void putBytes(String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        HashingOutputStream hashing = new HashingOutputStream(name, zip);
        hashing.write(bytes);
        hashing.record();
        zip.closeEntry();
    }

    /**
     * 打开流式条目（大数据文件）：写入完成后 close 该流（此时记账），再调 {@link #closeEntry()}
     * 关闭 zip 条目。
     */
    public HashingOutputStream openStream(String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        return new HashingOutputStream(name, zip);
    }

    /** 关闭当前流式条目（配套 {@link #openStream}，须在其流 close 之后调用） */
    public void closeEntry() throws IOException {
        zip.closeEntry();
    }

    /** 收尾：写 manifest.json + signature.sig（不进逐条目清单），完成 zip */
    public void finishManifest(byte[] manifestBytes, String signatureBase64) throws IOException {
        putRawEntry("manifest.json", manifestBytes);
        putRawEntry("signature.sig", signatureBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.finish();
    }

    /** manifest 之前写入的条目清单（写入顺序） */
    public List<EntryHash> entries() {
        return List.copyOf(entries);
    }

    /** 整包 SHA-256（{@link #close()} 后读取） */
    public String zipSha256() {
        return HexFormat.of().formatHex(fileOut.digest().digest());
    }

    /** 整包字节数（{@link #close()} 后读取） */
    public long size() {
        return fileOut.count();
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    private void putRawEntry(String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 写入即 SHA-256 摘要的条目输出流：close/record 恰好其一发生时记账（防双记） */
    public final class HashingOutputStream extends OutputStream {

        private final String name;
        private final OutputStream target;
        private final MessageDigest digest = sha256Digest();
        private long count;
        private boolean recorded;

        private HashingOutputStream(String name, OutputStream target) {
            this.name = name;
            this.target = target;
        }

        @Override
        public void write(int b) throws IOException {
            digest.update((byte) b);
            target.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            digest.update(b, off, len);
            target.write(b, off, len);
            count += len;
        }

        @Override
        public void close() {
            record();
        }

        private void record() {
            if (!recorded) {
                recorded = true;
                entries.add(new EntryHash(name, count, HexFormat.of().formatHex(digest.digest())));
            }
        }
    }

    /** 计数 + 摘要输出流（整包级） */
    static final class CountingDigestOutputStream extends OutputStream {

        private final FileOutputStream target;
        private final MessageDigest digest;
        private long count;

        CountingDigestOutputStream(FileOutputStream target, MessageDigest digest) {
            this.target = target;
            this.digest = digest;
        }

        MessageDigest digest() {
            return digest;
        }

        long count() {
            return count;
        }

        @Override
        public void write(int b) throws IOException {
            target.write(b);
            digest.update((byte) b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            target.write(b, off, len);
            digest.update(b, off, len);
            count += len;
        }
    }
}
