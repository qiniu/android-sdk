package com.qiniu.resumable;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import com.qiniu.auth.CallRet;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.auth.UpClient;
import com.qiniu.conf.Conf;
import com.qiniu.utils.InputStreamAt;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

/**
 * ======================================================
 * 上传函数调用流程图
 * ======================================================
 *     put
 *      |
 *      V                               -------
 * uploadBlock -> mkblock -> putblock ->| ctx |
 *      |                               |     |
 *      |                               |     |-> mkfile
 *      V                               |     |
 * uploadBlock -> mkblock -> putblock ->| ctx |
 *                                      -------
 * (每4M一个block, 并发进行)
 * ======================================================
 */
public class ResumableIO {
    public static String UNDEFINDED_KEY = "?";
	private static UpClient mClient;

	private static Exception errInvalidPutProgress = new Exception("invalid put progress");
	private static Exception errPutFailed = new Exception("resumable put failed");

	private static RputNotify notify = new RputNotify();
	private static int tryTimes = 3;
	private static int chunkSize = 256 * 1024;
	public static int BLOCK_SIZE = 4 * 1024 * 1024;

	public static void setClient(UpClient client) {
		mClient = client;
	}

	private static UpClient getClient(String token) {
		if (mClient == null) {
			mClient = UpClient.defaultClient(token);
		} else {
			mClient.updateToken(token);
		}
		return mClient;
	}

	/**
	 * 断点续上传二进制流.
	 * @param uptoken
	 * @param key
	 * @param is 二进制流
	 * @param extra 附加参数
	 * @param ret 回调函数
	 */
	public static void put(String uptoken, String key, final InputStreamAt is, final RputExtra extra, final JSONObjectRet ret) {
        if (key == null) { key = UNDEFINDED_KEY; }
        final String RKey = key;

		int blockCnt = blockCount(is.length());
        if (extra.notify != null) {
            extra.notify.setTotal(is.length());
        }
		if (extra.progresses == null) {
			extra.progresses = new BlkputRet[blockCnt];
		}
        if (extra.progresses.length != blockCnt) {
			ret.onFailure(errInvalidPutProgress);
			return;
		}
        initPutExtra(extra);

		final Client c = getClient(uptoken);
		final QueueTask tq = new QueueTask(blockCnt);

		for (int i=0; i<blockCnt; i++) {
			final int index = i;
			uploadBlock(c, is, index, extra, new CallRet() {
                int tryTime = extra.tryTimes;

                @Override
                public void onSuccess(byte[] obj) {
                    if (!tq.addFinishAndCheckIsFinishAll()) return;
                    mkfile(c, RKey, is.length(), extra, ret);
                }

                @Override
                public void onFailure(Exception ex) {
                    if (ex.getMessage().endsWith("401")) {
                        // unauthorized
                        tryTime = 0;
                    }

                    if (tryTime > 0) {
                        tryTime--;
                        uploadBlock(c, is, index, extra, this);
                        return;
                    }

                    tq.setFailure();
                    ret.onFailure(ex);
                }
            });
		}
	}

    private static void initPutExtra(RputExtra extra) {
        if (extra.chunkSize == 0) {
			extra.chunkSize = chunkSize;
		}
		if (extra.tryTimes == 0) {
			extra.tryTimes = tryTimes;
		}
		if (extra.notify == null) {
			extra.notify = notify;
		}
    }

    public static void putFile(
            Context mContext, String uptoken, String key, Uri localfile, RputExtra extra, final JSONObjectRet ret) {

        final InputStreamAt isa;
		try {
			isa = new InputStreamAt(mContext, mContext.getContentResolver().openInputStream(localfile));
		} catch (Exception e) {
			ret.onFailure(e);
            return;
        }
        put(uptoken, key, isa, extra, new JSONObjectRet() {
            @Override
            public void onSuccess(JSONObject obj) {
                ret.onSuccess(obj);
                isa.close();
            }

            @Override
            public void onFailure(Exception ex) {
                ret.onFailure(ex);
                isa.close();
            }
        });
	}

	public static void setSettings(int perChunkSize, int maxTryTime) {
		chunkSize = perChunkSize;
		tryTimes = maxTryTime;
	}

	// ---------------------------------------------------

	private static void uploadBlock(
       final Client client, final InputStreamAt is, final int index, final RputExtra extra, final CallRet ret) {

        int realBlockSize = min((int)is.length()-index*BLOCK_SIZE, BLOCK_SIZE);
        int offset = 0;
        if (extra.progresses[index] != null) {
            offset = extra.progresses[index].offset;
        }

        final int chunkSize = min(extra.chunkSize, realBlockSize-offset);
        if (chunkSize <= 0) {
            ret.onSuccess(null);
            return;
        }

		byte[] chunk = is.read(index*BLOCK_SIZE+offset, chunkSize);
        CRC32 crc32 = new CRC32();
        crc32.update(chunk);
        final long crc = crc32.getValue();
		if (chunk == null) {
			ret.onFailure(errPutFailed);
			return;
		}

        JSONObjectRet callback = new JSONObjectRet() {
		    @Override
		    public void onSuccess(JSONObject obj) {
		    	extra.progresses[index] = BlkputRet.parse(obj);
                if ( ! extra.progresses[index].checkCrc32(crc)) {
                    onFailure(new Exception("crc32 not matched"));
                    return;
                }
		    	extra.notify.onNotify(index, chunkSize, extra.progresses[index]);
		    	uploadBlock(client, is, index, extra, ret);
		    }

		    @Override
		    public void onFailure(Exception ex) {
                extra.notify.onError(index, chunkSize, ex);
		    	ret.onFailure(ex);
		    }
		};

        if (offset == 0) {
		    mkblock(client, realBlockSize, chunk, callback);
        } else {
            putblock(client, extra.progresses[index], chunk, callback);
        }
    }

	private static int blockCount(long fsize) {
		return (int) (fsize / BLOCK_SIZE) + 1;
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
		String entry = encodeUri(extra.bucket + ":" + key);
		String url = String.format("%s/rs-mkfile/%s/fsize/%d", Conf.UP_HOST, entry, fsize);

		if (extra.mimeType != null) {
			url += "/mimeType/" + encodeUri(extra.mimeType);
		}
		if (extra.customMeta != null) {
			url += "/meta/" + encodeUri(extra.customMeta);
		}
		if (extra.callbackParams != null) {
			url += "/params/" + encodeUri(extra.callbackParams);
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

    public static int min(int a, int b) {
        if (a > b) return b;
        return a;
    }


    public static String encodeUri(String uri) {
        return new String(Base64.encode(uri.getBytes(), Base64.URL_SAFE)).trim();
	}
}
