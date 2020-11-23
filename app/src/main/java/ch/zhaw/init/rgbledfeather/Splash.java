package ch.zhaw.init.rgbledfeather;

import android.os.Bundle;
import android.os.Handler;

import ch.zhaw.init.rgbledfeather.main.MainActivity;
import ch.zhaw.init.rgbledfeather.main.SacnnerActivity;

public class Splash extends Base {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                gotoNext();
            }
        }, 2000);
    }

    private void gotoNext() {
        gotoActivity(SacnnerActivity.class,null, true);
        finish();
    }
}