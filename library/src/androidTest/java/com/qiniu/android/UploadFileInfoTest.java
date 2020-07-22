package com.qiniu.android;

import com.qiniu.android.http.request.UploadFileInfo;

import java.util.ArrayList;

public class UploadFileInfoTest extends BaseTest {

    public void testCreateFromJsonError(){


        UploadFileInfo fileInfo = UploadFileInfo.fileFromJson(null);

        assertTrue(fileInfo == null);

        assertTrue(fileInfo.progress() == 0);

        assertTrue(fileInfo.blockWithIndex(0) == null);
        assertTrue(fileInfo.nextUploadData() == null);

        assertTrue(fileInfo.isAllUploaded() == true);

        assertTrue(fileInfo.allBlocksContexts() == null);

        fileInfo.clearUploadState();


        UploadFileInfo.UploadBlock block = UploadFileInfo.UploadBlock.blockFromJson(null);

        assertTrue(block == null);

        assertTrue(block.progress() == 0);

        assertTrue(block.uploadDataList == null);

        assertTrue(block.isCompleted() == true);

        assertTrue(fileInfo.allBlocksContexts() == null);


        UploadFileInfo.UploadData data = UploadFileInfo.UploadData.dataFromJson(null);

        assertTrue(data == null);

    }

}
