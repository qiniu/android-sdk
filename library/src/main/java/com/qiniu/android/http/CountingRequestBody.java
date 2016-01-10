package com.qiniu.android.http;

import com.qiniu.android.utils.AsyncRun;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by bailong on 16/1/8.
 */
public class CountingRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final RequestBody body;
    private final ProgressHandler progress;

    public CountingRequestBody(RequestBody body, ProgressHandler progress) {
        this.body = body;
        this.progress = progress;
    }

    @Override
    public long contentLength() throws IOException{
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
            super.write(source, byteCount);

            bytesWritten += byteCount;
            AsyncRun.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        progress.onProgress(bytesWritten, (int)contentLength());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
