package com.example.a_gun;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UdpClient {
    public interface OnFindServerListener {
        void onFindServer(String serverIP);
    }

    public boolean mContinueFinding;
    private String mIP = null;
    private final int mMainPort = 20415;
    private final int mReplyPort = 20416;
    private DatagramSocket mSocket = null;
    private DatagramSocket mServerFinderSocket = null;
    private OnFindServerListener onFindServerListener = null;
    private BlockingQueue<DatagramPacket> sendWaitQueue;

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
        sendWaitQueue = new ArrayBlockingQueue<>(10);
        // synchronized packet send (thread-safe send)
        (new Thread(()->{
            DatagramPacket packet;
            while (true) {
                try {
                    packet = sendWaitQueue.take(); // block
                    if (mSocket == null) {
                        mSocket = new DatagramSocket(mMainPort);
                    }
                    mSocket.send(packet);
                } catch (InterruptedException ie) {
                    Log.e("queue interrupted", ie.getMessage());
                    return;
                } catch (SocketException ioe) {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        Log.e("queue socket close", e.getMessage());
                    }
                    mSocket = null;
                } catch (Exception e) {
                    Log.e("queue exception", e.getMessage());
                }
            }
        })).start();
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
        mServerFinderSocket.close();
        mServerFinderSocket = null;
    }

    // find server's ip in LAN
    public void resumeServerFinder() {
        mContinueFinding = true;
        // receiver
        (new Thread(() -> {
            while (mContinueFinding && mIP == null) {
                try {
                    if (mServerFinderSocket == null)
                        mServerFinderSocket = new DatagramSocket(mReplyPort);
                    DatagramPacket dp = new DatagramPacket(new byte[10], 10);
                    // receive reply packet (without own packet)
                    mServerFinderSocket.receive(dp);
                    if (mIP == null && !isThisMyIpAddress(dp.getAddress())) {
                        mIP = dp.getAddress().getHostName();
                        if (onFindServerListener != null) {
                            onFindServerListener.onFindServer(mIP);
                            mContinueFinding = false;
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
                    // send broadcast packet
                    sendWaitQueue.add(new DatagramPacket(new byte[1], 1, InetAddress.getByName("255.255.255.255"), mReplyPort));
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

    public boolean send(byte[] data) {
        try {
            return sendWaitQueue.offer(new DatagramPacket(data, data.length, InetAddress.getByName(mIP), mMainPort));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }
}
