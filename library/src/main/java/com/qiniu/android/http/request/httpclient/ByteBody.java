package com.qiniu.android.http.request.httpclient;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by yangsen on 2020/6/10
 */
public class ByteBody extends RequestBody {

    private static final int SEGMENT_SIZE = 1024*16; // okio.Segment.SIZE

    private final MediaType mediaType;
    private final byte[] body;

    public ByteBody(MediaType mediaType,
                    byte[] body){

        this.mediaType = mediaType;
        this.body = body;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return body.length;
    }

    @Override
    public void writeTo(BufferedSink bufferedSink) throws IOException {

        int byteOffset = 0;
        int byteSize = SEGMENT_SIZE;
        while (byteOffset < body.length){
            byteSize = Math.min(byteSize, body.length - byteOffset);
            RequestBody requestBody = getRequestBodyWithRange(byteOffset, byteSize);
            requestBody.writeTo(bufferedSink);
            bufferedSink.flush();

            byteOffset += byteSize;
        }
    }


    private RequestBody getRequestBodyWithRange(int location, int size){
        byte[] data = Arrays.copyOfRange(body, location, location + size);
        return RequestBody.create(contentType(), data);
    }
}
