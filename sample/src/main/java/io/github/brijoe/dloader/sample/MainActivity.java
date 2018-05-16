package io.github.brijoe.dloader.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import io.github.brijoe.DLoader;


public class MainActivity extends Activity implements AbsListView.OnScrollListener {

    private static final String TAG = "MainActivity";

    private List<String> mUrList = new ArrayList<String>();
    private GridView mImageGridView;
    private BaseAdapter mImageAdapter;

    private boolean mIsGridViewIdle = true;
    private int mImageWidth = 0;
    private boolean mIsWifi = false;
    private boolean mCanGetBitmapFromNetWork = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intData();
        intView();
    }

    private void intData() {
        String[] imageUrls = {
                "http://ww3.sinaimg.cn/mw600/006XNEY7gy1frdgbu781fj30hs0qaq7c.jpg",
                "http://wx4.sinaimg.cn/mw600/0076BSS5ly1frdg6ggbn3j30p010s7wh.jpg",
                "http://wx1.sinaimg.cn/mw600/0076BSS5ly1frdfpp5o0wj30ja0rn4qp.jpg",
                "http://wx1.sinaimg.cn/mw600/0076BSS5ly1frdf9g5xtij30c50kktkb.jpg",
                "http://ww3.sinaimg.cn/mw600/006XNEY7gy1frdesbo6n8j30hs0qo0v2.jpg",
                "http://ww3.sinaimg.cn/mw600/0073tLPGgy1frdeshyjp3j30du0kugnm.jpg",
                "http://ww3.sinaimg.cn/mw600/0073tLPGgy1frdes5lwv7j30dw0j4myj.jpg",
                "http://wx2.sinaimg.cn/mw600/0076BSS5ly1frdepwgh30j30lc0w077t.jpg",
                "http://ww3.sinaimg.cn/mw600/0073tLPGgy1frdel2d6txj30u00jz11q.jpg",
                "http://ww3.sinaimg.cn/mw600/006XNEY7gy1frdebi06ebj30hs0qoacl.jpg",
                "http://pic.dbw.cn/0/01/33/59/1335968_847719.jpg",
                "http://a.hiphotos.baidu.com/image/pic/item/a8773912b31bb051a862339c337adab44bede0c4.jpg",
                "http://h.hiphotos.baidu.com/image/pic/item/f11f3a292df5e0feeea8a30f5e6034a85edf720f.jpg",
                "http://img0.pconline.com.cn/pconline/bizi/desktop/1412/ER2.jpg",
                "http://pic.58pic.com/58pic/11/25/04/91v58PIC6Xy.jpg",
                "http://img3.3lian.com/2013/c2/32/d/101.jpg",
                "http://pic25.nipic.com/20121210/7447430_172514301000_2.jpg",
                "http://img02.tooopen.com/images/20140320/sy_57121781945.jpg",
                "http://wx3.sinaimg.cn/mw600/0076BSS5ly1frd7171ndnj30dw0hcqeh.jpg",
                "http://ww3.sinaimg.cn/mw600/006XNEY7gy1frd654efujj30hs0qojv5.jpg"
        };
        for (String url : imageUrls) {
            mUrList.add(url);
        }
        int screenWidth = MyUtils.getScreenMetrics(this).widthPixels;
        int space = (int) MyUtils.dp2px(this, 20f);
        mImageWidth = (screenWidth - space) / 3;
        mIsWifi = MyUtils.isWifi(this);
        if (mIsWifi) {
            mCanGetBitmapFromNetWork = true;
        }
    }

    private void intView() {
        mImageGridView = (GridView) findViewById(R.id.gridView);
        mImageAdapter = new ImageAdapter(this);
        mImageGridView.setAdapter(mImageAdapter);
        mImageGridView.setOnScrollListener(this);

        if (!mIsWifi) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("初次使用会从网络下载大概5MB的图片，确认要下载吗？");
            builder.setTitle("注意");
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCanGetBitmapFromNetWork = true;
                    mImageAdapter.notifyDataSetChanged();
                }
            });
            builder.setNegativeButton("否", null);
            builder.show();
        }
    }


    private class ImageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Drawable mDefaultBitmapDrawable;

        private ImageAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            mDefaultBitmapDrawable = context.getResources().getDrawable(R.mipmap.image_default);
        }

        @Override
        public int getCount() {
            return mUrList.size();
        }

        @Override
        public String getItem(int position) {
            return mUrList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.image_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            ImageView imageView = holder.imageView;
            final String tag = (String) imageView.getTag();
            final String uri = getItem(position);
            if (!uri.equals(tag)) {
                imageView.setImageDrawable(mDefaultBitmapDrawable);
            }
            if (mIsGridViewIdle && mCanGetBitmapFromNetWork) {
                imageView.setTag(uri);
               DLoader.with(MainActivity.this).load(uri,imageView,mImageWidth,mImageWidth);
            }
            return convertView;
        }

    }

    private static class ViewHolder {
        public ImageView imageView;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            mIsGridViewIdle = true;
            mImageAdapter.notifyDataSetChanged();
        } else {
            mIsGridViewIdle = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // ignored

    }
}
