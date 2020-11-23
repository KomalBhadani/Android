package ch.zhaw.init.rgbledfeather;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import ch.zhaw.init.rgbledfeather.utils.AppPref;

public class Base extends AppCompatActivity  {

    private static final String TAG = "Base";
    public Toolbar toolbar;

    protected AppPref appPref;

    Dialog dialog;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPref=AppPref.getInstance(this);
    }

    public  boolean hasPermission( String[] permissions) {

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && permissions!=null)
        {
            for(String permission:permissions)
            {
                if(ActivityCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }

    public void gotoActivity(Class className, Bundle bundle, boolean isClearStack)
    {
        Intent intent=new Intent(this,className);

        if(bundle!=null)
            intent.putExtras(bundle);

        if(isClearStack)
        {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    public void gotoActivity(@NonNull Intent intent, boolean isClearStack)
    {
        if(isClearStack)
        {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    public void showToast(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    public  void showLoading()
    {
        if(dialog!=null)
            hideLoading();

        if(dialog==null)
        {
            dialog=new Dialog(this);
            if(dialog.getWindow()!=null)
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loading_bar);
        }
        if(!dialog.isShowing())
            dialog.show();
    }

    public  void hideLoading()
    {
        if(dialog!=null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    public void changeFrag(Fragment fragment, boolean isBackStack, boolean isPopBack)
    {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if(isPopBack)
        {
            fm.popBackStack();

        }
        if(isBackStack)
        {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.replace(R.id.fragment,fragment);
        fragmentTransaction.commit();
    }




}
