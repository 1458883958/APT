package com.wdl.aptdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.wdl.apt_annotation.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_btn)
    Button btn;
    @BindView(R.id.tv_t)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivityViewBinding.bind(this);
        //textView.setText("Succeed.....................");
    }
}
