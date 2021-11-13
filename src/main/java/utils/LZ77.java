package utils;

import org.apache.commons.io.FilenameUtils;
import utils.streams.BitInputStream;
import utils.streams.BitOutputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class LZ77 implements Compressor {

    public static int MAX_WINDOW_SIZE;
    public static int LOOK_AHEAD_BUFFER_SIZE;
    private int windowSize;

    public LZ77() {
        windowSize = 100;
        MAX_WINDOW_SIZE = (1 << 12) - 1;
        LOOK_AHEAD_BUFFER_SIZE = (1 << 4) - 1;
    }

    @Override
    public long compress(String inputFile, String outputFile) {
        File fileIn = new File(inputFile);

        if (outputFile == null) {
            outputFile = FilenameUtils.removeExtension(fileIn.getAbsolutePath());
        }

        try (BitOutputStream out = new BitOutputStream(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))))) {

            out.writeUTF(TAGS.HEAD.toString());

            out.writeUTF(TAGS.FILE.toString());

            out.writeUTF(fileIn.getName());

            out.writeUTF(TAGS.BODY.toString());

            byte[] data = Files.readAllBytes(Paths.get(inputFile));

            for (int i = 0; i < data.length; ) {
                Match match = findMatchInSlidingWindow(data, i);
                if (match != null) {
                    out.write(Boolean.TRUE);
                    out.write((byte) (match.getDistance() >> 4));
                    out.write((byte) (((match.getDistance() & 0x0F) << 4) | match.getLength()));

                    i = i + match.getLength();
                } else {
                    out.write(Boolean.FALSE);
                    out.write(data[i]);
                    i = i + 1;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return new File(outputFile).length();
    }

    @Override
    public boolean decompress(String inputFile, String extractPath) {

        try (BitInputStream in = new BitInputStream(new DataInputStream(new BufferedInputStream(new FileInputStream(inputFile))))) {

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

            try (RandomAccessFile out = new RandomAccessFile(outputFile, "rw");
                 FileChannel outputChannel = out.getChannel()) {
                ByteBuffer buffer = ByteBuffer.allocate(1);
                while (true) {
                    int flag = in.read();
                    if (flag == 0) {
                        buffer.clear();
                        buffer.put(in.readByte());
                        buffer.flip();
                        outputChannel.write(buffer, outputChannel.size());
                        outputChannel.position(outputChannel.size());
                    } else {
                        int byte1 = in.read(8);
                        int byte2 = in.read(8);
                        int distance = (byte1 << 4) | (byte2 >> 4);
                        int length = (byte2 & 0x0f);
                        for (int i = 0; i < length; i++) {
                            buffer.clear();
                            outputChannel.read(buffer, outputChannel.position() - distance);
                            buffer.flip();
                            outputChannel.write(buffer, outputChannel.size());
                            outputChannel.position(outputChannel.size());
                        }
                    }
                }
            } catch (EOFException ignored) {
            }

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private Match findMatchInSlidingWindow(byte[] data, int currentIndex) {
        Match match = new Match();
        int end = Math.min(currentIndex + LOOK_AHEAD_BUFFER_SIZE, data.length + 1);
        for (int j = currentIndex + 2; j < end; j++) {
            int startIndex = Math.max(0, currentIndex - windowSize);
            byte[] bytesToMatch = Arrays.copyOfRange(data, currentIndex, j);
            for (int i = startIndex; i < currentIndex; i++) {
                int repeat = bytesToMatch.length / (currentIndex - i);
                int remaining = bytesToMatch.length % (currentIndex - i);

                byte[] tempArray = new byte[(currentIndex - i) * repeat + (i + remaining - i)];
                int m = 0;
                for (; m < repeat; m++) {
                    int destPos = m * (currentIndex - i);
                    System.arraycopy(data, i, tempArray, destPos, currentIndex - i);
                }
                int destPos = m * (currentIndex - i);
                System.arraycopy(data, i, tempArray, destPos, remaining);
                if (Arrays.equals(tempArray, bytesToMatch) && bytesToMatch.length > match.getLength()) {
                    match.setLength(bytesToMatch.length);
                    match.setDistance(currentIndex - i);
                }
            }
        }
        if (match.getLength() > 0 && match.getDistance() > 0)
            return match;
        return null;
    }

    private class Match {

        private int length;
        private int distance;

        public Match() {
            this(-1, -1);
        }

        public Match(int matchLength, int matDistance) {
            this.length = matchLength;
            this.distance = matDistance;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int matchLength) {
            this.length = matchLength;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int matDistance) {
            this.distance = matDistance;
        }

    }
}
