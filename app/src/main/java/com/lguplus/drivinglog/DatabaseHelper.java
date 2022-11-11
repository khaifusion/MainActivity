package com.lguplus.drivinglog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    static String TAG = "===== Driving Log =====";
    public static String DB_NAME = "drivingLog.db";
    public static int VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists user("
                + "_id text PRIMARY KEY NOT NULL,"
                + "name text NOT NULL,"
                + "password text NOT NULL,"
                + "team text)";

        db.execSQL(sql);
        Log.d(TAG,"테이블 생성 완료 : user");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion > 1){
            db.execSQL("DROP TABLE IF EXISTS user");
        }
    }
}
