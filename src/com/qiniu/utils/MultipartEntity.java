package com.qiniu.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.ByteArrayBuffer;

import com.qiniu.conf.Conf;

public class MultipartEntity extends AbstractHttpEntity {

	private final Multipart mpart;
	private IOnProcess mNotify;
	private volatile long writed = 0;
	private volatile long length = -1;

	public MultipartEntity() {
		mpart = new Multipart();
		String boundary = mpart.getBoundary();
		contentType = new BasicHeader("Content-Type",
				"multipart/form-data; boundary=" + boundary);
	}

	public void addField(String key, String value) {
		mpart.add(new StringFormPart(key, value));
	}

	public void addFile(String field, String contentType, String fileName,
			InputStreamAt isa) {
		mpart.add(new FileInfo(field, contentType, fileName, isa));
	}
	
	public void setProcessNotify(IOnProcess ret) {
		mNotify = ret;
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		return null;
	}

	@Override
	public long getContentLength() {
		if(length < 0){
			length = mpart.getTotalLength();
		}
		return length;
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		mpart.writeTo(out);
	}
	
	static class Multipart {
		private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
				.toCharArray();

		private static final Charset DEFAULT_CHARSET = Charset.forName("US-ASCII");
		private static final ByteArrayBuffer CR_LF = encode(DEFAULT_CHARSET, "\r\n");
		private static final ByteArrayBuffer TWO_DASHES = encode(DEFAULT_CHARSET, "--");
		private static final ByteArrayBuffer CONTENT_DISP = encode(
				DEFAULT_CHARSET, "Content-Disposition: form-data");

		private static ByteArrayBuffer encode(final Charset charset,
				final String string) {
			ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
			ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
			bab.append(encoded.array(), encoded.position(), encoded.remaining());
			return bab;
		}

		private static ByteArrayBuffer encode(final String string) {
			final Charset charset = Charset.forName(Conf.CHARSET);
			return encode(charset, string);
		}

		private static void writeBytes(final ByteArrayBuffer b,
				final OutputStream out) throws IOException {
			out.write(b.buffer(), 0, b.length());
		}

		
		private final ArrayList<FormPart> parts;
		private final String boundary;

		public Multipart() {
			this.parts = new ArrayList<FormPart>();
			this.boundary = generateBoundary();
		}

		public void add(FormPart part) {
			parts.add(part);
		}
		
		public String getBoundary() {
			return boundary;
		}

		public long getTotalLength() {
			long contentLen = 0;
			for (FormPart part : this.parts) {
				long len = part.getContentLength();
				if (len >= 0) {
					contentLen += len;
				} else {
					return -1;
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				doWriteTo(out, false);
				byte[] extra = out.toByteArray();
				return contentLen + extra.length;
			} catch (IOException ex) {
				// Should never happen
				return -1;
			}
		}
		
		public void writeTo(final OutputStream out)throws IOException{
			doWriteTo(out, true);
		}

		private void doWriteTo(final OutputStream out, final boolean writeContent)
				throws IOException {
			ByteArrayBuffer boundaryByte = encode(DEFAULT_CHARSET, boundary);

			for (FormPart f : parts) {
				writeStart(out, boundaryByte);
				writeBytes(encode(generateName(f.getName())), out);
				if (f.getFilename() != null) {
					writeBytes(encode(generateFilename(f.getFilename())), out);
					writeBytes(CR_LF, out);
					writeBytes(encode(generateContentType(f.getContentType())),
							out);
				}
				writeBytes(CR_LF, out);
				writeBytes(CR_LF, out);
				if (writeContent) {
					f.writeTo(out);
				}
				writeBytes(CR_LF, out);
			}
			writeAllEnd(out, boundaryByte);
		}
		
		private void writeStart(final OutputStream out, ByteArrayBuffer boundaryByte) throws IOException{
			// 开始
			writeBytes(TWO_DASHES, out);
			writeBytes(boundaryByte, out);
			writeBytes(CR_LF, out);
			writeBytes(CONTENT_DISP, out);
		}
		
		private void writeAllEnd(final OutputStream out, ByteArrayBuffer boundaryByte) throws IOException{
			// 结束
			writeBytes(TWO_DASHES, out);
			writeBytes(boundaryByte, out);
			writeBytes(TWO_DASHES, out);
			writeBytes(CR_LF, out);
		}

		protected String generateName(String name) {
			String s = "; name=\"" + name + "\"";
			return s;
		}

		private String generateFilename(String filename) {
			String s = "; filename=\"" + filename + "\"";
			return s;
		}

		private String generateContentType(String contentType) {
			String s = "Content-Type: " + contentType;
			return s;
		}

		protected String generateBoundary() {
			StringBuilder buffer = new StringBuilder();
			Random rand = new Random();
			int count = rand.nextInt(11) + 30; // a random size from 30 to 40
			for (int i = 0; i < count; i++) {
				buffer.append(MULTIPART_CHARS[rand
						.nextInt(MULTIPART_CHARS.length)]);
			}
			return buffer.toString();
		}
	}

	interface FormPart {
		String getName();

		long getContentLength();

		String getFilename();

		String getContentType();

		void writeTo(OutputStream out) throws IOException;
	}

	class StringFormPart implements FormPart {
		private final byte[] content;
		private final String name;

		public StringFormPart(String name, String value) {
			this.content = Util.toByte(value);
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public long getContentLength() {
			return content.length;
		}

		public String getFilename() {
			return null;
		}

		public String getContentType() {
			return "text/plain";
		}

		public void writeTo(OutputStream out) throws IOException {
			if (out == null) {
				throw new IllegalArgumentException(
						"Output stream may not be null");
			}
			InputStream in = new ByteArrayInputStream(this.content);
			byte[] tmp = new byte[4096];
			int l;
			while ((l = in.read(tmp)) != -1) {
				out.write(tmp, 0, l);
			}
			out.flush();
			
			try{in.close();}catch(Exception e) {}
		}
	}

	class FileInfo implements FormPart {
		private final String name;
		private final String contentType;
		private final String filename;
		private final InputStreamAt isa;

		public FileInfo(String field, String contentType, String filename,
				InputStreamAt isa) {
			this.name = field;
			this.filename = getFilename(filename, isa);
			this.isa = isa;
			String tmp = contentType;
			if (contentType == null || contentType.length() == 0) {
				tmp = "application/octet-stream";
			}
			this.contentType = tmp;
		}
		
		private String getFilename(String filename, InputStreamAt isa){
			if(filename != null){
				return filename;
			}
			String fn = isa.getFilename();
			if(fn != null){
				return fn;
			}else{
				return "_null_";
			}	
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public long getContentLength() {
			return isa.length();
		}

		@Override
		public String getFilename() {
			return filename;
		}

		@Override
		public String getContentType() {
			return contentType;
		}
		
		@Override
		public void writeTo(OutputStream out) throws IOException {
			final long total = isa.length();
			final int len = Conf.ONCE_WRITE_SIZE;
			long idx = 0;
			while(idx < total){
				int read = (int)Math.min(len, total - idx);
				byte[] bs = isa.readNext(read).readAll();
				out.write(bs, 0, read);
				idx += read;
				if (mNotify != null) {
					writed += read;
					mNotify.onProcess(writed, total);
				}
			}
			out.flush();
		}
	}

}
