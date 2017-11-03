package org.bridge.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * 提供了图片压缩方法和从不同源中加载Bitmap的方法
 */
public class ImageResizer {
    private static final String TAG = "ImageResizer";

    public ImageResizer() {
    }

    /**
     * 根据请求的尺寸从资源文件中载入Bitmap
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */

    public Bitmap decodeSampleBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        //First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        //Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //Decode bitmap width inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 根据请求的bitmap 尺寸 从文件描述中载入bitmap
     *
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */

    public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd, int reqWidth, int reqHeight) {
        //First decode width inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        //Calculate inSampleSize

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //Decode bitmap width inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);


    }

    /**
     * 根据请求尺寸 计算 inSampleSize 参数
     * inSampleSize 参数指定了宽和高的缩放比例 例如 inSampleSize=1 那么就是原始尺寸
     * inSampleSize=2 那么 宽高就是 1/2  整体像素数减少 到1/4
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        //0 代表 不进行inSample 计算压缩
        if (reqWidth == 0 || reqHeight == 0) {
            return 1;
        }
        //Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.d(TAG, "origin,w=" + width + " h=" + height);
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            //Calculate the largest inSampleSize value that is a power of 2 and keep
            //both height adn width larger than the requested height ad width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize >= reqWidth)) {
                inSampleSize *= 2;
            }
        }
        Log.d(TAG, "sampleSize: " + inSampleSize);
        return inSampleSize;
    }
}
