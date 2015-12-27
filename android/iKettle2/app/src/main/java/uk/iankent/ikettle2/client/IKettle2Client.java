package uk.iankent.ikettle2.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
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

    static final byte commandSentAck = 3;
    static final byte kettleOffBase  = 7;
    static final byte kettleOnBase   = 8;

    protected String host;
    protected Integer port;

    private Socket clientSocket;
    public OnKettleResponse<KettleStatusResponse> onKettleStatus;
    public OnKettleResponse<KettleCommandAckResponse> onKettleCommandAck;

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

    public void Test() throws IOException {
        Connect();
        Disconnect();
    }

    public void Connect() throws IOException {
        clientSocket = new Socket(this.host, this.port);
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

    public void Process() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] readBuf = new byte[1024];
        while (clientSocket.isConnected()) {
            int n = clientSocket.getInputStream().read(readBuf);
            if (n < 0) {
                break;
            }
            for(int i = 0; i < n; i++) {
                byte b = readBuf[i];
                if(b == endMessage) {
                    byte[] cmd = out.toByteArray();
                    byte[] args = Arrays.copyOfRange(cmd, 1, cmd.length);
                    out.reset();

                    switch (cmd[0]) {
                        case kettleStatus:
                            if(this.onKettleStatus != null) {
                                this.onKettleStatus.onKettleResponse(KettleStatusResponse.fromBytes(args));
                            }
                            break;
                        case commandSentAck:
                            if(this.onKettleCommandAck != null) {
                                this.onKettleCommandAck.onKettleResponse(KettleCommandAckResponse.fromBytes(args));
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
    }

    public void Disconnect() throws IOException {
        clientSocket.close();
    }
}
