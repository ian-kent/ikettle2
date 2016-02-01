package uk.iankent.ikettle2.client;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by iankent on 02/01/2016.
 */
public class NetworkScanner {
    public OnKettleResponse<KettleDeviceInfoResponse> onKettleFound;
    public OnKettleResponse onScanFinished;

    final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    final int KEEP_ALIVE_TIME = 1;
    final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    final BlockingQueue<Runnable> mDiscoverWorkQueue = new LinkedBlockingQueue<Runnable>();

    protected boolean isBusy = false;
    protected int scanned;

    private Context ctx;

    final ThreadPoolExecutor mDiscoverThreadPool = new ThreadPoolExecutor(
            NUMBER_OF_CORES,       // Initial pool size
            NUMBER_OF_CORES * 25,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mDiscoverWorkQueue);

    public void Stop() {
        if(!isBusy)return;

        mDiscoverThreadPool.shutdown();
        mDiscoverWorkQueue.clear();
    }

    public void Start(Context ctx, final String subnetCIDR, final int perHostTimeout) {
        if(isBusy)return;
        isBusy = true;
        this.ctx = ctx;

        scanned = 0;

        new Thread(){
            @Override
            public void run() {
                SubnetUtils utils = new SubnetUtils(subnetCIDR);
                final String[] allIPs = utils.getInfo().getAllAddresses();
                for(int i = 0; i < allIPs.length; i++) {
                    final String ip = allIPs[i];
                    mDiscoverThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            IKettle2Client client = new IKettle2Client(ip);
                            client.setKettleListener(new IKettle2Client.KettleListener() {
                                @Override
                                public void onError(KettleError error) {
                                    // Do nothing
                                    scanned++;
                                    if(scanned == allIPs.length && onScanFinished != null) {
                                        onScanFinished.onKettleResponse(null);
                                        isBusy = false;
                                    }
                                }

                                @Override
                                public void onConnected() {
                                    // FIXME request device info
                                    Log.d("AddIKettle2", "onKettleResponse: found device: " + ip);
                                    if(NetworkScanner.this.onKettleFound != null) {
                                        NetworkScanner.this.onKettleFound.onKettleResponse(new KettleDeviceInfoResponse(ip));
                                    }

                                    scanned++;
                                    if(scanned == allIPs.length && onScanFinished != null) {
                                        onScanFinished.onKettleResponse(null);
                                        isBusy = false;
                                    }
                                }

                                @Override
                                public void onDisconnected() {

                                }

                                @Override
                                public void onKettleStatus(KettleStatusResponse response) {

                                }

                                @Override
                                public void onKettleCommandAck(KettleCommandAckResponse response) {

                                }

                                @Override
                                public void onKettleAutoDacResponse(KettleAutoDacResponse response) {

                                }
                            });
                            client.Connect(perHostTimeout);
                        }
                    });
                }
            }
        }.run();
    }

    public static String getSubnet(Context ctx) {
        WifiManager wifiMgr = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        // It's ok to use formatIpAdress even if deprecated.
        // formatIpAddress support only IPV4 but WifiInfo.getIpAddress() also support only IPV4
        String ipAddress = Formatter.formatIpAddress(ip);

        // The ipAdress should have this format: ABC.DEF.GHI.JKL
        // We need to replace JKL by 0/24 (the subnet we target):
        Pattern pat = Pattern.compile("([0-9]+)");
        Matcher m = pat.matcher(ipAddress);
        MatchResult lastMatch = null;
        while (m.find()) {
            lastMatch = m.toMatchResult();
        }

        // In case of a bug:
        if(lastMatch == null)
            return "192.168.100.0/24";
        else {
            String res = ipAddress.substring(0, lastMatch.start());
            res = res.concat("0/24");
            return res;
        }

    }

}
