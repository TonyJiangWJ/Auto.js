package com.tony.autojs.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.stardust.app.GlobalAppContext;

import java.util.List;

import androidx.annotation.Nullable;

public class ReaderDBHelper extends SQLiteOpenHelper {
    private List<String> tableEntriesCreate;
    private List<String> tableEntriesAlter;

    public ReaderDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public ReaderDBHelper(String databaseName, List<String> tableEntriesCreate, List<String> tableEntriesAlter, int version, Context context) {
        this(context == null ? GlobalAppContext.get() : context, databaseName, null, version);
        this.tableEntriesCreate = tableEntriesCreate;
        this.tableEntriesAlter = tableEntriesAlter;
    }

    public ReaderDBHelper(String databaseName, List<String> tableEntriesCreate, int version, Context context) {
        this(context == null ? GlobalAppContext.get() : context, databaseName, null, version);
        this.tableEntriesCreate = tableEntriesCreate;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (tableEntriesCreate != null && tableEntriesCreate.size() > 0) {
            for (String tableCreate : tableEntriesCreate) {
                db.execSQL(tableCreate);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion && tableEntriesAlter != null && tableEntriesAlter.size() > 0) {
            // 版本不匹配，需要升级
            for (String alterSql : tableEntriesAlter) {
                db.execSQL(alterSql);
            }
        }
    }
}
