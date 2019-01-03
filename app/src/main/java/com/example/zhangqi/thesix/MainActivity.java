package com.example.zhangqi.thesix;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //获取设备屏幕的分辨率
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;
        //获取到手机屏幕的密度
        float xdpi = dm.xdpi;
        float ydpi = dm.ydpi;

        Log.i(TAG, "xdpi = " + xdpi + " ydpi = " + ydpi);
        //
        Log.i(TAG, "width = " + widthPixels + " height = " + heightPixels);
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bitmap);
        mImageView.setImageBitmap(mBitmap);
        Log.i(TAG, "width =  " + mBitmap.getWidth());
        Log.i(TAG, "height = " + mBitmap.getHeight());
        Log.i(TAG, "size = " + mBitmap.getHeight() * mBitmap.getWidth() * 4);

    }

    private void initView() {
        mImageView = findViewById(R.id.mImageView);
    }
}
