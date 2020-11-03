package com.meis.widget.utils;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * 获取RecycleView 内部的各种缓存数据
 */
public class RecycleViewRefectUtil {
    public static void getCacheSize(RecyclerView.Recycler recyclerInstance){
        try {
            Field mRecycler =
                    Class.forName("android.support.v7.widget.RecyclerView").getDeclaredField("mRecycler");
            mRecycler.setAccessible(true);
//            RecyclerView.Recycler recyclerInstance = (RecyclerView.Recycler) mRecycler.get(recyclerView);

            Class<?> recyclerClass = Class.forName(mRecycler.getType().getName());
            Field mViewCacheMax = recyclerClass.getDeclaredField("mViewCacheMax");
            Field mAttachedScrap = recyclerClass.getDeclaredField("mAttachedScrap");
            Field mChangedScrap = recyclerClass.getDeclaredField("mChangedScrap");
            Field mCachedViews = recyclerClass.getDeclaredField("mCachedViews");
            Field mRecyclerPool = recyclerClass.getDeclaredField("mRecyclerPool");
            mViewCacheMax.setAccessible(true);
            mAttachedScrap.setAccessible(true);
            mChangedScrap.setAccessible(true);
            mCachedViews.setAccessible(true);
            mRecyclerPool.setAccessible(true);


            int mViewCacheSize = (int) mViewCacheMax.get(recyclerInstance);
            ArrayList<RecyclerView.ViewHolder> mAttached =
                    (ArrayList<RecyclerView.ViewHolder>) mAttachedScrap.get(recyclerInstance);
            ArrayList<RecyclerView.ViewHolder> mChanged =
                    (ArrayList<RecyclerView.ViewHolder>) mChangedScrap.get(recyclerInstance);
            ArrayList<RecyclerView.ViewHolder> mCached =
                    (ArrayList<RecyclerView.ViewHolder>) mCachedViews.get(recyclerInstance);
            RecyclerView.RecycledViewPool recycledViewPool =
                    (RecyclerView.RecycledViewPool) mRecyclerPool.get(recyclerInstance);

            Class<?> recyclerPoolClass = Class.forName(mRecyclerPool.getType().getName());

            Log.e("xiongliang", "mAttachedScrap（一缓） size is:" + mAttached.size() + ", \n" + "mCachedViews（二缓） max size is:" + mViewCacheSize + ","
                    + getMCachedViewsInfo(mCached) + "四级缓存的大小="+recycledViewPool.getRecycledViewCount(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static  String getRVPoolInfo(Class<?> aClass, RecyclerView.RecycledViewPool recycledViewPool) {
        try {
            Field mScrapField = aClass.getDeclaredField("mScrap");
            mScrapField.setAccessible(true);
            SparseArray mScrap = (SparseArray) mScrapField.get(recycledViewPool);

            Class<?> scrapDataClass =
                    Class.forName("android.support.v7.widget.RecyclerView$RecycledViewPool$ScrapData");
            Field mScrapHeapField = scrapDataClass.getDeclaredField("mScrapHeap");
            Field mMaxScrapField = scrapDataClass.getDeclaredField("mMaxScrap");
            mScrapHeapField.setAccessible(true);
            mMaxScrapField.setAccessible(true);
            String s = "\n mRecyclerPool（四缓） info:  ";
            for (int i = 0; i < mScrap.size(); i++) {
                ArrayList<RecyclerView.ViewHolder> item =
                        (ArrayList<RecyclerView.ViewHolder>) mScrapHeapField.get(mScrap.get(i));
                for (int j = 0; j < item.size(); j++) {
                    if (j == item.size() - 1) {
                        s += ">>> ";
                    } else if (j == 0) {
                        s += "mScrap[" + i + "] max size is:" + (mMaxScrapField.get(mScrap.get(i)));
                    }
                    s += "mScrap[" + i + "] 中的 mScrapHeap[" + j + "] info is:" + item.get(j) + "\n";
                }
            }
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return "  ";
        }
    }

    private static String getMCachedViewsInfo(ArrayList<RecyclerView.ViewHolder> viewHolders) {
        String s = "mCachedViews（二缓） info:  ";
        if (viewHolders.size() > 0) {
            int i = 0;
            for (; i < viewHolders.size(); i++) {
                s += "\n mCachedViews[" + i + "] is " + viewHolders.get(i).toString();
            }

            // append
            if (i == 0) {
                s += "      ";
            } else if (i == 1) {
                s += "    ";
            } else if (i == 2) {
                s += "  ";
            }
        } else {
            s += "      ";
        }
        return s
                + " \n";
    }
}
