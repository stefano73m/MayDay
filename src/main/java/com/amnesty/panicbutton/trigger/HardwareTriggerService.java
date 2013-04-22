package com.amnesty.panicbutton.trigger;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import com.amnesty.panicbutton.MessageAlerter;

public class HardwareTriggerService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        HardwareTriggerReceiver receiver = new HardwareTriggerReceiver(new MessageAlerter(this), new Triggers());
        registerReceiver(receiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
