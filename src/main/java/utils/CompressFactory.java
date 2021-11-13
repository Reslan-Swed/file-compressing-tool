package utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompressFactory {
    public static long compress(String fileIn, String fileOut, Compressor compressor) {
        File file = new File(fileIn);
        if (file.isDirectory()) {
            ArrayList<Path> content = getDirectoryContent(fileIn);
            File tempDir = new File(UUID.randomUUID().toString());
            tempDir.mkdirs();
            for (Path path :
                    content) {
                if (compress(path.toString(), tempDir.getAbsolutePath() + "/" + path.getFileName(), compressor) == -1) {
                    return -1;
                }
            }
            content = getDirectoryContent(tempDir.getAbsolutePath());
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileOut)))) {
                out.writeUTF(TAGS.DIRECTORY.toString());
                out.writeUTF(file.getName());
                for (Path path :
                        content) {
                    out.writeUTF(TAGS.FILE.toString());
//                    out.writeUTF(path.getFileName().toString());
                    out.writeLong(path.toFile().length());
                }
                out.writeUTF(TAGS.BODY.toString());
                for (Path path :
                        content) {
                    try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toString())))) {
                        int nextByte;
                        while ((nextByte = in.read()) != -1) {
                            out.write(nextByte);
                        }
                    }
                }
                FileUtils.deleteDirectory(new File(tempDir.getAbsolutePath()));
            } catch (Exception e) {
                return -1;
            }
            return new File(fileOut).length();
        } else {
            return compressor.compress(fileIn, fileOut);
        }
    }

    public static boolean decompress(String fileIn, String extractPath, Compressor compressor) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileIn)))) {
            if (in.readUTF().equals(TAGS.DIRECTORY.toString())) {
                File dir = new File((extractPath.endsWith("/") ? extractPath : extractPath + "/") + in.readUTF());
                dir.mkdirs();
//                Map<String, Long> fileSizeMap = new LinkedHashMap<>();
                ArrayList<Long> filesSize = new ArrayList<>();
                while (!in.readUTF().equals(TAGS.BODY.toString())) {
//                    fileSizeMap.put(in.readUTF(), in.readLong());
                    filesSize.add(in.readLong());
                }
//                for (Map.Entry<String, Long> f :
//                        fileSizeMap.entrySet()) {
                for (long file : filesSize) {
                    File fileName = new File(dir.getAbsolutePath() + "/" + UUID.randomUUID());
//                    File fileName = new File(dir.getAbsolutePath() + "/" + f.getKey());
//                    long fileSize = f.getValue();
                    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
                        while (file > 0) {
                            out.write(in.read());
                            file--;
                        }
                    }
                    if (!decompress(fileName.getAbsolutePath(), dir.getAbsolutePath(), compressor)) {
                        fileName.delete();
                        return false;
                    }
                    fileName.delete();
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return compressor.decompress(fileIn, extractPath);
    }

    private static ArrayList<Path> getDirectoryContent(String dir) {
        final Path rootPath = Paths.get(dir);
        ArrayList<Path> all = null;
        try {
            all = Files.list(rootPath)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return all;
    }
}
