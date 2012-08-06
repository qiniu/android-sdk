package com.qiniu.qbox.up;

public interface ProgressNotifier {
	/**
	 * Notifies the completion of a block
	 * 
	 * @param blockIndex The index of this block.
	 * @param checksum The checksum of this block.
	 */
	void notify(int blockIndex, String checksum);
}
