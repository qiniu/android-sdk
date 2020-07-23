package com.qiniu.android.http.networkCheck;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

class NetworkDetector {

    private final String host;
    private final int port;

    private Socket currentSocket;

    NetworkDetector(String host,int port){
        this.host = host;
        this.port = port;
        this.currentSocket = new Socket();
    }

    boolean check(int timeout){

        boolean success = true;
        try  {
            InetAddress address = InetAddress.getByName(host);
            SocketAddress socketAddress = new InetSocketAddress(address, port);

            currentSocket.connect(socketAddress, timeout * 1000);
        } catch (Exception e){
            success = false;
        }

        return success;
    }

    boolean cancel(){

        boolean success = true;
        try {
            currentSocket.close();
        } catch (Exception E){
            success = false;
        }
        return success;
    }
}

