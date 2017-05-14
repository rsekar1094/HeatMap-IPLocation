package com.example.rajrajas.heatmap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.util.DateTime;
import com.google.maps.android.heatmaps.WeightedLatLng;


import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by rajrajas on 5/11/2017.
 */

class Db_controller extends SQLiteOpenHelper {
     Db_controller(Context applicationcontext) {
        super(applicationcontext, "heatmap_new.db", null, 1);
    }

    //Creates Table
    @Override
    public void onCreate(SQLiteDatabase database) {

        String query;

        //Table Id: 0
        query = "CREATE TABLE IPLocation (IpAddress TEXT,IpLatitude TEXT,IpLongitude TEXT,IpTimeStamp TEXT)";
        database.execSQL(query);

        //Table Id: 1
        query = "CREATE TABLE IPLocation_insert (InsertDate DATETIME,Count INTEGER)";
        database.execSQL(query);

    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
        String query;
        query = "DROP TABLE IF EXISTS IPLocation";
        database.execSQL(query);

        query = "DROP TABLE IF EXISTS IPLocation_insert";
        database.execSQL(query);

    }

    void insert_IPLocation_insert(String res) {
        ContentValues values = new ContentValues();
        SQLiteDatabase database = this.getWritableDatabase();
        values.put("InsertDate", getDateTime());
        values.put("Count", res);
        database.insert("IPLocation_insert", null, values);

    }

    void insert_IPLocation(String[] res) {
        ContentValues values = new ContentValues();
        SQLiteDatabase database = this.getWritableDatabase();
        values.put("IpAddress", res[0]);
        values.put("IpLatitude", res[1]);
        values.put("IpLongitude", res[2]);
        values.put("IpTimeStamp", res[3]);
        database.insert("IPLocation", null, values);
    }

    List<LatLng> get_IPLocation(int temp) {
        List<LatLng> latLngs = new ArrayList<LatLng>();

        Calendar cal = Calendar.getInstance();

        String temp_date = get_query_string("select IpTimeStamp from IPLocation ORDER BY IpTimeStamp asc LIMIT 1");

        Timestamp first_timestamp = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {

            date = dateFormat.parse(temp_date);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (temp != 0) {

            first_timestamp = new Timestamp(date.getTime());
            cal.setTimeInMillis(first_timestamp.getTime());

            cal.add(Calendar.SECOND, temp * 30);
            first_timestamp = new Timestamp(cal.getTime().getTime());
        }

        Timestamp second_timestamp = new Timestamp(date.getTime());
        cal.setTimeInMillis(second_timestamp.getTime());


        if (temp == 0)
            cal.add(Calendar.SECOND, 30);
        else
            cal.add(Calendar.SECOND, temp * 30 + 30);
        second_timestamp = new Timestamp(cal.getTime().getTime());

        String query;

        if (temp == 0)
            query = "Select IpLatitude,IpLongitude from IPLocation WHERE IpTimeStamp between '" + temp_date + "' and '" + second_timestamp + "'";
        else
            query = "Select IpLatitude,IpLongitude from IPLocation WHERE IpTimeStamp between '" + first_timestamp + "' and '" + second_timestamp + "'";


        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            //for(int i=0;i<5;i++)
            while (!cursor.isAfterLast()) {
                latLngs.add(new LatLng(Double.parseDouble(cursor.getString(cursor.getColumnIndex("IpLatitude"))), Double.parseDouble(cursor.getString(cursor.getColumnIndex("IpLongitude")))));
                cursor.moveToNext();
            }
        }

        return latLngs;

    }


    List<LatLng> get_IPLocation_1()
    {
        List<LatLng> latLngs = new ArrayList<LatLng>();

        String query;
            query = "Select IpLatitude,IpLongitude from IPLocation ORDER BY IpTimeStamp ASC";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast())
            {
                double lat = Double.parseDouble(cursor.getString(cursor.getColumnIndex("IpLatitude")));
                double lon = Double.parseDouble(cursor.getString(cursor.getColumnIndex("IpLongitude")));
                latLngs.add(new LatLng(lat, lon));
                cursor.moveToNext();
            }
        }

        return latLngs;

    }


    String get_query_string(String selectQuery) {
        String res;
        SQLiteDatabase database1 = this.getWritableDatabase();
        Cursor cursor = database1.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        res = "";
        while (!cursor.isAfterLast()) {
            res = cursor.getString(0);
            cursor.moveToNext();
        }
        cursor.close();
        //database1.close();
        return res;
    }


    String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


}
