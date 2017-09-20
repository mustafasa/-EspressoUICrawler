package com.example.arifm2.expressouicrawler;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void kk(View view){
        TextView tv= (TextView)findViewById(R.id.textview);
        tv.setText(getResources().getString(R.string.SubStringTest,5));
    }
}
