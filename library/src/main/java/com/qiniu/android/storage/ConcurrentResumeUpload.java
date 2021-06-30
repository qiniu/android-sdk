package com.qiniu.android.storage;

import com.qiniu.android.utils.GroupTaskThread;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

class ConcurrentResumeUpload extends PartsUpload {

    private GroupTaskThread groupTaskThread;

    protected ConcurrentResumeUpload(UploadSource source,
                                     String key,
                                     UpToken token,
                                     UploadOptions option,
                                     Configuration config,
                                     Recorder recorder,
                                     String recorderKey,
                                     UpTaskCompletionHandler completionHandler) {
        super(source, key, token, option, config, recorder, recorderKey, completionHandler);
    }

    @Override
    protected int prepareToUpload() {
        return super.prepareToUpload();
    }

    @Override
    protected void uploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
        LogUtil.i("key:" + StringUtils.toNonnullString(key));

        GroupTaskThread.GroupTaskCompleteHandler taskCompleteHandler = new GroupTaskThread.GroupTaskCompleteHandler() {
            @Override
            public void complete() {
                completeHandler.complete();
            }
        };

        groupTaskThread = new GroupTaskThread(taskCompleteHandler);
        for (int i = 0; i < config.concurrentTaskCount; i++) {
            groupTaskThread.addTask(new GroupTaskThread.GroupTask() {
                @Override
                public void run(final GroupTaskThread.GroupTask task) {
                    performUploadRestData(new UploadFileRestDataCompleteHandler() {
                        @Override
                        public void complete() {
                            task.taskComplete();
                        }
                    });
                }
            });
        }

        groupTaskThread.start();
    }
}
