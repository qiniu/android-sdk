package com.qiniu.utils;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

public class MultipartEntity extends AbstractHttpEntity  {
	private String mBoundary;
	private StringBuffer mData = new StringBuffer();
	private ArrayList<FileInfo> mFiles = new ArrayList<FileInfo>();

	public MultipartEntity() {
		mBoundary = getRandomString(32);
		contentType = new BasicHeader("Content-Type", "multipart/form-data; boundary=" + mBoundary);
	}

	public void addField(String key, String value) {
		String tmp = "--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n";
		mData.append(String.format(tmp, mBoundary, key, value));
	}

	public void addFile(String field, String contentType, String fileName, InputStreamAt isa) {
		mFiles.add(new FileInfo(field, contentType, fileName, isa));
	}

	@Override
	public boolean isRepeatable() {
		return false;
	}

	@Override
	public long getContentLength() {
		long len = mData.toString().getBytes().length;
		for (FileInfo fi: mFiles) {
			len += fi.length();
		}
		len += 6 + mBoundary.length();
		return len;
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		return null;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException {
		outputStream.write(mData.toString().getBytes());
		outputStream.flush();
		for (FileInfo i: mFiles) {
			i.writeTo(outputStream);
		}
		outputStream.write(("--" + mBoundary + "--\r\n").getBytes());
		outputStream.flush();
		outputStream.close();
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	private String fileTpl =  "--%s\r\nContent-Disposition: form-data;name=\"%s\";filename=\"%s\"\r\nContent-Type: %s\r\n\r\n";

	class FileInfo {

		public String mField;
		public String mContentType;
		public String mFilename;
		public InputStreamAt mIsa;

		public FileInfo(String field, String contentType, String filename, InputStreamAt isa) {
			mField = field;
			mContentType = contentType;
			mFilename = filename;
			mIsa = isa;
			if (mContentType == null || mContentType.length() == 0) {
				mContentType = "application/octet-stream";
			}
		}
		
		public long length() {
			return fileTpl.length() - 2*4 + mBoundary.length() + mIsa.length() + 2 +
				mField.getBytes().length + mContentType.length() + mFilename.getBytes().length;
		}

		public void writeTo(OutputStream outputStream) throws IOException {
			outputStream.write(String.format(fileTpl, mBoundary, mField, mFilename, mContentType).getBytes());
			outputStream.flush();

			int blockSize = 256 * 1024;
			long index = 0;
			long length = mIsa.length();
			while (index < length) {
				int readLength = (int) StrictMath.min((long) blockSize, mIsa.length() - index);
				outputStream.write(mIsa.read(index, readLength));
				index += blockSize;
				outputStream.flush();
			}
			outputStream.write("\r\n".getBytes());
			outputStream.flush();
		}
	}

	private static String getRandomString(int length) {
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}
}
