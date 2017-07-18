package com.qiniu.android.http;

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
 */
public final class CountingRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final RequestBody body;
    private final ProgressHandler progress;
    private final long totalSize;
    private final CancellationHandler cancellationHandler;

    public CountingRequestBody(RequestBody body, ProgressHandler progress, long totalSize,
                               CancellationHandler cancellationHandler) {
        this.body = body;
        this.progress = progress;
        this.totalSize = totalSize;
        this.cancellationHandler = cancellationHandler;
    }

    @Override
    public long contentLength() throws IOException {
        return body.contentLength();
    }

    @Override
    public MediaType contentType() {
        return body.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink bufferedSink;

        CountingSink countingSink = new CountingSink(sink);
        bufferedSink = Okio.buffer(countingSink);

        body.writeTo(bufferedSink);

        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {

        private int bytesWritten = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

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
