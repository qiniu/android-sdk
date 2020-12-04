package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.GroupTaskThread;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

class ConcurrentResumeUpload extends PartsUpload {

    private GroupTaskThread groupTaskThread;

    protected ConcurrentResumeUpload(File file,
                                     String key,
                                     UpToken token,
                                     UploadOptions option,
                                     Configuration config,
                                     Recorder recorder,
                                     String recorderKey,
                                     UpTaskCompletionHandler completionHandler) {
        super(file, key, token, option, config, recorder, recorderKey, completionHandler);
    }

    @Override
    protected int prepareToUpload() {
        return super.prepareToUpload();
    }

    @Override
    protected void uploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
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
