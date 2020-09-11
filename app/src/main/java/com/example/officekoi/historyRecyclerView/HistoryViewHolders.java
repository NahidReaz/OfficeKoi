package com.example.officekoi.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.officekoi.HistorySingleActivity;
import com.example.officekoi.R;



public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView spaceId;
    public TextView time;
    public HistoryViewHolders(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        spaceId = (TextView) itemView.findViewById(R.id.spaceId);
        time = (TextView) itemView.findViewById(R.id.time);
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("spaceId", spaceId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
