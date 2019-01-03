package com.example.photowallsdemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

/**
 * @ProjectName: TheSix
 * @Package: com.example.photowallsdemo
 * @ClassName: PhotoWallsAdapter
 * @Author: DashingQI
 * @CreateDate: 2019/1/3 10:44 PM
 * @UpdateUser: 更新者
 * @UpdateDate: 2019/1/3 10:44 PM
 * @UpdateRemark:
 * @Version: 1.0
 */
public class PhotoWallsAdapter extends ArrayAdapter<String> {
    private static final String TAG = "PhotoWallsAdapter";


    private GridView mGridView;
    private final LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;

    /**
     * 每个Item的高度
     */
    private int itemHeight;
    private BufferedInputStream in;
    private HttpURLConnection urlConnection;
    private BufferedOutputStream out;
    private HashSet<BitmapWorkerTask> collectionTask;


    public PhotoWallsAdapter(@NonNull Context context, int textViewResourceId, @NonNull String[] objects, GridView photoWallsGridView) {
        super(context, textViewResourceId, objects);
        mGridView = photoWallsGridView;
        collectionTask = new HashSet<>();

        //获取到应用程序运行时的最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //将缓存内存设置为最大内存的1/8
        int cacheSize = maxMemory / 8;

        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        File diskCacheFile = getDiskCacheFile(context, "thumb");
        if (!diskCacheFile.exists())
            diskCacheFile.mkdirs();
        try {
            //打开磁盘缓存
            mDiskLruCache = DiskLruCache.open(diskCacheFile, getAppVersion(context), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.image_layout, parent, false);
        } else {
            view = convertView;
        }


        ImageView mImageView = view.findViewById(R.id.mImageView);
        if (mImageView.getLayoutParams().height != itemHeight) {
            mImageView.getLayoutParams().height = itemHeight;
        }
        //给ImageView设置一个tag，防止在异步加载的时候，乱序了。
        mImageView.setTag(url);
        mImageView.setImageResource(R.mipmap.ic_launcher);
        loadBitmaps(mImageView, url);
        return view;
    }

    /**
     * 获取到磁盘缓存的路径
     *
     * @param context
     * @param name
     * @return
     */
    private File getDiskCacheFile(Context context, String name) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + name);
    }

    /**
     * 获取到应用的版本号
     *
     * @param context
     * @return
     */
    private int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemoryCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 加载图片
     *
     * @param imageView
     * @param imageUrl
     */
    private void loadBitmaps(ImageView imageView, String imageUrl) {

        Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
        //内存中没有存在这个相片
        if (bitmap == null) {
            //就去下载
            BitmapWorkerTask task = new BitmapWorkerTask();

            task.execute(imageUrl);
        } else {
            if (bitmap != null && imageView != null) {
                //去显示图片
                imageView.setImageBitmap(bitmap);
            }
        }

    }

    /**
     * 将缓存记录同步到journal文件中
     */
    public void flushCache() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 使用MD5算法对传入的key进行加密并且返回
     *
     * @param key
     * @return
     */
    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(key.getBytes());
            cacheKey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }

        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 下载图片
     *
     * @param urlString
     * @param outputStream
     * @return
     */
    private boolean downloadFromUrlToStream(String urlString, OutputStream outputStream) {

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            int code = urlConnection.getResponseCode();
            if (code == 200) {
                in = new BufferedInputStream(urlConnection.getInputStream(), 10 * 1024);
                out = new BufferedOutputStream(outputStream, 10 * 1024);
                int b;
                while ((b = in.read()) != -1) {
                    out.write(b);
                }

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 将一张图片存储到LruCache中
     *
     * @param key    键 这里传入图片的URL地址
     * @param bitmap 传入网络上下载的Bitmap对象
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mLruCache.put(key, bitmap);
        }

    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private String url;


        @Override
        protected Bitmap doInBackground(String... args) {
            //获取到图片的地址
            url = args[0];
            DiskLruCache.Snapshot snapshot;
            FileInputStream fileInputStream = null;
            FileDescriptor fileDescriptor = null;

            try {
                //生成图片URL对应的key
                String key = hashKeyForDisk(url);
                Log.i(TAG, "doInBackground: "+key);
                //在磁盘中查找key对应的缓存
                snapshot = mDiskLruCache.get(key);
                if (snapshot == null) {
                    //如果没有找到对应的缓存，就要去网络中下载了，并写入到缓存中
                    DiskLruCache.Editor edit = mDiskLruCache.edit(key);
                    if (edit != null) {
                        OutputStream outputStream = edit.newOutputStream(0);
                        if (downloadFromUrlToStream(url, outputStream)) {
                            edit.commit();
                        } else {
                            edit.abort();
                        }
                    }
                    //缓存被写入后，再次查找key对应的缓存
                    snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                        fileDescriptor = fileInputStream.getFD();
                    }

                    Bitmap bitmap = null;

                    //将缓存数据解析成Bitmap对象
                    if (fileDescriptor != null) {
                        bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    }

                    if (bitmap != null) {
                        addBitmapToMemoryCache(key, bitmap);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileDescriptor == null && fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = mGridView.findViewWithTag(url);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            collectionTask.remove(this);
        }
    }

    /**
     * 取消所有正在下载或者等待下载的任务
     */
    public void cancelAllTasks() {
        if (collectionTask != null) {
            for (BitmapWorkerTask task : collectionTask) {
                task.cancel(false);
            }
        }
    }

    /**
     * 设置Item子项的高度
     *
     * @param height
     */
    public void setItemHeight(int height) {
        if (height == itemHeight)
            return;
        itemHeight = height;
        notifyDataSetChanged();
    }

}
