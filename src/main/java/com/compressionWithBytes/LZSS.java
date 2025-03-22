package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class LZSS {
    static final int MAX_LENGTH = 18;
    public static int MAX_DISTANCE = 4096;

    static int[] linkMatches(final byte[] data) {
        int[] prev = new int[256 * 256];
        Arrays.fill(prev, -1);
        
        int[] offsets = new int[data.length];
        for (int i = 0; i < data.length - 1; i++) {
            final int h = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
            offsets[i] = (prev[h] >= 0) ? (i - prev[h]) : 0;
            prev[h] = i;
        }
        return offsets;
    }

    static int matchLength(final byte[] data, int i, int j) {
        int n = 0;
        while (n < MAX_LENGTH && n + j < data.length && data[i + n] == data[j + n]) n++;
        return n;
    }

    static int[] bestMatch(final byte[] data, final int[] offsets, int j) {
        if (offsets[j] == 0) return new int[]{0, 0};

        int bestN = 0;
        int bestD = 0;
        int i = j - offsets[j];
        while (j - i <= MAX_DISTANCE) {
            final int n = matchLength(data, i, j);
            if (n > bestN) {
                bestN = n;
                bestD = j - i;
            }
            if (offsets[i] == 0) break;
            i -= offsets[i];
        }
        return new int[]{bestD, bestN};
    }

    static void encode(InputStream input, OutputStream output) throws IOException {
        byte[] data = input.readAllBytes();
        final int[] offsets = linkMatches(data);
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        
        final byte[] blk = new byte[17];
        int blkI = 1;
        int blkN = 0;

        for (int j = 0; j < data.length; ) {
            final int[] m = bestMatch(data, offsets, j);
            if (m[1] > 2) {
                blk[blkI++] = (byte) (((m[1] - 3) << 4) | ((m[0] - 1) >> 8));
                blk[blkI++] = (byte) ((m[0] - 1) & 0xFF);
                j += m[1];
            } else {
                blk[0] |= (0x80 >> blkN);
                blk[blkI++] = data[j++];
            }
            
            if (++blkN == 8) {
                compressed.write(blk, 0, blkI);
                blk[0] = 0;
                blkI = 1;
                blkN = 0;
            }
        }
        if (blkN > 0) compressed.write(blk, 0, blkI);
        output.write(compressed.toByteArray());
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        List<Byte> window = new ArrayList<>();
        DataInputStream dataIn = new DataInputStream(inputStream);
        int flag;
        while (dataIn.available() > 0) {
            flag = dataIn.read();
            if (flag == 1) {
                int offset = dataIn.readShort();
                int length = dataIn.read();
                for (int i = 0; i < length; i++) {
                    byte b = window.get(window.size() - offset);
                    outputStream.write(b);
                    window.add(b);
                }
            } else {
                byte b = dataIn.readByte();
                outputStream.write(b);
                window.add(b);
            }
            while (window.size() > MAX_DISTANCE) {
                window.remove(0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("test");
        File encodedFile = new File("tested");
        File decodedFile = new File("decoded");

        try (InputStream inputStream = new FileInputStream(inputFile);
             OutputStream outputStream = new FileOutputStream(encodedFile)) {
            encode(inputStream, outputStream);
        }

        try (InputStream inputStream = new FileInputStream(encodedFile);
             OutputStream outputStream = new FileOutputStream(decodedFile)) {
            decode(inputStream, outputStream);
        }
    }
}