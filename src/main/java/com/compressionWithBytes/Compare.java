package com.compressionWithBytes;

import java.io.*;

public class Compare {
    public static Boolean compare(InputStream inp1, InputStream inp2) throws IOException{
        byte[] i1 = inp1.readAllBytes();
        byte[] i2 = inp2.readAllBytes();
        System.out.println(i1.length + "+" + i2.length);
        if (i1.length != i2.length) System.out.println("error!!!");
        for (int i = 0; i < i1.length - 1; i++){
            if (i1[i] != i2[i]) {
                System.out.println(i);
                return false;
            }
        }
        return true;  
    }
    public static void main(String[] args) throws IOException{
        File inputFile = new File("enwik7_res/enwik7");
        File inpFile = new File("enwik7_res/enwik7_DECOMP_HA_MTF_BWT");

        try(InputStream inp1 = new FileInputStream(inputFile); InputStream inp2 = new FileInputStream(inpFile)) {
            System.out.println(compare(inp1, inp2));
        }
    }
}
