package com.tony.autojs.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteHelper {
    private final String databaseName;
    private List<String> tableEntriesCreate;
    private final List<String> tableAlters;
    private final int version;

    private ReaderDBHelper readerDBHelper;
    private SQLiteDatabase writableSQLiteDatabase;
    private SQLiteDatabase readableSQLiteDatabase;

    private Context context;

    public SQLiteHelper(String databaseName, List<String> tableEntriesCreate, List<String> tableAlters, int version) {
        this.databaseName = databaseName;
        this.tableEntriesCreate = tableEntriesCreate;
        if (tableEntriesCreate == null) {
            this.tableEntriesCreate = new ArrayList<>();
        }
        this.tableAlters = tableAlters;
        this.version = version;
        initOpenHelper();
    }
    public SQLiteHelper(String databaseName, List<String> tableEntriesCreate, List<String> tableAlters, int version, Context context) {
        this.context = context;
        this.databaseName = databaseName;
        this.tableEntriesCreate = tableEntriesCreate;
        if (tableEntriesCreate == null) {
            this.tableEntriesCreate = new ArrayList<>();
        }
        this.tableAlters = tableAlters;
        this.version = version;
        initOpenHelper();
    }

    public SQLiteHelper(String databaseName, int version) {
        this(databaseName, null, null, version);
    }

    public SQLiteHelper(String databaseName, int version, Context context) {
        this(databaseName, null, null, version);
    }

    public SQLiteHelper(String databaseName) {
        this(databaseName, null, null, 1);
    }

    public SQLiteHelper(String databaseName, Context context) {
        this(databaseName, null, null, 1, context);
    }

    public void initOpenHelper() {
        if (readerDBHelper != null) {
            readerDBHelper.close();
            readableSQLiteDatabase.close();
            writableSQLiteDatabase.close();
        }
        if (tableAlters == null || tableAlters.size() <= 0) {
            readerDBHelper = new ReaderDBHelper(databaseName, tableEntriesCreate, version, context);
        } else {
            readerDBHelper = new ReaderDBHelper(databaseName, tableEntriesCreate, tableAlters, version, context);
        }
        readableSQLiteDatabase = readerDBHelper.getReadableDatabase();
        writableSQLiteDatabase = readerDBHelper.getWritableDatabase();
    }

    public static int getDbVersion(String fileName) {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(fileName, null)) {
            return db.getVersion();
        } catch (Exception e) {
            return -1;
        }
    }

    public void updateDbInfo(List<String> tableAlters, int version) {
        if (readerDBHelper != null) {
            readerDBHelper.close();
            readableSQLiteDatabase.close();
            writableSQLiteDatabase.close();
        }
        readerDBHelper = new ReaderDBHelper(this.databaseName, this.tableEntriesCreate, tableAlters, version, context);
        readableSQLiteDatabase = readerDBHelper.getReadableDatabase();
        writableSQLiteDatabase = readerDBHelper.getWritableDatabase();
    }


    public void close() {
        if (readerDBHelper != null) {
            readerDBHelper.close();
            readableSQLiteDatabase.close();
            writableSQLiteDatabase.close();
        }
    }

    public SQLiteDatabase getReadableDb() {
        return readableSQLiteDatabase;
    }

    public SQLiteDatabase getWriteableDb() {
        return writableSQLiteDatabase;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    private ContentValues buildValues(Map<String, Object> contentObj) {
        ContentValues contentValues = new ContentValues();
        for (Map.Entry<String, Object> entry : contentObj.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                contentValues.put(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                contentValues.put(entry.getKey(), (Integer) value);
            } else if (value instanceof Boolean) {
                contentValues.put(entry.getKey(), (Boolean) value);
            } else if (value instanceof Short) {
                contentValues.put(entry.getKey(), (Short) value);
            } else if (value instanceof Double) {
                contentValues.put(entry.getKey(), (Double) value);
            } else if (value instanceof Long) {
                contentValues.put(entry.getKey(), (Long) value);
            } else if (value instanceof Float) {
                contentValues.put(entry.getKey(), (Float) value);
            } else if (value instanceof byte[]) {
                contentValues.put(entry.getKey(), (byte[]) value);
            } else {
                contentValues.put(entry.getKey(), String.valueOf(value));
            }
        }
        return contentValues;
    }

    public long insert(String tableName, Map<String, Object> contentObj) {
        return writableSQLiteDatabase.insertOrThrow(tableName, null, buildValues(contentObj));
    }

    public long insertWithModel(ModelIdentify modelIdentify, Object source) {
        return insert(modelIdentify.getTableName(), modelIdentify.convertToMap(source));
    }

    public int deleteById(String tableName, String id) {
        return writableSQLiteDatabase.delete(tableName, "ID = ?", new String[]{id});
    }

    public int deleteBySql(String tableName, String sql, String[] args) {
        return writableSQLiteDatabase.delete(tableName, sql, args);
    }

    public int updateById(String tableName, String id, Map<String, Object> contentObj) {
        return writableSQLiteDatabase.update(tableName, buildValues(contentObj), "ID = ?", new String[]{id});
    }

    public int updateByIdWithModel(ModelIdentify modelIdentify, String id, Object source) {
        return updateById(modelIdentify.getTableName(), id, modelIdentify.convertToMap(source));
    }

    public int updateBySql(String tableName, String whereClause, Map<String, Object> contentObj, String[] args) {
        return writableSQLiteDatabase.update(tableName, buildValues(contentObj), whereClause, args);
    }

    public int updateBySqlWithModel(ModelIdentify modelIdentify, String whereClause, Object source, String[] args) {
        return updateBySql(modelIdentify.getTableName(), whereClause, modelIdentify.convertToMap(source), args);
    }

    public Object selectById(ModelIdentify modelIdentify, String id, ModelFromCursorConverter converter) {
        Cursor cursor = readableSQLiteDatabase.query(modelIdentify.getTableName(), modelIdentify.getFullColumns(), "ID = ?", new String[]{id}, null, null, null);
        Object result = null;
        if (cursor.moveToFirst()) {
            result = converter.convert(cursor);
        }
        cursor.close();
        return result;
    }

    public Cursor rawQuery(String sql, String[] args) {
        return readableSQLiteDatabase.rawQuery(sql, args);
    }

    public List<Object> rawQueryWithModel(String sql, String[] args, ModelFromCursorConverter converter) {
        Cursor cursor = readableSQLiteDatabase.rawQuery(sql, args);
        List<Object> resultList = new ArrayList<>();
        while (cursor.moveToNext()) {
            resultList.add(converter.convert(cursor));
        }
        cursor.close();
        return resultList;
    }

    public void rawExecute(String sql, Object[] args) {
        writableSQLiteDatabase.execSQL(sql, args);
    }

    public interface ModelFromCursorConverter {
        Object convert(Cursor cursor);
    }

    public interface DataAdapter {
        Object convert(Object value);
    }

    public static abstract class ModelIdentify {
        private String tableName;
        private List<ColumnField> columnFields;

        public ModelIdentify(String tableName, List<ColumnField> columnFields) {
            this.tableName = tableName;
            this.columnFields = columnFields;
        }

        public ModelIdentify() {
        }

        public String[] getFullColumns() {
            String[] columns = new String[columnFields.size()];
            for (int i = 0; i < columnFields.size(); i++) {
                columns[i] = columnFields.get(i).getColumnName();
            }
            return columns;
        }

        public Map<String, Object> convertToMap(Object target) {
            Map<String, Object> paramsMap = new HashMap<>();
            for (ColumnField columnField : columnFields) {
                Object value = getValueByKey(columnField.getFieldName(), target);
                if (value != null) {
                    paramsMap.put(columnField.getColumnName(), columnField.dataAdapter.convert(value));
                }
            }
            return paramsMap;
        }

        public String getBaseColumnList() {
            StringBuilder sb = new StringBuilder();
            for (ColumnField columnField : columnFields) {
                sb.append(columnField.columnName).append(',');
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

        public abstract Object getValueByKey(String fieldName, Object target);

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List<ColumnField> getColumnFields() {
            return columnFields;
        }

        public void setColumnFields(List<ColumnField> columnFields) {
            this.columnFields = columnFields;
        }
    }

    public static class ColumnField {
        private String columnName;
        private String fieldName;
        private DataAdapter dataAdapter;

        public ColumnField(String columnName, String fieldName, DataAdapter dataAdapter) {
            this.columnName = columnName;
            this.fieldName = fieldName;
            this.dataAdapter = dataAdapter;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public DataAdapter getDataAdapter() {
            return dataAdapter;
        }

        public void setDataAdapter(DataAdapter dataAdapter) {
            this.dataAdapter = dataAdapter;
        }
    }
}
