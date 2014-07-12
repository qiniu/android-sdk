package com.qiniu.utils;

import android.content.Context;
import android.os.Environment;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.*;

import com.qiniu.auth.Client;

public class InputStreamAt implements Closeable {
	private RandomAccessFile mFileStream;
	private byte[] mData;

	private File mFile;
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
		if (file == null) {
			return null;
		}
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
		mFile = file;
		mDelWhenClose = delWhenClose;
		try {
			mFileStream = new RandomAccessFile(mFile, "r");
			mLength = mFileStream.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public InputStreamAt(byte[] data) {
		mData = data;
		mLength = mData.length;
	}

	public long partCrc32(long offset, int length) throws IOException {
		byte[] data = read(offset, length);
		return Crc32.calc(data);
	}

	public long crc32() throws IOException {
		if (mCrc32 >= 0) {
			return mCrc32;
		}
		if (mData != null) {
			mCrc32 = Crc32.calc(mData);
			return mCrc32;
		}
		if (mFile != null) {
			mCrc32 = Crc32.calc(mFile);
			return mCrc32;
		}
		return mCrc32;
	}

	public long length() {
		return mLength;
	}

	public byte[] read(long offset, int length) throws IOException {
		if (mClosed) {
			throw new IOException("inputStreamAt closed");
		}
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

	@Override
	public synchronized void close(){
		if (mClosed){
			return;
		}
		mClosed = true;

		if (mFileStream != null) {
			try {
				mFileStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (mFile != null && mDelWhenClose) {
			mFile.delete();
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

	private static byte[] copyOfRange(byte[] original, int from, int to) {
		int newLength = to - from;
		if (newLength < 0) {
			throw new IllegalArgumentException(from + " > " + to);
		}
		byte[] copy = new byte[newLength];
		System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
	    return copy;
	}


	private static File storeToFile(Context context, InputStream is) {
		if (is == null) {
			return null;
		}
		OutputStream os = null;
		File f = null;
		boolean failed = false;
		try {
			File outputDir = FileUri.getSDPath(context);
			f = File.createTempFile("qiniu-", "", outputDir);
			os = new FileOutputStream(f);
			byte[] buffer = new byte[64 * 1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
			failed = true;
		}
		try {
			is.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		if (os != null) {
			try {
				os.close();
			} catch (IOException e){
				e.printStackTrace();
			}
		}


		if (failed && f != null) {
			f.delete();
			f = null;
		}

		return f;
	}

	private byte[] fileStreamRead(long offset, int length) throws IOException {
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
}
