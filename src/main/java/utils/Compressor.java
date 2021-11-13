package utils;

public interface Compressor {
    long compress(String fileIn, String fileOut);

    boolean decompress(String fileIn, String extractPath);
}
