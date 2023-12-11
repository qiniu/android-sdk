package com.qiniu.android.http.request.httpclient;

import com.qiniu.android.http.CancellationHandler;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.utils.AsyncRun;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by bailong on 16/1/8.
 *
 * @hidden
 */
public final class CountingRequestBody extends RequestBody {
    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE


    private final RequestBody body;
    private final ProgressHandler progress;
    private final long totalSize;
    private final CancellationHandler cancellationHandler;

    /**
     * CountingRequestBody 构造函数
     *
     * @param body                请求体
     * @param progress            请求进度回调
     * @param totalSize           请求体总大小
     * @param cancellationHandler 取消函数
     */
    public CountingRequestBody(RequestBody body, ProgressHandler progress, long totalSize,
                               CancellationHandler cancellationHandler) {
        this.body = body;
        this.progress = progress;
        this.totalSize = totalSize;
        this.cancellationHandler = cancellationHandler;
    }

    /**
     * 获取请求体大小
     *
     * @return 请求体大小
     * @throws IOException 异常
     */
    @Override
    public long contentLength() throws IOException {
        return body.contentLength();
    }

    /**
     * 获取请求 ContentType
     *
     * @return 请求 ContentType
     */
    @Override
    public MediaType contentType() {
        return body.contentType();
    }

    /**
     * 写入数据
     *
     * @param sink BufferedSink
     * @throws IOException 异常
     */
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink bufferedSink;

        CountingSink countingSink = new CountingSink(sink);
        bufferedSink = Okio.buffer(countingSink);

        body.writeTo(bufferedSink);

        bufferedSink.flush();
    }

    /**
     * 请求进度 Sink
     *
     * @hidden
     */
    protected final class CountingSink extends ForwardingSink {

        private int bytesWritten = 0;

        /**
         * 构造方法
         *
         * @param delegate Sink
         */
        public CountingSink(Sink delegate) {
            super(delegate);
        }

        /**
         * 写入数据
         *
         * @param source    Buffer
         * @param byteCount byteCount
         * @throws IOException 异常
         */
        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            if (cancellationHandler == null && progress == null) {
                super.write(source, byteCount);
                return;
            }
            if (cancellationHandler != null && cancellationHandler.isCancelled()) {
                throw new CancellationHandler.CancellationException();
            }
            super.write(source, byteCount);
            bytesWritten += byteCount;
            if (progress != null) {
                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        progress.onProgress(bytesWritten, totalSize);
                    }
                });
            }
        }
    }
}
