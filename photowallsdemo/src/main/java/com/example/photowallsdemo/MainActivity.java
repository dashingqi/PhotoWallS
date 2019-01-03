package com.example.photowallsdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private PhotoWallsAdapter photoWallsAdapter;
    private int mImageThumbSize;
    private int mImageThumbSpacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
        mGridView = findViewById(R.id.mGridView);
        photoWallsAdapter = new PhotoWallsAdapter(this, 0, Images.imageThumbUrls, mGridView);
        mGridView.setAdapter(photoWallsAdapter);

        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final int numColumns = (int) Math.floor(mGridView.getWidth()/(mImageThumbSize+mImageThumbSpacing));

                if (numColumns>0){
                    int columnWidth = (mGridView.getWidth()/numColumns)-mImageThumbSpacing;
                    photoWallsAdapter.setItemHeight(columnWidth);
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        photoWallsAdapter.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoWallsAdapter.cancelAllTasks();
    }
}
