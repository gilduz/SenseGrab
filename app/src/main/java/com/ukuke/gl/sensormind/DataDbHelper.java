package com.ukuke.gl.sensormind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.ukuke.gl.sensormind.support.DataSample;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.lang.System.*;

/**
 *   Created by Leonardo on 30/01/2015.
 */
public class DataDbHelper extends SQLiteOpenHelper {

    // DB name
    public static final String DB_name = "DB_Sensormind"; //TODO salvare i dati su un Db diverso o sullo stesso? nel caso cambiare questo nome

    // Tables
    public static final String Data_table = "Data";

    // Data columns
    public static final String Data_id = "id"; //int
    public static final String Data_value1 ="value1"; //real
    public static final String Data_value2 ="value2"; //real
    public static final String Data_value3 ="value3"; //real
    public static final String Data_arrayCount ="arrayCount"; //int
    public static final String Data_long ="longitude"; //real //TODO: Gildo, il real è 8 bytes in SQLite, la stringa direi di più, confermi?
    public static final String Data_lat ="latitude"; //real
    public static final String Data_timestamp ="timestamp"; //int , SQLite has an integer format stored in 1, 2, 3, 4, 6, or 8 bytes
    public static final String Data_idFeed ="idFeed"; //String
    public static final String Data_sent ="sent"; //int, 1 for sent, 0 if it has to be sent

    // String to create table
    private static final String Create_Data_Table =
            "create table "+Data_table+"("+Data_id+" integer primary key autoincrement,"+
                    Data_value1+" real not null,"+Data_value2+" real,"+
                    Data_value3+" real,"+Data_arrayCount+" integer,"+
                    Data_long+" real,"+Data_lat+" real,"+
                    Data_timestamp+" integer"+Data_idFeed+" text not null"+
                    Data_sent+" integer not null"+")";

    public DataDbHelper(Context context) {
        super(context, DB_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //db.execSQL(Create_Type_Table);
        db.execSQL(Create_Data_Table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete tables
        // db.execSQL("drop table if exists"+Samp_type_table);
        db.execSQL("drop table if exists "+Data_table);
        // create new tables
        onCreate(db);
    }

    // Adapter methods

    // Insert data
    public boolean insertSingleData (DataSample data) {
        //Float is an object and can be set as null, float cannot be set as null; same for Long and long
        //arrayCount must be: -1 for non array values, index for arrays
        //there must be at least value1
        Float value1 = data.getValue_1();
        String feed = data.getFeedPath();

        if (value1 != null & feed != null){

            SQLiteDatabase db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            values.put(Data_value1, value1);
            values.put(Data_value2, data.getValue_2());
            values.put(Data_value3, data.getValue_3());
            values.put(Data_arrayCount, data.getArrayCount());
            values.put(Data_long, data.getLongitude());
            values.put(Data_lat, data.getLatitude());
            values.put(Data_timestamp, data.getTimestamp());
            values.put(Data_idFeed, feed);
            values.put(Data_sent, 0);

            db.insert(Data_table, null, values);
            db.close();
            return true;
        } else {
            return false;
        }
    }

    public boolean getListData (List<DataSample> listDataSample) {
        //TODO: Da fare
        return true;
    }


    public boolean insertListData (List<DataSample> listDataSample) {
        //TODO: Da fare
//        SQLiteDatabase db = this.getWritableDatabase(); //open database
//
//        for (int i = 0; i < listDataSample.size(); i++) {
//            Float value1 = listDataSample.get(i).getValue_1();
//            String feed = listDataSample.get(i).getFeedPath();
//
//            if (value1 != null & feed != null){
//
//                ContentValues values = new ContentValues();
//
//                values.put(Data_value1, value1);
//                values.put(Data_value2, listDataSample.get(i).getValue_2());
//                values.put(Data_value3, listDataSample.get(i).getValue_3());
//                values.put(Data_arrayCount, listDataSample.get(i).getArrayCount());
//                values.put(Data_long, listDataSample.get(i).getLongitude());
//                values.put(Data_lat, listDataSample.get(i).getLatitude());
//                values.put(Data_timestamp, listDataSample.get(i).getTimestamp());
//                values.put(Data_idFeed, feed);
//                values.put(Data_sent, 0);
//
//                db.insert(Data_table, null, values);
//            } else {
//                return false;
//            }
//        }
//        db.close();
        return true;
    }

    public boolean insertArrayOfData (List<DataSample> array){
        SQLiteDatabase db = this.getWritableDatabase(); //open database
        ContentValues values = new ContentValues();
        int i = 0;
        int wrongData = 0;
        int size = array.size();

        for (i=0; i<size; i+=1){
            Float value1 = array.get(i).getValue_1();
            if (value1 != null) {
                values.put(Data_value1, value1);
                values.put(Data_value2, array.get(i).getValue_2());
                values.put(Data_value3, array.get(i).getValue_3());
                values.put(Data_arrayCount, array.get(i).getArrayCount());
                values.put(Data_long, array.get(i).getLongitude());
                values.put(Data_lat, array.get(i).getLatitude());
                values.put(Data_timestamp, array.get(i).getTimestamp());
                values.put(Data_idFeed, array.get(i).getFeedPath());
                values.put(Data_sent, 0);

                db.insert(Data_table, null, values);
            } else {
                wrongData += 1;
            }
        }

        return wrongData < size;
    }

    public int numberOfEntries(){
        SQLiteDatabase db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table);
        db.close();
        return num;
    }

/*
    public Cursor getConfCursorById(int id){
        // TODO Remember to close the cursor on upper level after use
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from "+Samp_conf_table+" where id = "+id, null );
        return res;
    }

    public Cursor getConfCursorByName(String name){
        // TODO Remember to close the cursor on upper level after use
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from "+Samp_conf_table+" where "+Samp_conf_name+" = '"+name+"'", null );
        return res;
    }

    public Cursor getAllConfCursor(){
        // TODO Remember to close the cursor on upper level after use
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from "+Samp_conf_table, null );
        return res;
    }

    // UPDATE BY ID
    public boolean updateConfigurationById(int id, String name, int type, int time, String unit, int window, boolean gps) {
        //TODO on upper level: check if window is greater than sampling time for streaming sensors
        // check if values are correct
        if (time>0 & (unit.equals("sec") | unit.equals("min"))){

            int time_unit = covertTimeUnit(unit);
            int intGPS = covertGps(gps);

            SQLiteDatabase db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            values.put(Samp_conf_name, name);
            values.put(Samp_conf_type, type);
            values.put(Samp_conf_time, time);
            values.put(Samp_conf_time_unit, time_unit);
            values.put(Samp_conf_window, window);
            values.put(Samp_conf_gps, intGPS);
            values.put(Samp_conf_date, getDateTime());

            db.update(Samp_conf_table, values,"id = ? ", new String[] { Integer.toString(id) } );
            db.close();
            return true;
        } else {
            return false;
        }
    }

    // UPDATE BY NAME
    public boolean updateConfigurationByName(String name, int type, int time, String unit, int window, boolean gps) {
        //TODO on upper level: check if window is greater than sampling time for streaming sensors
        // check if values are correct
        if (time>0 & (unit.equals("sec") | unit.equals("min"))){

            int time_unit = covertTimeUnit(unit);
            int intGPS = covertGps(gps);

            SQLiteDatabase db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            values.put(Samp_conf_name, name);
            values.put(Samp_conf_type, type);
            values.put(Samp_conf_time, time);
            values.put(Samp_conf_time_unit, time_unit);
            values.put(Samp_conf_window, window);
            values.put(Samp_conf_gps, intGPS);
            values.put(Samp_conf_date, getDateTime());

            db.update(Samp_conf_table, values,Samp_conf_name+" = ? ", new String[] {name} );
            db.close();
            return true;
        } else {
            return false;
        }
    }

    // DELETE BY ID
    public int deleteConfigurationById(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        int del = db.delete(Samp_conf_table,"id = ?",new String[] { Integer.toString(id) });
        db.close();
        return del;
    }

    // DELETE BY NAME
    public int deleteConfigurationByName(String Name){
        SQLiteDatabase db = this.getWritableDatabase();
        int del = db.delete(Samp_conf_table,Samp_conf_name+" = ?",new String[] {Name});
        db.close();
        return del;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsWithoutOrder(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();

        //Log.d("DBHelper", "Sono arrivato a prima del Cursor");

        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table, null);

        //Log.d("DBHelper", "Scorro il Cursor");

        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }

        //Log.d("DBHelper", "Chiudo il cursor");

        res.close();

        //Log.d("DBHelper", "Chiudo il db");

        db.close();

        Log.d("DBHelper", "Ritorno");

        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByName(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+Samp_conf_name+" asc", null);
        db.close();
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByType(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+Samp_conf_type+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        db.close();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByTypeThenName(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+
                Samp_conf_type+" asc, "+Samp_conf_name+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        db.close();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfTypesWithoutOrder(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table, null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_type)));
            res.moveToNext();
        }
        res.close();
        db.close();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfTypesOrderedByType(){
        ArrayList array_list = new ArrayList();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+
                Samp_conf_type+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_type)));
            res.moveToNext();
        }
        res.close();
        db.close();
        return array_list;
    }



    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private int covertTimeUnit (String unit) {
        int time_unit = 0; //default seconds
        if (unit.equals("min")) {
            time_unit = 1;
        }
        return time_unit;
    }

    private int covertGps (boolean gps) {
        int intGPS = 0; //default false
        if (gps) {
            intGPS = 1;
        }
        return intGPS;
    }*/

}