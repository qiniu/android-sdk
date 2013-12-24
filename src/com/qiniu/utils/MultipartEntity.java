package com.qiniu.utils;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

public class MultipartEntity extends AbstractHttpEntity  {
	private String mBoundary;
	private long mContentLength = -1;
	private long writed = 0;
	private IOnProcess mNotify;
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
		return true;
	}

	@Override
	public long getContentLength() {
		if (mContentLength > 0) return mContentLength;
		long len = mData.toString().getBytes().length;
		for (FileInfo fi: mFiles) {
			len += fi.length();
		}
		len += 6 + mBoundary.length();
		mContentLength = len;
		return len;
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		return null;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException {
		writed = 0;
		outputStream.write(mData.toString().getBytes());
		outputStream.flush();
		writed += mData.toString().getBytes().length;
		if (mNotify != null) mNotify.onProcess(writed, getContentLength());
		for (FileInfo i: mFiles) {
			i.writeTo(outputStream);
		}
		byte[] data = ("--" + mBoundary + "--\r\n").getBytes();
		outputStream.write(data);
		outputStream.flush();
		writed += data.length;
		if (mNotify != null) mNotify.onProcess(writed, getContentLength());
		outputStream.close();
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	private String fileTpl =  "--%s\r\nContent-Disposition: form-data;name=\"%s\";filename=\"%s\"\r\nContent-Type: %s\r\n\r\n";

	public void setProcessNotify(IOnProcess ret) {
		mNotify = ret;
	}
	ExecutorService executor = Executors.newFixedThreadPool(1);

	class FileInfo {

		public String mField = "";
		public String mContentType = "";
		public String mFilename = "";
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
			byte[] data = String.format(fileTpl, mBoundary, mField, mFilename, mContentType).getBytes();
			outputStream.write(data);
			outputStream.flush();
			writed += data.length;
			if (mNotify != null) mNotify.onProcess(writed, getContentLength());

			int blockSize = (int) (getContentLength() / 100);
			if (blockSize > 256 * 1024) blockSize = 256 * 1024;
			if (blockSize < 32 * 1024) blockSize = 32 * 1024;
			long index = 0;
			long length = mIsa.length();
			while (index < length) {
				int readLength = (int) StrictMath.min((long) blockSize, mIsa.length() - index);
				int timeout = readLength * 2;
				try {
					write(timeout, outputStream, mIsa.read(index, readLength));
				} catch (Exception e) {
					mNotify.onFailure(e);
					return;
				}
				index += blockSize;
				outputStream.flush();
				writed += readLength;
				if (mNotify != null) mNotify.onProcess(writed, getContentLength());
			}
			outputStream.write("\r\n".getBytes());
			outputStream.flush();
			writed += 2;
			if (mNotify != null) mNotify.onProcess(writed, getContentLength());
		}
	}

	private void write(int timeout, final OutputStream outputStream, final byte[] data) throws InterruptedException, ExecutionException, TimeoutException {
		Callable<Object> readTask = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				outputStream.write(data);
				return null;
			}
		};
		Future<Object> future = executor.submit(readTask);
		future.get(timeout, TimeUnit.MILLISECONDS);
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
