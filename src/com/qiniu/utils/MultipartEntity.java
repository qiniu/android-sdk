package com.qiniu.utils;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

public class MultipartEntity extends AbstractHttpEntity  {
    private String mBoundary;
    private StringBuffer data = new StringBuffer();
    private ArrayList<FileInfo> files = new ArrayList<FileInfo>();

    public MultipartEntity() {
        mBoundary = getRandomString(32);
        contentType = new BasicHeader("Content-Type", "multipart/form-data; boundary=" + mBoundary);
    }

    public void addField(String key, String value) {
        String tmp = "--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n";
        data.append(String.format(tmp, mBoundary, key, value));
    }

    public void addFile(String field, String contentType, String fileName, InputStreamAt isa) {
        files.add(new FileInfo(field, contentType, fileName, isa));
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getContentLength() {
        long len = data.length();
        for (FileInfo fi: files) {
            len += fi.length();
        }
        len += 6 + mBoundary.length();
        return len;
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        return null;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        int index = 0;
        int blockSize = 256;
        while (index < data.length()) {
            int length = min(blockSize, data.length() - index);
            byte[] send = data.substring(index, index+length).getBytes();
            index += length;
            outputStream.write(send);
            outputStream.flush();
        }
        for (FileInfo i: files) {
            i.writeTo(outputStream);
        }
        outputStream.write(("--"+mBoundary+"--\r\n").getBytes());
        outputStream.flush();
        outputStream.close();
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    public int min(int a, int b) {
        if (a > b) return b;
        return a;
    }

    public long min(long a, long b) {
        if (a > b) return b;
        return a;
    }

    private String fileTmp =  "--%s\r\nContent-Disposition: form-data;name=\"%s\";filename=\"%s\"\r\nContent-Type: %s\r\n\r\n";

    class FileInfo {

        public String field;
        public String contentType;
        public String filename;
        public InputStreamAt isa;
        public FileInfo(String field, String contentType, String filename, InputStreamAt isa) {
            this.field = field;
            this.contentType = contentType;
            this.filename = filename;
            this.isa = isa;
            if (this.contentType == null || this.contentType.length() == 0) {
                this.contentType = "application/octet-stream";
            }
        }

        public long length() {
            return fileTmp.length() - 2*4 + mBoundary.length() +
                    field.length() + contentType.length() + filename.length() + isa.length() + 2;
        }


        public void writeTo(OutputStream is) throws IOException {
            is.write(String.format(fileTmp, mBoundary, field, filename, contentType).getBytes());
            is.flush();

            int blockSize = 256;
            long index = 0;
            while(index < isa.length()) {
                int readLength = (int) min((long)blockSize, isa.length()-index);
                is.write(isa.read(index, readLength));
                index += blockSize;
                is.flush();
            }
            is.write("\r\n".getBytes());
            is.flush();
        }
    }

    private static String getRandomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
}
