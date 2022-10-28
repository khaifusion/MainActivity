package com.example.gps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class NumberActivity extends AppCompatActivity {

    EditText inputEt;
    Button nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);

        init();
        SettingListener();

    }

    private void init() {
        inputEt = findViewById(R.id.inputEt);
        nextBtn = findViewById(R.id.nextBtn);
    }

    private void SettingListener() {
        // '다음' 버튼에 클릭 이벤트 적용
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String carNum = inputEt.getText().toString();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("carNum", carNum);
                startActivity(intent);
            }
        });
    }


}