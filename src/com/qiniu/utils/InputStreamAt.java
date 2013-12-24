package com.qiniu.utils;

import android.content.Context;
import android.os.Environment;
import com.qiniu.auth.Client;
import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.*;
import java.util.zip.CRC32;

public class InputStreamAt implements Closeable {
	private RandomAccessFile mFileStream;
	private byte[] mData;

	private File mTmpFile;
	private boolean mClosed;
	private boolean mDelWhenClose = false;
	private long mCrc32 = -1;
	private long mLength = -1;

	/**
	 * @param context
	 * @param is InputStream
	 */
	public static InputStreamAt fromInputStream(Context context, InputStream is) {
		File file = storeToFile(context, is);
		if (file == null) return null;
		InputStreamAt isa = new InputStreamAt(file, true);
		return isa;
	}

	public static InputStreamAt fromFile(File f) {
		InputStreamAt isa = new InputStreamAt(f);
		return isa;
	}

	public static InputStreamAt fromString(String str) {
		return new InputStreamAt(str.getBytes());
	}

	public InputStreamAt(File file) {
		this(file, false);
	}

	public InputStreamAt(File file, boolean delWhenClose) {
		mTmpFile = file;
		mDelWhenClose = delWhenClose;
		try {
			mFileStream = new RandomAccessFile(mTmpFile, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public InputStreamAt(byte[] data) {
		mData = data;
	}

	public long getCrc32(long offset, int length) throws IOException {
		CRC32 crc32 = new CRC32();
		byte[] data = read(offset, length);
		crc32.update(data);
		return crc32.getValue();
	}

	public long crc32() throws IOException {
		if (mCrc32 >= 0) return mCrc32;
		CRC32 crc32 = new CRC32();
		long index = 0;
		int blockSize = 128 * 1024;
		long length = length();
		while (index < length) {
			int size = length-index>=blockSize ? blockSize : (int) (length-index);

			byte[] data = read(index, size);
			if (data == null) return -1;
			crc32.update(data);
			index += size;
		}
		mCrc32 = crc32.getValue();
		return mCrc32;
	}

	public long length() {
		if (mLength >= 0) return mLength;
		if (mData != null) {
			mLength = mData.length;
			return mLength;
		}

		if (mFileStream != null) {
			try {
				mLength = mFileStream.length();
				return mLength;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	protected static File storeToFile(Context context, InputStream is) {
		try {
			File outputDir = getSDPath(context);
			File f = File.createTempFile("qiniu-", "", outputDir);
			OutputStream os = new FileOutputStream(f);
			byte[] buffer = new byte[64 * 1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			if (os != null) os.close();
			if (is != null) is.close();
			return f;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public byte[] read(long offset, int length) throws IOException {
		if (mClosed) throw new IOException("inputStreamAt closed");
		if (mFileStream != null) {
			return fileStreamRead(offset, length);
		}
		if (mData != null) {
			byte[] ret = new byte[length];
			System.arraycopy(mData, (int) offset, ret, 0, length);
			return ret;
		}
		throw new IOException("inputStreamAt not init");
	}

	protected byte[] fileStreamRead(long offset, int length) throws IOException {
		if (mFileStream == null) return null;
		long fileLength = mFileStream.length();
		if (length + offset > fileLength) length = (int) (fileLength - offset);
		byte[] data = new byte[length];

		int read;
		int totalRead = 0;
		synchronized (data) {
			mFileStream.seek(offset);
			do {
				read = mFileStream.read(data, totalRead, length - totalRead);
				if (read <= 0) break;
				totalRead += read;
			} while (length > totalRead);
		}

		if (totalRead != data.length) {
			data = copyOfRange(data, 0, totalRead);
		}
		return data;
	}

	public static byte[] copyOfRange(byte[] original, int from, int to) {
		int newLength = to - from;
		if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
		byte[] copy = new byte[newLength];
		System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
	    return copy;
	}

	@Override
	public synchronized void close(){
		if (mClosed) return;
		mClosed = true;

		if (mFileStream != null) {
			try {
				mFileStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (mTmpFile != null && mDelWhenClose) {
			mTmpFile.delete();
		}
	}

	public int read(byte[] data) throws IOException {
		return mFileStream.read(data);
	}

	public HttpEntity toHttpEntity(final long offset, final int length, final Client.ClientExecutor client) {
		final InputStreamAt input = this;
		return new AbstractHttpEntity() {
			@Override
			public boolean isRepeatable() {
				return false;
			}

			@Override
			public long getContentLength() {
				return length;
			}

			@Override
			public InputStream getContent() throws IOException, IllegalStateException {
				return null;
			}

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				int blockSize = 64 * 1024;
				long start = offset;
				long initStart = 0;
				long end = offset + length;
				long total = end - start;
				while (start < end) {
					if (mClosed) {
						outputStream.close();
						return;
					}
					int readLength = (int) StrictMath.min((long) blockSize, end-start);
					byte[] data = input.read(start, readLength);
					outputStream.write(data);
					outputStream.flush();
					initStart += readLength;
					client.upload(initStart, total);
					start += readLength;
				}
			}

			@Override
			public boolean isStreaming() {
				return false;
			}
		};
	}

	public static File getSDPath(Context context){
		File sdDir = context.getCacheDir();
		boolean sdCardExist = Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED);
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();
		}
		return sdDir;
	}
}
