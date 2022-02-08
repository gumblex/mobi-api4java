package org.rr.mobi4java.decompression;

import java.io.IOException;

/**
 * Description:
 * Author:  limengqi03@baidu.com
 * Created:  January 29, 15:16
 * Copyright (c) 2022, Baidu.com All Rights Reserved
 */
public class None implements Inflater {
    @Override
    public byte[] inflate(byte[] input) throws IOException {
        return input;
    }
}
