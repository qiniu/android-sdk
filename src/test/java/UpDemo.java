import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.qiniu.qbox.Config;
import com.qiniu.qbox.auth.AuthPolicy;
import com.qiniu.qbox.auth.UpTokenClient;
import com.qiniu.qbox.rs.PutFileRet;
import com.qiniu.qbox.rs.RSClient;
import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.ProgressNotifier;
import com.qiniu.qbox.up.UpService;


public class UpDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config.ACCESS_KEY = "<Please apply your access key>";
		Config.SECRET_KEY = "<Dont send your secret key to anyone>";


		String bucketName = "bucketName";
		String key = "RSDemo.class";
		
		String path = RSDemo.class.getClassLoader().getResource("").getPath();		
		System.out.println("Resumably putting file: " + path + key);

		AuthPolicy policy = new AuthPolicy("bucketName", 3600);
		String token = policy.makeAuthTokenString();
		
		UpTokenClient upTokenClient = new UpTokenClient(token);
		UpService upClient = new UpService(upTokenClient);

		try {
			RandomAccessFile f = new RandomAccessFile(path + key, "r");

			long fsize = f.length();
			long blockCount = UpService.blockCount(fsize);
			
			String[] checksums = new String[(int)blockCount];
			BlockProgress[] progresses = new BlockProgress[(int)blockCount];
			
			Notifier notif = new Notifier();

			PutFileRet putFileRet = RSClient.resumablePutFile(upClient, 
					checksums, progresses, 
					(ProgressNotifier)notif, (BlockProgressNotifier)notif, 
					bucketName, key, "", f, fsize, "CustomMeta", "");

			if (putFileRet.ok()) {
				System.out.println("Successfully put file resumably: " + putFileRet.getHash());
			} else {
				System.out.println("Failed to put file resumably: " + putFileRet);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
