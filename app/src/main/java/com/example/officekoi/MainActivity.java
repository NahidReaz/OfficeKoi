package com.example.officekoi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button mOwner, mCustomer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOwner = (Button) findViewById(R.id.owner);
        mCustomer = (Button) findViewById(R.id.customer);

        startService(new Intent(MainActivity.this, onAppKilled.class));
        mOwner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OwnerLoginActivity.class);
                startActivity(intent);
                finish();

            }
        });

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CustomerLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
