package com.example.a_gun;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

public class UdpClient {
    public interface OnFindServerListener {
        void onFindServer(String serverIP);
    }

    public boolean mContinueFinding;
    private String mIP = null;
    private final int mMainPort = 20415;
    private final int mReplyPort = 20416;
    private DatagramSocket mSocket = null;
    private DatagramSocket mReplySocket = null;
    private int mPacketCount = 0;
    private OnFindServerListener onFindServerListener = null;

    // If the address is localhost return true. source : https://stackoverflow.com/questions/2406341/how-to-check-if-an-ip-address-is-the-local-host-on-a-multi-homed-system
    private boolean isThisMyIpAddress(InetAddress addr) {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public UdpClient() {
    }

    public String getIP() {
        return mIP;
    }

    public int getPort() {
        return mMainPort;
    }

    // stop finding server
    public void pauseServerFinder() {
        mContinueFinding = false;
        mReplySocket.close();
        mReplySocket = null;
    }

    // find server's ip in LAN
    public void resumeServerFinder() {
        mContinueFinding = true;
        // receiver
        (new Thread(() -> {
            while (mContinueFinding && mIP == null) {
                try {
                    if (mReplySocket == null)
                        mReplySocket = new DatagramSocket(mReplyPort);
                    DatagramPacket dp = new DatagramPacket(new byte[10], 10);
                    // receive reply packet (without own packet)
                    mReplySocket.receive(dp);
                    if (mIP == null && !isThisMyIpAddress(dp.getAddress())) {
                        mIP = dp.getAddress().getHostName();
                        if (onFindServerListener != null) {
                            onFindServerListener.onFindServer(mIP);
                        }
                    }
                } catch (Exception ex) {
                    Log.e("resumeServerFinder", ex.getMessage());
                }
            }

        })).start();

        // sender
        (new Thread(()-> {
            while (mContinueFinding && mIP == null) {
                try {
                    if (mReplySocket == null)
                        mReplySocket = new DatagramSocket(mReplyPort);

                    // send broadcast packet
                    send(mReplySocket, new byte[1], "255.255.255.255", mReplyPort);
                    Thread.sleep(500);
                } catch (Exception ex) {
                    Log.e("resumeServerFinder", ex.getMessage());
                }
            }
        })).start();
    }

    public void setOnFindServerListener(OnFindServerListener onFindServerListener) {
        this.onFindServerListener = onFindServerListener;
    }

    private void send(DatagramSocket socket, byte[] data, String destinationIP, int destinationPort) {
        (new Thread(()-> {
            try {
                mPacketCount++;
                DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName(destinationIP), destinationPort);
                socket.send(dp);
            } catch (SocketException se) {
            Log.e("send", se.getMessage());
            } catch (IOException ioe) {
                Log.e("send", ioe.getMessage());
            }
        })).start();
    }

    public boolean send(byte[] data) {
        if (mIP != null) {
            if (mSocket == null) {
                try {
                    mSocket = new DatagramSocket(mMainPort);
                } catch (SocketException e) {
                    mSocket = null;
                    e.printStackTrace();
                }
            }
            send(mSocket, data, mIP, mMainPort);
            return true;
        }
        else {
            Log.i("send", "server not found");
            return false;
        }
    }
}
