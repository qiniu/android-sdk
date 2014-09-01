package com.qiniu.resumableio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;

import com.qiniu.auth.Authorizer;
import com.qiniu.conf.Conf;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.ChunkUploadCallRet;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.PutExtra;
import com.qiniu.utils.Crc32;
import com.qiniu.utils.InputStreamAt;
import com.qiniu.utils.InputStreamAt.Input;
import com.qiniu.utils.UploadTask;
import com.qiniu.utils.Util;

public class SliceUploadTask extends UploadTask {
	private List<Block> lastUploadBlocks;
	
	private volatile long uploadLength = 0;
	
	public SliceUploadTask(Authorizer auth, InputStreamAt isa, String key,
			PutExtra extra, CallBack ret) throws IOException {
		super(auth, isa, key, extra, ret);
	}
	

	public void setLastUploadBlocks(List<Block> blocks) {
		lastUploadBlocks = blocks;
	}
	
	@Override
	protected CallRet execDoInBackground(Object... arg0) {
		try{
			List<Block> blks = new ArrayList<Block>();
			CallRet ret = uploadBlocks(blks);
			if(ret == null){
				ret = mkfile(blks);
			}
			return ret;
		} catch (Exception e) {
			return new CallRet(Conf.ERROR_CODE, "", e);
		}
	}
	
	protected void clean() {
		super.clean();
		lastUploadBlocks = null;
	}
	
	private CallRet uploadBlocks(List<Block> blks) throws IOException{
		final long len = orginIsa.length();
		final int blkCount = (int) ((len + Conf.BLOCK_SIZE - 1) / Conf.BLOCK_SIZE);
		
		for(int i=0; i < blkCount; i++){
			int l = (int)Math.min(Conf.BLOCK_SIZE, len - Conf.BLOCK_SIZE * i);
			Block blk = getFromLastUploadedBlocks(i);
			Input data = orginIsa.readNext(l);
			if(blk != null){
				data = null;
				blks.add(blk);
				addUpLength(l);
			}else{
				ChunkUploadCallRet chunkRet = upBlock(l, data, Conf.CHUNK_TRY_TIMES);
				data = null;
				if(!chunkRet.isOk()){
					return chunkRet;
				}
				Block nblk = new Block(i, chunkRet.getCtx(), l, chunkRet.getHost());
				blks.add(nblk);
				this.publishProgress(nblk);
			}
			//data 最大为4M， 尽快回收。 
			data = null;
		}
		
		return null;
	}


	private ChunkUploadCallRet upBlock(int len, Input data, int time) throws IOException {
		UpBlock blkUp = new UpBlock(this, len, data);
		ChunkUploadCallRet chunkRet = blkUp.exec();
		if(!chunkRet.isOk()){
			addUpLength(-blkUp.getUpTotal());
			if(chunkRet.getStatusCode() == 701 && time > 0){
				blkUp = null;
				chunkRet = null;
				return upBlock(len, data, time - 1);
			}
		}
		return chunkRet;
	}
	
	private void addUpLength(long len){
		uploadLength += len;
		this.publishProgress(uploadLength, contentLength);
	}
	
	private Block getFromLastUploadedBlocks(int idx){
		if(lastUploadBlocks == null){
			return null;
		}
		for(Block block : lastUploadBlocks){
			if(idx == block.getIdx()){
				return block;
			}
		}
		return null;
	}
	

    private CallRet mkfile(List<Block> rets) {
		String ctx = mkCtx(rets);
		String lastHost = rets.get(rets.size() - 1).getHost();
		return mkfile(ctx, lastHost, Conf.CHUNK_TRY_TIMES + 1);
	}

    private String mkCtx(List<Block> rets) {
		StringBuffer sb = new StringBuffer();
		for (Block ret : rets) {
			sb.append(",").append(ret.getCtx());
		}
		return sb.substring(1);
	}

	private CallRet mkfile(String ctx, String lastHost, int time) {
		try {
			String url = buildMkfileUrl(lastHost);
			HttpPost post = Util.newPost(url);
	        post.setHeader("Authorization", "UpToken " + auth.getUploadToken());
			post.setEntity(new StringEntity(ctx));
			HttpResponse response = getHttpClient().execute(post);
			CallRet ret = Util.handleResult(response);
			// 500 服务端失败; 579 回调失败
			if (ret.getStatusCode() != 579 && ret.getStatusCode() / 100 == 5
					&& time > 0) {
				return mkfile(ctx, lastHost, time - 1);
			}
			return ret;
		} catch (Exception e) {
			if (time > 0) {
				return mkfile(ctx, lastHost, time - 1);
			}
			throw new RuntimeException(e);
		}
	}

	private String buildMkfileUrl(String lastHost) {
		StringBuilder url = new StringBuilder(lastHost + "/mkfile/" + contentLength);
		if (null != key) {
			url.append("/key/").append(Util.urlsafeBase64(key));
		}
		if (null != extra.mimeType && extra.mimeType.trim().length() != 0) {
			url.append("/mimeType/").append(Util.urlsafeBase64(extra.mimeType));
		}
		if(extra.params != null){
			for (Map.Entry<String, String> xvar : extra.params.entrySet()) {
				if (xvar.getKey().startsWith("x:")) {
					url.append("/").append(xvar.getKey()).append("/").
						append(Util.urlsafeBase64(xvar.getValue()));
				}
			}
		}
		return url.toString();
	}

	
	public static class Block{
		private final int idx;
		private final String ctx;
		private final long length;
		private final String host;

		public Block(int idx, String ctx, long length, String host){
			this.idx = idx;
			this.ctx = ctx;
			this.length = length;
			this.host = host;
		}
		
		public int getIdx() {
			return idx;
		}

		public String getCtx() {
			return ctx;
		}

		public String getHost() {
			return host;
		}
		
		public long getLength(){
			return length;
		}
	}


	static class UpBlock{
		private Input blkData;
		private SliceUploadTask task;
		private volatile String orginHost = Conf.UP_HOST;
		private volatile int total = 0;
		private final int length;
		
		public int getUpTotal(){
			return total;
		}
		
		UpBlock(SliceUploadTask task, int len, Input isa){
			this.task = task;
			this.length = len;
			this.blkData = isa;
		}
		
		ChunkUploadCallRet exec() throws IOException{
			try{
				return uploadChunk();
			}finally{
				blkData = null;
				task = null;
				orginHost = null;
			}
		}
		
		ChunkUploadCallRet uploadChunk() throws IOException{
			final int FIRST_CHUNK = Conf.FIRST_CHUNK;
			final int CHUNK_SIZE = Conf.CHUNK_SIZE;
			
			int flen = (int)Math.min(length, FIRST_CHUNK);
			ChunkUploadCallRet ret = uploadMkblk(length, blkData.readNext(flen));
			if(!ret.isOk()){
				return ret;
			}
			addContentLength(flen);
			if (length > FIRST_CHUNK) {
			    final int count = (int)((length - FIRST_CHUNK + CHUNK_SIZE - 1) / CHUNK_SIZE);
			    for(int i = 0; i < count; i++) {
			    	if(task.isCancelled()){
		    			return new ChunkUploadCallRet(Conf.CANCEL_CODE, "", Conf.PROCESS_MSG);
		    		}
			    	int start = CHUNK_SIZE * i + FIRST_CHUNK;
			        int len = (int)Math.min(length - start, CHUNK_SIZE);
			        ret = uploadChunk(ret, blkData.readNext(len));
			        if(!ret.isOk()){
			        	return ret;
			        }
			        addContentLength(len);
			    }
			}
			return ret;
		}
		
		private void addContentLength(int len){
			total += len;
			task.addUpLength(len);
		}
		
		private ChunkUploadCallRet uploadMkblk(long blkLength, byte[] chunkData) {
	       return uploadMkblk(blkLength, chunkData, Conf.CHUNK_TRY_TIMES);
	    }
		
		private ChunkUploadCallRet uploadMkblk(long blkLength, byte[] chunkData, int time){
			String url = getMkblkUrl(blkLength);
			 ChunkUploadCallRet ret = upload(url, chunkData, time);
	        if(!ret.isOk()){
	        	if(Util.needChangeUpAdress(ret)){
	        		orginHost = Conf.UP_HOST.equals(orginHost) ? Conf.UP_HOST2 : Conf.UP_HOST;
	        	}
	        	if(time > 0){
	        		ret = null;
	        		return uploadMkblk(blkLength, chunkData, time - 1);
	        	}
	        }
	        return ret;
		}

	    private ChunkUploadCallRet uploadChunk(ChunkUploadCallRet ret, byte[] chunkData) {
	            String url = getBlkUrl(ret);
	            return upload(url, chunkData, Conf.CHUNK_TRY_TIMES);
	    }

	    private ChunkUploadCallRet upload(String url, byte[] chunkData, int time)  {
	    	try {
	    		if(task.isCancelled()){
	    			time -= (Conf.CHUNK_TRY_TIMES * 2);
	    			return new ChunkUploadCallRet(Conf.CANCEL_CODE, "", Conf.PROCESS_MSG);
	    		}
	    		task.post = Util.newPost(url);
	    		task.post.setHeader("Authorization", "UpToken " + task.auth.getUploadToken());
	    		task.post.setEntity(buildHttpEntity(chunkData));
	            HttpResponse response = task.getHttpClient().execute(task.post);
	            ChunkUploadCallRet ret = new ChunkUploadCallRet(Util.handleResult(response));
	            
	            return checkAndRetryUpload(url, chunkData, time, ret);
			} catch (Exception e) {
				int status = task.isCancelled() ? Conf.CANCEL_CODE : Conf.ERROR_CODE;
	    		return new ChunkUploadCallRet(status, e);
			}
	    }

	    private HttpEntity buildHttpEntity(byte[] isa) throws IOException {
			ByteArrayEntity en = new ByteArrayEntity(isa);
			return en;
		}
	    
		private ChunkUploadCallRet checkAndRetryUpload(String url, 
				byte[] chunkData, int time, ChunkUploadCallRet ret) throws IOException {
			if(!ret.isOk()){
				if(time > 0 && needPutRetry(ret)){
					return upload(url, chunkData, time - 1);
				}else{
					return ret;
				}
			}
			else{
				
				long crc32 = Crc32.calc(chunkData);
				// 上传的数据 CRC32 校验错。
				if(ret.getCrc32() != crc32){
					if(time > 0){
						return upload(url, chunkData, time - 1);
			    	}else{
			    		// 406	上传的数据 CRC32 校验错。
			    		return new ChunkUploadCallRet(Conf.ERROR_CODE, "", "local's crc32 do not match.");
			    	}
				}else{
				    return ret;
				}
			}
		}
	    
		// 701	上传数据块校验出错需要整个块重试，块重试单独判断。
		// 406	上传的数据 CRC32 校验错；500  服务端失败； 996 ??；
		private boolean needPutRetry(ChunkUploadCallRet ret){
			return ret.getStatusCode() == 406 || ret.getStatusCode() == 996 || ret.getStatusCode() / 100 == 5;
		}

	    private String getMkblkUrl(long blkLength) {
	        String url = orginHost + "/mkblk/" + blkLength;
	        return url;
	    }

	    private String getBlkUrl(ChunkUploadCallRet ret) {
	        String url = ret.getHost() + "/bput/" + ret.getCtx() + "/" + ret.getOffset();
	        return url;
	    }
		
	}

}
