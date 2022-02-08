package org.rr.mobi4java.decompression;

import org.rr.mobi4java.ByteUtils;
import org.rr.mobi4java.MobiContent;
import org.rr.mobi4java.MobiContentHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Description:
 * https://github.com/blakesmith/cloji/blob/master/docs/huff-cdic-desc.txt
 * https://github.com/kevinhendricks/KindleUnpack/blob/master/lib/mobi_uncompress.py
 * Author:  limengqi03@baidu.com
 * Created:  January 29, 15:18
 * Copyright (c) 2022, Baidu.com All Rights Reserved
 */
public class HuffCdic implements Inflater {

    private List<HuffCode> dict1;
    private List<Long> maxCode;
    private List<Long> minCode;

    private List<CdicSlice> dictionary;


    public HuffCdic(List<MobiContent> contents) {
        dict1 = new ArrayList<>();
        maxCode = new ArrayList<>(Collections.singletonList(4294967295L));
        minCode = new ArrayList<>(Collections.singletonList(0L));
        dictionary = new ArrayList<>();
        MobiContentHeader contentHeader = (MobiContentHeader) contents.get(0);
        int offset = contentHeader.getHuffmanRecordOffset();
        MobiContent huff = contents.get(offset);
        loadHuff(huff.getContent());
        for (int i = 1; i < contentHeader.getHuffmanRecordCount(); i++) {
            MobiContent cdic = contents.get(offset + i);
            loadCdic(cdic.getContent());
        }
    }

    @Override
    public byte[] inflate(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int bitsLeft = input.length * 8;
        byte[] data = new byte[input.length + 8];
        for (int i = 0; i < input.length; i++) {
            data[i] = input[i];
        }
        for (int i = 0; i < 8; i++) {
            data[input.length + i] = 0x00;
        }

        int pos = 0;
        long x = ByteUtils.getLong(data, pos, 8);
        int n = 32;

        while (bitsLeft >= 0) {
            if (n <= 0) {
                pos += 4;
                x = ByteUtils.getLong(data, pos, 8);
                n += 32;
            }
            long code = (x >>> n) & 0xFFFFFFFFL;

            int dict1Index = (int) (code >> 24);
            HuffCode huffCode = dict1.get(dict1Index);
            int codeLen = huffCode.len;
            long maxcode = huffCode.max;
            if (huffCode.term <= 0) {
                while (code < minCode.get(codeLen)) {
                    codeLen++;
                }
                maxcode = maxCode.get(codeLen);
            }
            n -= codeLen;
            bitsLeft -= codeLen;

            int r = (int) (maxcode - code) >> (32 - codeLen);
            CdicSlice cs = dictionary.get(r);
            byte[] slice = cs.slice;
            if (cs.blen <= 0) {
                if (cs.blen == -1) {
                    return new byte[0];
                }
                dictionary.set(r, new CdicSlice(slice, -1));
                slice = inflate(slice);
                dictionary.set(r, new CdicSlice(slice, 1));
            }

            bos.write(slice);
        }
        return bos.toByteArray();
    }

    private void loadHuff(byte[] data) {
        int termLen = 4;
        int offset1 = ByteUtils.getInt(data, 8, termLen);
        int offset2 = ByteUtils.getInt(data, 12, termLen);
        for (int i = 0; i < 256; i++) {
            int iOffset = offset1 + termLen * i;
            long code = ByteUtils.getLong(data, iOffset, termLen);
            dict1.add(new HuffCode(code));
        }

        for (int i = 0; i < 64; i++) {
            int iOffset = offset2 + termLen * i;
            long code = ByteUtils.getLong(data, iOffset, termLen);
            int index = i / 2 + 1;
            if (i % 2 == 0) {
                minCode.add(code << (32 - index));
            } else {
                maxCode.add(((code + 1) << (32 - index)) - 1);
            }
        }
    }

    private void loadCdic(byte[] data) {
        int phrases = ByteUtils.getInt(data, 8, 4);
        int bits = ByteUtils.getInt(data, 12, 4);
        int n = Math.min(1 << bits, phrases - dictionary.size());

        for (int i = 0; i < n; i++) {
            int iOffset = 16 + 2 * i;
            int sliceOffset = ByteUtils.getInt(data, iOffset, 2);
            int blen = ByteUtils.getInt(data, 16 + sliceOffset, 2);
            byte[] slice = ByteUtils.getBytes(data, 18 + sliceOffset, blen & 0x7FFF);
            dictionary.add(new CdicSlice(slice, blen & 0x8000));
        }
    }

    class HuffCode {
        int len;
        int term;
        long max;

        public HuffCode(long code) {
            len = (int) (code & 0x1F);
            term = (int) (code & 0x80);
            max = code >> 8;
            max = ((max + 1) << (32 - len)) - 1;
        }

        @Override
        public String toString() {
            return "HuffCode{" +
                    "len=" + len +
                    ", term=" + term +
                    ", max=" + max +
                    '}';
        }
    }

    class CdicSlice {
        byte[] slice;
        int blen;

        public CdicSlice(byte[] slice, int blen) {
            this.slice = slice;
            this.blen = blen;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < slice.length; i++) {
                sb.append(Integer.toHexString(slice[i] & 0xFF));
            }

            return "CdicSlice{" +
                    "slice=" + sb.toString() +
                    ", blen=" + blen +
                    '}';
        }
    }

}
