package com.ukuke.gl.sensormind;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.DatabaseUtils;
import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

/**
 *   Created by Leonardo on 21/01/2015.
 */
public class DbHelper extends SQLiteOpenHelper {

    // DB name
    public static final String DB_name = "DB_Sensormind";

    // Tables
    public static final String Samp_conf_table = "Samp_conf";

    // Sampling configurations columns
    public static final String Samp_conf_id = "id"; //int
    public static final String Samp_conf_name ="name"; //string
    public static final String Samp_conf_type ="type"; //int
    public static final String Samp_conf_time ="time"; //int, sampling time
    public static final String Samp_conf_time_unit ="unit"; //int, 0 for sec, 1 for min
    public static final String Samp_conf_window ="window"; //int window for streaming sensors, must be less than sampling time
    public static final String Samp_conf_gps ="gps"; //int, not boolean in SQLite, 0 is false, 1 is true
    public static final String Samp_conf_date = "created";


    private static final String Create_Conf_Table =
            "create table "+Samp_conf_table+"("+Samp_conf_id+" integer primary key autoincrement,"+
                    Samp_conf_name+" text not null,"+Samp_conf_type+" integer not null,"+
                    Samp_conf_time+" integer not null,"+Samp_conf_time_unit+" integer not null,"+
                    Samp_conf_window+" integer,"+Samp_conf_gps+" integer not null,"+
                    Samp_conf_date+" datetime"+")";

    public DbHelper(Context context) {
        super(context, DB_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //db.execSQL(Create_Type_Table);
        db.execSQL(Create_Conf_Table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete tables
        // db.execSQL("drop table if exists"+Samp_type_table);
        db.execSQL("drop table if exists"+Samp_conf_table);
        // create new tables
        onCreate(db);
    }


    // Adapter methods

    //NEW CONFIGURATION
    public boolean newConfiguration (String name, int type, int time, String unit, int window, boolean gps) {
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

            db.insert(Samp_conf_table, null, values);
            db.close();
            return true;
        } else {
            return false;
        }
    }

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
        } else return false;
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
        // TODO: Bisogna aggiungere anche il tipo di sensore, altrimenti due sensori diversi non possono avere una configurazione con lo stesso nome. In generale sarebbe meglio se tutti i metodi prendessero come input direttamente l'oggetto ServiceComponent e l'oggetto Configuration
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
// TODO: convertire nel database l'unità base come ms salvati in int e nell'interfaccia convertire il valore salvato in ms, sec, min, hour
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
    }

}