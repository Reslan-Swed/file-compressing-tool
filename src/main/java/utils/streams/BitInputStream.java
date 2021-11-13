package utils.streams;

import java.io.*;

/**
 * Stream to read individual bits from under laying Byte stream in Big-endian
 */
public final class BitInputStream implements AutoCloseable {

    private DataInputStream inputStream;
    private int nextBits;
    private int numberOfBitsRemaining;
    private boolean isEndOfStream;

    public BitInputStream(DataInputStream in) {
        if (in == null) {
            throw new NullPointerException("Argument is null");
        }
        inputStream = in;
        numberOfBitsRemaining = 0;
        isEndOfStream = false;
    }

    /**
     * @return zero or one if there is a bit available. or -1 for end of stream
     * @throws IOException
     */
    public int read() throws IOException {
        if (isEndOfStream)
            return -1;
        if (numberOfBitsRemaining == 0) {
            nextBits = inputStream.read();
            if (nextBits == -1) {
                isEndOfStream = true;
                return -1;
            }
            numberOfBitsRemaining = 8;
        }
        numberOfBitsRemaining--;
        return (nextBits >>> numberOfBitsRemaining) & 1;
    }

    /**
     * Read next n bits from stream and return.
     *
     * @param n no.of bits to read from stream
     * @return the next n bits of this input stream, interpreted as an <code>int</code>.
     * @throws IOException
     */
    public int read(int n) throws IOException {
        int output = 0;
        for (int i = 0; i < n; i++) {
            int val = readNoEof();
            output = output << 1 | val;
        }
        return output;
    }

    public byte readByte() throws IOException {
        return (byte) read(8);
    }

    /**
     * @return Zero or one if there is a bit available. throws EOFException for end of stream
     * @throws IOException
     */
    public int readNoEof() throws IOException {
        int result = read();
        if (result != -1)
            return result;
        else
            throw new EOFException("End of stream reached");
    }

    /**
     * @return a Unicode string.
     * @throws EOFException
     * @throws IOException
     * @throws UTFDataFormatException
     */
    public String readUTF() throws IOException {
        return inputStream.readUTF();
    }

    /**
     * @return the next four bytes of this input stream, interpreted as an <code>int</code>.
     * @throws IOException
     */
    public int readInt() throws IOException {
        return inputStream.readInt();
    }

    /**
     * @return the next eight bytes of this input stream, interpreted as a <code>long</code>.
     * @throws IOException
     */
    public long readLong() throws IOException {
        return inputStream.readLong();
    }

    /**
     * close the input stream.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}