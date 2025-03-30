package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class BWTBytes {
    public static final int BLOCK_SIZE = 1024;

    private static <T> void mergeSort(T[] array, Comparator<T> comparator) {
        if (array.length < 2) return;
        int mid = array.length / 2;
    
        T[] left = Arrays.copyOfRange(array, 0, mid);
        T[] right = Arrays.copyOfRange(array, mid, array.length);
    
        mergeSort(left, comparator);
        mergeSort(right, comparator);
        merge(array, left, right, comparator);
    }
    
    private static <T> void merge(T[] array, T[] left, T[] right, Comparator<T> comparator) {
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            if (comparator.compare(left[i], right[j]) <= 0) array[k++] = left[i++];
            else array[k++] = right[j++];
        }
        while (i < left.length) array[k++] = left[i++];
        while (j < right.length) array[k++] = right[j++];
    }

    private static <T> void mergeSort(List<T> list, Comparator<T> comparator) {
        if (list.size() < 2) return;
        int mid = list.size() / 2;
    
        List<T> left = new ArrayList<>(list.subList(0, mid));
        List<T> right = new ArrayList<>(list.subList(mid, list.size()));
    
        mergeSort(left, comparator);
        mergeSort(right, comparator);
        merge(list, left, right, comparator);
    }
    
    private static <T> void merge(List<T> list, List<T> left, List<T> right, Comparator<T> comparator) {
        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) list.set(k++, left.get(i++));
            else list.set(k++, right.get(j++));
        }
        while (i < left.size()) list.set(k++, left.get(i++));
        while (j < right.size()) list.set(k++, right.get(j++));
    }

    public static void encode(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BLOCK_SIZE];
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            byte[] block = Arrays.copyOf(buffer, bytesRead);
            BWTResult result = encodeBlock(block);
            writeInt(output, result.transformedData.length);
            writeInt(output, result.originalIndex);
            output.write(result.transformedData);
        }
    }

    private static BWTResult encodeBlock(byte[] data) {
        int n = data.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        mergeSort(indices, new BWTComparator(data));

        byte[] transformed = new byte[n];
        int originalIndex = -1;
        for (int i = 0; i < n; i++) {
            int shiftedIndex = (indices[i] + n - 1) % n;
            transformed[i] = data[shiftedIndex];
            if (indices[i] == 1) originalIndex = i;
        }

        if (originalIndex == -1) throw new RuntimeException("Original index not found");
        return new BWTResult(transformed, originalIndex);
    }

    private static class BWTComparator implements Comparator<Integer> {
        private final byte[] data;

        BWTComparator(byte[] data) {
            this.data = data;
        }

        @Override
        public int compare(Integer i1, Integer i2) {
            int n = data.length;
            for (int k = 0; k < n; k++) {
                byte b1 = data[(i1 + k) % n];
                byte b2 = data[(i2 + k) % n];
                int cmp = Byte.compare(b1, b2);
                if (cmp != 0) return cmp;
            }
            return 0;
        }
    }

    private static class BWTResult {
        byte[] transformedData;
        int originalIndex;

        BWTResult(byte[] transformedData, int originalIndex) {
            this.transformedData = transformedData;
            this.originalIndex = originalIndex;
        }
    }

    private static class Pair {
        byte value;
        int index;

        Pair(byte value, int index) {
            this.value = value;
            this.index = index;
        }

        byte getValue() { return value; }
        int getIndex() { return index; }
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) return -1;
        return (ch1 << 24) | (ch2 << 16) | (ch3 << 8) | ch4;
    }

    public static void decode(InputStream input, OutputStream output) throws IOException {
        while (true) {
            int blockSize = readInt(input);
            if (blockSize == -1) break;
            
            int originalIndex = readInt(input);
            byte[] transformedData = new byte[blockSize];
            int bytesRead = input.readNBytes(transformedData, 0, blockSize);
            if (bytesRead != blockSize) throw new IOException("Unexpected end of stream");
            
            byte[] decoded = decodeBlock(transformedData, originalIndex);
            output.write(decoded);
        }
    }

    private static byte[] decodeBlock(byte[] transformedData, int originalIndex) {
        int n = transformedData.length;
        Pair[] pairs = new Pair[n];
        
        for (int i = 0; i < n; i++) pairs[i] = new Pair(transformedData[i], i);

        mergeSort(pairs, Comparator.comparing((Pair p) -> p.value).thenComparingInt(p -> p.index));

        int[] next = new int[n];
        for (int i = 0; i < n; i++) next[i] = pairs[i].index;

        byte[] decoded = new byte[n];
        int current = originalIndex;
        for (int i = 0; i < n; i++) {
            decoded[i] = transformedData[current];
            current = next[current];
        }
        return decoded;
    }
    public static void main(String[] args) throws IOException{
        File inputFile = new File("col.raw");
        File encodedFile = new File("tested.bwt");
        File decodedFile = new File("decoded.raw");
        try(InputStream inputStream = new FileInputStream(inputFile);
            OutputStream outputStream = new FileOutputStream(encodedFile)){
                encode(inputStream, outputStream);
        }
        try (InputStream inputStream = new FileInputStream(encodedFile);
            OutputStream outputStream = new FileOutputStream(decodedFile)) {
           decode(inputStream, outputStream);
       }
    }
}
