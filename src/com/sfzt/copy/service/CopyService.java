package com.sfzt.copy.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sfzt.copy.FileInfo;
import com.sfzt.copy.FileInfoManager;
import com.sfzt.copy.service.FileManagerService.OperationEventListener;
import com.sfzt.copy.service.MultiMediaStoreHelper.PasteMediaStoreHelper;
import com.sfzt.copy.utils.LogUtils;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class CopyService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public void copyDrv(String from , String dist){
		
	}
	
	private void runInFg() {
		Notification.Builder builder = new Notification.Builder(this);
		startForeground(1, builder.build());
	}
	
	static class CopyPasteFilesTask extends FileOperationTask {
        private static final String TAG = "CopyPasteFilesTask";
        List<FileInfo> mSrcList = null;
        String mDstFolder = null;

        public CopyPasteFilesTask(FileInfoManager fileInfoManager,
                OperationEventListener operationEvent, Context context, List<FileInfo> src,
                String destFolder) {
            super(fileInfoManager, operationEvent, context);
            mSrcList = src;
            mDstFolder = destFolder;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            synchronized (mContext.getApplicationContext()) {
                LogUtils.i(TAG, "doInBackground...");
                List<File> fileList = new ArrayList<File>();
                UpdateInfo updateInfo = new UpdateInfo();
                int ret = getAllFileList(mSrcList, fileList, updateInfo);
                if (ret < 0) {
                    LogUtils.i(TAG, "doInBackground,ret = " + ret);
                    return ret;
                }

                PasteMediaStoreHelper pasteMediaStoreHelper = new PasteMediaStoreHelper(
                        mMediaProviderHelper);

                // Set dstFolder so we can scan folder instead of scanning each file one by one.
                pasteMediaStoreHelper.setDstFolder(mDstFolder);

                HashMap<File, FileInfo> copyFileInfoMap = new HashMap<File, FileInfo>();
                for (FileInfo fileInfo : mSrcList) {
                    copyFileInfoMap.put(fileInfo.getFile(), fileInfo);
                }

//                if(isGreaterThan4G(updateInfo) && isFat32Disk(mDstFolder)) {
//                	LogUtils.i(TAG, "doInBackground, destination is FAT32.");
//                	return OperationEventListener.ERROR_CODE_COPY_GREATER_4G_TO_FAT32;
//                }
                
                if (!isEnoughSpace(updateInfo, mDstFolder)) {
                    LogUtils.i(TAG, "doInBackground, not enough space.");
                    return OperationEventListener.ERROR_CODE_NOT_ENOUGH_SPACE;
                }

                publishProgress(new ProgressInfo("", 0, TOTAL, 0, updateInfo.getTotalNumber()));

                byte[] buffer = new byte[BUFFER_SIZE];
                HashMap<String, String> pathMap = new HashMap<String, String>();
                if (!fileList.isEmpty()) {
                    pathMap.put(fileList.get(0).getParent(), mDstFolder);
                }
                for (File file : fileList) {
                    File dstFile = getDstFile(pathMap, file, mDstFolder);
                    if (isCancelled()) {
                        pasteMediaStoreHelper.updateRecords();
                        LogUtils.i(TAG, "doInBackground,user cancel.");
                        return OperationEventListener.ERROR_CODE_USER_CANCEL;
                    }
                    if (dstFile == null) {
                        publishProgress(new ProgressInfo(
                                OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS, true));
                        continue;
                    }
                    if (file.isDirectory()) {
                        if (mkdir(pathMap, file, dstFile)) {
                            pasteMediaStoreHelper.addRecord(dstFile.getAbsolutePath());
                            addItem(copyFileInfoMap, file, dstFile);
                            updateInfo.updateProgress(file.length());
                            updateInfo.updateCurrentNumber(1);
                            updateProgressWithTime(updateInfo, file);
                        }
                    } else {
                        if (FileInfo.isDrmFile(file.getName()) || !file.canRead()) {
                            publishProgress(new ProgressInfo(
                                    OperationEventListener.ERROR_CODE_COPY_NO_PERMISSION, true));
                            updateInfo.updateProgress(file.length());
                            updateInfo.updateCurrentNumber(1);
                            continue;
                        }
                        updateInfo.updateCurrentNumber(1);
                        ret = copyFile(buffer, file, dstFile, updateInfo);
                        if (ret == OperationEventListener.ERROR_CODE_USER_CANCEL) {
                            pasteMediaStoreHelper.updateRecords();
                            return ret;
                        } else if (ret < 0) {
                            publishProgress(new ProgressInfo(ret, true));
                            updateInfo.updateProgress(file.length());
                            updateInfo.updateCurrentNumber(1);
                        } else {
                            pasteMediaStoreHelper.addRecord(dstFile.getAbsolutePath());
                            addItem(copyFileInfoMap, file, dstFile);
                        }
                    }
                }
                pasteMediaStoreHelper.updateRecords();
                LogUtils.i(TAG, "doInBackground,return success.");
                return OperationEventListener.ERROR_CODE_SUCCESS;
            }
        }
    }
	
}
