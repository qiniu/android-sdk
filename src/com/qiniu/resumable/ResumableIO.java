package com.qiniu.resumable;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.qiniu.auth.CallRet;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.auth.UpClient;
import com.qiniu.conf.Conf;
import com.qiniu.utils.ThreadSafeInputStream;
import com.qiniu.utils.Utils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.*;

/**
 * =====================================================
 * 上传函数调用流程图
 * =====================================================
 *       put                                    mkfile
 *        |                                        ^
 *        |                                        |
 *        V     上传剩余chunk                       |
 * resumableMkBlock -->  resumablePutBlock -->     |
 *        |          ^      | ^      | ^     | 完成 |
 *        |          |      V |      V |     |----->
 *        |          |   mkblock  putblock   |     |
 *        V          <-----------------------V 完成 |
 * resumableMkBlock --> ... ----------------------->
 *
 * (每4M一个block, 并发进行)
 * =====================================================
 */
public class ResumableIO {
	private static UpClient mClient;
	private static Exception errInvalidPutProgress = new Exception("invalid put progress");
	private static Exception errPutFailed = new Exception("resumable put failed");

	private static RputNotify notify = new RputNotify();
	private static int tryTimes = 3;
	private static int chunkSize = 256 * 1024;
	private static int blockSize = 4 * 1024 * 1024;

	public static void setClient(UpClient client) {
		mClient = client;
	}

	private static UpClient getClient(String token) {
		if (mClient == null) {
			mClient = UpClient.defaultClient(token);
		}
		return mClient;
	}

	/**
	 * 断点续上传二进制流.
	 * @param uptoken
	 * @param key
	 * @param is 二进制流
	 * @param fsize 二进制流长度
	 * @param extra 附加参数
	 * @param ret 回调函数
	 */
	public static void put(String uptoken, final String key, InputStream is,
			final long fsize, final RputExtra extra, final JSONObjectRet ret) {

		int blockCnt = blockCount(fsize);
		if (extra.progresses == null) {
			extra.progresses = new BlkputRet[blockCnt];
		} else if (extra.progresses.length != blockCnt) {
			ret.onFailure(errInvalidPutProgress);
			return;
		}

		if (extra.chunkSize == 0) {
			extra.chunkSize = chunkSize;
		}
		if (extra.tryTimes == 0) {
			extra.tryTimes = tryTimes;
		}
		if (extra.notify == null) {
			extra.notify = notify;
		}

		final Client c = getClient(uptoken);
		final QueueTask tq = new QueueTask(blockCnt);
		for (int i=0; i<blockCnt; i++) {
			final int readLength;
			if ((i+1)*blockSize > fsize) {
				readLength = (int) fsize - i*blockSize;
			} else {
				readLength = blockSize;
			}

			final ThreadSafeInputStream stream = new ThreadSafeInputStream(is);
			final int index = i;
			resumableMkBlock(c, stream, index, readLength, extra, new CallRet() {
				int tryTime = extra.tryTimes;

				@Override
				public void onSuccess(byte[] obj) {
					if ( ! tq.isFinishAll()) return;
					mkfile(c, key, fsize, extra, ret);
				}

				@Override
				public void onFailure(Exception ex) {
					if (tryTime <= 0) {
						ret.onFailure(ex);
						return;
					}
					tryTime--;
					resumableMkBlock(c, stream, index, readLength, extra, this);
				}
			});
		}

	}

	public static void putFile(Context mContext,
							   String uptoken, String key, Uri localfile, RputExtra extra, JSONObjectRet ret) {

		try {
			long size = Utils.getSizeFromUri(mContext, localfile);
			InputStream is = mContext.getContentResolver().openInputStream(localfile);
			put(uptoken, key, is, size, extra, ret);
		} catch (FileNotFoundException e) {
			ret.onFailure(e);
		} catch (IOException e) {
			ret.onFailure(e);
		}
	}

	public static void setSettings(int perChunkSize, int maxTryTime) {
		chunkSize = perChunkSize;
		tryTimes = maxTryTime;
	}

	// ---------------------------------------------------

	private static void resumableMkBlock(final Client client, final ThreadSafeInputStream is,
			final int index, final int size, final RputExtra extra, final CallRet ret) {

		if (extra.progresses[index] != null) {
			resumablePutBlock(client, is, index, size, extra, ret);
			return;
		}

		int chunkSize = extra.chunkSize;
		if (chunkSize > size) {
			chunkSize = size;
		}
		byte[] firstChunk = is.read(index*blockSize, chunkSize);
		if (firstChunk == null) {
			ret.onFailure(errPutFailed);
			return;
		}
		final int firstChunkSize = firstChunk.length;
		mkblock(client, size, firstChunk, new JSONObjectRet() {

			@Override
			public void onSuccess(JSONObject obj) {
				extra.progresses[index] = BlkputRet.parse(obj);
				extra.notify.onNotify(index, firstChunkSize, extra.progresses[index]);
				resumablePutBlock(client, is, index, size, extra, ret);
			}

			@Override
			public void onFailure(Exception ex) {
				ret.onFailure(ex);
			}
		});
	}

	private static void resumablePutBlock(final Client client, final ThreadSafeInputStream is,
										  final int index, final int size, final RputExtra extra,
										  final CallRet ret) {

		if (extra.progresses[index].offset >= size) {
			ret.onSuccess(null);
			return;
		}

		int offset = index * blockSize + extra.progresses[index].offset;
		int chunkSize = extra.chunkSize;
		if (chunkSize > index*blockSize+size-offset) {
			chunkSize = index*blockSize+size-offset;
		}
		byte[] chunk = is.read(offset, chunkSize);
		final int chunkLength = chunk.length;
		putblock(client, extra.progresses[index], chunk, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				extra.progresses[index] = BlkputRet.parse(obj);
				extra.notify.onNotify(index, chunkLength, extra.progresses[index]);
				resumablePutBlock(client, is, index, size, extra, ret);
			}

			@Override
			public void onFailure(Exception ex) {
				ret.onFailure(ex);
				extra.notify.onError(index, size, ex);
			}
		});
	}

	private static int blockCount(long fsize) {
		return (int) (fsize / blockSize) + 1;
	}

	public static void mkblock(Client client, long blkSize, byte[] firstChunk, JSONObjectRet ret) {
		String url = String.format("%s/mkblk/%d", Conf.UP_HOST, blkSize);
		client.call(url, "application/octet-stream", new ByteArrayEntity(firstChunk), ret);
	}

	public static void putblock(Client client, BlkputRet blockret, byte[] chunk, JSONObjectRet ret) {
		String url = String.format("%s/bput/%s/%d", blockret.host, blockret.ctx, blockret.offset);
		client.call(url, "application/octet-stream", new ByteArrayEntity(chunk), ret);
	}

	public static void mkfile(Client client, String key, long fsize, RputExtra extra, JSONObjectRet ret) {
		String entry = Utils.encodeUri(extra.bucket + ":" + key);
		String url = String.format("%s/rs-mkfile/%s/fsize/%d", Conf.UP_HOST, entry, fsize);

		if (extra.mimeType != null) {
			url += "/mimeType/" + Utils.encodeUri(extra.mimeType);
		}
		if (extra.customMeta != null) {
			url += "/meta/" + Utils.encodeUri(extra.customMeta);
		}
		if (extra.callbackParams != null) {
			url += "/params/" + Utils.encodeUri(extra.callbackParams);
		}

		StringBuffer ctxes = new StringBuffer();
		for (BlkputRet a: extra.progresses) {
			ctxes.append(",");
			ctxes.append(a.ctx);
		}
		String ctxString = ctxes.toString();
		if (ctxString.length() > 0) {
			ctxString = ctxString.substring(1);
		}

		try {
			StringEntity se = new StringEntity(ctxString);
			client.call(url, "text/plain", se, ret);
		} catch (UnsupportedEncodingException e) {
			ret.onFailure(e);
		}

	}
}
