package org.bridge.imageloader;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ImageLoader class
 */
public class ImageLoader {

    /**
     * Log TAG
     */

    private static final String TAG = "ImageLoader";


    /**
     * Handler Message.what 值，标识用于切换到主线程进行图片显示
     */
    private static final int MESSAGE_POST_RESULT = 1;

    /**
     * 可用CPU 核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * CORE_POOL_SIZE
     */
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    /**
     * MAXIMUM_POOL_SIZE
     */
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    /**
     * 存活时间10
     */
    private static final long KEEP_ALIVE = 10L;

    /**
     * imageView tag标识
     */
    private static final int TAG_KEY_URI = R.id.imageloader_uri;
    /**
     * DISK_CACHE_SIZE
     */
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    /**
     * IO_BUFFER_SIZE
     */
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    /**
     * DISK_CACHE_INDEX
     */
    private static final int DISK_CACHE_INDEX = 0;
    /**
     * 磁盘缓存创建标识 默认false
     */
    private boolean mIsDiskLruCacheCreated = false;


    private static class LoaderResult {
        public ImageView imageView;
        public String uri;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }

    /**
     * 线程工厂方法
     */

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };
    /**
     * 线程池
     */
    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), sThreadFactory);
    /**
     * 主线程Handler
     */
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            imageView.setImageBitmap(result.bitmap);
            //检查ImageView URL 是否发生变化，解决ListView/GridView 加载乱序问题
            String uri = (String) imageView.getTag(TAG_KEY_URI);
            if (uri.equals(result.uri)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                Log.d(TAG, "set Image bitmap,but uri has changed, ignore!");
            }
        }
    };

    /**
     * mContext
     */
    private final Context mContext;
    /**
     * ImageResizer 图片压缩和加载器
     */
    private ImageResizer mImageResizer = new ImageResizer();
    /**
     * LurCache 内存缓存
     */
    private LruCache<String, Bitmap> mMemoryCache;
    /**
     * DiskLruCache 磁盘缓存
     */
    private DiskLruCache mDiskLruCache;

    /**
     * 私有构造方法
     *
     * @param context
     */
    private ImageLoader(Context context) {
        mContext = context.getApplicationContext();
        //当前进程可用最大内存
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //内存缓存大小是可用最大内存的1/8
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        //创建DiskCacheDir磁盘缓存
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建实例
     *
     * @param context
     * @return a new instance of ImageLoader
     */

    public static ImageLoader build(Context context) {
        return new ImageLoader(context);
    }

    /**
     * 将 指定的bitmap 加入 内存缓存中
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从 内存缓存中取得bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemCache(String key) {

        return mMemoryCache.get(key);
    }

    /**
     * load bitmap from memory cache or disk cache or network aync,then bind imageView and
     * bitmap
     * <p/>
     * NOTE THAT:should run id UI Thread
     *
     * @param uri       http url
     * @param imageView bitmap's bind object
     */

    public void bindBitmap(final String uri, final ImageView imageView) {
        bindBitmap(uri, imageView, 0, 0);
    }

    /**
     * @param uri
     * @param imageView
     * @param reqWidth
     * @param reqHeight
     */
    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight) {


        //内存中有直接取得

        imageView.setTag(TAG_KEY_URI, uri);
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        //否则创建Runnable,丢到线程池中进行处理

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmapInternal(uri, reqWidth, reqHeight);
                if (bitmap != null) {
                    LoaderResult result = new LoaderResult(imageView, uri, bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }


    /**
     * 从缓存，磁盘，或者网络中加载
     *
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return bitmap, maybe null.
     */
    private Bitmap loadBitmapInternal(String uri, int reqWidth, int reqHeight) {

        //1.先从内存中取
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmapFromMemCache,uri:" + uri);
            return bitmap;
        }

        try {
            //2.从磁盘缓存当中取
            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
            if (bitmap != null) {
                Log.d(TAG, "loadBitmapFromDiskCache,uri:" + uri);
                return bitmap;
            }
            //3.从网络中取，后续执行了缓存操作
            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
            Log.d(TAG, "loadBitmapFromHttp,uri:" + uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //遇到错误，硬盘缓存没有创建那么直接从网络中取，不进行缓存
        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.w(TAG, "encounter error,DiskLruCache is not created.");
            bitmap = downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }

    /**
     * 从内存中加载bitmap
     *
     * @param uri
     * @return
     */
    private Bitmap loadBitmapFromMemCache(String uri) {
        final String key = hashKeyFormUrl(uri);
        Bitmap bitmap = getBitmapFromMemCache(key);
        return bitmap;
    }

    /**
     * 从网络加载Bitmap
     *
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     * @throws IOException
     */
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI Thread");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        //创建磁盘条目
        String key = hashKeyFormUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            //从网络加载数据，并写入文件系统
            if (downLoadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        //最后再从磁盘中取得这个bitmap
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    /**
     * 从磁盘缓存中加载Bitmap
     *
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return Bitmap
     * @throws IOException
     */
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "load bitmap from UI Thread,it's not recommended!");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        Bitmap bitmap = null;
        String key = hashKeyFormUrl(url);
        DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
        if (snapShot != null) {
            //取硬盘缓存文件输入流
            FileInputStream fileInputStream = (FileInputStream) snapShot.getInputStream(DISK_CACHE_INDEX);
            //拿到文件描述符
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            //加载图片
            bitmap = mImageResizer.decodeSampleBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            //加入内存缓存中
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 从网络下载图片，构建一个输出流
     *
     * @param urlString
     * @param outputStream
     * @return
     */
    private boolean downLoadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadBitmap fail: " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 建立网络连接，下载图片，返回Bitmap
     *
     * @param urlString
     * @return
     */
    private Bitmap downloadBitmapFromUrl(String urlString) {

        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error in downloadBitmap: " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            //MyUtils.close(in);
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;

    }

    /**
     * 对URL进行 MD5加密，返回加密后得到字符串
     *
     * @param url
     * @return
     */
    private String hashKeyFormUrl(String url) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());

        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    /**
     * 字节数据转化为 16进制字符串
     *
     * @param bytes
     * @return
     */
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
     * 判断SD卡是否可用，
     * 如果可用返回SD卡存储路径，否则返回内部存储路径
     *
     * @param context
     * @param uniqueName
     * @return file 路径
     */
    private File getDiskCacheDir(Context context, String uniqueName) {

        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * 获得分区可用空间大小单位为字节
     *
     * @param path
     * @return
     */

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();

    }

}
