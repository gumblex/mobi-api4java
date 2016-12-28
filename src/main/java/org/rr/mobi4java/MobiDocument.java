package org.rr.mobi4java;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.collect;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.rr.mobi4java.ByteUtils.chunk;
import static org.rr.mobi4java.MobiUtils.removeRandomBytes;
import static org.rr.mobi4java.MobiUtils.removeUtfReplacementCharacter;
import static org.rr.mobi4java.util.MobiLz77.lz77Decode;
import static org.rr.mobi4java.util.MobiLz77.lz77Encode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.Transformer;
import org.rr.mobi4java.EXTHRecord.RECORD_TYPE;
import org.rr.mobi4java.MobiContent.CONTENT_TYPE;
import org.rr.mobi4java.MobiContentHeader.COMPRESSION_CODE;


public class MobiDocument {

	private PDBHeader pdbHeader;
	
	private MobiContentHeader mobiHeader;
	
	private List<MobiContent> mobiContents;
	
	private MobiMetaData mobiMetaData;
	
	MobiDocument(PDBHeader pdbHeader, MobiContentHeader mobiHeader, List<MobiContent> mobiContent) {
		this.pdbHeader = pdbHeader;
		this.mobiHeader = mobiHeader;
		this.mobiContents = mobiContent;
		this.mobiMetaData = new MobiMetaData(mobiHeader);
	}

	PDBHeader getPdbHeader() {
		return pdbHeader;
	}

	MobiContentHeader getMobiHeader() {
		return mobiHeader;
	}
	
	List<MobiContent> getMobiContents() {
		return mobiContents;
	}
	
	public MobiMetaData getMetaData() {
		return mobiMetaData;
	}
	
	/**
	 * Get the image bytes for the cover of this {@link MobiDocument}.
	 * 
	 * @return The cover bytes or <code>null</code> if no cover was found.
	 */
	public byte[] getCover() {
		MobiContent content = getCoverByType(CONTENT_TYPE.COVER);
		return content != null ? content.getContent() : null;
	}
	
	/**
	 * Store the given image data as cover. If the document already have a cover, the old cover will be replaced with the given one.
	 * The thumbnail for this document is not affected by this method. The thumbnail must be set separately with the {@link #setThumbnail(byte[])} method.
	 * 
	 * @param image The image bytes to be used as cover. Take sure that the image format is supported by the target device.
	 * @throws IllegalArgumentException if the given image is null.
	 */
	public void setCover(byte[] image) throws IllegalArgumentException {
		if(image == null) {
			throw new IllegalArgumentException("No image bytes available.");
		} 
		
		MobiContent content= getCoverByType(CONTENT_TYPE.COVER);
		if(content != null) {
			content.setContent(image);
		} else {
			int indexAfterFirstImageRecord = 0;
			createCoverOrThumbnailRecord(RECORD_TYPE.COVER_OFFSET, indexAfterFirstImageRecord, image);
		}
	}

	/**
	 * Store the given image data as thumbnail. If the document already have a thumbnail, the old thumbnail will be replaced with the given one.
	 * 
	 * @param image The image bytes to be used as thumbnail. Take sure that the image format is supported by the target device.
	 * @throws IllegalArgumentException if the given image is null.
	 */
	public void setThumbnail(byte[] image) {
		if(image == null) {
			throw new IllegalArgumentException("No image bytes available.");
		}
		
		MobiContent content= getCoverByType(CONTENT_TYPE.THUMBNAIL);
		if(content != null) {
			content.setContent(image);
		} else {
			int indexAfterFirstImageRecord = getCover() != null ? 1 : 0;
			createCoverOrThumbnailRecord(RECORD_TYPE.THUMBNAIL_OFFSET, indexAfterFirstImageRecord, image);
		}
	}
	
	private void createCoverOrThumbnailRecord(RECORD_TYPE type, int indexAfterFirstImageRecord, byte[] image) {
		EXTHRecord exthRecord = new EXTHRecord(type.getType());
		exthRecord.setIntData(indexAfterFirstImageRecord);
		mobiHeader.addEXTHRecord(exthRecord);
		
		MobiContent content;
		if(type == RECORD_TYPE.COVER_OFFSET) {
			content = MobiContentRecordFactory.createCoverRecord(image);
		} else if(type == RECORD_TYPE.THUMBNAIL_OFFSET) {
			content = MobiContentRecordFactory.createThumbnailRecord(image);
		} else {
			throw new IllegalArgumentException("Failed to create record type " + type);
		}
		mobiContents.add(mobiHeader.getFirstImageIndex() + indexAfterFirstImageRecord, content);
	}
	
	/**
	 * Get the image bytes for the thumbnail of this {@link MobiDocument}.
	 * 
	 * @return The thumbnail bytes or <code>null</code> if no thumbnail was found.
	 */
	public byte[] getThumbnail() {
		MobiContent content = getCoverByType(CONTENT_TYPE.THUMBNAIL);
		return content != null ? content.getContent() : null; 
	}
	
	public List<byte[]> getImages() {
		List<MobiContent> imageContents = getImageContents();
		List<byte[]> images = new ArrayList<>(imageContents.size());
		for (MobiContent imageContent : imageContents) {
			images.add(imageContent.getContent());
		}
		return images;
	}
	
	List<MobiContent> getImageContents() {
		int firstImageIndex = mobiHeader.getFirstImageIndex();
		List<MobiContent> imageContents = new ArrayList<>(mobiContents.size() - firstImageIndex);
		for(int i = firstImageIndex; i < mobiContents.size(); i++) {
			MobiContent content = mobiContents.get(i);
			if(content.getType() == CONTENT_TYPE.IMAGE || content.getType() == CONTENT_TYPE.COVER || content.getType() == CONTENT_TYPE.THUMBNAIL) {
				imageContents.add(content);
			}
		}
		return imageContents;
	}
	
	private MobiContent getCoverByType(CONTENT_TYPE type) {
		for (MobiContent mobiContent : mobiContents) {
			if(mobiContent.getType() == type) {
				return mobiContent;
			}
		}
		return null;
	}
	
	public String getFullName() {
		return getMobiHeader().getFullName();
	}
	
	public void setFullName(String name) throws UnsupportedEncodingException {
		getMobiHeader().setFullName(name);
	}
	
	public String getCharacterEncoding() {
		return mobiHeader.getCharacterEncoding();
	}
	
	/**
	 * Get the mobi html formatted content.
	 *  
	 * @return The text part if the mobi document. Never returns <code>null</code>.
	 * @throws IOException
	 */
  public String getTextContent() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  	List<MobiContent> contents = MobiUtils.findContentsByType(mobiContents, CONTENT_TYPE.CONTENT);
  	for (MobiContent mobiContent : contents) {
  		byte[] decoded = null;
			if (mobiHeader.getCompressionCode() == COMPRESSION_CODE.PALM_DOC) {
				decoded = lz77Decode(mobiContent.getContent());
			} else if (mobiHeader.getCompressionCode() == COMPRESSION_CODE.NONE) {
				decoded = mobiContent.getContent();
			} else if (mobiHeader.getCompressionCode() == COMPRESSION_CODE.HUFF_CDIC) {
				// TODO 
				throw new UnsupportedEncodingException("HUFF/CDIC encoding is not supported."); 
			} else {
				throw new IllegalArgumentException("Compression not supported " + mobiHeader.getCompressionCode());
			}

			decoded = removeRandomBytes(decoded);
			
			if (mobiContent.getType() != CONTENT_TYPE.INDEX) {
				outputStream.write(decoded);
			}
		}
		return removeUtfReplacementCharacter(outputStream.toString(getCharacterEncoding()));
  }
  
  /**
   * Removes the old text content and applies the given text to the {@link MobiDocument}. If a book index exists it will be removed. 
   * 
   * @param mobiText The mobi html formatted text which should be set to the {@link MobiDocument}.
   * @throws IOException
   */
  public void setTextContent(String mobiText) throws IOException {
  	int firstContentIndex = MobiUtils.getTextContentStartIndex(mobiHeader);
  	int lastContentIndex = firstContentIndex + mobiHeader.getRecordCount();

  	// remove text content including a possible book index and a EOT record.
  	removeContent(firstContentIndex, Math.max(lastContentIndex, mobiHeader.getFirstImageIndex()));
  	
  	byte[] encodedMobiText = lz77Encode(mobiText.getBytes(getCharacterEncoding()));
  	mobiHeader.setCompressionCode(COMPRESSION_CODE.PALM_DOC);
  	
  	Collection<byte[]> chunkedMobiText = chunk(encodedMobiText, MobiContentHeader.DEFAULT_RECORD_SIZE);
  	
  	Collection<MobiContent> contentRecords = union(toMobiContent(chunkedMobiText), singletonList(MobiContentRecordFactory.createEndOfTextRecord()));
  	mobiContents.addAll(firstContentIndex, contentRecords);
  	
  	mobiHeader.setTextLength(mobiText.length());
  	mobiHeader.setRecordCount(chunkedMobiText.size());
  	mobiHeader.setRecordSize(MobiContentHeader.DEFAULT_RECORD_SIZE);
  	
  	// set non book index and first image index to the same value because there is no book index at this point,
  	// which is usually located between these two indices.
  	mobiHeader.setFirstNonBookIndex(firstContentIndex + contentRecords.size());
  	mobiHeader.setFirstImageIndex(firstContentIndex + contentRecords.size());
  }

	private Collection<MobiContent> toMobiContent(Collection<byte[]> chunkedMobiText) {
		return collect(chunkedMobiText, new Transformer<byte[], MobiContent>() {
			
			@Override
			public MobiContent transform(byte[] content) {
				return MobiContentRecordFactory.createContentRecord(content);
			}
		});
	}
  
  private void removeContent(int firstContentIndex, int lastContentIndex) {
  	mobiContents.removeAll(mobiContents.subList(firstContentIndex, lastContentIndex));
  }

}
