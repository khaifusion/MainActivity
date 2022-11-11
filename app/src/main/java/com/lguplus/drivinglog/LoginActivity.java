package com.lguplus.drivinglog;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

public class LoginActivity extends AppCompatActivity {

    static String TAG = "===== Driving Log =====";

    public static String DB_NAME = "drivingLogDB.db";
    public static String TABLE_NAME = "user";
    public static int VERSION = 1;

    private Button loginBtn;
    private AppCompatEditText idAppCompatEditText;
    private AppCompatEditText passwordAppCompatEditText;

    DatabaseHelper dbHelper;
    SQLiteDatabase database;
    String tableName;

    private String id;
    private String password;
    private boolean loginFlag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 변수 초기화
        init();

        // DB생성
        createDatabase(DB_NAME);

        // Table생성
        createTable(TABLE_NAME);

        // 레코드 insert
        if(checkRecord() == 0){
            insertRecord();
        }


        // 로그인 버튼 클릭 시 동작 정의
        loginBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                executeQuery();

                if(loginFlag){
                    Toast.makeText(getApplicationContext(), id + " 님 반갑습니다", Toast.LENGTH_LONG).show();
                    loginFlag = false;
                    Intent intent = new Intent(getApplicationContext(), NumberActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "아이디/패스워드를 다시 확인해주세요", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public void init(){
        idAppCompatEditText = findViewById(R.id.username_textField);
        passwordAppCompatEditText = findViewById(R.id.password_textField);
        loginBtn = findViewById(R.id.loginBtn);
    }

    private void createDatabase(String name){
        //database = openOrCreateDatabase(name, MODE_PRIVATE, null);
        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();
        Log.d(TAG, "데이터베이스 생성 완료 : " + name);
    }

    private void createTable(String name){
        if(database == null){
            Toast.makeText(this, "DB를 생성해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        database.execSQL("create table if not exists " + name + "("
                + "_id text PRIMARY KEY NOT NULL,"
                + "name text NOT NULL,"
                + "password text NOT NULL,"
                + "team text)");
        tableName = name;
        Log.d(TAG, "테이블 생성 완료 : " + tableName);
    }

    private void insertRecord(){
        if(database == null){
            Toast.makeText(this, "DB를 생성해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if(tableName == null){
            Toast.makeText(this, "테이블을 생성해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        database.execSQL("insert into " + tableName
                        + "(_id, name, password, team)"
                        + " values "
                        + "('1', '김종민', '1', 'SOHO/SME개발팀'), "
                        + "('kimjongmin', '김종민', '02150155', 'SOHO/SME개발팀'), "
                        + "('jyoung02', '박지영', '02161181', 'SOHO/SME개발팀')");
        Log.d(TAG, "레코드 추가됨");
    }

    public void executeQuery(){
        id = idAppCompatEditText.getText().toString();
        password = passwordAppCompatEditText.getText().toString();
        Log.d(TAG, "id/pw : " + id + ", " + password);
        Cursor cursor = database.rawQuery("select * from user where _id='"+id+"' and " + "password='"+password+"'", null);
        int recordCount = cursor.getCount();

        Log.d(TAG, "레코드 개수 : " + recordCount);

        if(recordCount > 0){
            loginFlag = true;
        }

        for(int i=0; i<recordCount; i++){
            cursor.moveToNext();
            String id = cursor.getString(0);
            String name = cursor.getString(1);
            String password = cursor.getString(2);
            String team = cursor.getString(3);

            Log.d(TAG, i + "번째 : " + id + ", " + name + ", " + password + ", " + team);
        }
        cursor.close();
    }

    public int checkRecord(){
        Cursor cursor = database.rawQuery("select * from user", null);

        return cursor.getCount();
    }


}