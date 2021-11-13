package utils.streams;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes stream of bits into output stream. accumulates 8 bits and writes as byte to the stream.
 */
public final class BitOutputStream implements AutoCloseable {

    private DataOutputStream output;
    private int currentByte;
    private int numberOfBitsInCurrentByte;

    public BitOutputStream(DataOutputStream out) {
        if (out == null)
            throw new NullPointerException("Output stream can not be null");
        output = out;
        currentByte = 0;
        numberOfBitsInCurrentByte = 0;
    }

    /**
     *
     * @param b - a <code>bit</code> to be written.
     * @throws IOException
     */
    public void write(boolean b) throws IOException {
        currentByte = currentByte << 1 | (b ? 1 : 0);
        numberOfBitsInCurrentByte++;
        if (numberOfBitsInCurrentByte == 8) {
            output.write(currentByte);
            numberOfBitsInCurrentByte = 0;
        }
    }

    /**
     * Write 8 bit Byte to the stream.
     *
     * @param b byte
     * @throws IOException
     */
    public void write(byte b) throws IOException {
//        int n = b.intValue();
        int n = (b & 0xFF);
        for (int i = 7; i >= 0; i--) {
            write((n >> i & 1) > 0);
        }
    }

    /**
     * Writes a string to the underlying output stream using
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * encoding in a machine-independent manner.
     * <p>
     *
     * @param str a string to be written.
     * @throws IOException
     */
    public void writeUTF(String str) throws IOException {
        output.writeUTF(str);
    }

    /**
     *
     * @param v – an <code>int</code> to be written.
     * @throws IOException
     */
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    /**
     *
     * @param v – a <code>long</code> to be written.
     * @throws IOException
     */
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    /**
     * if there are no less than 8 bits, then pad with remaining bits at the end and close the stream.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        while (numberOfBitsInCurrentByte != 0) {
            write(Boolean.FALSE);
        }
        output.close();
    }

}
