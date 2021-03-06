package org.jorge.cmp.io.database;

/*
 * This file is part of LoLin1.
 *
 * LoLin1 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LoLin1 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LoLin1. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by Jorge Antonio Diaz-Benito Soriano.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import org.jorge.cmp.BuildConfig;
import org.jorge.cmp.R;
import org.jorge.cmp.datamodel.FeedArticle;
import org.jorge.cmp.datamodel.Realm;
import org.jorge.cmp.io.backup.LoLin1BackupAgent;
import org.jorge.cmp.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SQLiteDAO extends RobustSQLiteOpenHelper {

    public static final Object DB_LOCK = new Object();
    private static final String TABLE_KEY_TIMESTAMP = "TABLE_KEY_TIMESTAMP";
    private static final String TABLE_KEY_TITLE = "TABLE_KEY_TITLE";
    private static final String TABLE_KEY_URL = "TABLE_KEY_URL";
    private static final String TABLE_KEY_DESC = "TABLE_KEY_DESC";
    private static final String TABLE_KEY_READ = "TABLE_KEY_READ";
    private static final String TABLE_KEY_IMG_URL = "TABLE_KEY_IMG_URL";
    private static final String COMMUNITY_TABLE_NAME = "COMMUNITY", SCHOOL_TABLE_NAME = "SCHOOL";
    private final int LOLIN1_V1_59_DB_VERSION;
    private static Context mContext;
    private static SQLiteDAO singleton;

    public static String getCommunityTableName() {
        return COMMUNITY_TABLE_NAME;
    }

    public static String getNewsTableName(Realm r, String l) {
        return String.format(Locale.ENGLISH, mContext.getString(R.string.news_table_name_pattern)
                , r, l).toUpperCase(Locale.ENGLISH);
    }

    public static String getSchoolTableName() {
        return SCHOOL_TABLE_NAME;
    }

    private SQLiteDAO(@NonNull Context _context) {
        super(_context, _context.getString(R.string.database_name), null, BuildConfig.VERSION_CODE);
        mContext = _context;
        LOLIN1_V1_59_DB_VERSION = mContext.getResources().getInteger(R.integer
                .lolin1_v1_v59_db_version);
    }

    public synchronized static void setup(@NonNull Context _context) {
        if (singleton == null) {
            singleton = new SQLiteDAO(_context);
            mContext = _context;
            getInstance().getWritableDatabase(); //Force database creation
        }
    }

    public synchronized static SQLiteDAO getInstance() {
        if (singleton == null)
            throw new IllegalStateException("SQLiteDAO.setup(Context) must be called before " +
                    "trying to retrieve the instance.");
        return singleton;
    }

    @Override
    public void onRobustUpgrade(SQLiteDatabase db, int oldVersion,
                                int newVersion) throws SQLiteException {
        if (oldVersion == LOLIN1_V1_59_DB_VERSION) {
            //Clean the stored data which is no longer necessary.
            //Stored news and account data will be lost,
            // but neither of them are an issue.
            final Resources resources = mContext.getResources();

            final String[] oldTableNames = resources.getStringArray(R.array
                    .lolin1_v1_59_news_table_names);

            synchronized (DB_LOCK) {
                for (String oldTableName : oldTableNames)
                    db.execSQL("DROP TABLE IF EXISTS " + oldTableName);
            }
        }
        LoLin1BackupAgent.requestBackup(mContext);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        super.onCreate(db);

        final Realm[] allRealms = Realm.getAllRealms();
        final List<String> createTableCommands = new ArrayList<>();

        for (Realm realm : allRealms) {
            for (String locale : realm.getLocales())
                createTableCommands.add(("CREATE TABLE IF NOT EXISTS " + SQLiteDAO.getNewsTableName
                        (realm,
                                locale) + " ( " +
                        TABLE_KEY_TIMESTAMP + " INTEGER NOT NULL ON CONFLICT IGNORE, " +
                        TABLE_KEY_TITLE + " TEXT NOT NULL ON CONFLICT IGNORE, " +
                        TABLE_KEY_URL + " TEXT PRIMARY KEY ON CONFLICT IGNORE, " +
                        TABLE_KEY_DESC + " TEXT, " +
                        TABLE_KEY_READ + " INTEGER NOT NULL ON CONFLICT IGNORE, " +
                        TABLE_KEY_IMG_URL + " TEXT NOT NULL ON CONFLICT IGNORE " + ")").toUpperCase
                        (Locale.ENGLISH));
        }

        createTableCommands.add(("CREATE TABLE IF NOT EXISTS " + COMMUNITY_TABLE_NAME + " ( " +
                TABLE_KEY_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                TABLE_KEY_TITLE + " TEXT PRIMARY KEY ON CONFLICT IGNORE, " +
                TABLE_KEY_URL + " TEXT NOT NULL ON CONFLICT REPLACE, " +
                TABLE_KEY_DESC + " TEXT, " +
                TABLE_KEY_READ + " INTEGER NOT NULL ON CONFLICT IGNORE, " +
                TABLE_KEY_IMG_URL + " TEXT NOT NULL ON CONFLICT IGNORE " + ")").toUpperCase
                (Locale.ENGLISH));

        createTableCommands.add(("CREATE TABLE IF NOT EXISTS " + SCHOOL_TABLE_NAME + " ( " +
                TABLE_KEY_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                TABLE_KEY_TITLE + " TEXT PRIMARY KEY ON CONFLICT IGNORE, " +
                TABLE_KEY_URL + " TEXT NOT NULL ON CONFLICT REPLACE, " +
                TABLE_KEY_DESC + " TEXT, " +
                TABLE_KEY_READ + " INTEGER NOT NULL ON CONFLICT IGNORE, " +
                TABLE_KEY_IMG_URL + " TEXT NOT NULL ON CONFLICT IGNORE " + ")").toUpperCase
                (Locale.ENGLISH));

        synchronized (DB_LOCK) {
            for (String cmd : createTableCommands)
                db.execSQL(cmd);
            for (Realm realm : allRealms)
                for (String locale : realm.getLocales())
                    RobustSQLiteOpenHelper.addTableName(getNewsTableName(realm, locale));
            RobustSQLiteOpenHelper.addTableName(COMMUNITY_TABLE_NAME);
            RobustSQLiteOpenHelper.addTableName(SCHOOL_TABLE_NAME);
        }
        LoLin1BackupAgent.requestBackup(mContext);
    }

    public void insertArticlesIntoTable(@NonNull List<FeedArticle> articles,
                                        @NonNull String tableName) {
        if (Utils.isMainThread()) {
            throw new IllegalStateException("Attempted call to insertArticlesIntoTable on main " +
                    "thread!");
        }

        SQLiteDatabase db = getWritableDatabase();
        List<ContentValues> storableArticles = new ArrayList<>();
        for (FeedArticle article : articles)
            storableArticles.add(mapFeedArticleToStorable(article));

        synchronized (DB_LOCK) {
            db.beginTransaction();
            for (ContentValues storableArticle : storableArticles)
                db.insert(tableName, null, storableArticle);
            db.setTransactionSuccessful();
            db.endTransaction();
            LoLin1BackupAgent.requestBackup(mContext);
        }
    }

    private ContentValues mapFeedArticleToStorable(FeedArticle article) {
        ContentValues ret = new ContentValues();
        ret.put(TABLE_KEY_TITLE, article.getTitle());
        ret.put(TABLE_KEY_TIMESTAMP, System.currentTimeMillis());
        try {
            Thread.sleep(1); //To make sure that the millis time is always unique
        } catch (InterruptedException e) {
            Crashlytics.logException(e);
        }
        ret.put(TABLE_KEY_URL, article.getUrl());
        ret.put(TABLE_KEY_DESC, article.getPreviewText());
        ret.put(TABLE_KEY_IMG_URL, article.getImageUrl());
        ret.put(TABLE_KEY_READ, article.isRead() ? 1 : 0);
        return ret;
    }

    private FeedArticle mapStorableToFeedArticle(Cursor articleCursor) {
        return new FeedArticle(articleCursor.getString(articleCursor.getColumnIndex
                (TABLE_KEY_TITLE)),
                articleCursor.getString(articleCursor.getColumnIndex(TABLE_KEY_URL)),
                articleCursor.getString(articleCursor.getColumnIndex(TABLE_KEY_IMG_URL)),
                articleCursor.getString(articleCursor.getColumnIndex(TABLE_KEY_DESC)),
                articleCursor.getInt(articleCursor.getColumnIndex(TABLE_KEY_READ)) == 0 ? Boolean
                        .FALSE : Boolean.TRUE);
    }

    public List<FeedArticle> getFeedArticlesFromTable(String tableName) {
        List<FeedArticle> ret;
        SQLiteDatabase db = getReadableDatabase();
        synchronized (DB_LOCK) {
            db.beginTransaction();
            Cursor allStorableArticles = db.query(tableName, null, null, null, null, null,
                    TABLE_KEY_TIMESTAMP + " DESC");
            ret = new ArrayList<>();
            if (allStorableArticles != null && allStorableArticles.moveToFirst()) {
                do {
                    ret.add(mapStorableToFeedArticle(allStorableArticles));
                } while (allStorableArticles.moveToNext());

            }
            if (allStorableArticles != null)
                allStorableArticles.close();
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        return ret;
    }

    public void markArticleAsRead(FeedArticle article, String tableName) {
        if (Utils.isMainThread()) {
            throw new IllegalStateException("Attempted call to markArticleAsRead on main " +
                    "thread!");
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues newReadContainer = new ContentValues();
        newReadContainer.put(TABLE_KEY_READ, 1);
        synchronized (DB_LOCK) {
            db.beginTransaction();
            db.update(tableName, newReadContainer, TABLE_KEY_URL + " = '" + article.getUrl() +
                    "'", null);
            db.setTransactionSuccessful();
            db.endTransaction();
            LoLin1BackupAgent.requestBackup(mContext);
        }
    }
}
