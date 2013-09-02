package com.qiniu.resumableio;

import android.content.Context;
import android.net.Uri;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.utils.ICancel;
import com.qiniu.utils.InputStreamAt;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.HashMap;

public class ResumableIO {
    ResumableClient mClient;
    private int BLOCK_SIZE = 4 * 1024 * 1024;
    private int CHUNK_SIZE = 256 * 1024;
    private static int atomicId = 0;
    private static HashMap<Integer, ICancel[][]> idCancels = new HashMap<Integer, ICancel[][]>();

    public ResumableIO(String uptoken) {
        mClient = new ResumableClient(Client.getMultithreadClient(), uptoken);
    }
    public ResumableIO(ResumableClient client) {
        mClient = client;
    }

    private synchronized Integer newTask(ICancel[][] c) {
        idCancels.put(atomicId, c);
        return atomicId++;
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
        for (int i=0; i<blkCount; i++) {
            final long startPos = i * BLOCK_SIZE;
            final PutRet process = extra.processes[i];
            final int index = i;
            cancelers[i] = mClient.putblock(input, process, startPos, new JSONObjectRet() {
                int retryTime = 3;
                @Override
                public void onSuccess(JSONObject obj) {
                    success[0]++;
                    if (success[0] == blkCount) {
                        String ctxes = "";
                        for (PutRet ret: extra.processes) {
                            ctxes += "," + ret.ctx;
                        }
                        if (ctxes.length() > 0) {
                            ctxes = ctxes.substring(1);
                        }
                        mClient.mkfile(key, input.length(), extra.mimeType, extra.params, ctxes, ret);
                    }
                }
                @Override
                public void onProcess(long current, long total) {
                    uploaded[index] = current;
                    current = 0;
                    for (long c: uploaded) { current += c; }
                    ret.onProcess(current, input.length());
                }
                @Override
                public void onFailure(Exception ex) {
                    if (ex.getMessage().contains("Unauthorization")) {
                        ret.onFailure(ex);
                        return;
                    }
                    retryTime--;
                    if (retryTime <= 0) {
                        ret.onFailure(ex);
                        return;
                    }
                    mClient.putblock(input, process, startPos, this);
                }
            });
        }

        return newTask(cancelers);
    }

    public int putFile(Context mContext, String key, Uri uri, PutExtra extra, final JSONObjectRet ret) {
        final InputStreamAt isa;
        try {
            isa = InputStreamAt.fromInputStream(mContext, mContext.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            ret.onFailure(e);
            return -1;
        }

        return put(key, isa, extra, new JSONObjectRet() {
            @Override
            public void onSuccess(JSONObject obj) {
                isa.close();
                ret.onSuccess(obj);
            }

            @Override
            public void onProcess(long current, long total) {
                ret.onProcess(current, total);
            }

            @Override
            public void onFailure(Exception ex) {
                isa.close();
                ret.onFailure(ex);
            }
        });
    }

    public synchronized static void stop(int id) {
        ICancel[][] c = idCancels.get(id);
        for (ICancel[] cc: c) {
            cc[0].cancel(true);
        }
        idCancels.remove(c);
    }

    public static int put(String uptoken, String key, InputStreamAt isa, PutExtra extra, JSONObjectRet ret) {
        return new ResumableIO(new ResumableClient(Client.getMultithreadClient(), uptoken)).put(key, isa, extra, ret);
    }

    public static int putFile(Context mContext, String uptoken, String key, Uri uri, PutExtra extra, JSONObjectRet ret) {
        return new ResumableIO(new ResumableClient(Client.getMultithreadClient(), uptoken)).putFile(mContext, key, uri, extra, ret);
    }

}
