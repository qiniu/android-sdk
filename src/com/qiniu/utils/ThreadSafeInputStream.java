package com.qiniu.utils;

import android.content.Context;
import com.qiniu.resumable.ResumableIO;

import java.io.*;
import java.util.Arrays;

public class ThreadSafeInputStream implements Closeable{
	private InputStream mInputStream;
	private RandomAccessFile mFileStream;
	private static int maxMemory;
	private File tmpFile;
	private boolean closed;

	/**
	 * @param context
	 * @param is InputStream
	 * @param maxMemory if stream length large than maxMemory, will store data to disk
	 */
	public ThreadSafeInputStream(Context context, InputStream is, int maxMemory) {
		this.maxMemory = maxMemory;

		if ( ! isLargeStream(is)) {
			// not need to support `seek` if stream size less than maxMemory
			mInputStream = is;
			if ( ! mInputStream.markSupported()) {
				mInputStream = new BufferedInputStream(mInputStream, maxMemory);
			}
			mInputStream.mark(maxMemory);
			return;
		}

		// if stream size large than maxMemory, may store to file for seek
		storeToFile(context, is);
	}

	protected void storeToFile(Context context, InputStream is) {
		File outputDir = context.getCacheDir(); // context being the Activity pointer
		try {
			tmpFile = File.createTempFile("qiniu-", "", outputDir);

			OutputStream os = new FileOutputStream(tmpFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			mFileStream = new RandomAccessFile(tmpFile, "r");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	protected boolean isLargeStream(InputStream is) {
		int remain;
		try {
			remain = is.available();
		} catch (IOException e) {
			return false;
		}
		return remain > maxMemory;
	}

	public synchronized byte[] read(int offset, int length) {
		if (closed) return null;
		try {
			if (mInputStream != null) {
				return inputStreamRead(offset, length);
			}

			if (mFileStream != null) {
				return fileStreamRead(offset, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected byte[] fileStreamRead(int offset, int length) throws IOException {
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

	protected byte[] inputStreamRead(int offset, int length) throws IOException {
		byte[] data = new byte[length];
		mInputStream.reset();
		mInputStream.skip(offset);

		int readed;
		int totalReaded = 0;
		do {
			readed = mInputStream.read(data, totalReaded, length);
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
		if (mInputStream != null) {
			try {
				mInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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
