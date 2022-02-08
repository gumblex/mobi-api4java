package org.rr.mobi4java.decompression;

import java.io.IOException;

/**
 * Description:
 * Author:  limengqi03@baidu.com
 * Created:  January 29, 15:17
 * Copyright (c) 2022, Baidu.com All Rights Reserved
 */
public class PlamDoc implements Inflater {
    @Override
    public byte[] inflate(byte[] input) throws IOException {
        byte[] out = new byte[input.length * 8];
        int i = 0;
        int o = 0;
        while (i < input.length) {
            int c = input[i++] & 0x00FF;
            if (c >= 0x01 && c <= 0x08) {
                for (int j = 0; j < c && i + j < input.length; j++) {
                    out[o++] = input[i + j];
                }
                i += c;
            } else if (c <= 0x7f) {
                out[o++] = (byte) c;
            } else if (c >= 0xC0) {
                out[o++] = ' ';
                out[o++] = (byte) (c ^ 0x80);
            } else if (c <= 0xbf) {
                if (i < input.length) {
                    c = c << 8 | input[i++] & 0xFF;
                    int length = (c & 0x0007) + 3;
                    int location = (c >> 3) & 0x7FF;
                    if (location > 0 && location <= o) {
                        for (int j = 0; j < length; j++) {
                            int idx = o - location;
                            out[o++] = out[idx];
                        }
                    } else {
//                        throw new IllegalArgumentException("invalid index");
                        System.err.println("invalid index"); // ignore
                    }
                }
            } else {
//                throw new IllegalArgumentException("unknown input");
                System.err.println("unknown input");
            }
        }
        byte[] result = new byte[o];
        System.arraycopy(out, 0, result, 0, o);
        return result;
    }
}
