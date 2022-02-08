package org.rr.mobi4java;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PDBRecord {

    private static final int PDB_RECORD_OFFSET = 78;

    private static final int PDB_RECORD_LENGTH = 8;

    private byte[] recordDataOffset;
    private byte[] recordAttributes;
    private byte[] uniqueID;

    PDBRecord(byte[] recordDataOffset, byte[] recordAttributes, byte[] uniqueID) {
        this.recordDataOffset = recordDataOffset;
        this.recordAttributes = recordAttributes;
        this.uniqueID = uniqueID;
    }

    PDBRecord(byte[] mobiData, int index) {
        byte[] content = ByteUtils.getBytes(mobiData, getPDBRecordOffset(index), 8);
        recordDataOffset = ByteUtils.getBytes(content, 0, 4);
        recordAttributes = ByteUtils.getBytes(content, 4, 1);
        uniqueID = ByteUtils.getBytes(content, 5, 3);
    }

    public void writeRecord(OutputStream out) throws IOException {
        ByteUtils.write(recordDataOffset, 4, out);
        ByteUtils.write(recordAttributes, 1, out);
        ByteUtils.write(uniqueID, 3, out);
    }

    int getPDBRecordOffset(int index) {
        return PDB_RECORD_OFFSET + (index * PDB_RECORD_LENGTH);
    }

    public int getLength() {
        return PDB_RECORD_LENGTH;
    }

    public long getRecordDataOffset() {
        return ByteUtils.getLong(recordDataOffset);
    }

    public byte getRecordAttributes() {
        return recordAttributes[0];
    }

    public int getUniqueID() {
        return ByteUtils.getInt(uniqueID);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("recordDataOffset", getRecordDataOffset())
                .append("recordAttributes", getRecordAttributes())
                .append("uniqueID", getUniqueID())
                .toString();
    }
}
