package com.ly.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import androidx.annotation.NonNull;
import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;


public class WifiPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {
    String TAG = "WifiPlugin";
    private Registrar registrar;
    private WifiDelegate delegate;

    private Activity activity;

    private MethodChannel channel;
    private Context context;

    public WifiPlugin() {

    }

    private WifiPlugin(Registrar registrar, WifiDelegate delegate) {
        this.registrar = registrar;
        this.delegate = delegate;
    }


    public static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            ContextWrapper wrapper = (ContextWrapper) context;
            return findActivity(wrapper.getBaseContext());
        } else {
            return null;
        }
    }

//    public static void registerWith(Registrar registrar) {
//        final MethodChannel channel = new MethodChannel(registrar.messenger(), "plugins.ly.com/wifi");
//        WifiManager wifiManager = (WifiManager) registrar.activeContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        final WifiDelegate delegate = new WifiDelegate(registrar.activity(), wifiManager);
//        registrar.addRequestPermissionsResultListener(delegate);
//
//        // support Android O,listen network disconnect event
//        // https://stackoverflow.com/questions/50462987/android-o-wifimanager-enablenetwork-cannot-work
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        registrar
//                .context()
//                .registerReceiver(delegate.networkReceiver, filter);
//
//        channel.setMethodCallHandler(new WifiPlugin(registrar, delegate));
//    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "onAttachedToEngine");

        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "plugins.ly.com/wifi");
        context = flutterPluginBinding.getApplicationContext();


        // support Android O,listen network disconnect event
        // https://stackoverflow.com/questions/50462987/android-o-wifimanager-enablenetwork-cannot-work

        channel.setMethodCallHandler(this);
//        channel.setMethodCallHandler(new WifiPlugin(null, delegate));
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
//        if (registrar.activity() == null) {
//            result.error("no_activity", "wifi plugin requires a foreground activity.", null);
//            return;
//        }
        switch (call.method) {
            case "ssid":
                delegate.getSSID(call, result);
                break;
            case "moran":
                delegate.getMoran(call, result);
                break;
            case "level":
                delegate.getLevel(call, result);
                break;
            case "ip":
                delegate.getIP(call, result);
                break;
            case "list":
                delegate.getWifiList(call, result);
                break;
            case "connection":
                delegate.connection(call, result);
                break;
            case "isEnable":
                delegate.isEnable(call, result);
                break;
            case "enableWifi":
                delegate.enableWifi(call, result);
                break;


            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        Log.d(TAG, "onAttachedToActivity");


        this.activity = binding.getActivity();
        if (context == null) {
            context = binding.getActivity().getApplicationContext();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);


        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        delegate = new WifiDelegate(activity, wifiManager);

        context.registerReceiver(delegate.networkReceiver, filter);
        binding.addRequestPermissionsResultListener(delegate);

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

        Log.d(TAG, "onReattachedToActivityForConfigChanges");

    }

    @Override
    public void onDetachedFromActivity() {

    }

}
