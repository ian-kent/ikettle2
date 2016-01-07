package uk.iankent.ikettle2.client;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by iankent on 27/12/2015.
 */

public class IKettle2Client {
    static final byte autoDacKettle         = 44;
    static final byte autoDacKettleResponse = 45;
    static final byte endMessage            = 126;
    static final byte kettleScheduleRequest = 65;
    static final byte kettleStatus          = 20;
    static final byte turnOffKettle         = 22;
    static final byte turnOnKettle          = 21;
    static final byte listWifiNetworks      = 13;
    static final byte configureWifi         = 12;
    static final byte configureWifiSSID     = 5;
    static final byte configureWifiPassword = 7;
    static final byte getDeviceInfo         = 100;
    static final byte startFirmwareUpdate   = 109;

    static final byte commandSentAck      = 3;
    static final byte kettleOffBase       = 7;
    static final byte kettleOnBase        = 8;
    static final byte wifiNetworkResponse = 14;
    static final byte deviceInfoResponse  = 101;

    protected KettleStatusResponse lastStatus;

    protected boolean autoDacPending = false;
    protected int autoDacOffBaseWeight = 0;

    protected String host;
    protected Integer port;

    private Socket clientSocket;
    public OnKettleResponse<KettleStatusResponse> onKettleStatus;
    public OnKettleResponse<KettleCommandAckResponse> onKettleCommandAck;
    public OnKettleResponse<KettleWifiNetworksResponse> onKettleWifiNetworksResponse;
    public OnKettleResponse<KettleDeviceInfoResponse> onKettleDeviceInfoResponse;
    public OnKettleResponse<KettleAutoDacResponse> onKettleAutoDacResponse;
    public OnKettleResponse onConnected;
    public OnKettleResponse onDisconnected;
    public OnKettleResponse<KettleError> onError;

    public boolean IsConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }

    public IKettle2Client() {
        this(null);
    }

    public IKettle2Client(String host) {
        this(host, 2081);
    }

    public IKettle2Client(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    protected void handleError(KettleError e) {
        e.exception.printStackTrace();
        if(this.onError != null) {
            this.onError.onKettleResponse(e);
        }
    }

    public void Connect() {
        Connect(5000);
    }

    public void Connect(final int timeout) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    clientSocket = new Socket();
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setSoLinger(true, 0);
                    clientSocket.connect(new InetSocketAddress(IKettle2Client.this.host, IKettle2Client.this.port), timeout);
                    if(IKettle2Client.this.onConnected != null) {
                        IKettle2Client.this.onConnected.onKettleResponse(null);
                    }
                } catch (IOException e) {
                    IKettle2Client.this.handleError(new KettleConnectError(e));
                }
                return null;
            }
        }.execute();
    }

    public void Start() {
        this.Start(100);
    }
    public void Start(int temperature) {
        try {
            clientSocket.getOutputStream().write(new byte[]{turnOnKettle, (byte)temperature, 0, endMessage});
        } catch (IOException e) {
            // TODO
        }
    }
    public void Stop() {
        try {
            clientSocket.getOutputStream().write(new byte[]{turnOffKettle, endMessage});
        } catch (IOException e) {
            // TODO
        }
    }

    public void Process() {
        final IKettle2Client this$ = this;

        new Thread() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] readBuf = new byte[1024];
                    while (clientSocket.isConnected()) {
                        int n = this$.clientSocket.getInputStream().read(readBuf);
                        if (n < 0) {
                            break;
                        }
                        for (int i = 0; i < n; i++) {
                            byte b = readBuf[i];
                            if (b == endMessage) {
                                byte[] cmd = out.toByteArray();
                                byte[] args = Arrays.copyOfRange(cmd, 1, cmd.length);
                                out.reset();

                                Log.d("IKettle", "Process: " + String.valueOf((int) cmd[0]));
                                Log.d("IKettle", "Process: " + Arrays.toString(cmd));

                                switch (cmd[0]) {
                                    case kettleStatus:
                                        if (this$.onKettleStatus != null) {
                                            KettleStatusResponse res = KettleStatusResponse.fromBytes(args);
                                            this$.lastStatus = res;
                                            if(autoDacPending) {
                                                autoDacPending = false;
                                                autoDacOffBaseWeight = res.getWaterLevel();
                                                completeCalibrate();
                                                break;
                                            }
                                            this$.onKettleStatus.onKettleResponse(res);
                                        }
                                        break;
                                    case commandSentAck:
                                        if (this$.onKettleCommandAck != null) {
                                            this$.onKettleCommandAck.onKettleResponse(KettleCommandAckResponse.fromBytes(args));
                                        }
                                        break;
                                    case wifiNetworkResponse:
                                        if (this$.onKettleWifiNetworksResponse != null) {
                                            this$.onKettleWifiNetworksResponse.onKettleResponse(KettleWifiNetworksResponse.fromBytes(args));
                                        }
                                        break;
                                    case deviceInfoResponse:
                                        if (this$.onKettleDeviceInfoResponse != null) {
                                            this$.onKettleDeviceInfoResponse.onKettleResponse(KettleDeviceInfoResponse.fromBytes(args));
                                        }
                                        break;
                                    case autoDacKettleResponse:
                                        Log.d("IKettle2Client", "Process: got autoDacKettleResponse");
                                        if (this$.onKettleAutoDacResponse != null) {
                                            this$.onKettleAutoDacResponse.onKettleResponse(new KettleAutoDacResponse(autoDacOffBaseWeight));
                                        }
                                        break;
                                    default:
                                        // FIXME invalid command
                                        break;
                                }
                                continue;
                            }
                            out.write(readBuf[i]);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("IKettle2Client", "Process: socket no longer connected");
                if(this$.onDisconnected != null) {
                    this$.onDisconnected.onKettleResponse(null);
                }
            }
        }.start();
    }

    public void GetWifiConnections() {
        try {
            clientSocket.getOutputStream().write(new byte[]{listWifiNetworks, endMessage});
        } catch (IOException e) {
            this.handleError(new KettleGetWifiError(e));
        }
    }

    public void SetWifiSSID(String ssid) {
        try {
            byte[] b = ssid.getBytes(Charset.forName("ASCII"));
            byte[] b2 = new byte[b.length + 2];
            b2[0] = configureWifiSSID;
            b2[b2.length - 1] = endMessage;
            System.arraycopy(b, 0, b2, 1, b.length);
            Log.d("IKettle2Client", "SetWifiSSID: " + new String(b2, "ASCII"));
            Log.d("IKettle2Client", "Bytes: " + Arrays.toString(b2));
            clientSocket.getOutputStream().write(b2);
        } catch (IOException e) {
            this.handleError(new KettleSaveWifiSSIDError(e));
        }
    }

    public void GetDeviceInfo() {
        try {
            clientSocket.getOutputStream().write(new byte[]{getDeviceInfo, endMessage});
        } catch (IOException e) {
            this.handleError(new KettleGetDeviceInfoError(e));
        }
    }

    public void SetWifiPassword(String password) {
        try {
            byte[] b = password.getBytes(Charset.forName("ASCII"));
            byte[] b2 = new byte[b.length + 2];
            b2[0] = configureWifiPassword;
            b2[b2.length - 1] = endMessage;
            System.arraycopy(b, 0, b2, 1, b.length);
            Log.d("IKettle2Client", "SetWifiPassword: " + new String(b2, "ASCII"));
            Log.d("IKettle2Client", "Bytes: " + Arrays.toString(b2));
            clientSocket.getOutputStream().write(b2);
        } catch (IOException e) {
            this.handleError(new KettleSaveWifiPasswordError(e));
        }
    }

    public void SaveWifiSettings() {
        try {
            Log.d("IKettle2Client", "SaveWifiSettings");
            byte[] b2 = new byte[]{configureWifi, endMessage};
            Log.d("IKettle2Client", "Bytes: " + Arrays.toString(b2));
            clientSocket.getOutputStream().write(b2);
        } catch (IOException e) {
            this.handleError(new KettleSaveWifiError(e));
        }
    }

    public void Disconnect() throws IOException {
        clientSocket.close();
    }

    public KettleStatusResponse getLastStatus() {
        if(lastStatus == null) {
            return new KettleStatusResponse();
        }
        return lastStatus;
    }

    public void Calibrate() {
        Log.d("IKettle2Client", "Calibrate: setting autoDacPending");
        autoDacPending = true;
    }

    protected void completeCalibrate() {
        Log.d("IKettle2Client", "completeCalibrate: sending autoDacKettle command");
        try {
            clientSocket.getOutputStream().write(new byte[]{autoDacKettle, endMessage});
        } catch (IOException e) {
            this.handleError(new KettleCalibrateError(e));
        }
    }
}
