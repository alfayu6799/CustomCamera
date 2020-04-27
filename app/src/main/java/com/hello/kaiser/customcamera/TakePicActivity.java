package com.hello.kaiser.customcamera;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

public class TakePicActivity extends AppCompatActivity {

    private final String TAG = TakePicActivity.class.getSimpleName();

    private ImageView button;
    private CameraSurfaceView mCameraSurfaceView;
    private Activity activity;
    String filePath;
    private Button exposure;
    private ImageView flash;
    private SeekBar seekBar;

    //flag to detect flash is on or off
    private boolean isLighton = false;  //default is turn off

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        getBundleData();

        initSet();
        initView();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraSurfaceView.takePicture(activity, filePath);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int i = progress -2;
                mCameraSurfaceView.exposure(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLighton){
                    isLighton = false;
                    mCameraSurfaceView.offFlash();
                    flash.setImageResource(R.drawable.ic_flash_off_white_24dp);
                }else {
                    isLighton = true;
                    mCameraSurfaceView.openFlash();
                    flash.setImageResource(R.drawable.ic_flash_on_white_24dp);
                }
            }
        });
    }

    private void initSet() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_take_pic);
    }


    private void initView() {
        mCameraSurfaceView = (CameraSurfaceView) findViewById(R.id.cameraSurfaceView);
        button = (ImageView) findViewById(R.id.takePic);
        flash = (ImageView) findViewById(R.id.takeflash);
        exposure = (Button) findViewById(R.id.takexposure);
        seekBar = (SeekBar) findViewById(R.id.sbexposue);
    }

    private void getBundleData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            filePath = bundle.getString("url");
        }
        Log.d("checkpoint", "check filePath - " + filePath);
    }
}
