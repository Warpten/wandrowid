package org.warpten.wandrowid;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;

public class SocketService extends Service {
    public static final int MSG_SENDPACKET = 1;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ResultReceiver mReceiver = null;

    public SocketService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mReceiver = intent.getParcelableExtra("messageHandler");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mReceiver = null;

    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_SENDPACKET:

            }
        }
    }
}
