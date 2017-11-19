package cn.edu.gdmec.android.mobileguard.m1home.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.gdmec.android.mobileguard.R;
import cn.edu.gdmec.android.mobileguard.m1home.HomeActivity;
import cn.edu.gdmec.android.mobileguard.m1home.entity.VersionEntity;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by Administrator on 2017/9/17.
 */
//获取版本号  对比版本号  下载更新
public class VersionUpdateUtils {
    private static final int MESSAGE_IO_ERROR = 102;//网络错误代号
    private static final int MESSAGE_JSON_ERROR = 103;//JSON错误代号
    private static final int MESSAGE_SHOW_ERROR = 104;//SHOW错误代号
    private static final int MESSAGE_ENTERHOME = 105;//HOME错误代号
    private String mVersion;
    private Activity context;
    private VersionEntity versionEntity;
    private ProgressDialog mProgressDialog;
    private Class<?> nextActivty;
    private DownloadCallback downloadCallback;
    private long downloadId;
    private BroadcastReceiver broadcastReceiver;
    //handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_IO_ERROR:
                    Toast.makeText(context, "IO错误", Toast.LENGTH_LONG).show();
                    //测试用 网络错误也进入主界面
                    Intent intent1 = new Intent(context, HomeActivity.class);
                    context.startActivity(intent1);
                    context.finish();

                    break;
                case MESSAGE_JSON_ERROR:
                    Toast.makeText(context, "JSON解析错误", Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_SHOW_ERROR:
                    showUpdateDialog(versionEntity);
                    break;
                case MESSAGE_ENTERHOME:
                    Intent intent = new Intent(context, HomeActivity.class);
                    context.startActivity(intent);
                    context.finish();
                    break;

            }
        }
    };

    /**
     *
     * @param mVersion
     * @param context
     */
    public VersionUpdateUtils(String mVersion, Activity context,DownloadCallback downloadCallback,Class<?> nextActivty) {
        this.mVersion = mVersion;
        this.context = context;
        this.downloadCallback = downloadCallback;
        this.nextActivty = nextActivty;
    }

    public void getCloudVersion(String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            //设置超时
            HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 5000);
            HttpConnectionParams.setSoTimeout(httpClient.getParams(), 5000);
//            请求链接
            //HttpGet httpGet = new HttpGet("http://android2017.duapp.com/updateinfo.html");
            HttpGet httpGet = new HttpGet(url);
//            执行
            HttpResponse execute = httpClient.execute(httpGet);
//            比对返回码 200 为成功
            if (execute.getStatusLine().getStatusCode() == 200) {
//                获取服务器返回的内容并处理
                HttpEntity httpEntity = execute.getEntity();
                String result = EntityUtils.toString(httpEntity, "utf-8");
                JSONObject jsonObject = new JSONObject(result);
                versionEntity = new VersionEntity();
                versionEntity.versioncode = jsonObject.getString("code");
                versionEntity.description = jsonObject.getString("des");
                versionEntity.apkurl = jsonObject.getString("apkurl");
                //Log.d("Tag", "getCloudVersion 本地版本为: " + mVersion);
                if (!mVersion.equals(versionEntity.versioncode)) {
                    //版本不同 需升级
//                    Toast.makeText(context, versionEntity.description, Toast.LENGTH_SHORT).show();
                    handler.sendEmptyMessage(MESSAGE_SHOW_ERROR);
                }
            }
        } catch (IOException e) {
            handler.sendEmptyMessage(MESSAGE_IO_ERROR);
            e.printStackTrace();
        } catch (JSONException e) {
            handler.sendEmptyMessage(MESSAGE_JSON_ERROR);
            e.printStackTrace();
        }


    }

    /**
     * 选择是否升级的对话框
     * @param versionEntity 网络版本号
     */
    private void showUpdateDialog(final VersionEntity versionEntity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("检查到有新版本：" + versionEntity.versioncode);
        builder.setMessage(versionEntity.description);
        builder.setCancelable(false);//设置不能被忽视
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setPositiveButton("立即升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               /* Log.d("Tag", "getCloudVersion 网络版本为: " + versionEntity.versioncode);
                DownloadUtils downloadUtils = new DownloadUtils();
                downloadUtils.downloadApk(versionEntity.apkurl, "mobileguard.apk", context);
                Log.d("Tag", "下载成功");*/
                downloadNewApk(versionEntity.apkurl);
            }
        });
        builder.setNegativeButton("暂不升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
//                enterHome();
            }
        });
        builder.show();
    }

    /**
     * 通过handler的message进入主界面
     */
    private void enterHome() {
        handler.sendEmptyMessage(MESSAGE_ENTERHOME);
    }
    private void downloadNewApk(String apkurl){
        DownloadUtils downloadUtils = new DownloadUtils();
        //downloadUtils.downloadApk(apkurl,"mobileguard.apk",context);
        String filename = "downloadfile";
        String suffixes="avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|pdf|rar|zip|docx|doc|apk|db";
        Pattern pat= Pattern.compile("[\\w]+[\\.]("+suffixes+")");//正则判断
        Matcher mc=pat.matcher(apkurl);//条件匹配
        while(mc.find()){
            filename = mc.group();//截取文件名后缀名
        }
        downapk(apkurl, filename, context);

    }
    public void downapk(String url,String targetFile,Context context){
        //创建下载任务
        DownloadManager.Request request = new DownloadManager.Request( Uri.parse(url));
        request.setAllowedOverRoaming(false);//漫游网络是否可以下载

        //设置文件类型，可以在下载结束后自动打开该文件
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
        request.setMimeType(mimeString);

        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(true);

        //sdcard的目录下的download文件夹，必须设置
        request.setDestinationInExternalPublicDir("/download/", targetFile);
        //request.setDestinationInExternalFilesDir(),也可以自己制定下载路径

        //将下载请求加入下载队列
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        //加入下载队列后会给该任务返回一个long型的id，
        //通过该id可以取消任务，重启任务等等，看上面源码中框起来的方法
        downloadId = downloadManager.enqueue(request);
        listener(downloadId,targetFile);

    }
    private void listener(final long Id,final String filename) {
        // 注册广播监听系统的下载完成事件。
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (ID == Id) {
                    //Toast.makeText(context.getApplicationContext(), "任务:" + Id + " 下载完成!", Toast.LENGTH_LONG).show();
                    Toast.makeText(context.getApplicationContext(), "下载编号:" + Id +"的"+filename+" 下载完成!", Toast.LENGTH_LONG).show();
                }
                context.unregisterReceiver(broadcastReceiver);
                downloadCallback.afterDownload(filename);
            }
        };
        context.registerReceiver(broadcastReceiver, intentFilter);

    }
    public interface DownloadCallback{
        void afterDownload(String filename);
    }
}
