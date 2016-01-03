package uk.iankent.ikettle2.client;

import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;

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

    public void Start() {
        Start(100);
    }

    public void Start(final int perHostTimeout) {
        if(isBusy)return;
        isBusy = true;

        scanned = 0;

        new Thread(){
            @Override
            public void run() {
                // FIXME get subnet from current IP?
                String subnet = "192.168.100.0/24";
                SubnetUtils utils = new SubnetUtils(subnet);
                final String[] allIPs = utils.getInfo().getAllAddresses();
                for(int i = 0; i < allIPs.length; i++) {
                    final String ip = allIPs[i];
                    mDiscoverThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            IKettle2Client client = new IKettle2Client(ip);
                            client.onConnected = new OnKettleResponse() {
                                @Override
                                public void onKettleResponse(KettleResponse response) {
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
                            };
                            client.onError = new OnKettleResponse<KettleError>() {
                                @Override
                                public void onKettleResponse(KettleError response) {
                                    // Do nothing
                                    scanned++;
                                    if(scanned == allIPs.length && onScanFinished != null) {
                                        onScanFinished.onKettleResponse(null);
                                        isBusy = false;
                                    }
                                }
                            };
                            client.Connect(perHostTimeout);
                        }
                    });
                }
            }
        }.run();
    }
}
