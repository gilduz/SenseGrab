package com.ukuke.gl.sensormind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
//import android.os.Environment; // for saving on external memory

import com.ukuke.gl.sensormind.support.DataSample;

//import java.io.File; // for saving on external memory
import java.util.ArrayList;
import java.util.List;

/**
 *   Created by Leonardo on 30/01/2015.
 */
public class DataDbHelper extends SQLiteOpenHelper {

    private static final String TAG = DataDbHelper.class.getSimpleName();

    // DB name, comment which you won't use
    // Internal database
    public static final String DB_name = "DB_Sensormind_Data";
    // External database
    //public static final String DB_name = Environment.getExternalStorageDirectory()
    //        + File.separator + "Sensormind" + File.separator + "DB_Sensormind_Data";

    // Tables
    public static final String Data_table = "Data";

    // Data columns
    public static final String Data_id = "_id"; //int
    public static final String Data_value1 = "value1"; //real
    public static final String Data_value2 = "value2"; //real
    public static final String Data_value3 = "value3"; //real
    public static final String Data_arrayCount = "arrayCount"; //int
    public static final String Data_long = "longitude"; //real
    public static final String Data_lat = "latitude"; //real
    public static final String Data_timestamp = "timestamp"; //int , SQLite has an integer format stored in 1, 2, 3, 4, 6, or 8 bytes
    public static final String Data_idFeed = "idFeed"; //String
    public static final String Data_sent = "sent"; //int, 1 for sent, 0 if it has to be sent

    // String to create table
    private static final String Create_Data_Table =
            "create table " + Data_table + "(" + Data_id + " integer primary key autoincrement, " +
                    Data_value1 + " real not null, " + Data_value2 + " real, " +
                    Data_value3 + " real, " + Data_arrayCount + " integer, " +
                    Data_long + " real, " + Data_lat + " real, " +
                    Data_timestamp + " integer, " + Data_idFeed + " text not null, " +
                    Data_sent + " integer not null" + ")";

    private SQLiteDatabase db;

    public DataDbHelper(final Context context) {
        super(context, DB_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(Create_Data_Table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete tables
        db.execSQL("drop table if exists " + Data_table);
        // create new tables
        onCreate(db);
    }

    // Adapter methods

    //---------------INSERT METHODS----------------------

    public boolean insertSingleData(DataSample data) {
        //Float is an object and can be set as null, float cannot be set as null; same for Long and long
        //arrayCount must be: -1 for non array values, index for arrays
        //there must be at least value1
        Float value1 = data.getValue_1();
        String feed = data.getFeedPath();

        if (value1 != null & feed != null) {

            db = this.getWritableDatabase(); //open database
            ContentValues values = new ContentValues();

            values.put(Data_value1, value1);
            values.put(Data_value2, data.getValue_2());
            values.put(Data_value3, data.getValue_3());
            values.put(Data_arrayCount, data.getArrayCount());
            values.put(Data_long, data.getLongitude());
            values.put(Data_lat, data.getLatitude());
            values.put(Data_timestamp, data.getTimestamp());
            values.put(Data_idFeed, feed);
            values.put(Data_sent, "0");

            db.insert(Data_table, null, values);
            closeDb();
            return true;
        } else {
            return false;
        }
    }

    public boolean insertListOfData(List<DataSample> array) {
        db = this.getWritableDatabase(); //open database
        ContentValues values = new ContentValues();
        int i = 0;
        int wrongData = 0;
        int size = array.size();

        for (i = 0; i < size; i += 1) {
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
                values.put(Data_sent, "0");

                db.insert(Data_table, null, values);
            } else {
                wrongData += 1;
            }
        }

        closeDb();
        return wrongData < size;
    }


    //---------------NUMERIC METHODS----------------------

    public int numberOfEntries() {
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table);
        closeDb();
        return num;
    }

    public int numberOfUnsentEntries() {
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table, Data_sent + " = 0");
        closeDb();
        return num;
    }

    public int numberOfSentEntries() {
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table, Data_sent + " = 1");
        closeDb();
        return num;
    }

    public int numberOfUnsentArrays() {
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table, Data_sent+" = 0 and "+Data_arrayCount + " = 0");
        closeDb();
        return num;
    }

    public int numberOfCompleteUnsentArraysOnFeed(String feed) {
        // This method has to be modified because it returns
        // the right value for arrays of the same sensor type
        // but return an higher value if there are arrays
        // of different sensor type, that's why sometimes
        // getFirstArray... returns an empty list
        db = this.getReadableDatabase();
        int num = (int) DatabaseUtils.queryNumEntries(db, Data_table, Data_idFeed + " = '" + feed +
                "' and " + Data_arrayCount + " = 0 and "+ Data_sent + " = 0");
        closeDb();
        return num - 1;
    }

    //---------------CURSOR METHODS----------------------

    public Cursor getAllUnsentCursor() {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_sent + " = 0", null);
        return res;
    }

    public Cursor getAllSentCursor() {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_sent + " = 1", null);
        return res;
    }

    public Cursor getAllUnsentSingleDataCursor() {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_sent + " = 0 and " + Data_arrayCount + " = -1", null);
        return res;
    }

    public Cursor getAllUnsentArrayDataCursor() {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_sent + " = 0 and " + Data_arrayCount + " > -1", null);
        return res;
    }

    public Cursor getAllUnsentArrayFirstElementCursor() {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_sent + " = 0 and " + Data_arrayCount + " = 0", null);
        return res;
    }

    private Cursor getAllPossibleElementsOfAnArray(Cursor currentRow) {
        String feed = currentRow.getString(currentRow.getColumnIndex(Data_idFeed));
        int id = currentRow.getInt(currentRow.getColumnIndex(Data_id));
        String query = "select * from " + Data_table + " where " + Data_idFeed +
                " = '" + feed + "' and " + Data_id + " >= " + Integer.toString(id) +
                " and " + Data_arrayCount + " >= 0";
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery(query, null);
        return res;
    }

    private Cursor getAllPossibleElementOfAnArrayByFeedAndId(String feed, int id) {
        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + Data_table + " where " + Data_idFeed +
                " = '" + feed + "' and " + Data_id + " > " + Integer.toString(id) +
                " and " + Data_arrayCount + " >= 0", null);
        return res;
    }

    //---------------LIST OF DATASAMPLES METHODS----------------------

    public List<DataSample> getAllUnsentDataSamples() {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentCursor();
        List<DataSample> list = new ArrayList<>();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        while (!res.isAfterLast()) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
        return list;
    }

    public List<DataSample> getFirstNUnsentDataSamples(int N) {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentCursor();
        List<DataSample> list = new ArrayList<>();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        for (int i = 0; i < N; i++) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
        return list;
    }

    public List<DataSample> getAllUnsentSingleDataSamples() {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentSingleDataCursor();
        List<DataSample> list = new ArrayList<>();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        while (!res.isAfterLast()) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
        return list;
    }

    public List<DataSample> getFirstUnsentArrayDataSamples() {
        db = this.getReadableDatabase();
        Cursor unsentArrayFirstElDb = this.getAllUnsentArrayFirstElementCursor();
        List<DataSample> list = new ArrayList<>();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;
        int numArrays = unsentArrayFirstElDb.getCount();

        //Log.d(TAG,"unsentArrayFirstElDb ha " + unsentArrayFirstElDb.getCount()+ " elementi");

        unsentArrayFirstElDb.moveToFirst();
        //eseguo il ciclo for alla ricerca del primo array completo
        for (int i = 0; i < (numArrays - 1); i++) {
            //Imposto il feed per il quale cerco un array completo
            //Log.d("DataDbHelper","Sono dentro al primo for");
            feed = unsentArrayFirstElDb.getString(unsentArrayFirstElDb.getColumnIndex(Data_idFeed));
            //Controllo se esiste un array completo
            if (numberOfCompleteUnsentArraysOnFeed(feed) > 0) {
                //Log.d("DataDbHelper","Per il feed corrente c'è almeno un array completo");
                //esiste un array completo, ciclo fino a che non arrivo alla fine (NON devo avere diverse robe nello stesso feed)
                Cursor currentArrayCursor = getAllPossibleElementsOfAnArray(unsentArrayFirstElDb);
                currentArrayCursor.moveToFirst();
                //eseguo il ciclo almeno la prima volta, e dopo faccio il check dell'arrayCount
                //alla fine del primo ciclo nella condizione mi ritrovo 1>0
                //il ciclo finisce quando arrivo al primo elemento dell'array successivo
                do {
                    //Log.d("DataDbHelper","Sono dentro al ciclo do while");

                    value1 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value1));
                    value2 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value2));
                    value3 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value3));
                    arrayCount = currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_arrayCount));
                    timestamp = currentArrayCursor.getLong(currentArrayCursor.getColumnIndex(Data_timestamp));
                    longitude = currentArrayCursor.getDouble(currentArrayCursor.getColumnIndex(Data_long));
                    latitude = currentArrayCursor.getDouble(currentArrayCursor.getColumnIndex(Data_lat));
                    id = currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_id));

                    data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
                    data.setDbId(id);
                    list.add(data);
                    currentArrayCursor.moveToNext();
                }
                while (currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_arrayCount)) > 0);
                //Ho finito di completare la lista e la restituisco
                currentArrayCursor.close();
                unsentArrayFirstElDb.close();
                closeDb();
                return list;
            } else {//Non esiste un array completo per questo feed, guardo il successivo
                unsentArrayFirstElDb.moveToNext();
            }
        }

        unsentArrayFirstElDb.close();
        closeDb();
        return list;
    }

    //---------------POPULATE METHODS--------------------

    // These methods are the same of upper arraylist,
    // but these populate a list created externally

    public void populateAllUnsentDataSamples(List<DataSample> list) {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentCursor();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        while (!res.isAfterLast()) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
    }

    public void populateFirstNUnsentDataSamples(int N, List<DataSample> list) {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentCursor();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        for (int i = 0; i < N; i++) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
    }

    public void populateAllUnsentSingleDataSamples(List<DataSample> list) {
        db = this.getReadableDatabase();
        Cursor res = this.getAllUnsentSingleDataCursor();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;

        res.moveToFirst();
        while (!res.isAfterLast()) {
            feed = res.getString(res.getColumnIndex(Data_idFeed));
            value1 = res.getFloat(res.getColumnIndex(Data_value1));
            value2 = res.getFloat(res.getColumnIndex(Data_value2));
            value3 = res.getFloat(res.getColumnIndex(Data_value3));
            arrayCount = res.getInt(res.getColumnIndex(Data_arrayCount));
            timestamp = res.getLong(res.getColumnIndex(Data_timestamp));
            longitude = res.getDouble(res.getColumnIndex(Data_long));
            latitude = res.getDouble(res.getColumnIndex(Data_lat));
            id = res.getInt(res.getColumnIndex(Data_id));

            data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
            data.setDbId(id);
            list.add(data);
            res.moveToNext();
        }
        res.close();
        closeDb();
    }

    public void populateFirstUnsentArrayDataSamples(List<DataSample> list) {
        db = this.getReadableDatabase();
        Cursor unsentArrayFirstElDb = this.getAllUnsentArrayFirstElementCursor();
        DataSample data;
        String feed;
        Float value1, value2, value3;
        Double longitude, latitude;
        Long timestamp;
        int arrayCount, id;
        int numArrays = unsentArrayFirstElDb.getCount();

        //Log.d(TAG,"unsentArrayFirstElDb ha " + unsentArrayFirstElDb.getCount()+ " elementi");

        unsentArrayFirstElDb.moveToFirst();
        //eseguo il ciclo for alla ricerca del primo array completo
        for (int i = 0; i < (numArrays - 1); i++) {
            //Imposto il feed per il quale cerco un array completo
            //Log.d("DataDbHelper","Sono dentro al primo for");
            feed = unsentArrayFirstElDb.getString(unsentArrayFirstElDb.getColumnIndex(Data_idFeed));
            //Controllo se esiste un array completo
            if (numberOfCompleteUnsentArraysOnFeed(feed) > 0) {
                //Log.d("DataDbHelper","Per il feed corrente c'è almeno un array completo");
                //esiste un array completo, ciclo fino a che non arrivo alla fine (NON devo avere diverse robe nello stesso feed)
                Cursor currentArrayCursor = getAllPossibleElementsOfAnArray(unsentArrayFirstElDb);
                currentArrayCursor.moveToFirst();
                //eseguo il ciclo almeno la prima volta, e dopo faccio il check dell'arrayCount
                //alla fine del primo ciclo nella condizione mi ritrovo 1>0
                //il ciclo finisce quando arrivo al primo elemento dell'array successivo
                do {
                    //Log.d("DataDbHelper","Sono dentro al ciclo do while");

                    value1 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value1));
                    value2 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value2));
                    value3 = currentArrayCursor.getFloat(currentArrayCursor.getColumnIndex(Data_value3));
                    arrayCount = currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_arrayCount));
                    timestamp = currentArrayCursor.getLong(currentArrayCursor.getColumnIndex(Data_timestamp));
                    longitude = currentArrayCursor.getDouble(currentArrayCursor.getColumnIndex(Data_long));
                    latitude = currentArrayCursor.getDouble(currentArrayCursor.getColumnIndex(Data_lat));
                    id = currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_id));

                    data = new DataSample(feed, value1, value2, value3, arrayCount, timestamp, longitude, latitude);
                    data.setDbId(id);
                    list.add(data);
                    currentArrayCursor.moveToNext();
                }
                while (currentArrayCursor.getInt(currentArrayCursor.getColumnIndex(Data_arrayCount)) > 0);
                //Ho finito di completare la lista e la restituisco
                currentArrayCursor.close();
                unsentArrayFirstElDb.close();
                closeDb();
            } else {//Non esiste un array completo per questo feed, guardo il successivo
                unsentArrayFirstElDb.moveToNext();
            }
        }

        unsentArrayFirstElDb.close();
        closeDb();
    }

    //---------------DELETE METHODS----------------------

    public int deleteAllDataSamples() {
        db = this.getWritableDatabase();
        int del = db.delete(Data_table, null, null);
        closeDb();
        return del;
    }

    public int deleteAllUnsentDataSamples() {
        db = this.getWritableDatabase();
        int del = db.delete(Data_table, Data_sent + " = 0", null);
        closeDb();
        return del;
    }

    public int deleteAllSentDataSamples() {
        db = this.getWritableDatabase();
        int del = db.delete(Data_table, Data_sent + " = 1", null);
        closeDb();
        return del;
    }

    public int deleteSentDataSamplesBeforeTimestamp(Long timestamp) {
        db = this.getWritableDatabase();
        int del = db.delete(Data_table, Data_sent + " = ? and " + Data_timestamp + " < ?", new String[]{"1", Long.toString(timestamp)});
        closeDb();
        return del;
    }

    //---------------SET SENT METHODS----------------------

    public boolean setSentListOfDataSamples(List<DataSample> array) {
        int size = array.size(), rightSet = 0;
        db = this.getWritableDatabase();
        for (int i = 0; i < size; i++) {
            rightSet += internalSetSentDataSampleById(array.get(i).getDbId(), db);
        }
        closeDb();
        return rightSet == size; //return true if all samples are set as sent
    }

    private int internalSetSentDataSampleById(int id, SQLiteDatabase DataBase) {
        ContentValues value = new ContentValues();
        value.put(Data_sent, "1");
        return DataBase.update(Data_table, value, Data_id + " = " + Integer.toString(id), null);
    }

    public int setSentDataSampleById(int id) {
        db = this.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put(Data_sent, "1");
        int res = db.update(Data_table, value, Data_id + " = " + Integer.toString(id), null);
        closeDb();
        return res;
    }

    //---------------CLOSE METHOD----------------------

    public void closeDb() {
        if (db.isOpen()) {
            db.close();
        }
    }
}