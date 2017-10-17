package im.shimo.react.x5.webview;

import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;

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
import android.webkit.URLUtil;
import android.util.Base64;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

            Uri uri = Uri.parse(url);
            if (!isDownloadManagerAvailable(reactContext))
            // if (1==1)
            {
                System.out.print(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                reactContext.startActivity(intent);
            } else {
                DownloadManager.Request request = new DownloadManager.Request(uri);

                System.out.print(url);
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);

                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                try{
                    Pattern pattern = Pattern.compile("\\?utf-8\\?B\\?(.*)?\\?=");
                    Matcher matcher = pattern.matcher(contentDisposition);

                    while(matcher.find()){
                        fileName =new String(Base64.decode(matcher.group(1).getBytes(),Base64.DEFAULT),"utf-8");
                    }
                }catch(Exception e){
                }
                System.out.print(fileName);

                request.setTitle(fileName);
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
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("NativeCustomEvent",
                        "startDownload");
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
            intent.setClassName("com.android.providers.downloads.ui",
                    "com.android.providers.downloads.ui.DownloadList");
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
