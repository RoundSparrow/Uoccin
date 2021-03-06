package net.ggelardi.uoccin.serv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Storage extends SQLiteOpenHelper {
	private static final String TAG = "Storage";
	
	public static final String NAME = "Uoccin.db";
	public static final int VERSION = 4;
	
	public Storage(Context context) {
		super(context, NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_MOVIE);
		db.execSQL(CREATE_TABLE_MOVTAG);
		db.execSQL(CREATE_TABLE_SERIES);
		db.execSQL(CREATE_TABLE_SERTAG);
		db.execSQL(CREATE_TABLE_EPISODE);
		db.execSQL(CREATE_TABLE_QUEUEIN);
		db.execSQL(CREATE_TABLE_QUEUEOUT);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion <= 4) {
			db.execSQL("DROP TABLE IF EXISTS queue_out");
			db.execSQL("DROP TABLE IF EXISTS queue_in");
			db.execSQL("DROP TABLE IF EXISTS episode");
			db.execSQL("DROP TABLE IF EXISTS sertag");
			db.execSQL("DROP TABLE IF EXISTS series");
			db.execSQL("DROP TABLE IF EXISTS movtag");
			db.execSQL("DROP TABLE IF EXISTS movie");
			db.execSQL(CREATE_TABLE_MOVIE);
			db.execSQL(CREATE_TABLE_MOVTAG);
			db.execSQL(CREATE_TABLE_SERIES);
			db.execSQL(CREATE_TABLE_SERTAG);
			db.execSQL(CREATE_TABLE_EPISODE);
			db.execSQL(CREATE_TABLE_QUEUEIN);
			db.execSQL(CREATE_TABLE_QUEUEOUT);
			return;
		}
		int upgradeTo = oldVersion + 1;
		while (upgradeTo <= newVersion) {
			Log.d(TAG, "Upgrading database to version " + Integer.toString(upgradeTo));
			switch (upgradeTo) {
				case 5:
					break;
			}
			upgradeTo++;
		}
	}
	
	private static final String CS = ", ";
	private static final String PK = " PRIMARY KEY NOT NULL";
	private static final String DT_STR = " TEXT";
	private static final String DT_DBL = " REAL";
	private static final String DT_INT = " INTEGER";
	private static final String DT_FLG = " INTEGER NOT NULL DEFAULT 0";
	private static final String CC_NNU = " NOT NULL";
	
	private static final String CREATE_TABLE_MOVIE = "CREATE TABLE movie (" +
		"imdb_id" + DT_STR + PK + CC_NNU + CS +
		"name" + DT_STR + CC_NNU + CS +
		"year" + DT_INT + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"genres" + DT_STR + CS + // comma delimited
		"language" + DT_STR + CS +
		"director" + DT_STR + CS +
		"writers" + DT_STR + CS + // comma delimited
		"actors" + DT_STR + CS + // comma delimited
		"country" + DT_STR + CS +
		"released" + DT_INT + CS +
		"runtime" + DT_INT + CS + // minutes
		"rated" + DT_STR + CS +
		"awards" + DT_STR + CS +
		"metascore" + DT_INT + CS +
		"tomatoMeter" + DT_INT + CS +
		"imdbRating" + DT_DBL + CS +
		"imdbVotes" + DT_INT + CS +
		"rating" + DT_INT + CS +
		"subtitles" + DT_STR + CS +
		"watchlist" + DT_FLG + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT 0" + CS +
		"createtime" + DT_INT + CC_NNU +
		")";
	
	private static final String CREATE_TABLE_MOVTAG = "CREATE TABLE movtag (" +
		"movie" + DT_STR + CC_NNU + " REFERENCES movie(imdb_id) ON DELETE CASCADE" + CS +
		"tag" + DT_STR + CC_NNU + CS +
		"PRIMARY KEY (movie, tag)" +
		")";
	
	private static final String CREATE_TABLE_SERIES = "CREATE TABLE series (" +
		"tvdb_id" + DT_STR + PK + CC_NNU + CS +
		"name" + DT_STR + CC_NNU + CS +
		"year" + DT_INT + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"genres" + DT_STR + CS + // comma delimited
		"actors" + DT_STR + CS + // comma delimited
		"imdb_id" + DT_STR + CS +
		"status" + DT_STR + CS +
		"network" + DT_STR + CS +
		"firstAired" + DT_INT + CS +
		"airsDay" + DT_INT + CS +
		"airsTime" + DT_INT + CS +
		"runtime" + DT_INT + CS + // minutes
		"rated" + DT_STR + CS +
		"banner" + DT_STR + CS + // url
		"fanart" + DT_STR + CS + // url
		"rating" + DT_INT + CS +
		"watchlist" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT 0" + CS +
		"createtime" + DT_INT + CC_NNU +
		")";
	
	private static final String CREATE_TABLE_SERTAG = "CREATE TABLE sertag (" +
		"series" + DT_STR + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
		"tag" + DT_STR + CC_NNU + CS +
		"PRIMARY KEY (series, tag)" +
		")";
	
	private static final String CREATE_TABLE_EPISODE = "CREATE TABLE episode (" +
		"series" + DT_STR + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
		"season" + DT_INT + CC_NNU + CS +
		"episode" + DT_INT + CC_NNU + CS +
		"name" + DT_STR + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"writers" + DT_STR + CS + // comma delimited
		"director" + DT_STR + CS +
		"guestStars" + DT_STR + CS + // comma delimited
		"firstAired" + DT_INT + CS +
		"tvdb_id" + DT_STR + CS +
		"imdb_id" + DT_STR + CS +
		"subtitles" + DT_STR + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT 0" + CS +
		"PRIMARY KEY (series, season, episode)" +
		")";
	
	public static final String CREATE_TABLE_QUEUEIN = "CREATE TABLE queue_in (" +
		"timestamp" + DT_INT + CC_NNU + CS + // UTC
		"target" + DT_STR + CC_NNU + " CHECK (target IN ('movie', 'series'))" + CS +
		"title" + DT_STR + CC_NNU + CS +
		"field" + DT_STR + CS +
		"value" + DT_STR +
		")";
	
	public static final String CREATE_TABLE_QUEUEOUT = "CREATE TABLE queue_out (" +
		"timestamp" + DT_INT + CC_NNU + CS + // UTC
		"target" + DT_STR + CC_NNU + " CHECK (target IN ('movie', 'series'))" + CS +
		"title" + DT_STR + CC_NNU + CS +
		"field" + DT_STR + CS +
		"value" + DT_STR +
		")";
}