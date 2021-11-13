package utils;

import org.apache.commons.io.FilenameUtils;

import java.io.*;

public class RLE implements Compressor {

    @Override
    public long compress(String inputFile, String outputFile) {
        File fileIn = new File(inputFile);

        if (outputFile == null) {
            outputFile = FilenameUtils.removeExtension(fileIn.getAbsolutePath());
        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileIn));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {

            out.writeUTF(TAGS.HEAD.toString());

            out.writeUTF(TAGS.FILE.toString());

            out.writeUTF(fileIn.getName());

            out.writeUTF(TAGS.BODY.toString());

            int byteReader = in.read();
            while (byteReader != -1) {
                int currentByte = byteReader;
                int count = 1;
                while ((byteReader = in.read()) != -1 && byteReader == currentByte) {
                    count++;
                }
                out.write(currentByte);
                out.writeInt(count);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return new File(outputFile).length();
    }

    @Override
    public boolean decompress(String inputFile, String extractPath) {

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(inputFile)))) {
            if (!in.readUTF().equals(TAGS.HEAD.toString())) {
                return false;
            }

            if (!in.readUTF().equals(TAGS.FILE.toString())) {
                return false;
            }

            String outputFile = in.readUTF();

            if (!in.readUTF().equals(TAGS.BODY.toString())) {
                return false;
            }

            if (extractPath != null) {
                File dir = new File(extractPath);
                if (!dir.exists() || !dir.isDirectory()) {
                    dir.mkdirs();
                }
                outputFile = dir.getAbsolutePath() + "/" + outputFile;
            }

            int byteReader;
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                while ((byteReader = in.read()) != -1) {
                    long count = in.readInt();
                    for (long i = 0; i < count; i++) {
                        out.write(byteReader);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
