package ch.zhaw.init.rgbledfeather.main;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import ch.zhaw.init.rgbledfeather.Base;
import ch.zhaw.init.rgbledfeather.R;

public class SacnnerActivity extends Base {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sacnner);

        changeFrag(ScannerFragment.newInstance(),false,false);
    }
}