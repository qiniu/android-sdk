import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.ProgressNotifier;

public class Notifier implements ProgressNotifier, BlockProgressNotifier {

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
