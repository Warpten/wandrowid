package org.warpten.wandrowid;

/**
public class ServiceCommunicator extends ResultReceiver implements ServiceConnection {
    SocketService mService = null;
    Messenger mServiceMessenger = null;
    boolean mBound = false;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = (SocketService)iBinder;
        mServiceMessenger = new Messenger(iBinder);
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;
        mServiceMessenger = null;
        mBound = false;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
    }
}*/