package uk.iankent.ikettle2.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import uk.iankent.ikettle2.Constants;
import uk.iankent.ikettle2.R;
import uk.iankent.ikettle2.UseIKettle2;
import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.data.Kettle;

// This service is to be used as a foreground service.
// It'll display a notification with the state
// of the kettle and update the notification with appropriate actions/informations:
//     - While boiling:
//         - Infos: State(Boiling) & Temperature
//         - Actions:
//             - Cancel button (stop boiling + kill services)
//             - Keep warm checkbox (keep water warm after boiling during 20 minutes)
//     - Once boiled:
//         - Info: State (Ready !)
//         - Action: Discard notification button and keep warm toggle
public class UseKettle2Service extends Service {

    //
    // Transition:
    //  - [DISCONNECTED]->[CONNECTED] When we receive the connected event from the kettle client
    //  - [CONNECTED]->[BOILING] if user press start, of if the user press the hardware button
    //  - [BOILING]->[BOILED] When the kettle stop and the desired temperature has been reached
    //  - [BOILING]->[CONNECTED] If user press cancel or remove kettle from base
    //  - [BOILED]->[CONNECTED] After 5 minutes of if user press OK in the notification
    //
    public enum ServiceState {
        DISCONNECTED, // Default state
        CONNECTED,
        BOILING, // The kettle is currently heating the water
        BOILED, // The kettle has finished to heat the water. It is the same has "Idle" except that the water has boiled in the last 5 minutes or less.
    }

    ServiceState state = ServiceState.DISCONNECTED;

    Kettle kettle;
    IKettle2Client client = null;
    int desiredTemp;
    int binded = 0;

    // Binder given to clients
    final LocalBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ArrayList<IKettle2Client.KettleListener> listeners = new ArrayList<IKettle2Client.KettleListener> ();

        public void setKettleListener(IKettle2Client.KettleListener list) {
            listeners.add(list);
        }
        public void removeKettleListener(IKettle2Client.KettleListener list) {
            listeners.remove(list);
        }

        public ServiceState getState() {
            return state;
        }

        /*public IKettle2Client getClient() {
            return UseKettle2Service.this.client;
        }*/
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("service", "onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("service", "onBind");
        binded++;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        binded--;
        Log.i("service", "onUnbind");
        if(state == ServiceState.CONNECTED) {
            try {
                client.Disconnect();
            } catch(IOException e) {
                // Do nothing
            }
            stopForeground(true);
            stopSelf();
        }
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null)
            return START_STICKY;
        Log.i("service", "Received : " + intent.getAction());
        if (intent.getAction().equals(Constants.START_ACTION)) {
            kettle = intent.getParcelableExtra("kettle");
            if(client == null) {
                client = new IKettle2Client(kettle.Host, kettle.Port);

                client.setKettleListener(new IKettle2Client.KettleListener() {
                    @Override
                    public void onError(KettleError error) {
                        Log.i("service", "onError");
                        for (IKettle2Client.KettleListener list : mBinder.listeners) {
                            list.onError(error);
                        }
                    }

                    @Override
                    public void onConnected() {
                        client.Process();
                        if(state == ServiceState.DISCONNECTED)
                            state = ServiceState.CONNECTED;
                        Log.i("service", "onConnected");
                        for (IKettle2Client.KettleListener list : mBinder.listeners) {
                            list.onConnected();
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        Log.i("service", "onDisconnected");
                        updateNotification();
                        for (IKettle2Client.KettleListener list : mBinder.listeners) {
                            list.onDisconnected();
                        }
                    }

                    @Override
                    public void onKettleStatus(KettleStatusResponse response) {
                        switch (state) {
                            case CONNECTED:
                                if(response.getStatus() == KettleStatusResponse.State.Boiling) {
                                    Log.d("service", "[IDLE]->[BOILING]");
                                    state = ServiceState.BOILING;
                                }
                                break;
                            case BOILING:
                                if(response.getStatus() == KettleStatusResponse.State.Ready) {
                                    Log.d("service", "[BOILING]->[BOILED]");
                                    state = ServiceState.BOILED;
                                }
                                break;
                            case BOILED:
                                if(response.getTemperature() == 127) {
                                    // Kettle off base
                                    if(binded<=0) {
                                        try {
                                            client.Disconnect();
                                        } catch(IOException e) {
                                            // Do nothing
                                        }
                                        stopForeground(true);
                                        stopSelf();
                                    }
                                }
                                break;
                        }
                        updateNotification();
                        for (IKettle2Client.KettleListener list : mBinder.listeners) {
                            list.onKettleStatus(response);
                        }
                    }

                    @Override
                    public void onKettleCommandAck(KettleCommandAckResponse response) {
                        Log.i("service", "onKettleCommandAck");
                        updateNotification();
                        for (IKettle2Client.KettleListener list : mBinder.listeners) {
                            list.onKettleCommandAck(response);
                        }
                    }

                    @Override
                    public void onKettleAutoDacResponse(KettleAutoDacResponse response) {
                        // Ignored
                    }
                });
            }
            client.Connect();
        } else if (intent.getAction().equals(Constants.START_BOILING_ACTION)) {
            desiredTemp = intent.getIntExtra("temp", 100);
            updateNotification();
            client.Start(desiredTemp);
        } else if (intent.getAction().equals(Constants.STOP_BOILING_ACTION)) {
            client.Stop();
            state = ServiceState.CONNECTED;
            stopForeground(true);
            // Check is service is binded. If not, stop the service too
            if(binded<=0) {
                try {
                    client.Disconnect();
                } catch(IOException e) {
                    // Do nothing
                }
                stopSelf();
            }
        } else if(intent.getAction().equals(Constants.STOP_ACTION)) {
            try {
                client.Disconnect();
            } catch(IOException e) {
                // Do nothing
            }
            stopForeground(true);
            stopSelf();
        } else if(intent.getAction().equals(Constants.START_CALIBRATE)) {
            client.Calibrate();
        }
        return START_STICKY;
    }

    private void updateNotification() {
        String stateStr = "Boiling";

        // Launch activity intent used in notifications
        Intent resultIntent = new Intent(this, UseIKettle2.class);
        resultIntent.putExtra("kettle", kettle);
        PendingIntent resultPendingIntent =
        PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        switch(state) {
            case CONNECTED:
                return; // We don't display notif here
            case BOILING: {
                // Nothing to do
                KettleStatusResponse s = client.getLastStatus();
                int temp = s.getTemperature();

                Log.i("service", "onKettleStatus: " + state);

                // Cancel intent
                Intent cancelIntent = new Intent(this, UseKettle2Service.class);
                cancelIntent.setAction(Constants.STOP_BOILING_ACTION);
                PendingIntent pcancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);

                Notification notif = new NotificationCompat.Builder(this)
                        .setContentTitle("Ikettle 2.0")
                        .setContentText(state + " - " + Integer.toString(temp) + "°C")
                        .setSmallIcon(R.mipmap.kettle)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(resultPendingIntent)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_cancel, "Stop", pcancelIntent).build();
                startForeground(Constants.NOTIFICATION_ID, notif);
                break;
            } case BOILED: {
                stateStr = "Ready";
                KettleStatusResponse s = client.getLastStatus();
                int temp = s.getTemperature();

                Log.i("service", "onKettleStatus: " + state);

                Intent cancelIntent = new Intent(this, UseKettle2Service.class);
                cancelIntent.setAction(Constants.STOP_BOILING_ACTION);
                PendingIntent pcancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);

                Notification notif = new NotificationCompat.Builder(this)
                        .setContentTitle("Ikettle 2.0")
                        .setContentText(state + " - " + Integer.toString(temp) + "°C")
                        .setSmallIcon(R.mipmap.kettle)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(resultPendingIntent)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_cancel, "Ok", pcancelIntent).build();
                startForeground(Constants.NOTIFICATION_ID, notif);
                break;
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("service", "In onDestroy");
    }
}
