package com.ukuke.gl.sensormind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static java.lang.System.*;

/**
 *   Created by Leonardo on 21/01/2015.
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
    public static final String Data_idFeed ="idFeed"; //int for comparison with another table
    public static final String Data_sent ="sent"; //int, 1 for sent, 0 if it has to be sent

    // String to create table
    private static final String Create_Data_Table =
            "create table "+Data_table+"("+Data_id+" integer primary key autoincrement,"+
                    Data_value1+" real not null,"+Data_value2+" real,"+
                    Data_value3+" real,"+Data_arrayCount+" integer,"+
                    Data_long+" real,"+Data_lat+" real,"+
                    Data_timestamp+" integer"+Data_idFeed+" integer not null"+
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
        db.execSQL("drop table if exists"+Data_table);
        // create new tables
        onCreate(db);
    }


    // Adapter methods

    // Insert data
    public boolean insertSingleData (Float value1, Float value2, Float value3, int arrayCount, Float GPSlong, Float GPSlat, Long timestamp, int idFeed) {
        //Float is an object and can be set as null, float cannot be set as null; same for Long and long
        //arrayCount must be: -1 for non array values, index for arrays
        //there must be at least value1
        if (value1 != null & idFeed >= 0){

            SQLiteDatabase db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            values.put(Data_value1, value1);
            values.put(Data_value2, value2);
            values.put(Data_value3, value3);
            values.put(Data_arrayCount, arrayCount);
            values.put(Data_long, GPSlong);
            values.put(Data_lat, GPSlat);
            values.put(Data_timestamp, timestamp);
            values.put(Data_idFeed, idFeed);
            values.put(Data_sent, 0);

            db.insert(Data_table, null, values);
            db.close();
            return true;
        } else {
            return false;
        }
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
        // TODO: Non funziona... e se non ci sono configurazioni? controllare anche altri getallconf...
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

    public int numberOfConfigurations(){
        SQLiteDatabase db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Samp_conf_table);
        db.close();
        return num;
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