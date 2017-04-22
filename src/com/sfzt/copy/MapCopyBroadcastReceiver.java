package com.sfzt.copy;

import java.io.File;

import cn.wl.utils.SDCardUtils;

import com.sfzt.copy.utils.Contants;
import com.sfzt.copy.utils.LogUtils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class MapCopyBroadcastReceiver extends BroadcastReceiver {
	
	private final static String TAG = "MapCopyBroadcastReceiver";
	private final static int RESULT_HAVE_BACKCOPY = 0;
	private final static int RESULT_HAVE_MAPINFO = 1;
	private final static int RESULT_NOT_HAVE = 2;
	private ListSDFileTask mListSDFileTask;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			// 开机检测，是否有插入TF卡
			if(SDCardUtils.checkFsWritable(Contants.SD_CARD_PATH)){
				mListSDFileTask = new ListSDFileTask(context);
				mListSDFileTask.execute(Contants.SD_CARD_PATH);
			}
		}else if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
			Log.i(TAG,"path: "+intent.getData().getPath());
			if(isSDPath(intent.getData().getPath())){
				if(mListSDFileTask != null){
					mListSDFileTask.cancel(true);
				}
				mListSDFileTask = new ListSDFileTask(context);
				mListSDFileTask.execute(Contants.SD_CARD_PATH);
			}
		}else if(intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){
			if(isSDPath(intent.getData().getPath())){
				if(mListSDFileTask != null){
					mListSDFileTask.cancel(true);
					mListSDFileTask = null;
				}
			}
		}
	}
	
	private boolean isSDPath(String path){
		if(path != null && path.equals(Contants.SD_CARD_PATH)){
			return true;
		}
		return false;
	}
	
	class ListSDFileTask extends AsyncTask<String,Integer,Integer> {
		
		private Context mContext;
		
		public ListSDFileTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			switch (result) {
			case RESULT_HAVE_BACKCOPY:
//				Intent _i = new Intent();
//				_i.setComponent(new ComponentName("com.sfzt.copy", "com.sfzt.copy.FileManagerOperationActivity"));
//				_i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				mContext.startActivity(_i);
			case RESULT_HAVE_MAPINFO:
				Intent _i = new Intent();
				_i.setComponent(new ComponentName("com.sfzt.copy", "com.sfzt.copy.FileManagerOperationActivity"));
				_i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(_i);
				break;
			case RESULT_NOT_HAVE:
				
				break;
			default:
				break;
			}
		}

		@Override
		protected Integer doInBackground(String... arg0) {
			String _path = arg0[0];
			File[] files = null;
			File dir = new File(_path);
			files = dir.listFiles();
			if (files == null) {
				return RESULT_NOT_HAVE;
	        }
			for (int i = 0; i < files.length; i++) {
                if (isCancelled()) {
                    LogUtils.w(TAG, " doInBackground,calcel.");
                    return RESULT_NOT_HAVE;
                }
                
                if(files[i].getName().equals(Contants.IX_NAVI_DIR)){
                	return RESULT_HAVE_BACKCOPY;
                }
                
                if(files[i].getName().equals(Contants.BAIDU_MAP_DIR) || files[i].getName().equals(Contants.GAODE_MAP_DIR)
                		|| files[i].getName().equals(Contants.KLD_MAP_DIR)){
                	return RESULT_HAVE_MAPINFO;
                }
                
            }
			return RESULT_NOT_HAVE;
		}
	}
}
