package com.qiniu.qbox.up;

/**
 * Notifies the progress of a block upload.
 */
public interface BlockProgressNotifier {
	void notify(int blockIndex, BlockProgress progress);
}
