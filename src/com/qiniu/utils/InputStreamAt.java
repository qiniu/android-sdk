package com.qiniu.utils;

import android.content.Context;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.*;
import java.util.Arrays;
import java.util.zip.CRC32;

public class InputStreamAt implements Closeable {
	private RandomAccessFile mFileStream;
	private byte[] mData;

	private File mTmpFile;
	private boolean mClosed;
	private long mCrc32 = -1;

	/**
	 * @param context
	 * @param is InputStream
	 */
	public static InputStreamAt fromInputStream(Context context, InputStream is) {
		File file = storeToFile(context, is);
		InputStreamAt isa = new InputStreamAt(file);
		return isa;
	}

	public static InputStreamAt fromString(String str) {
		return new InputStreamAt(str.getBytes());
	}

	public InputStreamAt(File file) {
		mTmpFile = file;
		try {
			mFileStream = new RandomAccessFile(mTmpFile, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public InputStreamAt(byte[] data) {
		mData = data;
	}

	public long crc32() {
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
		if (mData != null) {
			return mData.length;
		}

		if (mFileStream != null) {
			try {
				return mFileStream.length();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	protected static File storeToFile(Context context, InputStream is) {
		File outputDir = context.getCacheDir(); // context being the Activity pointer
		File f = null;
		OutputStream os = null;
		try {
			f = File.createTempFile("qiniu-", "", outputDir);
			os = new FileOutputStream(f);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}

	public byte[] read(long offset, int length) {
		if (mClosed) return null;
		try {
			if (mFileStream != null) {
				return fileStreamRead(offset, length);
			}
			if (mData != null) {
				byte[] ret = new byte[length];
				System.arraycopy(mData, (int) offset, ret, 0, length);
				return ret;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected byte[] fileStreamRead(long offset, int length) throws IOException {
		if (mFileStream == null) return null;
		byte[] data = new byte[length];

		int read;
		int totalRead = 0;
		synchronized (data) {
			mFileStream.seek(offset);
			do {
				read = mFileStream.read(data, totalRead, length);
				if (read <= 0) break;
				totalRead += read;
			} while (length > totalRead);
		}

		if (totalRead != data.length) {
			data = Arrays.copyOfRange(data, 0, totalRead);
		}
		return data;
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

		if (mTmpFile != null) {
			mTmpFile.delete();
		}
	}

	public int read(byte[] data) throws IOException {
		return mFileStream.read(data);
	}

    public HttpEntity toHttpEntity(final long offset, final int length) {
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
                int blockSize = 128 * 1024;
                long start = offset;
                long end = offset + length;
                while (start < end) {
                    int readLength = (int) StrictMath.min((long) blockSize, end-start);
                    outputStream.write(input.read(start, readLength));
                    outputStream.flush();
                    start += readLength;
                }
            }

            @Override
            public boolean isStreaming() {
                return false;
            }
        };
    }
}
