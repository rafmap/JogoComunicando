package com.example.client_servertcp_pingpong.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.client_servertcp_pingpong.R;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickServer(View v){
        startActivity(new Intent(this, ServerTCP.class));
    }
    public void onClickClient(View v){
        startActivity(new Intent(this, ClienteTCP.class));
    }
}