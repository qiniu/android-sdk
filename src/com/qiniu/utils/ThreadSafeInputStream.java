package com.qiniu.utils;

import android.content.Context;
import com.qiniu.resumable.ResumableIO;

import java.io.*;

public class ThreadSafeInputStream implements Closeable{
	private InputStream mInputStream;
	private RandomAccessFile mFileStream;
	private static int maxMenory = ResumableIO.BLOCK_SIZE;
	private File tmpFile;

	public ThreadSafeInputStream(Context context, InputStream is) {
		if (is.markSupported()) {
			mInputStream = is;
			mInputStream.mark(maxMenory);
			return;
		}

		if ( ! isLargeStream(is)) {
			// not need to support `mark` if stream size less than BLOCK_SIZE
			mInputStream = is;
			return;
		}

		// if stream size large than BLOCK_SIZE, may store to file for seek
		storeToFile(context, is);
	}

	private void storeToFile(Context context, InputStream is) {
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
			return;
		}
	}

	private boolean isLargeStream(InputStream is) {
		int remain;
		try {
			remain = is.available();
		} catch (IOException e) {
			return false;
		}
		return remain > maxMenory;
	}

	public synchronized byte[] read(int offset, int length) {
		try {
			if (mInputStream != null) {
				return inputStreamRead(offset, length);
			}

			if (mFileStream != null) {
				return fileStreamRead(offset, length);
			}
		} catch (IOException e) {

		}

		return null;
	}

	public byte[] fileStreamRead(int offset, int length) throws IOException {
		byte[] data = new byte[length];

		mFileStream.seek(offset);
		mFileStream.read(data);
		return data;
	}

	public byte[] inputStreamRead(int offset, int length) throws IOException {
		byte[] data = new byte[length];

		if (mInputStream.markSupported()) {
			mInputStream.reset();
			mInputStream.skip(offset);
		}

		mInputStream.read(data);
		return data;
	}


	@Override
	public void close(){

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
