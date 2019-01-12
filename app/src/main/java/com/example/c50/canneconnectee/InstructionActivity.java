package com.example.c50.canneconnectee;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class InstructionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);

        String instruction = getIntent().getExtras().getString("instruct");
        TextView textView = findViewById(R.id.svtv);
        textView.setText(instruction);

    }
}
