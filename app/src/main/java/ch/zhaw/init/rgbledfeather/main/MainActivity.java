package ch.zhaw.init.rgbledfeather.main;


import android.graphics.Color;
import android.os.Bundle;

import ch.zhaw.init.rgbledfeather.Base;
import ch.zhaw.init.rgbledfeather.R;
import ch.zhaw.init.rgbledfeather.utils.AppLog;
import ch.zhaw.init.rgbledfeather.utils.ColorPicker;

public class MainActivity extends Base implements ColorPicker.ColorListener {

    private static final String TAG = "MainActivity";
    private int[] mColor = { 128, 128, 128 };

    MainFragment mainFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        changeFrag(mainFragment = MainFragment.newInstance(getIntent().getExtras()),false,false);
    }

    @Override
    public void onColorChanged(int color) {
        mColor[0] = Color.red(color);
        mColor[1] = Color.green(color);
        mColor[2] = Color.blue(color);

       mainFragment.onColorSelected(mColor,color);

        AppLog.e(TAG,"selected color: "+mColor);
    }


}