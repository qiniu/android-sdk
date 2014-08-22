package com.qiniu.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.*;
import java.util.ArrayList;

public abstract class InputStreamAt implements Closeable {

	public static UriInput fromUri(Context context, Uri uri){
		return new UriInput(context, uri);
	}
	
	public static FileInput fromInputStream(Context context, InputStream is) throws IOException {
		return fromInputStream(context, is, null);
	}

	public static FileInput fromInputStream(Context context, InputStream is, String filename) throws IOException {
		final File file = Util.storeToFile(context, is);
		if (file == null) {
			return null;
		}
		
		FileInput isa = null;
		try{
			isa = new FileInput(file, filename);
			isa.cleans.add(new CleanCallBack(){

				@Override
				public void clean() {
					file.delete();
				}
				
			});
		}catch(IOException e){
			if(file != null){
				file.delete();
			}
			throw e;
		}

		return isa;
	}

	public static FileInput fromFile(File f) throws FileNotFoundException {
		return new FileInput(f);
	}
	
	public static ByteInput fromByte(byte[] data){
		return new ByteInput(data);
	}
	
	protected static Input buildInput(final InputStreamAt isa, final int len){
		return new Input(){
			private final long start = isa.offset;
			private int innerOffset = 0;
			@Override
			public byte[] readAll() throws IOException {
				return isa.read(start, len);
			}

			@Override
			public byte[] readNext(int length) throws IOException {
				if(innerOffset + length > len){
					length = len - innerOffset;
				}
				if(length <= 0){
					return new byte[0];
				}
				byte[] bs = isa.read(start + innerOffset, length);
				innerOffset += length;
				return bs;
			}
			
		};
	}
	
	protected long offset;
	
	protected ArrayList<CleanCallBack> cleans = new ArrayList<CleanCallBack>();

	public abstract long crc32() throws IOException;

	public abstract long length();
	
	public String getFilename(){
		return null;
	}
	
	public Input readNext(int length) throws IOException{
		return buildInput(this, length);
	}
	
	protected abstract byte[] read(long offset, int length) throws IOException;

	public void close(){
		try{doClose();}catch(Exception e){}
		for(CleanCallBack clean : cleans){
			try{clean.clean();}catch(Exception e){}
		}
	}
	
	protected abstract void doClose();

	public abstract void reset() throws IOException;

	public static interface CleanCallBack{
		void clean();
	}
	
	public static interface Input{
		byte[] readAll() throws IOException;
		byte[] readNext(int length) throws IOException;
	}
	
	public static class ByteInput extends InputStreamAt{
		private final byte[] data;
		
		public ByteInput(byte[] data){
			this.data = data;
		}
		
		public long length(){
			return data.length;
		}
		
		public long crc32() throws IOException {
			return Crc32.calc(data);
		}
		
		public void reset() throws IOException{
			offset = 0;
		}
		
		public void doClose(){
			
		}
		
		protected byte[] read(long off, int length) {
			int offset = (int) off;
			if(offset == 0 && length == length()){
				return data;
			}
			if(length + offset > length()){
				length = (int)(length() - offset);
			}
			if(length <= 0){
				return new byte[0];
			}
			byte[] bs = new byte[length];
			System.arraycopy(data, offset, bs, 0, length);
			offset += length;
			return bs;
		}
		
	}
	
	public static class FileInput extends InputStreamAt{
		private final RandomAccessFile randomAccessFile;
		private final File file;
		private final String filename;
		
		public FileInput(File file) throws FileNotFoundException{
			this(file, null);
		}
		
		public FileInput(File file, String aliasFilename) throws FileNotFoundException{
			this.file = file;
			this.randomAccessFile = new RandomAccessFile(file, "r");
			this.filename = (aliasFilename != null && aliasFilename.trim().length() > 0) ? aliasFilename : file.getName();
		}
		
		
		public long length(){
			return file.length();
		}
		
		public String getFilename(){
			return filename;
		}
		
		public long crc32() throws IOException {
			return Crc32.calc(file);
		}
		
		public void reset() throws IOException{
			offset = 0;
		}
		
		public void doClose(){
			if(randomAccessFile !=null){
				try {randomAccessFile.close();} catch (IOException e) {}
			}
		}
		
		protected byte[] read(long offset, int l) throws IOException {
			if(offset >= length()){
				return null;
			}
			int len = l;
			if(len + offset >= length()){
				len = (int)(length() - offset);
			}
			byte[] bs = new byte[len];
			randomAccessFile.seek(offset);
			randomAccessFile.read(bs);
			offset += len;
			return bs;
		}
	}
	
	public static class UriInput extends InputStreamAt{
		private final UriInfo uriInfo;

		public UriInput(Context context, Uri uri) {
			uriInfo = new UriInfo(context, uri);
		}
		
		public long length(){
			return uriInfo.length();
		}
		
		public String getFilename(){
			return uriInfo.getName();
		}
		
		public long crc32() throws IOException {
			return uriInfo.crc32();
		}
		
		public void reset() throws IOException{
			if(uriInfo != null){
				uriInfo.reset();
			}
		}
		
		public void doClose(){
			if(uriInfo != null){
				uriInfo.close();
			}
		}
		
		
		public Input readNext(int len) throws IOException {
			return uriInfo.readNext(len);
		}
		
		protected byte[] read(long offset, int len) throws IOException {
			throw new UnsupportedOperationException();
		}
	}
	
	public static class UriInfo{
		private final Context context;
		private final Uri uri;
		
		private InputStream is;
		private File file;
		private String name;
		private String mimeType;
		private String path;
		private long length = -1;
		
    	private long offset = 0;
		private FileInput fileInput;

		/**
		 * 通过uri查找文件，若找到，构建基于文件的 InputStreamIsa ，委托七处理;
		 * 否则获取流
		 */
		UriInfo(Context context, Uri uri){
			this.context = context;
			this.uri = uri;
			build();
			if(hasFile()){
				try {
					fileInput = new FileInput(file);
				} catch (FileNotFoundException e) {
				}
			}
		}
		

		private void build(){
			tryContentFile(uri.getPath());
			if(hasFile()){
				return;
			}
			tryContentField();
			
			checkContent();
			
			tryContentFile(path);
			if(hasFile()){
				return;
			}
			
			getContentIs();
		}
		
		
		
		public void reset() throws IOException{
			if(fileInput != null){
				fileInput.reset();
				return;
			}else{
				close();
				offset = 0;
				getContentIs();
			}
		}
		
		private boolean hasFile(){
			return file != null && file.isFile();
		}
		
		
		private void tryContentFile(String path){
			if(path != null){
				try{file = new File(path);}catch(Exception e){}
				if(hasFile()){
					return;
				}
				try{file = new File("/mnt" + path);}catch(Exception e){}
				if(hasFile()){
					return;
				}
				try{file = new File("/mnt/" + path);}catch(Exception e){}
				if(hasFile()){
					return;
				}
			}
		}
		
		private void tryContentField(){
			if(hasFile()){
				name = file.getName();
        		length = file.length();
                path = file.getAbsolutePath();
			}
		}
		
		private void getContentIs(){
			try {
				is = context.getContentResolver().openInputStream(uri);
			} catch (FileNotFoundException e) {
				
			}
		}
		
		private void checkContent(){
			if ("content".equalsIgnoreCase(uri.getScheme())){
	    		Cursor cursor = null;
	            try{
	            	ContentResolver resolver = context.getContentResolver();
	            	String [] col = {MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DISPLAY_NAME,
	            			MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATA};
	                cursor = resolver.query(uri, col, null, null, null);
		            if(cursor != null && cursor.moveToFirst()){
		                int cc = cursor.getColumnCount();
		                for(int i=0; i < cc; i++){
		                    String colName = cursor.getColumnName(i);
		                    String colValue = cursor.getString(i);
		                    if(MediaStore.MediaColumns.DISPLAY_NAME.equalsIgnoreCase(colName)){
		                        name = colValue;
		                    }else if(MediaStore.MediaColumns.SIZE.equalsIgnoreCase(colName)){
		                    	length = cursor.getLong(i);
		                    }else if(MediaStore.MediaColumns.MIME_TYPE.equalsIgnoreCase(colName)){
		                        mimeType = colValue;
		                    }else if(MediaStore.MediaColumns.DATA.equalsIgnoreCase(colName)){
		                        path = colValue;
		                    }
		                }
		            }
	            }finally{
	            	if(cursor != null){
	            		try{cursor.close();}catch(Exception e){}
	            	}
	            }
	        }

		}
		
		public long crc32() throws IOException {
			if(fileInput != null){
				return fileInput.crc32();
			}else{
				UriInfo info = new UriInfo(context, uri);
				return Crc32.calc(info.is);
			}
		}
		
		public Input readNext(final int len) throws IOException {
			if(fileInput != null){
				return fileInput.readNext(len);
			}else{
				// 流不支持随机读取，先读取整块byte[]，再从其中读取部分
				return new Input(){
					private byte[] content;
					private ByteInput isa;
					@Override
					public byte[] readAll() throws IOException {
						if(content == null){
							content = readNextContent(len);
							isa = new ByteInput(content);
						}
						return content;
					}

					@Override
					public byte[] readNext(int length) throws IOException {
						readAll();
						return isa.readNext(length).readAll();
					}
					
				};
			}
		}

		public byte[] readNextContent(int l) throws IOException {
			if(offset >= length){
				return null;
			}
			int len = l;
			if(len + offset >= length){
				len = (int)(length - offset);
			}
			byte[] bs = new byte[len];
			is.read(bs);
			offset += len;
			return bs;
		}

		public void close(){
			if(is != null){
				try {is.close();} catch (Exception e) {}
			}
			is = null;
		}
		
		
		public String getName() {
			return name;
		}

		public long length() {
			return length;
		}

	}
}
