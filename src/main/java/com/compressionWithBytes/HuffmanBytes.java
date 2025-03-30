package com.compressionWithBytes;

import java.io.*;
import java.util.*;

class BitBuffer {
    int size;
    int current;
    byte[] bytes;

    private byte[] masks = new byte[] {0b00000001, 0b00000010, 0b00000100, 0b00001000, 0b00010000, 0b00100000, 0b01000000, (byte) 0b10000000};

    public BitBuffer(int size) {
        this.size = size;
        int sizeInBytes = size >> 3;
        if (size % 8 > 0){
            sizeInBytes = sizeInBytes + 1;
        }
        bytes = new byte[sizeInBytes];
    }

    public BitBuffer(int size, byte[] bytes){
        this.size = size;
        this.bytes = bytes;
    }

    public void setCurrent(int current){
        this.current = current;
    }

    public int get(int index){
        int byteIndex = index >> 3;
        int bitIndex = index & 0b111;
        return (bytes[byteIndex] & masks[bitIndex]) != 0 ? 1 : 0;
    }

    public void set(int index, int value){
        int byteIndex = index >> 3;
        int bitIndex = index & 0b111;
        if (value != 0){
            bytes[byteIndex] = (byte) (bytes[byteIndex] | masks[bitIndex]);
        }
        else {
            bytes[byteIndex] = (byte) (bytes[byteIndex] & ~masks[bitIndex]);
        }
    }

    public void append (int value){
        int byteIndex = current >> 3;
        int bitIndex = current & 0b111;
        if (value != 0){
            bytes[byteIndex] = (byte) (bytes[byteIndex] | masks[bitIndex]);
        }
        else {
            bytes[byteIndex] = (byte) (bytes[byteIndex] & ~masks[bitIndex]);
        }
        current += 1;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++){
            sb.append(get(i) > 0 ? '1' : '0');
        }
        return sb.toString();
    }

    public int getSize(){
        return size;
    }

    public int setSizeInBytes(){
        return bytes.length;
    }

    public byte[] getBytes(){
        return bytes;
    }
}


class BitToByteWriter implements AutoCloseable{
    int bitBuffer;
    int bitWritten;
    int bytesWritten;

    OutputStream target;

    public BitToByteWriter(OutputStream target){
        this.target = target;
    }

    public void close() throws IOException{
        if ((bitWritten & 0b111) != 0){
            target.write(bitBuffer);
            bytesWritten += 1;
        }
        target.flush();
        target.close();
    }

    public void writeBit(int bit) throws IOException{
        int bitIndex = bitWritten & 0b111;
        int mask = 1 << bitIndex;

        switch (bit) {
            case 1:
                bitBuffer |= mask;
                break;
            case 0:
                bitBuffer &= ~mask;
                break;
        }
        bitWritten += 1;
        if (bitIndex + 1 == 8){
            target.write(bitBuffer);
            bytesWritten += 1;
        }
    }

    public void writeByte(int val) throws IOException {
        int mask = 1;
        for (int i = 0; i < 8; i++){
            writeBit((val & mask) > 0 ? 1 : 0);
            mask <<= 1;
        }
    }
}

class CodeTreeNode implements Comparable<CodeTreeNode>{
    Byte content;
    int weight;
    CodeTreeNode left;
    CodeTreeNode right;

    int length;

    public CodeTreeNode(Byte content, int weigth){
        this.content = content;
        this.weight = weigth;
    }

    public CodeTreeNode(Byte content, int weigth, CodeTreeNode left, CodeTreeNode right){
        this.content = content;
        this.weight = weigth;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(CodeTreeNode o){
        return o.weight - weight;
    }

    public String getCodeForCharacter(Byte ch, String parentPath){
        if(content == ch){
            return parentPath;
        }
        else{
            if (left != null){
                String path = left.getCodeForCharacter(ch, parentPath + 0);
                if (path != null){
                    return path;
                }
            }
            if (right != null){
                String path = right.getCodeForCharacter(ch, parentPath + 1);
                if (path != null){
                    return path;
                }
            }
        }
        return null;
    }
}

public class HuffmanBytes {
    public static CodeTreeNode encode (InputStream inputStream, OutputStream outputStream) throws IOException{
        byte[] Info = inputStream.readAllBytes();
        Map<Byte, Integer> frequences = countFrequency(Info);
        ArrayList<CodeTreeNode> codeTreeNodes = new ArrayList<>();
        for (byte b : frequences.keySet()){
            codeTreeNodes.add(new CodeTreeNode(b, frequences.get(b)));
        }
        CodeTreeNode tree = huffman(codeTreeNodes); 

        TreeMap<Byte, String> codes = new TreeMap<>();
        for (Byte b : frequences.keySet()) {
            codes.put(b, tree.getCodeForCharacter(b, ""));
        }
        int length = 0;
        for (int i = 0; i < Info.length; i++){
            byte byteCur = Info[i];
            String code = codes.get(byteCur);
            length = length + code.length();
        }


        tree.length = length % 8;

        BitToByteWriter writer = new BitToByteWriter(outputStream);
        for (int i = 0; i < Info.length; i++){
            byte byteCur = Info[i];
            String code = codes.get(byteCur);
            for (char bit : code.toCharArray()) {
                writer.writeBit(bit == '1' ? 1 : 0);
            }
        }
        writer.close();
        return tree;
    }

    public static CodeTreeNode getTree (String inputFilePath) throws IOException{
        try(InputStream inputStream = new FileInputStream(inputFilePath)){
            byte[] Info = inputStream.readAllBytes();
            Map<Byte, Integer> frequences = countFrequency(Info);
            ArrayList<CodeTreeNode> codeTreeNodes = new ArrayList<>();
            for (byte b : frequences.keySet()){
                codeTreeNodes.add(new CodeTreeNode(b, frequences.get(b)));
            }
            CodeTreeNode tree = huffman(codeTreeNodes);
            TreeMap<Byte, String> codes = new TreeMap<>();
            for (Byte b : frequences.keySet()) {
                codes.put(b, tree.getCodeForCharacter(b, ""));
            }
            int length = 0;
            for (int i = 0; i < Info.length; i++){
                byte byteCur = Info[i];
                String code = codes.get(byteCur);
                length = length + code.length();
            }
            tree.length = length % 8;
            return tree;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void decode(InputStream inputStream, OutputStream outputStream, CodeTreeNode huffmanTree) throws IOException{
        CodeTreeNode curNode = huffmanTree;
        int length = curNode.length;
        byte[] info = inputStream.readAllBytes();

        Boolean isFull = false;
        if (length == 0) isFull = true;

        for (int i = 0; i < info.length; i++){
            for (int j = 0; j < 8; j++){
                if ((i == info.length - 1) && !isFull && length <= 0) break;
                int bit = (info[i] >> j) & 1;
                curNode = bit == 0 ? curNode.left : curNode.right;

                if (curNode.content != null){
                    outputStream.write(curNode.content);
                    curNode = huffmanTree;
                }
                if (i == info.length - 1 && !isFull) length--;
            }
        }
    }

    public static Map<Byte, Integer> countFrequency(byte[] info){
        Map<Byte, Integer> frequency = new TreeMap<>();
        for (int i = 0; i < info.length; i++){
            frequency.put(info[i], frequency.getOrDefault(info[i], 0) + 1);
        }
        return frequency;
    }

    private static CodeTreeNode huffman(ArrayList<CodeTreeNode> codeTreeNodes){
        while (codeTreeNodes.size() > 1){
            Collections.sort(codeTreeNodes);
            CodeTreeNode left = codeTreeNodes.remove(codeTreeNodes.size() - 1);
            CodeTreeNode right = codeTreeNodes.remove(codeTreeNodes.size() - 1);

            CodeTreeNode parent = new CodeTreeNode(null, right.weight + left.weight, left, right);
            codeTreeNodes.add(parent);
        }
        return codeTreeNodes.get(0);
    }
    public static void main(String[] args) throws IOException {
        File inputFile = new File("bw.raw.");
        File encodedFile = new File("ha");
        File decodedFile = new File("bwh.raw");
        CodeTreeNode tree = new CodeTreeNode(null, 0);

        try (InputStream inputStream = new FileInputStream(inputFile);
             OutputStream outputStream = new FileOutputStream(encodedFile)) {
            tree = encode(inputStream, outputStream);
        }

        try (InputStream inputStream = new FileInputStream(encodedFile);
             OutputStream outputStream = new FileOutputStream(decodedFile)) {
            decode(inputStream, outputStream, tree);
        }
    }
}