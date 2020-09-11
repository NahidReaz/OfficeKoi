package com.example.officekoi;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.telecom.Call;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officekoi.historyRecyclerView.HistoryAdapter;
import com.example.officekoi.historyRecyclerView.HistoryObject;
import com.google.android.gms.common.api.Response;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.net.MediaType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;


public class HistoryActivity<Callback> extends AppCompatActivity {
    private String customerOrOwner, userId, spaceId;

    private RecyclerView mHistoryRecyclerView;
    private HistoryAdapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;

    private TextView mBalance;

    private Double Balance = 0.0;

    private Button mPayout;

    private EditText mPayoutEmail;
    private DatabaseError databaseError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mBalance = findViewById(R.id.balance);
        mPayout = findViewById(R.id.payout);
        mPayoutEmail = findViewById(R.id.payoutEmail);

        mHistoryRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);


        customerOrOwner = getIntent().getExtras().getString("customerOrOwner");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();

        if(customerOrOwner.equals("Owners")){
            mBalance.setVisibility(View.VISIBLE);
            mPayout.setVisibility(View.VISIBLE);
            mPayoutEmail.setVisibility(View.VISIBLE);
        }

        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payoutRequest();
            }
        });
    }

    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrOwner).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                        FetchSpaceInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void FetchSpaceInformation(String spaceKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(spaceKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener(){
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String spaceId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    Double spacePrice = 0.0;

                    if(dataSnapshot.child("timestamp").getValue() != null){
                        timestamp = Long.valueOf(dataSnapshot.child("timestamp").getValue().toString());
                    }

                    if(dataSnapshot.child("customerPaid").getValue() != null && dataSnapshot.child("ownerPaidOut").getValue() == null){
                        
                            spacePrice = Double.valueOf(dataSnapshot.child("price").getValue().toString());
                            Balance += spacePrice;
                            mBalance.setText("Balance: " + String.valueOf(Balance) + " $");
                        }
                    }


                    HistoryObject obj = new HistoryObject(spaceId, getDate(Timestamp));
                    resultsHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        public void onCancelled("databaseError")
        };


    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        String date = DateFormat.format("MM-dd-yyyy hh:mm", cal).toString();
        return date;
    }

    private ArrayList resultsHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultsHistory;
    }}




    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest() {
        progress = new ProgressDialog(this);
        progress.setTitle("Processing your payout");
        progress.setMessage("Please Wait...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            postData.put("email", mPayoutEmail.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = (RequestBody) RequestBody.create(MEDIA_TYPE, postData.toString());

        final TextLinks.Request request = new TextLinks.Request.Builder()
                .url("https://us-central1-uberapp-408c8.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Your Token")
                .addHeader("cache-control", "no-cache")
                .build();
        client.new Call(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                int responseCode = response.notify();


                if (response.isSuccessful()) {
                    switch (responseCode) {
                        case 200:
                            Snackbar.make(findViewById(R.id.action_settings), "Payout Successful!", Snackbar.LENGTH_LONG).show();
                            break;
                        case 501:
                            Snackbar.make(findViewById(R.id.action_settings), "Error: no payout available", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.action_settings), "Error: couldn't complete the transaction", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                } else
                    Snackbar.make(findViewById(R.id.action_settings), "Error: couldn't complete the transaction", Snackbar.LENGTH_LONG).show();

                progress.dismiss();
            }
        }