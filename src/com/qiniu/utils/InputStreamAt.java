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
	
	protected static Input buildNextInput(final InputStreamAt isa, final int len){
		return new Input(){
			private final long start = isa.getOffset();
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
	
	protected long outterOffset;
	
	protected ArrayList<CleanCallBack> cleans = new ArrayList<CleanCallBack>();

	public abstract long crc32() throws IOException;

	public abstract long length();
	
	public String getFilename(){
		return null;
	}
	
	public Input readNext(int len) throws IOException{
		if(len + outterOffset >= length()){
			len = (int)(length() - outterOffset);
		}

		Input input =  buildNextInput(this, len);
		outterOffset += len;
		return input;
	}

	public void reset() throws IOException{
		outterOffset = 0;
	}
	
	protected long getOffset(){
		return outterOffset;
	}
	
	protected abstract byte[] read(long offset, int length) throws IOException;

	public void close(){
		try{doClose();}catch(Exception e){}
		for(CleanCallBack clean : cleans){
			try{clean.clean();}catch(Exception e){}
		}
	}
	
	protected abstract void doClose();

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
		
		public void doClose(){
			
		}
		
		protected byte[] read(long off, int len) {
			int offset = (int) off;
			if(offset == 0 && len == length()){
				return data;
			}
			if(len <= 0){
				return new byte[0];
			}
			byte[] bs = new byte[len];
			System.arraycopy(data, offset, bs, 0, len);
			offset += len;
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
		
		public void doClose(){
			if(randomAccessFile !=null){
				try {randomAccessFile.close();} catch (IOException e) {}
			}
		}
		
		protected byte[] read(long offset, int len) throws IOException {
			if(offset >= length()){
				return null;
			}
			byte[] bs = new byte[len];
			randomAccessFile.seek(offset);
			randomAccessFile.read(bs);
			offset += len;
			return bs;
		}
	}
	
	public static class UriInput extends InputStreamAt{
		private UriInfo uriInfo;
		private FileInput fileInput;
		private InputStream is;

		public UriInput(Context context, Uri uri){
			uriInfo = new UriInfo(context, uri);
			File f = uriInfo.getFile();
			if(f != null && f.exists() && f.isFile()){
				try {
					fileInput = new FileInput(f);
				} catch (FileNotFoundException e) {

				}
			}
		}
		
		private void genIs() throws FileNotFoundException{
			if(fileInput == null && is == null && uriInfo != null){
				is = uriInfo.getIs();
			}
		}
		
		public long length(){
			return uriInfo.length();
		}
		
		public String getFilename(){
			return uriInfo.getName();
		}
		
		public long crc32() throws IOException {
			if(fileInput != null){
				return fileInput.crc32();
			}else{
				UriInfo info = new UriInfo(uriInfo.getContext(), uriInfo.getUri());
				return Crc32.calc(info.getIs());
			}
		}
		
		public void reset() throws IOException{
			super.reset();
			if(fileInput != null){
				fileInput.reset();
			}else if(uriInfo != null){
				uriInfo.reset();
				outterOffset = 0;
				is = uriInfo.getIs();
			}
		}
		
		public void doClose(){
			if(is != null){
				try {is.close();} catch (IOException e) {}
			}
			if(uriInfo != null){
				uriInfo.close();
			}
			if(fileInput != null){
				fileInput.close();
			}
			
			is = null;
			uriInfo = null;
			fileInput = null;
		}
		
		
		public Input readNext(final int len) throws IOException {
			if(fileInput != null){
				return fileInput.readNext(len);
			}else{
				// 流不支持随机读取，先读取整块byte[]，再从其中读取部分
				return new Input(){
					private byte[] content;
					private ByteInput bi;
					
					//保证 流 被正常消耗
					{
						content = readNextContent(len);
						bi = new ByteInput(content);
					}
					
					@Override
					public byte[] readAll() throws IOException {
						return content;
					}

					@Override
					public byte[] readNext(int length) throws IOException {
						return bi.readNext(length).readAll();
					}
					
				};
			}
		}
		
		private byte[] readNextContent(int l) throws IOException {
			if(outterOffset >= length()){
				return null;
			}
			genIs();
			int len = l;
			if(len + outterOffset >= length()){
				len = (int)(length() - outterOffset);
			}
			byte[] bs = new byte[len];
			is.read(bs);
			outterOffset += len;
			return bs;
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

		/**
		 * 通过uri查找文件，若找到，构建基于文件的 InputStreamIsa ，委托七处理;
		 * 否则获取流
		 * @throws FileNotFoundException 
		 */
		UriInfo(Context context, Uri uri){
			this.context = context;
			this.uri = uri;
			build();
		}
		

		private void build() {
			tryContentFile(uri.getPath());
			tryContentField();
			if(hasFile()){
				return;
			}
			
			checkContent();
			
			tryContentFile(path);
			if(hasFile()){
				return;
			}
		}
		
		
		
		public void reset() throws IOException{
				close();
				genContentIs();
		}
		
		private boolean hasFile(){
			return file != null && file.exists() && file.isFile();
		}
		
		
		private void tryContentFile(String path){
			if(path != null){
				String[] ps = {"", "/mnt", "/mnt/"};
				for(String p : ps){
					try{file = new File(p + path);}catch(Exception e){}
					if(hasFile()){
						return;
					}
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
		
		private void genContentIs() throws FileNotFoundException{
			is = context.getContentResolver().openInputStream(uri);
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
		
		public void close(){
			if(is != null){
				try {is.close();} catch (Exception e) {}
			}
			is = null;
		}
		
		public Context getContext() {
			return context;
		}


		public Uri getUri() {
			return uri;
		}

		
		public File getFile(){
			return file;
		}
		
		public InputStream getIs() throws FileNotFoundException{
			if(is == null){
				genContentIs();
			}
			return is;
		}
		
		
		public String getName() {
			return name;
		}

		public long length() {
			return length;
		}

	}
}
