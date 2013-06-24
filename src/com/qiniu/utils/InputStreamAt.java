package com.qiniu.utils;

import android.content.Context;

import java.io.*;
import java.util.Arrays;
import java.util.zip.CRC32;

public class InputStreamAt implements Closeable{
	private RandomAccessFile mFileStream;
	private CRC32 crc32 = new CRC32();
	private File tmpFile;
	private long length;
	private boolean closed;

	/**
	 * @param context
	 * @param is InputStream
	 */
	public InputStreamAt(Context context, InputStream is) {
		saveFile(storeToFile(context, is));
	}

	public InputStreamAt(File file) {
		saveFile(file);
	}

	public long crc32() {
		return crc32.getValue();
	}

	public void saveFile(File file) {
		tmpFile = file;
		try {
			length = tmpFile.length();
			mFileStream = new RandomAccessFile(tmpFile, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public long length() {
		return length;
	}

	protected File storeToFile(Context context, InputStream is) {
		File outputDir = context.getCacheDir(); // context being the Activity pointer
		try {
			File e = File.createTempFile("qiniu-", "", outputDir);

			OutputStream os = new FileOutputStream(e);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				crc32.update(buffer, 0, bytesRead);
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			return e;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public synchronized byte[] read(long offset, int length) {
		if (closed) return null;
		try {
			return fileStreamRead(offset, length);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected byte[] fileStreamRead(long offset, int length) throws IOException {
		byte[] data = new byte[length];

		mFileStream.seek(offset);
		int readed;
		int totalReaded = 0;
		do {
			readed = mFileStream.read(data, totalReaded, length);
			if (readed <= 0) break;
			totalReaded += readed;
		} while (readed > 0 && length > totalReaded);

		if (totalReaded != data.length) {
			data = Arrays.copyOfRange(data, 0, totalReaded);
		}
		return data;
	}

	@Override
	public synchronized void close(){
		if (closed) return;
		closed = true;

		if (mFileStream != null) {
			try {
				mFileStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (tmpFile != null) {
			tmpFile.delete();
		}
	}
}
