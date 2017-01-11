package com.laudien.p1xelfehler.batterywarner.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

public class GraphDbHelper extends SQLiteOpenHelper {
    //private static final String TAG = "GraphDbHelper";
    private static GraphDbHelper instance;
    private static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 4; // if the version is changed, a new database will be created!
    public static final String TABLE_NAME = "ChargeCurve";
    public static final String TABLE_COLUMN_TIME = "time";
    public static final String TABLE_COLUMN_PERCENTAGE = "percentage";
    public static final String TABLE_COLUMN_TEMP = "temperature";
    public static final int TYPE_PERCENTAGE = 0;
    public static final int TYPE_TEMPERATURE = 1;
    private static final String CREATE_QUERY =
            "CREATE TABLE " + TABLE_NAME
                    + " (" + TABLE_COLUMN_TIME + " TEXT,"
                    + TABLE_COLUMN_PERCENTAGE + " INTEGER,"
                    + TABLE_COLUMN_TEMP + " INTEGER);";

    private GraphDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //Log.i(TAG, "Database created/opened!");
    }

    public static GraphDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GraphDbHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_QUERY);
        //Log.i(TAG, "Table created!");
    }


    public void addValue(long time, int percentage, int temperature) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TABLE_COLUMN_TIME, time);
        contentValues.put(TABLE_COLUMN_PERCENTAGE, percentage);
        contentValues.put(TABLE_COLUMN_TEMP, temperature);
        try {
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            getWritableDatabase().execSQL(CREATE_QUERY);
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        }
        //Log.i(TAG, "Added value (" + percentage + "%/" + time + "ms/" + temperature / 10 + "°C)");
        close();
    }

    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME);
        //Log.i(TAG, "Table reset!");
        close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    @Override
    public synchronized void close() {
        super.close();
        //Log.i(TAG, "Database closed!");
    }

    public LineGraphSeries<DataPoint>[] getGraphs (){
        LineGraphSeries<DataPoint>[] output = new LineGraphSeries[2];
        output[TYPE_PERCENTAGE] = new LineGraphSeries<>();
        output[TYPE_TEMPERATURE] = new LineGraphSeries<>();
        SQLiteDatabase database = getReadableDatabase();
        String[] columns = {
                GraphDbHelper.TABLE_COLUMN_TIME,
                GraphDbHelper.TABLE_COLUMN_PERCENTAGE,
                GraphDbHelper.TABLE_COLUMN_TEMP};
        Cursor cursor = database.query(GraphDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphDbHelper.TABLE_COLUMN_TIME + "), " + GraphDbHelper.TABLE_COLUMN_TIME);

        if (cursor.moveToFirst()){ // if the cursor has data
            double time, temperature;
            int percentage;
            do {
                time = (double) cursor.getLong(0) / 60000;
                percentage = cursor.getInt(1);
                temperature = (double) cursor.getInt(2)/10;
                output[TYPE_PERCENTAGE].appendData(new DataPoint(time, percentage), true, 1000);
                output[TYPE_TEMPERATURE].appendData(new DataPoint(time, temperature), true, 1000);
            } while (cursor.moveToNext()); // while the cursor has data
            cursor.close();
            close();
            output[TYPE_PERCENTAGE].setDrawBackground(true);
            output[TYPE_TEMPERATURE].setColor(Color.GREEN);
            return output;
        } else {
            cursor.close();
            close();
            return null;
        }
    }
}