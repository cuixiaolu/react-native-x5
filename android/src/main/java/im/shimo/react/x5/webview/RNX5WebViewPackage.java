package im.shimo.react.x5.webview;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.ViewManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.export.external.interfaces.JsResult;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.annotation.TargetApi;
import android.content.ClipData;

public class RNX5WebViewPackage implements ReactPackage {

    private X5WebViewModule module;

    private RNX5WebViewManager viewManager;

    public class X5WebViewModule extends ReactContextBaseJavaModule implements ActivityEventListener {

        private RNX5WebViewPackage aPackage;

        private ValueCallback<Uri> mUploadMessage = null;
        private ValueCallback<Uri[]> mUploadMessageArr = null;

        /* FOR UPLOAD DIALOG */
        private final static int REQUEST_SELECT_FILE = 1001;
        private final static int REQUEST_SELECT_FILE_LEGACY = 1002;

        private ReactApplicationContext mReactContext;

        public X5WebViewModule(ReactApplicationContext reactContext) {
            super(reactContext);
            reactContext.addActivityEventListener(this);
            mReactContext = reactContext;

        }

        @Override
        public String getName() {
            return "X5WebView";
        }

        @ReactMethod
        public void getX5CoreVersion(Callback callback) {
            callback.invoke(WebView.getTbsCoreVersion(mReactContext));
        }

        public void setPackage(RNX5WebViewPackage aPackage) {
            this.aPackage = aPackage;
        }

        public RNX5WebViewPackage getPackage() {
            return this.aPackage;
        }

        @SuppressWarnings("unused")
        public Activity getActivity() {
            return getCurrentActivity();
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public boolean startFileChooserIntent(ValueCallback<Uri> uploadMsg, String acceptType) {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;

            }

            mUploadMessage = uploadMsg;

            if (acceptType == null || acceptType.isEmpty()) {
                acceptType = "*/*";
            }

            Intent intentChoose = new Intent(Intent.ACTION_GET_CONTENT);
            intentChoose.addCategory(Intent.CATEGORY_OPENABLE);
            intentChoose.setType(acceptType);

            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                return false;
            }

            try {
                currentActivity.startActivityForResult(intentChoose, REQUEST_SELECT_FILE_LEGACY, new Bundle());
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();

                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }
                return false;
            }

            return true;
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean startFileChooserIntent(ValueCallback<Uri[]> filePathCallback, Intent intentChoose) {
            if (mUploadMessageArr != null) {
                mUploadMessageArr.onReceiveValue(null);
                mUploadMessageArr = null;
            }
            System.out.print("android 5.0");
            Log.d("android X5","current version 5.0+");
            mUploadMessageArr = filePathCallback;

            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                return false;
            }

            try {
                currentActivity.startActivityForResult(intentChoose, REQUEST_SELECT_FILE, new Bundle());
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();

                if (mUploadMessageArr != null) {
                    mUploadMessageArr.onReceiveValue(null);
                    mUploadMessageArr = null;
                }
                return false;
            }

            return true;
        }

        @SuppressLint({ "NewApi", "Deprecated" })
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_SELECT_FILE_LEGACY) {
                if (mUploadMessage == null)
                    return;

                Uri result = ((data == null || resultCode != Activity.RESULT_OK) ? null : data.getData());

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            } else if (requestCode == REQUEST_SELECT_FILE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mUploadMessageArr == null)
                    return;
                // onActivityResultAboveL(requestCode,resultCode,data);
                mUploadMessageArr.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                mUploadMessageArr = null;
            }
        }

        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            this.onActivityResult(requestCode, resultCode, data);
        }

        //这里intent.getClipData()方法需要在api16以上才能使用这个
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
            if (requestCode != REQUEST_SELECT_FILE || mUploadMessageArr == null)
                return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    ClipData clipData = intent.getClipData();
                    if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    if (dataString != null)
                        results = new Uri[] { Uri.parse(dataString) };
                }
            }
            mUploadMessageArr.onReceiveValue(results);
            mUploadMessageArr = null;
        }

        public void onNewIntent(Intent intent) {
        }

    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        module = new X5WebViewModule(reactContext);
        module.setPackage(this);
        List<NativeModule> modules = new ArrayList<>();
        modules.add(module);
        return modules;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        viewManager = new RNX5WebViewManager(reactContext);
        viewManager.setPackage(this);
        List<ViewManager> modules = new ArrayList<>();
        modules.add(viewManager);
        return modules;
    }

    public X5WebViewModule getModule() {
        return module;
    }

    public RNX5WebViewManager getViewManager() {
        return viewManager;
    }
}
