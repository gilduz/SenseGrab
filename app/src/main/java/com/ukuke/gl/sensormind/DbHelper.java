package com.ukuke.gl.sensormind;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.DatabaseUtils;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    public static final String Samp_conf_id = "_id"; //int
    public static final String Samp_conf_name ="name"; //string
    public static final String Samp_conf_type ="type"; //int
    public static final String Samp_conf_service_name = "service";//string
    public static final String Samp_conf_path = "feed";//string
    public static final String Samp_conf_time ="time"; //int, sampling time
    public static final String Samp_conf_window ="window"; //int window for streaming sensors, must be less than sampling time
    public static final String Samp_conf_gps ="gps"; //int, not boolean in SQLite, 0 is false, 1 is true
    public static final String Samp_conf_active = "active";//int
    public static final String Samp_conf_date = "created";

    private SQLiteDatabase db;

    //Structure               : ||_id|| name || type ||service|| feed ||time||window||gps||active||created||
    //Data types on dbHelper  : ||int||String||String||String ||String||int || int  ||int|| int  || date  ||


    private static final String Create_Conf_Table =
            "create table "+Samp_conf_table+"("+Samp_conf_id+" integer primary key autoincrement,"+
                    Samp_conf_name+" text not null,"+Samp_conf_type+" integer not null,"+
                    Samp_conf_service_name+" text not null,"+Samp_conf_path+" text not null,"+
                    Samp_conf_time+" integer not null,"+
                    Samp_conf_window+" integer,"+Samp_conf_gps+" integer not null,"+
                    Samp_conf_active+" int not null,"+Samp_conf_date+" datetime"+")";

    public DbHelper(Context context) {
        super(context, DB_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase DataBase) {
        DataBase.execSQL(Create_Conf_Table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase DataBase, int oldVersion, int newVersion) {
        // delete tables
        DataBase.execSQL("drop table if exists"+Samp_conf_table);
        // create new tables
        onCreate(db);
    }


    //---------------------------Adapter methods---------------------------

    //---------------------------ADD OR MODIFY-----------------------------

    public boolean addOrUpdateConfiguration (ServiceManager.ServiceComponent.Configuration conf, ServiceManager.ServiceComponent comp, boolean isActive){
        int _id = conf.getDbId();
        if (_id>-1){
            // è già stato creato, modifico
            return this.updateConfiguration(conf,comp,isActive);
        } else if (_id==-1) {
            // è una nuova configurazione perché non gli è ancora stato assegnato un id, creo nuova configurazione
            return this.newConfiguration(conf,comp,isActive);
        } else return false;
    }

    //--------------------------NEW CONFIGURATION----------------------------

    private boolean newConfiguration (ServiceManager.ServiceComponent.Configuration conf, ServiceManager.ServiceComponent comp, boolean isActive) {
        // check if values are correct
        long interval = conf.getInterval();
        if (interval>=0){

            db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            //Structure               : ||_id|| name || type ||service|| feed ||time||window||gps||active||created||
            //Data types on dbHelper  : ||int||String||String||String ||String||int || int  ||int|| int  || date  ||

            values.put(Samp_conf_name, conf.getConfigurationName());
            values.put(Samp_conf_type, comp.getSensorType());
            values.put(Samp_conf_service_name, comp.getDysplayName());
            values.put(Samp_conf_path, conf.getPath());
            values.put(Samp_conf_time, interval);
            values.put(Samp_conf_window, conf.getWindow());
            values.put(Samp_conf_gps, convertBoolToInt(conf.getAttachGPS()));
            values.put(Samp_conf_active, isActive);
            values.put(Samp_conf_date, getDateTime());

            int _id = (int) db.insert(Samp_conf_table, null, values);
            conf.setDbId(_id);

            closeDb();
            return true;
        } else {
            return false;
        }
    }

    //------------------------CURSOR METHODS---------------------------

    public Cursor getConfCursorById(int id){
        // Remember to close the cursor on upper level after use
        db = this.getReadableDatabase();
        return db.rawQuery( "select * from "+Samp_conf_table+" where id = "+id, null );
    }

    public Cursor getConfCursorByName(String name){
        // Remember to close the cursor on upper level after use
        db = this.getReadableDatabase();
        return db.rawQuery( "select * from "+Samp_conf_table+" where "+Samp_conf_name+" = '"+name+"'", null );
    }

    public Cursor getAllConfCursor(){
        // Remember to close the cursor on upper level after use
        db = this.getReadableDatabase();
        return db.rawQuery( "select * from "+Samp_conf_table, null );
    }

    //--------------------------UPDATE METHODS-------------------------

    private boolean updateConfiguration (ServiceManager.ServiceComponent.Configuration conf, ServiceManager.ServiceComponent comp, boolean isActive) {
        // check if values are correct
        long interval = conf.getInterval();
        if (interval>=0){

            db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            //Structure         : ||_id|| name || type ||service|| feed ||time||window||gps||active||created||
            //Data types on dbHelper  : ||int||String||String||String ||String||int || int  ||int|| int  || date  ||

            values.put(Samp_conf_name, conf.getConfigurationName());
            values.put(Samp_conf_type, comp.getSensorType());
            values.put(Samp_conf_service_name, comp.getDysplayName());
            values.put(Samp_conf_path, conf.getPath());
            values.put(Samp_conf_time, interval);
            values.put(Samp_conf_window, conf.getWindow());
            values.put(Samp_conf_gps, convertBoolToInt(conf.getAttachGPS()));
            values.put(Samp_conf_active, isActive);

            db.update(Samp_conf_table, values,Samp_conf_id+" = ?",new String[] {Integer.toString(conf.getDbId())});
            closeDb();
            return true;
        } else {
            return false;
        }
    }

    //------------------------------DELETE METHODS------------------------------

    public int deleteConfigurationById(int id){
        db = this.getWritableDatabase();
        int del = db.delete(Samp_conf_table, Samp_conf_id + " = ?",new String[] { Integer.toString(id) });
        closeDb();
        return del;
    }

    public int deleteConfigurationByName(String Name){
        // Use deleteConfigurationById instead of this method,
        // because this deletes all configurations with the same name
        db = this.getWritableDatabase();
        int del = db.delete(Samp_conf_table, Samp_conf_name+" = ?",new String[] {Name});
        closeDb();
        return del;
    }

    //-----------------------------RETURN ARRAYLIST METHODS-------------------------

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsWithoutOrder(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table, null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByName(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+Samp_conf_name+" asc", null);
        closeDb();
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByType(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+Samp_conf_type+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfigurationsOrderedByTypeThenName(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+
                Samp_conf_type+" asc, "+Samp_conf_name+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_name)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfTypesWithoutOrder(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table, null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_type)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    @SuppressWarnings("unchecked")
    public ArrayList getAllConfTypesOrderedByType(){
        ArrayList array_list = new ArrayList();
        db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+Samp_conf_table+" order by "+
                Samp_conf_type+" asc", null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(Samp_conf_type)));
            res.moveToNext();
        }
        res.close();
        closeDb();
        return array_list;
    }

    //-------------------------------------POPULATE METHODS-----------------------------------

    public int populateServiceComponentListWithAllConfigurations(List<ServiceManager.ServiceComponent> serviceComponentList){
        // Deletes all entries of the list
        clearConfigurationList(serviceComponentList);
        // Get all configurations
        Cursor allConf = this.getAllConfCursor();
        int size = allConf.getCount();

        allConf.moveToFirst();

        for (int i = 0; i <size; i++) {
            // Get the id in service component list with the service name
            int id = getServiceComponentListIdByServiceName(allConf.getString(allConf.getColumnIndex(Samp_conf_service_name)), serviceComponentList);
            if (id>-1) {
                // There is a valid component in serviceComponentList, get the values from db
                String name = allConf.getString(allConf.getColumnIndex(Samp_conf_name));
                String path = allConf.getString(allConf.getColumnIndex(Samp_conf_path));
                long time = allConf.getLong(allConf.getColumnIndex(Samp_conf_time));
                int window = allConf.getInt(allConf.getColumnIndex(Samp_conf_window));
                int attachGPS = allConf.getInt(allConf.getColumnIndex(Samp_conf_gps));
                // Create new configuration with these values
                ServiceManager.ServiceComponent.Configuration conf = new ServiceManager.ServiceComponent.Configuration(name,path,time,window, covertIntToBool(attachGPS));
                // Get the id from db and set it in the configuration
                conf.setDbId(allConf.getInt(allConf.getColumnIndex(Samp_conf_id)));
                // avendo l'id di serviceComponentList vado ad aggiungere alla lista di configuration corrispondente
                serviceComponentList.get(id).addConfiguration(conf);
                // se la configurazione che sto guardando è attiva la metto in activeconfiguration
                if (allConf.getInt(allConf.getColumnIndex(Samp_conf_active))!=0){
                    serviceComponentList.get(id).setActiveConfiguration(conf);
                }
            }
            allConf.moveToNext();
        }
        allConf.close();
        closeDb();
        return size;
    }

    //----------------------------------------NUMERIC----------------------------------------

    public int numberOfConfigurations(){
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Samp_conf_table);
        closeDb();
        return num;
    }

    //---------------CLOSE METHOD----------------------

    public void closeDb() {
        if (db.isOpen()) {
            db.close();
        }
    }


    //-----------------------------------ACCESSORIES-------------------------------------

    private int getServiceComponentListIdByServiceName(String name, List<ServiceManager.ServiceComponent> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getDysplayName().equals(name)){ return i;}
        }
        return -1;
    }

    private void clearConfigurationList(List<ServiceManager.ServiceComponent> list){
        for (int i = 0; i < list.size(); i++) {
            list.get(i).configurationList.clear();
            list.get(i).setActiveConfiguration(null);
        }
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private int convertBoolToInt (boolean val) {
        return val ? 1 : 0;
    }

    private boolean covertIntToBool(int k) {
        return (k!=0);
    }
}