package com.example.camera_location;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class DBHandler {

    public static final class Contract {
        public static final String DB_NAME = "com.example.camera_location.todolist.db";
        public static final int DB_VERSION = 1;

        public static final class TaskEntry implements BaseColumns {
            public static final String TABLE = "gallery";
            public static final String COL_ID = "id";
            public static final String COL_IMAGE_PATH = "image_path";
            public static final String COL_LATITUDE = "latitude";
            public static final String COL_LONGITUDE = "longitude";
        }
    }

    public static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context) {
            super(context, Contract.DB_NAME, null, Contract.DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + Contract.TaskEntry.TABLE + " ( "
                    + Contract.TaskEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Contract.TaskEntry.COL_IMAGE_PATH + " TEXT NOT NULL, "
                    + Contract.TaskEntry.COL_LATITUDE + " TEXT NOT NULL, "
                    + Contract.TaskEntry.COL_LONGITUDE + " TEXT NOT NULL);";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + Contract.TaskEntry.TABLE);
            onCreate(db);
        }

        public List<String> getAllImagePaths() {
            List<String> imagePaths = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(Contract.TaskEntry.TABLE,
                    new String[]{Contract.TaskEntry.COL_IMAGE_PATH},
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex(Contract.TaskEntry.COL_IMAGE_PATH);
                imagePaths.add(cursor.getString(idx));
            }
            cursor.close();
            db.close();
            return imagePaths;
        }

        public List<String> getAllImageLatitude() {
            List<String> latitudeValue = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(Contract.TaskEntry.TABLE,
                    new String[]{Contract.TaskEntry.COL_LATITUDE},
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex(Contract.TaskEntry.COL_LATITUDE);
                latitudeValue.add(cursor.getString(idx));
            }
            cursor.close();
            db.close();
            return latitudeValue;
        }

        public List<String> getAllImageLongitude() {
            List<String> longitudeValue = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(Contract.TaskEntry.TABLE,
                    new String[]{Contract.TaskEntry.COL_LONGITUDE},
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex(Contract.TaskEntry.COL_LONGITUDE);
                longitudeValue.add(cursor.getString(idx));
            }
            cursor.close();
            db.close();
            return longitudeValue;
        }
    }
}
