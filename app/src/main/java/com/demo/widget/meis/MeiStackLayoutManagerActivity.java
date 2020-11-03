package com.demo.widget.meis;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.demo.widget.R;
import com.meis.widget.manager.MyStackLayoutManager;
import com.meis.widget.manager.StackLayoutManager;

import java.util.Random;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/27
 */
public class MeiStackLayoutManagerActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;

    MyStackLayoutManager stackLayoutManager;

    Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mei_stack_activity);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRecyclerView = findViewById(R.id.recycler);
        // new StackLayoutManager(this,-56) 堆叠模式
        mRecyclerView.setLayoutManager(stackLayoutManager = new MyStackLayoutManager());

        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_stack_card,
                        viewGroup, false);
                view.setBackgroundColor(Color.argb(255,
                        new Random().nextInt(255),
                        new Random().nextInt(255),
                        new Random().nextInt(255)));
                final BaseViewHolder holder = new BaseViewHolder(view);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = mRecyclerView.getChildAdapterPosition(holder.itemView);
                        stackLayoutManager.startCorrectPosition(position);
                    }
                });
                Log.i("xiongliang","onCreateViewHolder");
                return holder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
                ((TextView) viewHolder.itemView.findViewById(R.id.tv)).setText("" + i);
                Log.i("xiongliang","onBindViewHolder");

            }

            @Override
            public int getItemViewType(int position) {
                return 1;
            }

            @Override
            public int getItemCount() {
                return 18;
            }
        });

        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getAdapter().notifyItemChanged(4);
            }
        },2000);

    }

    class BaseViewHolder extends RecyclerView.ViewHolder {

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
