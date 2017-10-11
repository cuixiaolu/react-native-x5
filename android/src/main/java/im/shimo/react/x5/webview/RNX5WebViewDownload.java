package im.shimo.react.x5.webview;

import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.DownloadListener;


import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import android.app.DownloadManager;
import android.os.Build;
import android.os.Environment;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;

import java.util.List;
/**
 * 下载
 */
public class RNX5WebViewDownload implements DownloadListener {
    ReactContext reactContext;

    public RNX5WebViewDownload(ReactContext reactContext) {
         this.reactContext = reactContext;
     }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                long contentLength) {
        try {

            if (!isDownloadManagerAvailable(reactContext)) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                reactContext.startActivity(intent);
            } else {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                Uri uri = Uri.parse(url);
                String[] path = uri.getPath().split("/");
                String  fileName = "";
                if(path.length>1){
                    fileName = path[path.length - 1];
                }
                request.setTitle(fileName);
                request.setDescription("下载完成后，点击开始安装。");
// in order for this if to run, you must use the android 3.2 to compile your app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                }
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
// get download service and enqueue file
                DownloadManager manager = (DownloadManager) reactContext.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);

               // 开始下载了，就发送一个 事件通知到 React Native 做相关处理，记得加上监听
               reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("NativeCustomEvent", "startDownload");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否支持 Download Manager
     * @param context used to check the device version and DownloadManager information
     * @return true if the download manager is available
     */
    public static boolean isDownloadManagerAvailable(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
