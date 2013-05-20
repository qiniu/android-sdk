package com.qiniu.utils;

import java.io.IOException;
import java.io.InputStream;

public class ThreadSafeInputStream {
	private InputStream mInputStream;
	public ThreadSafeInputStream(InputStream is) {
		mInputStream = is;
	}

	public synchronized byte[] read(int offset, int length) {
		byte[] data = new byte[length];
		try {
			mInputStream.reset();
			mInputStream.skip(offset);

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			mInputStream.read(data);
		} catch (IOException e) {
			return null;
		}
		return data;
	}
}
