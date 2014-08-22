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
	
	protected ArrayList<CleanCallBack> cleans = new ArrayList<CleanCallBack>();

	public abstract long crc32() throws IOException;

	public abstract long length();
	
	public String getFilename(){
		return null;
	}
	
	public abstract byte[] readNext(int length) throws IOException;

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
	
	public static class ByteInput extends InputStreamAt{
		private final byte[] data;
		private int offset = 0;
		
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
		
		public byte[] readNext(int l) throws IOException {
			if(offset >= length()){
				return null;
			}
			int len = l;
			if(len + offset > length()){
				len = (int)(length() - offset);
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
		private long offset = 0;
		
		
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
		
		public byte[] readNext(int l) throws IOException {
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
			UriInfo info = new UriInfo(uriInfo.getContext(), uriInfo.getUri());
			return Crc32.calc(info.getIs());
		}
		
		public void reset(){
			if(uriInfo != null){
				uriInfo.reset();
			}
		}
		
		public void doClose(){
			if(uriInfo != null){
				uriInfo.close();
			}
		}
		
		public byte[] readNext(int len) throws IOException {
			return uriInfo.readNext(len);
		}
	}
	
	public static class UriInfo{
		private final Context context;
		private final Uri uri;
		
		private InputStream is = null;
		private String name = null;
		private String mimeType = null;
		private String path = null;
		private long length = -1;
		
    	private long offset = 0;

		UriInfo(Context context, Uri uri){
			this.context = context;
			this.uri = uri;
			reset();
		}
		
		public byte[] readNext(int l) throws IOException {
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
		
		public void reset(){
			close();
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
	    	else{
	        	String filePath = uri.getPath();
	        	try{
	        		File file = new File(filePath);
	        		if(file != null && file.isFile()){
		        		name = file.getName();
		        		length = file.length();
		                path = filePath;
	        		}
	        	}catch(Exception e){
	        		
	        	}
	        }
	       
	        try {
				is = context.getContentResolver().openInputStream(uri);
			} catch (FileNotFoundException e) {
				
			}
		}
		
		public Context getContext() {
			return context;
		}
		
		public Uri getUri() {
			return uri;
		}

		public InputStream getIs() {
			return is;
		}

		public String getName() {
			return name;
		}

		public String getMimeType() {
			return mimeType;
		}

		public String getPath() {
			return path;
		}

		public long length() {
			return length;
		}

	}
}
