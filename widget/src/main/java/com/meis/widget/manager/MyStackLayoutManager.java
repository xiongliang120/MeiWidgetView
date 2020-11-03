package com.meis.widget.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.meis.widget.utils.RecycleViewRefectUtil;

import java.util.List;

/**
 * 相关api:
 * measureChildWithMargins(View child, int widthUsed, int heightUsed),测量
 * getDecoratedMeasuredHeight(View child), 获取测量高度,包括ItemDecorated
 * getDecoratedMeasuredWidth(View child), 获取测量宽度
 * layoutDecoratedWithMargins(View child, int left, int top, int right,int bottom), 对view 进行布局
 * detachAndScrapAttachedViews(Recycler recycler), 将屏幕中item分离出来,放到临时缓存中
 * recycler.getViewForPosition(int position), 获取指定position的view
 * recycler.recycleView(viewHolder.itemView), 对itemView 进行回收
 *
 *
 * 布局:
 * onLayoutChild 布局
 * detachAndScrapAttachedView(recycler)
 * 循环通过recycler.getViewForPosition()
 * measureChildWithMargins()
 * layoutDecorateWithMargins()
 *
 * 滑动(分两段进行计算)：
 * canScrollHorizontally()
 * scrollHorizontalBy() 布局
 *
 * 第一个可见View完全消失的偏移量 fisrstChildCompleteScrollLength.
 * View 到屏幕正中心的偏移量 onceCompeleteScrollLength.
 * 累计偏移量 offsetTotal, 即 dx 累加。
 * view 距离到正中心偏移量的百分比  fraction
 *
 * 第一种场景: offsetTotal 小于 fisrstChildCompleteScrollLength
 * firstVisiPosition =0
 * fisrstChildCompleteScrollLength = getWidth()/2 + childWidth/2
 * onceCompeleteScrollLength = fisrstChildCompleteScrollLength
 * fraction =  offsetToal%onceCompeleteScrollLength/(onceCompeleteScrollLength*1.0f)
 *
 * 第二种场景: offsetTotal 大于等于 fisrstChildCompleteScrollLength
 * onceCompeleteScrollLength = childWidth + viewGrap;
 * firstVisiPosition = Math.abs(offsetTotal - fisrstChildCompleteScrollLength)/onceCompeleteScrollLength
 * fraction = offsetTotal%onceCompeleteScrollLength/(onceCompeleteScrollLength*1.0f)
 *
 * 边界:
 * 右滑到左边界, dx < 0 并且 offsetTotal <=0 时, 将 offsetTotal = 0,dx=0
 * 左滑到右边界, dx > 0 并且 offsetTotal >= getMaxOffset(), getMaxOffset = itemCount *(childWidth+viewGrap),
 * 将offset = getMaxOffset, dx=0.
 *
 * 回收：
 * 滑动之后
 * recycler.getScrapList()  获取屏幕是上的Item
 * 遍历集合,recycler.recycleView(viewHolder.itemView) 进行回收
 *
 * 滑动偏移矫正:
 * 获取应该选中的position
 * Math.abs(offset)/(childWidth+viewGrap); Math.ads(offset)%(childWidth+viewGrap), selectPosition 应该四舍五入即跟childWidth/2 比较。
 * 获取矫正偏移量 (childWidth+viewGrap)*selectPosition - offset
 * ValueAnimator.ofFloat(0,distance), 在update回调中 更新offset,并且重新requestLayout,进行位置矫正。
 *
 * 点击偏移矫正：
 *
 *
 *
 * 参考链接：
 * https://blog.csdn.net/u012551350/article/details/93971801
 *
 *
 */

public class MyStackLayoutManager extends RecyclerView.LayoutManager {
    private int firstVisiblePositin; //屏幕第一个可见的item

    private int lastVisiblePosition; //屏幕最后一个可见的item

    private int firstChildCompleteScrollLength = -1;  //第一个view 的偏移量

    private int onceCompleteScrollLength  =-1;  //一次完整聚焦滑动的偏移量

    private int offsetTotal = 0;  //水平方向的整体偏移量

    private int viewGrap = 30; //View 之间的间距

    private int childWidth = 0; //子View的宽度

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * 布局Item
     * @param recycler
     * @param state
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
         if(state.getItemCount() == 0){
             removeAndRecycleAllViews(recycler);
             return;
         }
         detachAndScrapAttachedViews(recycler);

         fill(recycler,state,0);
    }

    /**
     * 开始布局
     * @param recycler
     * @param state
     */
    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state,int dx){
        if(dx < 0){ //右滑
            if(offsetTotal <=0){ //到达左边界
                offsetTotal = 0;
                dx = 0;
            }
        }

        if(dx > 0){ //左滑
            if(offsetTotal >= getMaxOffset()){ //到达右边界
                offsetTotal = getMaxOffset();
                dx =0;
            }
        }

        detachAndScrapAttachedViews(recycler);

        int statX = 0;


        float fraction = 0f; //移动占onceCompleteScrollLength 的比例
        float normalViewOffset = 0f;

        if(onceCompleteScrollLength == -1){ //计算初始数据
            View itemView = recycler.getViewForPosition(0);
            measureChildWithMargins(itemView,0,0);
            childWidth = getDecoratedMeasuredWidth(itemView);
        }

        firstChildCompleteScrollLength = getWidth()/2 + childWidth/2;

        //分两种情况
        if(offsetTotal >= firstChildCompleteScrollLength){   //总移动量大于第一个View偏移量
            firstVisiblePositin = Math.abs(offsetTotal - firstChildCompleteScrollLength) / onceCompleteScrollLength + 1;
            onceCompleteScrollLength = childWidth + viewGrap;
            fraction = Math.abs(offsetTotal - firstChildCompleteScrollLength) % onceCompleteScrollLength/(onceCompleteScrollLength*1.0f);
            statX = viewGrap;
        }else{  //总移动量大于第一个View偏移量
            firstVisiblePositin = 0;
            onceCompleteScrollLength = firstChildCompleteScrollLength;
            fraction = (Math.abs(offsetTotal) % onceCompleteScrollLength)/(onceCompleteScrollLength*1.0f);
            statX = (getWidth() - childWidth)/2;
        }

        normalViewOffset = onceCompleteScrollLength*fraction;

        statX -= normalViewOffset; //计算firstVisiPosition 坐标

        lastVisiblePosition = state.getItemCount()-1;
        for (int i=firstVisiblePositin;i<=lastVisiblePosition;i++){
            View itemView = recycler.getViewForPosition(i);

            addView(itemView);

            measureChildWithMargins(itemView,0,0);

            int left = statX;
            int top = getPaddingTop();
            int right = left + getDecoratedMeasuredWidth(itemView);
            int bottom = top + getDecoratedMeasuredHeight(itemView);

            layoutDecoratedWithMargins(itemView,left,top,right,bottom);

            statX += (childWidth + viewGrap);
            if (statX > getWidth() - getPaddingRight()){ //只创建可见的item数量
                lastVisiblePosition = i;
                break;
            }
        }

        recycleView(recycler);

        RecycleViewRefectUtil.getCacheSize(recycler);

        return dx;  //做边界判断,到达边界了返回0
    }

    /**
     * 回收view
     */
    public void recycleView(RecyclerView.Recycler recycler){
        List<RecyclerView.ViewHolder> viewHolderList  = recycler.getScrapList();
        for (int i=0;i<viewHolderList.size();i++){
            RecyclerView.ViewHolder viewHolder = viewHolderList.get(i);
            recycler.recycleView(viewHolder.itemView);
        }
    }

    /**
     * 获取最大位移量
     * @return
     */
    public int getMaxOffset(){
       if(childWidth == 0 || getItemCount() == 0) return 0;
       return (childWidth+viewGrap)*(getItemCount() -1);
    }


    /**
     * 支持水平滚动
     * @return
     */
    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    /**
     * 处理滚动逻辑
     * @param dx
     * @param recycler
     * @param state
     * @return
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        offsetTotal += dx;

        dx = fill(recycler,state,dx);

        return dx;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if(state == RecyclerView.SCROLL_STATE_IDLE){ //停止滑动
            startCorrectPosition(findShouldSelectPosition());
        }else if(state == RecyclerView.SCROLL_STATE_DRAGGING){ //手指按下,开始拖拽

        }
    }


    /**
     * 获取应该选中的position
     * @return
     */
    public int findShouldSelectPosition(){
        if(childWidth == 0 || getItemCount() == 0){
            return -1;
        }
        int selectPosition = offsetTotal/(childWidth+viewGrap);
        int remainder = offsetTotal % (childWidth+viewGrap);

        if(remainder > (childWidth+viewGrap)/2){
            if(selectPosition + 1 < getItemCount()){
                selectPosition = selectPosition +1;
            }
        }
        return selectPosition;
    }

    /**
     * 滑动矫正位置
     */
    public void startCorrectPosition(int selectPosition){
        int distance = (selectPosition * (childWidth + viewGrap)) - offsetTotal; //有正负

        int duration = 300;
        final int startOffset = offsetTotal;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,distance);
        valueAnimator.setDuration(duration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
               float value = (float) valueAnimator.getAnimatedValue();
               offsetTotal = (int) (startOffset + value);
               requestLayout();  //执行onLayoutChildren
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        valueAnimator.start();

    }
}
