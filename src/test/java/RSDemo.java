import java.util.HashMap;

import com.qiniu.qbox.Config;
import com.qiniu.qbox.auth.DigestAuthClient;
import com.qiniu.qbox.rs.DeleteRet;
import com.qiniu.qbox.rs.DropRet;
import com.qiniu.qbox.rs.GetRet;
import com.qiniu.qbox.rs.PublishRet;
import com.qiniu.qbox.rs.PutAuthRet;
import com.qiniu.qbox.rs.PutFileRet;
import com.qiniu.qbox.rs.RSClient;
import com.qiniu.qbox.rs.RSService;
import com.qiniu.qbox.rs.StatRet;

public class RSDemo {
	
	public static void main(String[] args) throws Exception {

		Config.ACCESS_KEY = "<Please apply your access key>";
		Config.SECRET_KEY = "<Dont send your secret key to anyone>";

		DigestAuthClient conn = new DigestAuthClient();

		String bucketName = "bucketName";
		String key = "RSDemo.class";

		String DEMO_DOMAIN = "http://iovip.qbox.me/bucketName";
		
		String path = RSDemo.class.getClassLoader().getResource("").getPath();
        System.out.println("Test to put local file: " + path + key);   
		
		RSService rs = new RSService(conn, bucketName);
		PutAuthRet putAuthRet = rs.putAuth();
		System.out.println("Put URL: " + putAuthRet.getUrl());
		
		HashMap<String, String> callbackParams = new HashMap<String, String>();
		callbackParams.put("key", key);
		
		System.out.println("Delete " + key);
		DeleteRet deleteRet = rs.delete(key);
		System.out.println("Result of delete: " + (deleteRet.ok() ? "Succeeded." : deleteRet));
		
		System.out.println("Putting file: " + path + key);
		PutFileRet putFileRet = RSClient.putFile(
			putAuthRet.getUrl(), bucketName, key, "", path + key, "CustomData", callbackParams);
		if (!putFileRet.ok()) {
			System.out.println("Failed to put file " + path + key + ": " + putFileRet);
			return;
		}
		
		try {
			StatRet statRet = rs.stat(key);
			if (!statRet.ok()) {
				System.out.println("Failed to stat " + key + ": " + statRet);
			} else {
				System.out.println("Result of stat() for " + key);
				System.out.println("  Hash: " + statRet.getHash());
				System.out.println("  Fsize: " + String.valueOf(statRet.getFsize()));
				System.out.println("  MimeType: " + statRet.getMimeType());
				System.out.println("  PutTime: " + String.valueOf(statRet.getPutTime()));
			}

			GetRet getRet = rs.get(key, key);
			System.out.println("Result of get() for " + key);
			if (!getRet.ok()) {
				System.out.println("Failed to get " + key + ": " + getRet);
			} else {
				System.out.println("  Hash: " + getRet.getHash());
				System.out.println("  Fsize: " + String.valueOf(getRet.getFsize()));
				System.out.println("  MimeType: " + getRet.getMimeType());
				System.out.println("  URL: " + getRet.getUrl());
			}
			
			GetRet getIfNotModifiedRet = rs.getIfNotModified(key, key, getRet.getHash());
			System.out.println("Result of getIfNotModified() for " + key);
			if (!getIfNotModifiedRet.ok()) {
				System.out.println("  Did not find " + key + ", or, the entity has been changed.");
			} else {
				System.out.println("  Hash: " + getIfNotModifiedRet.getHash());
				System.out.println("  Fsize: " + String.valueOf(getIfNotModifiedRet.getFsize()));
				System.out.println("  MimeType: " + getIfNotModifiedRet.getMimeType());
				System.out.println("  URL: " + getIfNotModifiedRet.getUrl());
			}
			
			System.out.println("Publish " + bucketName + " as: " + DEMO_DOMAIN);
			PublishRet publishRet = rs.publish(DEMO_DOMAIN);
			System.out.println("Result of publish: " + (publishRet.ok() ? "Succeeded." : "Failed."));

			System.out.println("Unpublish " + bucketName + " as: " + DEMO_DOMAIN);
			PublishRet unpublishRet = rs.unpublish(DEMO_DOMAIN);
			System.out.println("Result of unpublish: " + (unpublishRet.ok() ? "Succeeded." : "Failed."));

			System.out.println("Delete " + key);
			deleteRet = rs.delete(key);
			System.out.println("Result of delete: " + (deleteRet.ok() ? "Succeeded." : "Failed."));
			
			System.out.println("Drop table " + bucketName);
			DropRet dropRet = rs.drop();
			System.out.println("Result of drop: " + (dropRet.ok() ? "Succeeded." : "Failed."));
			
		} catch (Exception e) {
			System.out.println("Failed testing " + key + " with reason:" + e.getMessage());
		}
	}
}
