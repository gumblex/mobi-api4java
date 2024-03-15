package org.rr.mobi4java;

import static org.rr.mobi4java.ByteUtils.write;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class MobiContent {

    public enum CONTENT_TYPE {
        HEADER, CONTENT, INDEX, TAGX, TAG, IDXT, FLIS, FCIS, FDST, DATP, SRCS, CMET, AUDI, VIDE, END_OF_TEXT,
        COVER, THUMBNAIL, IMAGE, UNKNOWN, HUFF, CDIC
    }

    public static Map<Integer, String> codePageMap = new HashMap<>();
    static {
        codePageMap.put(932, "MS932");
        codePageMap.put(936, "GB18030");
        codePageMap.put(949, "MS949");
        codePageMap.put(950, "MS950_HKSCS");
        codePageMap.put(951, "MS950_HKSCS");
        codePageMap.put(1251, "Cp1251");
        codePageMap.put(1252, "Cp1252");
        codePageMap.put(65000, "UTF-7");
        codePageMap.put(65001, "UTF-8");
        codePageMap.put(65002, "UTF-16");
    }

    protected byte[] content;

    private CONTENT_TYPE type;

    MobiContent(byte[] content, CONTENT_TYPE type) {
        this.content = content;
        this.type = type;
    }

    byte[] writeContent(OutputStream out) throws IOException {
        write(content, out);
        return content;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getSize() {
        return content.length;
    }

    public CONTENT_TYPE getType() {
        return type;
    }

    protected String getCharacterEncoding(int textEncoding) {
        String encoding = codePageMap.get(textEncoding);
        if (encoding != null) {
            return encoding;
        }
        encoding = String.format("Cp%d", textEncoding);
        if (Charset.availableCharsets().containsKey(encoding)) {
            return encoding;
        }
        encoding = String.format("MS%d", textEncoding);
        if (Charset.availableCharsets().containsKey(encoding)) {
            return encoding;
        }
        return null;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("contentType", getType())
                .append("content", ByteUtils.dumpByteArray(content))
                .toString();
    }
}
