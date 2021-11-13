package utils;

import org.apache.commons.io.FilenameUtils;

import java.io.*;

public class AdaptiveHuffman implements Compressor {

    private Node root;
    private Node NYT;
    private Node[] table;
    private Node[] list;
    private int listTop;
    
    public AdaptiveHuffman() {
        table = new Node[256];
        list = new Node[513];
        listTop = 513;
        list[--listTop] = root = NYT = new Node(-1, 0, 512, null, null, null);
    }

    @Override
    public long compress(String inputFile, String outputFile) {
        File fileIn = new File(inputFile);

        if (outputFile == null) {
            outputFile = FilenameUtils.removeExtension(fileIn.getAbsolutePath());
        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {

            out.writeUTF(TAGS.HEAD.toString());

            out.writeUTF(TAGS.FILE.toString());

            out.writeUTF(fileIn.getName());

            out.writeUTF(TAGS.BODY.toString());

            Node temp;
            int nextByte = 0;
            int bitNumber = 7;
            byte[] data = new byte[1000];
            int top;

            while (true) {
                int intRead = in.read();
                if (intRead != -1) {
                    if (table[intRead] != null) {
                        temp = table[intRead];
                        top = 0;
                        while (temp.parent != null) {
                            if (temp.parent.left == temp) data[top++] = 0;
                            else data[top++] = 1;
                            temp = temp.parent;
                        }
                        for (int i = top; i > 0; i--) {
                            if (data[--top] == 1)
                                nextByte |= 1 << bitNumber;
                            if (bitNumber-- == 0) {
                                bitNumber = 7;
                                out.write(nextByte);
                                nextByte = 0;
                            }
                        }
                    } else {

                        temp = NYT;
                        top = 0;
                        while (temp.parent != null) {
                            if (temp.parent.left == temp) data[top++] = 0;
                            else data[top++] = 1;
                            temp = temp.parent;
                        }
                        for (int i = top; i > 0; i--) {
                            if (data[--top] == 1)
                                nextByte |= 1 << bitNumber;
                            if (bitNumber-- == 0) {
                                bitNumber = 7;
                                out.write(nextByte);
                                nextByte = 0;
                            }
                        }

                        if (bitNumber-- == 0) {
                            bitNumber = 7;
                            out.write(nextByte);
                            nextByte = 0;
                        }

                        for (int i = 1; i < 256; i *= 2) {
                            if ((intRead & i) != 0) nextByte |= (1 << bitNumber);
                            if (bitNumber-- == 0) {
                                bitNumber = 7;
                                out.write(nextByte);
                                nextByte = 0;
                            }
                        }
                    }
                    insert(intRead);
                } else {

                    temp = NYT;
                    top = 0;
                    while (temp.parent != null) {
                        if (temp.parent.left == temp) data[top++] = 0;
                        else data[top++] = 1;
                        temp = temp.parent;
                    }
                    for (int i = top; i > 0; i--) {
                        if (data[--top] == 1)
                            nextByte |= 1 << bitNumber;
                        if (bitNumber-- == 0) {
                            bitNumber = 7;
                            out.write(nextByte);
                            nextByte = 0;
                        }
                    }

                    nextByte |= 1 << bitNumber;
                    out.write(nextByte);
                    break;
                }
            }
        } catch (IOException e) {
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

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {

                int val;
                Node iterator = root;
                int bitNumber = 7;
                int nextByte = in.read();

                if (nextByte == -1) {
                    System.out.println(" File to be decoded is empty!");
                    System.exit(2);
                }

                while (true) {

                    if (iterator.left != null) {
                        if ((1 << bitNumber & (byte) nextByte) == 0) iterator = iterator.left;
                        else iterator = iterator.right;
                        if (bitNumber-- == 0) {
                            bitNumber = 7;
                            nextByte = in.read();
                        }
                    } else {
                        if (NYT == iterator) {

                            if ((1 << bitNumber & (byte) nextByte) != 0) val = 1;
                            else val = 0;
                            if (bitNumber-- == 0) {
                                bitNumber = 7;
                                nextByte = in.read();
                            }
                            if (val == 1) {
                                break; 
                            } else {

                                for (int i = 0; i < 8; i++) {
                                    if ((1 << bitNumber & (byte) nextByte) != 0) val |= 1 << i;
                                    if (bitNumber-- == 0) {
                                        bitNumber = 7;
                                        nextByte = in.read();
                                    }
                                }
                            }
                        } else {
                            val = iterator.val;
                        }
                        out.write(val);
                        insert(val);
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

    private void insert(int val) {

        Node t = table[val];
        if (table[val] == null) {
            Node temp = NYT;
            Node retVal = new Node(val, 1, NYT.order - 1, null, null, temp);
            list[--listTop] = retVal;
            NYT = new Node(-1, 0, NYT.order - 2, null, null, temp);
            list[--listTop] = NYT;
            temp.left = NYT;
            temp.right = retVal;
            temp.weight++;
            table[val] = retVal;
            if (table[val].parent == root) {
                return;
            }
            t = table[val].parent.parent;
        }

        while (t != root) {
            Node temp = t;

            int i = t.order + 1;
            for (; (list[i].weight == t.weight) && (i < 512); i++) ;
            i--;

            if ((list[i].order > temp.order) && (list[i] != t.parent)) {
                temp = list[i];
                Node temp2 = list[temp.order];
                list[temp.order] = list[t.order];
                list[t.order] = temp2;
                if (t.parent.left == t) t.parent.left = temp;
                else t.parent.right = temp;
                if (temp.parent.left == temp) temp.parent.left = t;
                else temp.parent.right = t;
                temp2 = temp.parent;
                temp.parent = t.parent;
                t.parent = temp2;
                int order = t.order;
                t.order = temp.order;
                temp.order = order;
            }
            t.weight++;
            t = t.parent;
        }
        t.weight++;
    } 

    private class Node {

        int val;
        int weight;
        int order;
        Node left;
        Node right;
        Node parent;

        Node(int value, int wei, int num, Node l, Node r, Node p) {
            val = value;
            weight = wei;
            order = num;
            left = l;
            right = r;
            parent = p;
        }
    }
}