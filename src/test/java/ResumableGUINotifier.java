import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import org.json.JSONObject;

import com.qiniu.qbox.Config;
import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.ProgressNotifier;

public class ResumableGUINotifier implements ProgressNotifier,
		BlockProgressNotifier {

	private PrintStream os;
	private long current;  
	private long amount;
	private long lastTime; //the timestamp of last chunck upload
	private long realSpeed; //the speed of the file upload
	// 创建一条垂直进度条
	private JProgressBar bar;
	private JLabel realTimeSpeedLabel = new JLabel();
	JFrame frame;
	boolean pauseFlag = false;

	public ResumableGUINotifier(String progressFile, BlockProgress[] progresses, long fileSize) throws Exception {

		OutputStream out = new FileOutputStream(progressFile, true);
		this.os = new PrintStream(out, true);
		long currentSize = 0;
		for (int i = 0; i < progresses.length; i++) {
			if (progresses[i] != null) {
				currentSize += progresses[i].offset;
			}
		}

		this.current = currentSize;
		this.amount = fileSize;
		this.realSpeed = 0;
		this.frame = new JFrame("断点续传测试");
		// 创建一条垂直进度条
		this.bar = new JProgressBar(JProgressBar.HORIZONTAL);

		final JButton terminal = new JButton("退出");

		Box funcBox = new Box(BoxLayout.Y_AXIS);
		Box infoBox = new Box(BoxLayout.Y_AXIS);
		Box realSpeedBox = new Box(BoxLayout.X_AXIS);

		funcBox.add(terminal);

		realSpeedBox.add(new JLabel("上传即时速度："));
		realSpeedBox.add(this.realTimeSpeedLabel);
		
		infoBox.add(this.bar);
		infoBox.add(new JLabel("文件大小：" + fileSize/(1024 * 1024) + "M"),1);
		infoBox.add(realSpeedBox);
		
		this.frame.setLayout(new FlowLayout());
		this.frame.add(infoBox);
		this.frame.add(funcBox);


		// 设置在进度条中绘制完成百分比
		this.bar.setStringPainted(true);

		// 设置进度条的最大值和最小值,
		this.bar.setMinimum(0);
		// 以总任务量作为进度条的最大值
		this.bar.setMaximum((int) this.amount);
		Timer timer = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// 以任务的当前完成量设置进度条的value
				bar.setValue((int) current);
				realTimeSpeedLabel.setText(realSpeed/1024 + "KB/s");
				if(bar.getPercentComplete() == 1.0) {
					frame.dispose();
				}
			}
		});
		timer.start();

		terminal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.pack();
		this.frame.setVisible(true);

		this.lastTime = System.currentTimeMillis();
	}

	@Override
	public void notify(int blockIndex, String checksum) {

		try {
			HashMap<String, Object> doc = new HashMap<String, Object>();
			doc.put("block", blockIndex);
			doc.put("checksum", checksum);
			String json = JSONObject.valueToString(doc);
			os.println(json);
			System.out.println("Progress Notify:" + "\n\tBlockIndex: "
					+ String.valueOf(blockIndex) + "\n\tChecksum: " + checksum);
		} catch (Exception e) {
			// nothing to do;
		}
	}

	@Override
	public void notify(int blockIndex, BlockProgress progress) {

		try {
			this.realSpeed = Config.PUT_CHUNK_SIZE / (System.currentTimeMillis() - this.lastTime) * 1000;
			if (this.pauseFlag) {
				wait();
			}
			HashMap<String, Object> doc = new HashMap<String, Object>();
			doc.put("block", blockIndex);

			Map<String, String> map = new HashMap<String, String>();
			map.put("context", progress.context);
			map.put("offset", progress.offset + "");
			map.put("restSize", progress.restSize + "");
			doc.put("progress", map);

			String json = JSONObject.valueToString(doc);
			os.println(json);

			this.current = (int) this.current + Config.PUT_CHUNK_SIZE;		
			this.lastTime = System.currentTimeMillis();
		} catch (Exception e) {
			// nothing to do;
		}
	}

}
