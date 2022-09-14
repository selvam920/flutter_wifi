package com.ly.wifi;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import android.util.Log;


public class
WifiDelegate implements PluginRegistry.RequestPermissionsResultListener {
    private static final String TAG = WifiDelegate.class.getSimpleName();
    private Activity activity;
    private WifiManager wifiManager;
    private PermissionManager permissionManager;
    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHANGE_WIFI_STATE_PERMISSION = 2;
    NetworkChangeReceiver networkReceiver;

    private WifiAutoConnectManager wifiAuto; // 这个连接成功概率更大

    interface PermissionManager {
        boolean isPermissionGranted(String permissionName);

        void askForPermission(String permissionName, int requestCode);
    }

    public WifiDelegate(final Activity activity, final WifiManager wifiManager) {
        this(activity, wifiManager, null, null, new PermissionManager() {

            @Override
            public boolean isPermissionGranted(String permissionName) {
                return ActivityCompat.checkSelfPermission(activity, permissionName) == PackageManager.PERMISSION_GRANTED;
            }

            @Override
            public void askForPermission(String permissionName, int requestCode) {
                ActivityCompat.requestPermissions(activity, new String[]{permissionName}, requestCode);
            }
        });
    }

    private MethodChannel.Result result;
    private MethodCall methodCall;

    WifiDelegate(
            Activity activity,
            WifiManager wifiManager,
            MethodChannel.Result result,
            MethodCall methodCall,
            PermissionManager permissionManager) {
        this.networkReceiver = new NetworkChangeReceiver();
        this.activity = activity;
        this.wifiManager = wifiManager;
        this.wifiAuto = new WifiAutoConnectManager(wifiManager);
        this.result = result;
        this.methodCall = methodCall;
        this.permissionManager = permissionManager;
    }

    public void getSSID(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        launchSSID();
    }

    public void getMoran(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        launchMoran();
    }

    public void isEnable(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        launchIsEnable();
    }

    public void enableWifi(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }

        boolean flag = methodCall.argument("flag");

        launchEnableWifi(flag);
    }


    public void getLevel(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        launchLevel();
    }

    private void launchSSID() {
        String wifiName = wifiManager != null ? wifiManager.getConnectionInfo().getSSID().replace("\"", "") : "";
        if (!wifiName.isEmpty()) {
            result.success(wifiName);
            clearMethodCallAndResult();
        } else {
            finishWithError("unavailable", "wifi name not available.");
        }
    }

    private void launchMoran() {
        result.success("moran");
        clearMethodCallAndResult();
    }

    private void launchIsEnable() {
        boolean ret = wifiManager.isWifiEnabled();
        result.success(ret);
        clearMethodCallAndResult();
    }

    private void launchEnableWifi(boolean flag) {
        if (flag && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if (!flag && wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }

        boolean ret = wifiManager.isWifiEnabled();
        result.success(ret);
        clearMethodCallAndResult();
    }

    private void launchLevel() {
        int level = wifiManager != null ? wifiManager.getConnectionInfo().getRssi() : 0;
        if (level != 0) {
            if (level <= 0 && level >= -55) {
                result.success(3);
            } else if (level < -55 && level >= -80) {
                result.success(2);
            } else if (level < -80 && level >= -100) {
                result.success(1);
            } else {
                result.success(0);
            }
            clearMethodCallAndResult();
        } else {
            finishWithError("unavailable", "wifi level not available.");
        }
    }

    public void getIP(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        launchIP();
    }


    private void launchIP() {
        NetworkInfo info = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                result.success(inetAddress.getHostAddress());
                                clearMethodCallAndResult();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
                result.success(ipAddress);
                clearMethodCallAndResult();
            }
        } else {
            finishWithError("unavailable", "ip not available.");
        }
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    public void getWifiList(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();
            return;
        }
        if (!permissionManager.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionManager.askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION_PERMISSION);
            return;
        }
        launchWifiList();
    }

    private void launchWifiList() {
        String key = methodCall.argument("key");
        List<HashMap> list = new ArrayList<>();
        if (wifiManager != null) {
            List<ScanResult> scanResultList = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResultList) {
                int level;
                if (scanResult.level <= 0 && scanResult.level >= -55) {
                    level = 3;
                } else if (scanResult.level < -55 && scanResult.level >= -80) {
                    level = 2;
                } else if (scanResult.level < -80 && scanResult.level >= -100) {
                    level = 1;
                } else {
                    level = 0;
                }
                HashMap<String, Object> maps = new HashMap<>();
                if (key.isEmpty()) {
                    maps.put("ssid", scanResult.SSID);
                    maps.put("level", level);
                    list.add(maps);
                } else {
                    if (scanResult.SSID.contains(key)) {
                        maps.put("ssid", scanResult.SSID);
                        maps.put("level", level);
                        list.add(maps);
                    }
                }
            }
        }
        result.success(list);
        clearMethodCallAndResult();
    }

    public void connection(MethodCall methodCall, MethodChannel.Result result) {

        Log.d(TAG, "methodCall connect use delegate");

        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError();

            Log.d(TAG, "methodCall connect use delegate: setPendingMethodCallAndResult");
            return;
        }

        if (!permissionManager.isPermissionGranted(Manifest.permission.CHANGE_WIFI_STATE)) {
            permissionManager.askForPermission(Manifest.permission.CHANGE_WIFI_STATE, REQUEST_ACCESS_FINE_LOCATION_PERMISSION);
            Log.d(TAG, "methodCall connect use delegate: permissionManager");
            return;
        }
//        connection();
        connection2();
    }


    public void connection2() {
        Log.d(TAG, "call connect use delegate");

        String ssid = methodCall.argument("ssid");
        String password = methodCall.argument("password");

        Log.d(TAG, "start call connect2 use delegate params: " + String.format("%s =>%s", ssid, password));
        wifiAuto.connect(ssid, password, WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);
    }

        public void connection () {
            Log.d(TAG, "call connect use delegate");

            String ssid = methodCall.argument("ssid");
            String password = methodCall.argument("password");


            Log.d(TAG, "call connect use delegate params: " + String.format("%s =>%s", ssid, password));
//        WifiConfiguration wifiConfig = createWifiConfig(ssid, password);
            WifiConfiguration wifiConfig = CreateWifiInfo(ssid, password, 3);
            if (wifiConfig == null) {
                finishWithError("unavailable", "wifi config is null!");
                Log.d(TAG, "wifi config is null!");
                return;
            }
            int netId = wifiManager.addNetwork(wifiConfig);
            Log.d(TAG, "net id is: " + String.format("%d!", netId));
            if (netId == -1) {// 可能是曾过来的wifi
                Log.d(TAG, "net id is -1!");

                connectWifi(ssid);

                result.success(0);
                clearMethodCallAndResult();
            } else {
                // support Android O
                // https://stackoverflow.com/questions/50462987/android-o-wifimanager-enablenetwork-cannot-work
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                    Log.d(TAG, "Build.VERSION.SDK_INT < Build.VERSION_CODES.O");
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();

                    result.success(1);
                    clearMethodCallAndResult();
                } else {
                    networkReceiver.connect(netId);
                }
            }
        }

        private WifiConfiguration createWifiConfig (String ssid, String Password){
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssid + "\"";
            config.allowedAuthAlgorithms.clear();
            config.allowedGroupCiphers.clear();
            config.allowedKeyManagement.clear();
            config.allowedPairwiseCiphers.clear();
            config.allowedProtocols.clear();
            WifiConfiguration tempConfig = isExist(wifiManager, ssid);
            if (tempConfig != null) {
                wifiManager.removeNetwork(tempConfig.networkId);
            }
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
            return config;
        }

        private WifiConfiguration isExist (WifiManager wifiManager, String ssid){
            List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
            if (existingConfigs != null) {
                for (WifiConfiguration existingConfig : existingConfigs) {
                    if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                        return existingConfig;
                    }
                }
            }
            return null;
        }

        private boolean setPendingMethodCallAndResult (MethodCall methodCall, MethodChannel.Result
        result){
            if (this.result != null) {
                return false;
            }
            this.methodCall = methodCall;
            this.result = result;
            return true;
        }

        @Override
        public boolean onRequestPermissionsResult ( int requestCode, String[] permissions,
        int[] grantResults){
            boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            switch (requestCode) {
                case REQUEST_ACCESS_FINE_LOCATION_PERMISSION:
                    if (permissionGranted) {
                        launchWifiList();
                    }
                    break;
                case REQUEST_CHANGE_WIFI_STATE_PERMISSION:
                    if (permissionGranted) {
                        connection();
                    }
                    break;
                default:
                    return false;
            }
            if (!permissionGranted) {
                clearMethodCallAndResult();
            }
            return true;
        }

        private void finishWithAlreadyActiveError () {
            finishWithError("already_active", "wifi is already active");
        }

        private void finishWithError (String errorCode, String errorMessage){
            result.error(errorCode, errorMessage, null);
            clearMethodCallAndResult();
        }

        private void clearMethodCallAndResult () {
            methodCall = null;
            result = null;
        }

        // support Android O
        // https://stackoverflow.com/questions/50462987/android-o-wifimanager-enablenetwork-cannot-work
        public class NetworkChangeReceiver extends BroadcastReceiver {
            private int netId;
            private boolean willLink = false;

            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                Log.d(TAG, "onReceive status" + String.format("%s", info.getState().toString()));

                if (info.getState() == NetworkInfo.State.DISCONNECTED && willLink) {
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                    result.success(1);
                    willLink = false;
                    clearMethodCallAndResult();
                }
            }

            public void connect(int netId) {
                this.netId = netId;
                willLink = true;
                wifiManager.disconnect();
            }

            public void tunonWifi() {
                if (!wifiManager.isWifiEnabled())
                    wifiManager.setWifiEnabled(true);
            }

            public void tunoffWifi() {
                if (wifiManager.isWifiEnabled())
                    wifiManager.setWifiEnabled(false);
            }
        }

        public WifiConfiguration CreateWifiInfo (String SSID, String Password,
        int Type){
            WifiConfiguration config = new WifiConfiguration();
            config.allowedAuthAlgorithms.clear();
            config.allowedGroupCiphers.clear();
            config.allowedKeyManagement.clear();
            config.allowedPairwiseCiphers.clear();
            config.allowedProtocols.clear();
            config.SSID = "\"" + SSID + "\"";

            WifiConfiguration tempConfig = this.IsExsits(SSID);
            if (tempConfig != null) {
                wifiManager.removeNetwork(tempConfig.networkId);
            }

            if (Type == 1) // WIFICIPHER_NOPASS  == open free
            {
                config.wepKeys[0] = "";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
            }
            if (Type == 2) // WIFICIPHER_WEP
            {
                // WEP Security
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (getHexKey(Password)) config.wepKeys[0] = Password;
                else config.wepKeys[0] = "\"" + Password + "\"";
                config.wepTxKeyIndex = 0;

            }
            if (Type == 3) // WIFICIPHER_WPA
            {
                config.preSharedKey = "\"" + Password + "\"";
                config.hiddenSSID = true;
                config.allowedAuthAlgorithms
                        .set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers
                        .set(WifiConfiguration.PairwiseCipher.TKIP);
                // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers
                        .set(WifiConfiguration.PairwiseCipher.CCMP);
                config.status = WifiConfiguration.Status.ENABLED;
            }
            return config;
        }

//    连接曾连过的wifi
        public void connectWifi (String ssid){
            WifiConfiguration configuration = getWifiConfig(ssid);
            if (configuration != null) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(configuration.networkId, true);
            }

        }
        public WifiConfiguration getWifiConfig (String ssid){

            if (ssid != null && ssid != "") {
                return null;
            }
            String newSSID;
            if (!(ssid.startsWith("\"") && ssid.endsWith("\""))) {
                newSSID = "\"" + ssid + "\"";
            } else {
                newSSID = ssid;
            }
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

            for (WifiConfiguration configuration : configuredNetworks) {
                if (newSSID.equalsIgnoreCase(configuration.SSID)) {
                    return configuration;
                }
            }

            return null;
        }
        /**
         * WEP has two kinds of password, a hex value that specifies the key or
         * a character string used to generate the real hex. This checks what kind of
         * password has been supplied. The checks correspond to WEP40, WEP104 & WEP232
         * @param s
         * @return
         */
        private static boolean getHexKey (String s){
            if (s == null) {
                return false;
            }

            int len = s.length();
            if (len != 10 && len != 26 && len != 58) {
                return false;
            }

            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                    continue;
                }
                return false;
            }
            return true;
        }

        private WifiConfiguration IsExsits (String SSID){
            List<WifiConfiguration> existingConfigs = wifiManager
                    .getConfiguredNetworks();
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
            return null;
        }


    }
