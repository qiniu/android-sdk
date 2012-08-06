package com.qiniu.qbox.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.qiniu.qbox.R;
import com.qiniu.qbox.auth.UpTokenClient;
import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.Config;
import com.qiniu.qbox.up.ProgressNotifier;
import com.qiniu.qbox.up.PutFileRet;
import com.qiniu.qbox.up.UpClient;
import com.qiniu.qbox.up.UpService;

public class MainActivity extends Activity {

	private Button chooseButton;

	public class FileProgressNotifier implements ProgressNotifier, BlockProgressNotifier {

		@Override
		public void notify(int blockIndex, String checksum) {
			System.out.println("Progress Notify:" +
					"\n\tBlockIndex: " + String.valueOf(blockIndex) + 
					"\n\tChecksum: " + checksum);
		}

		@Override
		public void notify(int blockIndex, BlockProgress progress) {
			System.out.println("BlockProgress Notify:" +
					"\n\tBlockIndex: " + String.valueOf(blockIndex) + 
					"\n\tContext: " + progress.context +
					"\n\tOffset: " + String.valueOf(progress.offset) +
					"\n\tRestSize: " + String.valueOf(progress.restSize));
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.chooseButton = (Button)this.findViewById(R.id.chooseButton);
        
		Config.ACCESS_KEY = "RLT1NBD08g3kih5-0v8Yi6nX6cBhesa2Dju4P7mT";
		Config.SECRET_KEY = "k6uZoSDAdKBXQcNYG3UOm4bP3spDVkTg-9hWHIKm";

      //处理事件
        this.chooseButton.setOnClickListener( new Button.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
        		String bucketName = "tblName";
        		String key = "localread20120709-2.txt";
        		
        		AuthPolicy policy = new AuthPolicy("tblName", 3600);
        		String token = policy.makeAuthTokenString();
        		
        		UpTokenClient upTokenClient = new UpTokenClient(token);
        		UpService upClient = new UpService(upTokenClient);

        		try {
        			//RandomAccessFile f = new RandomAccessFile(path + key, "r");
        			RandomAccessFile f = new RandomAccessFile("/sdcard/image.JPG", "r");

        			long fsize = f.length();
        			long blockCount = UpService.blockCount(fsize);
        			
        			String[] checksums = new String[(int)blockCount];
        			BlockProgress[] progresses = new BlockProgress[(int)blockCount];
        			
        			FileProgressNotifier notif = new FileProgressNotifier();

        			PutFileRet putFileRet = UpClient.resumablePutFile(upClient, 
        					checksums, progresses, 
        					(FileProgressNotifier)notif, (BlockProgressNotifier)notif, 
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
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
