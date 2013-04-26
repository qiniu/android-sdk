package com.qiniu.utils;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.nio.ByteBuffer;

public class MultipartFormData {
	private static final int BOUNARY_LENGTH = 32;

	private boolean mClosed = false;
	private ByteBuffer mBuf;
	private String mBounary;


	public MultipartFormData(int capacity) {
		mBuf = ByteBuffer.allocate(capacity);
		generateBounary();
	}

	public void addField(String field, String data) {
		StringBuffer sb = new StringBuffer("--");
		sb.append(mBounary);
		sb.append("\r\n");

		sb.append("Content-Disposition: form-data; name=\"");
		sb.append(field);
		sb.append("\"\r\n\r\n");

		sb.append(data);
		sb.append("\r\n");

		mBuf.put(sb.toString().getBytes());
	}

	public void addFile(String field, String fileName, byte[] fileData) {
		StringBuffer sb = new StringBuffer("--");
		sb.append(mBounary);
		sb.append("\r\n");

		sb.append("Content-Disposition: form-data;");
		sb.append("name=\"" + field + "\";");
		sb.append("filename=\"" + fileName + "\"");
		sb.append("\r\n");
		sb.append("Content-Type: application/octet-stream\r\n\r\n");

		mBuf.put(sb.toString().getBytes());
		mBuf.put(fileData);
		mBuf.put("\r\n".getBytes());
	}

	public void close() {
		if (mClosed) return;
		mClosed = true;

		mBuf.put(String.format("--%s--\r\n", mBounary).getBytes());
	}

	public String getContentType() {
		return "multipart/form-data; boundary=" + mBounary;
	}

	public HttpEntity getEntity() {
		close();
		return new ByteArrayEntity(mBuf.array());
	}

	private void generateBounary() {
		mBounary = Utils.getRandomString(BOUNARY_LENGTH);
	}
}
