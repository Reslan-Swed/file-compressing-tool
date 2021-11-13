package utils;

import org.apache.commons.io.FilenameUtils;
import utils.streams.BitInputStream;
import utils.streams.BitOutputStream;

import java.io.*;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Stack;

public class Huffman implements Compressor {

    private int[] bytesFrequency;
    private Node[] byteNodeMap;
    private static final int EOF = 256;

    public Huffman() {
        bytesFrequency = new int[257];
        byteNodeMap = new Node[257];
    }

    @Override
    public long compress(String inputFile, String outputFile) {
        buildFreqTable(inputFile);
        buildHuffmanTree();
        File fileIn = new File(inputFile);

        if (outputFile == null) {
            outputFile = FilenameUtils.removeExtension(fileIn.getAbsolutePath());
        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileIn));
             BitOutputStream out = new BitOutputStream(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))))) {

            out.writeUTF(TAGS.HEAD.toString());

            out.writeUTF(TAGS.FILE.toString());

            out.writeUTF(fileIn.getName());

            out.writeUTF(TAGS.DICTIONARY.toString());

            for (int i = 0; i < bytesFrequency.length - 1; i++) {
                out.writeInt(bytesFrequency[i]);
            }

            out.writeUTF(TAGS.BODY.toString());

            Node temp;
            int intRead;
            Stack<Integer> code = new Stack<>();

            while ((intRead = in.read()) != -1) {
                if (byteNodeMap[intRead] != null) {
                    temp = byteNodeMap[intRead];
                    while (temp.parent != null) {
                        if (temp.parent.left == temp) code.push(0);
                        else code.push(1);
                        temp = temp.parent;
                    }
                    while (!code.isEmpty()) {
                        out.write(code.pop() == 1);
                    }
                } else {
                    System.out.println("this should not happen");
                    System.exit(2);
                }
            }

            temp = byteNodeMap[EOF];
            while (temp.parent != null) {
                if (temp.parent.left == temp) code.push(0);
                else code.push(1);
                temp = temp.parent;
            }
            while (!code.isEmpty()) {
                out.write(code.pop() == 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
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


            if (!in.readUTF().equals(TAGS.DICTIONARY.toString())) {
                return false;
            }

            for (int i = 0; i < bytesFrequency.length - 1; i++) {
                bytesFrequency[i] = in.readInt();
            }

            bytesFrequency[EOF] = 1;
            Node root = buildHuffmanTree();

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

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                Node iterator = root;
                int nextByte;

                while ((nextByte = in.read()) != -1) {
                    if (iterator.left != null) {
                        if (nextByte == 0) iterator = iterator.left;
                        else iterator = iterator.right;
                    }
                    if (iterator.left == null && iterator.right == null) {
                        if (iterator.byteCode == EOF) {
                            break;
                        }
                        out.write(iterator.byteCode);
                        iterator = root;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Node buildHuffmanTree() {

        PriorityQueue<Node> nodePriorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));

        for (int i = 0; i < bytesFrequency.length; i++) {
            if (bytesFrequency[i] > 0) {
                byteNodeMap[i] = new Node(i, bytesFrequency[i], null, null, null);
                nodePriorityQueue.add(byteNodeMap[i]);
            }
        }

        while (nodePriorityQueue.size() > 1) {
            Node left = nodePriorityQueue.poll();
            Node right = nodePriorityQueue.poll();
            Node tmpNode = new Node(null, left.frequency + right.frequency, left, right, null);
            left.parent = tmpNode;
            right.parent = tmpNode;
            nodePriorityQueue.add(tmpNode);
        }

        return nodePriorityQueue.poll();
    }

    private void buildFreqTable(String inputFile) {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(inputFile));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            int ch;
            while ((ch = in.read()) != -1) {
                bytesFrequency[ch]++;
            }

            bytesFrequency[256] = 1;
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    private class Node {

        private Node parent;
        private Node left;
        private Node right;
        private Integer byteCode;
        private int frequency;

        Node(Integer byteCode, int frequency, Node left, Node right, Node parent) {
            this.byteCode = byteCode;
            this.frequency = frequency;
            this.left = left;
            this.right = right;
            this.parent = parent;
        }

    }

}