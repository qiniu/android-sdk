package com.qiniu.resumableio;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.utils.ICancel;
import com.qiniu.utils.InputStreamAt;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class ResumableIO {
	ResumableClient mClient;
	private int BLOCK_SIZE = 4 * 1024 * 1024;
	private int CHUNK_SIZE = 256 * 1024;
	private static int atomicId = 0;
	static HashMap<Integer, ICancel> idCancels = new HashMap<Integer, ICancel>();

	public ResumableIO(String uptoken) {
		mClient = new ResumableClient(Client.getMultithreadClient(), uptoken);
	}
	public ResumableIO(ResumableClient client) {
		mClient = client;
	}

	private synchronized Integer newTask(ICancel c) {
		idCancels.put(atomicId, c);
		return atomicId++;
	}

	private synchronized void removeTask(Integer id) {
		idCancels.remove(id);
	}

	public int putAndClose(final String key, final InputStreamAt input, final PutExtra extra, final JSONObjectRet ret) {
		return put(key, input, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				input.close();
				ret.onSuccess(obj);
			}

			@Override
			public void onProcess(long current, long total) {
				ret.onProcess(current, total);
			}

			@Override
			public void onPause(Object tag) {
				ret.onPause(tag);
			}

			@Override
			public void onFailure(Exception ex) {
				input.close();
				ret.onFailure(ex);
			}
		});
	}

	public int put(final String key, final InputStreamAt input, final PutExtra extra, final JSONObjectRet ret) {
		final int blkCount = (int) (input.length() / BLOCK_SIZE) + 1;
		if (extra.processes == null) {
			extra.processes = new PutRet[blkCount];
			for (int i=0; i<extra.processes.length; i++) {
				extra.processes[i] = new PutRet();
			}
		}
		final int[] success = new int[] {0};
		final long[] uploaded = new long[blkCount];
		final ICancel[][] cancelers = new ICancel[blkCount][1];
		final int taskId = newTask(new ICancel() {

			@Override
			public boolean cancel(boolean isIntercupt) {
				for (ICancel[] cancel: cancelers) {
					if (cancel == null || cancel[0] == null) continue;
					cancel[0].cancel(true);
				}
				ret.onPause(extra);
				return false;
			}
		});
		final boolean[] failured = new boolean[] {false};
		for (int i=0; i<blkCount; i++) {
			final long startPos = i * BLOCK_SIZE;
			final int index = i;
			cancelers[i] = mClient.putblock(input, extra.processes[i], startPos, new JSONObjectRet() {
				int retryTime = 5;
				@Override
				public void onSuccess(JSONObject obj) {
					if (failured[0]) return;
					success[0]++;
					if (success[0] == blkCount) {
						String ctxes = "";
						for (PutRet ret: extra.processes) {
							ctxes += "," + ret.ctx;
						}
						if (ctxes.length() > 0) {
							ctxes = ctxes.substring(1);
						}
						removeTask(taskId);
						mClient.mkfile(key, input.length(), extra.mimeType, extra.params, ctxes, ret);
					}
				}

				@Override
				public synchronized void onProcess(long current, long total) {
					if (failured[0]) return;
					uploaded[index] = current;
					current = 0;
					for (long c: uploaded) { current += c; }
					ret.onProcess(current, input.length());
				}

				@Override
				public void onFailure(Exception ex) {
					retryTime--;
					if (retryTime <= 0 || (ex.getMessage() != null && ex.getMessage().contains("Unauthorized"))) {
						removeTask(taskId);
						failured[0] = true;
						ret.onFailure(ex);
						return;
					}
					if (ex.getMessage() != null && ex.getMessage().contains("invalid BlockCtx")) {
						uploaded[index] = 0;
						extra.processes[index] = new PutRet();
					}
					cancelers[index] = mClient.putblock(input, extra.processes[index], startPos, this);
				}
			});
		}

		return taskId;
	}

	public int putFile(String key, File file, PutExtra extra, final JSONObjectRet ret) {
		final InputStreamAt isa = InputStreamAt.fromFile(file);
		return putAndClose(key, isa, extra, ret);
	}

	public int putFile(Context mContext, String key, Uri uri, PutExtra extra, final JSONObjectRet ret) {
		if (!uri.toString().startsWith("file")) {
			uri = convertFileUri(mContext, uri);
		}
		File file = null;
		try {
			file = new File(new URI(uri.toString()));
		} catch (URISyntaxException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			ret.onFailure(e);
			return -1;
		}
		if (!file.exists()) {
			ret.onFailure(new Exception("not exist"));
			return -1;
		}
		InputStreamAt isa = InputStreamAt.fromFile(file);
		return putAndClose(key, isa, extra, ret);
	}

	public static Uri convertFileUri(Context mContext, Uri uri) {
		String filePath = null;
		if (uri != null && "content".equals(uri.getScheme())) {
			Cursor cursor = mContext.getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
			cursor.moveToFirst();
			filePath = cursor.getString(0);
			cursor.close();
		}
		else {
			filePath = uri.getPath();
		}
		return Uri.parse("file://" + filePath);
	}

	public synchronized static void stop(int id) {
		ICancel c = idCancels.get(id);
		if (c == null) return;
		c.cancel(true);
		idCancels.remove(id);
	}

	public static ResumableIO defaultInstance(String uptoken) {
		return new ResumableIO(new ResumableClient(Client.getMultithreadClient(), uptoken));
	}

	public static int putAndClose(String uptoken, String key, InputStreamAt isa, PutExtra extra, JSONObjectRet ret) {
		return ResumableIO.defaultInstance(uptoken).putAndClose(uptoken, key, isa, extra, ret);
	}

	public static int put(String uptoken, String key, InputStreamAt isa, PutExtra extra, JSONObjectRet ret) {
		return ResumableIO.defaultInstance(uptoken).put(key, isa, extra, ret);
	}

	public static int putFile(Context mContext, String uptoken, String key, Uri uri, PutExtra extra, JSONObjectRet ret) {
		return ResumableIO.defaultInstance(uptoken).putFile(mContext, key, uri, extra, ret);
	}

}
