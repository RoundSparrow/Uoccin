package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.api.TVDB;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class Episode extends Title {

	public static Episode get(Context context, String imdb_id) {
		return (Episode) Title.get(context, Episode.class, imdb_id, EPISODE);
	}
	
	public Episode(Context context, String imdb_id) {
		super(context, imdb_id);
		
		type = EPISODE;
	}
	
	public String series_imdb_id;
	public int series_tvdb_id;
	public int tvdb_id; // episode tvdb_id
	public int season;
	public int episode;
	public String director;
	public List<String> writers = new ArrayList<String>();
	public long firstAired;
	public boolean collected = false;
	public boolean watched = false;

	@Override
	protected void load(Cursor cr) {
		super.load(cr);

		series_imdb_id = cr.getString(cr.getColumnIndex("series_imdb_id"));
		series_tvdb_id = cr.getInt(cr.getColumnIndex("series_tvdb_id"));
		tvdb_id = cr.getInt(cr.getColumnIndex("tvdb_id"));
		season = cr.getInt(cr.getColumnIndex("season"));
		episode = cr.getInt(cr.getColumnIndex("episode"));
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		int ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
	}
	
	@Override
	protected void save(boolean isnew) {
		super.save(isnew);
		
		ContentValues cv = new ContentValues();
		cv.put("series_imdb_id", series_imdb_id);
		cv.put("series_tvdb_id", series_tvdb_id);
		cv.put("tvdb_id", tvdb_id);
		cv.put("season", season);
		cv.put("episode", episode);
		cv.put("director", director);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("firstAired", firstAired);
		cv.put("collected", collected);
		cv.put("watched", watched);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			session.getDB().insertOrThrow(EPISODE, null, cv);
		} else {
			session.getDB().update(EPISODE, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	@Override
	public void refresh() {
		dispatch(TitleEvent.LOADING);
		Callback<TVDB.Episode> callback = new Callback<TVDB.Episode>() {
			@Override
			public void success(TVDB.Episode result, Response response) {
				updateFromTVDB(result);
				dispatch(TitleEvent.READY);
			}
			@Override
			public void failure(RetrofitError error) {
				// TODO Auto-generated method stub

				dispatch(TitleEvent.ERROR);
			}
		};
		TVDB.getInstance().getEpisode(series_tvdb_id, season, episode, Locale.getDefault().getLanguage(), callback);
	}
	
	protected void updateFromTVDB(TVDB.Episode data) {
		// title
		if (data.overview != null && !data.overview.trim().equals(""))
			plot = data.overview;
		if (data.guestStars != null && !data.guestStars.isEmpty())
			actors = new ArrayList<String>(data.guestStars);
		if (data.poster != null && !data.poster.trim().equals(""))
			poster = data.poster;
		// episode
		tvdb_id = data.tvdb_id;
		if (data.director != null && !data.director.trim().equals(""))
			director = data.director;
		if (data.writers != null && !data.writers.isEmpty())
			writers = new ArrayList<String>(data.writers);
		if (data.firstAired != null)
			firstAired = data.firstAired.getTime();
		save();
	}
	
	public Series series() {
		return Series.get(context, series_imdb_id);
	}
}