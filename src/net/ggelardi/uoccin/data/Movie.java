package net.ggelardi.uoccin.data;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.OMDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;
import net.ggelardi.uoccin.serv.SimpleCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Movie extends Title {
	private static final String TAG = "Movie";
	private static final String TABLE = "movie";
	
	private static final SimpleCache cache = new SimpleCache(500);

	private int rating = 0;
	private List<String> tags = new ArrayList<String>();
	private boolean watchlist = false;
	private boolean collected = false;
	private boolean watched = false;
	
	public String imdb_id;
	public String name;
	public int year;
	public String plot;
	public String poster;
	public List<String> genres = new ArrayList<String>();
	public String language;
	public String director;
	public List<String> writers = new ArrayList<String>();
	public List<String> actors = new ArrayList<String>();
	public String country;
	public long released;
	public int runtime;
	public String rated;
	public String awards;
	public int metascore;
	public Double imdbRating;
	public int imdbVotes;
	public List<String> subtitles = new ArrayList<String>();
	
	public Movie(Context context, String imdb_id) {
		this.session = Session.getInstance(context);
		this.imdb_id = imdb_id;
	}
	
	private static synchronized Movie getInstance(Context context, String imdb_id) {
		Object tmp = cache.get(imdb_id);
		if (tmp != null) {
			Log.v(TAG, "Movie " + imdb_id + " found in cache");
			return (Movie) tmp;
		}
		Movie res = new Movie(context, imdb_id);
		cache.add(imdb_id, res);
		res.reload();
		return res;
	}
	
	public static void drop() {
		cache.clear();
	}
	
	public static Movie get(Context context, String imdb_id) {
		Movie res = Movie.getInstance(context, imdb_id);
		if (res.isOld())
			res.refresh(false);
		return res;
	}

	public static Movie get(Context context, Element xml) {
		String imdb_id = Commons.XML.attrText(xml, "imdbID");
		Movie res = Movie.getInstance(context, imdb_id);
		res.load(xml);
		// no commit here
		return res;
	}
	
	public static List<Movie> get(Context context, List<String> imdb_ids) {
		List<Movie> res = new ArrayList<Movie>();
		for (String id: imdb_ids)
			res.add(Movie.get(context, id));
		return res;
	}
	
	public static List<Movie> get(Context context, String query, String ... args) {
		List<Movie> res = new ArrayList<Movie>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			int ci = cur.getColumnIndex("imdb_id");
			String imdb_id;
			while (cur.moveToNext()) {
				imdb_id = cur.getString(ci);
				res.add(Movie.get(context, imdb_id));
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static List<Movie> find(Context context, String text) {
		List<Movie> res = new ArrayList<Movie>();
		Document doc = null;
		try {
			doc = OMDB.getInstance().findMovie(text);
		} catch (Exception err) {
			Log.e(TAG, "find", err);
			dispatch(OnTitleListener.ERROR, null);
			return res;
		}
		if (doc != null) {
			NodeList lst = doc.getElementsByTagName("Movie");
			for (int i = 0; i < lst.getLength(); i++) {
				String imdbID = Commons.XML.nodeText((Element) lst.item(i), "imdbID");
				// since OMDB search returns id and title only, we'll use the factory method that retrieves full data
				if (!TextUtils.isEmpty(imdbID))
					res.add(Movie.get(context, imdbID));
			}
		}
		if (res.isEmpty())
			dispatch(OnTitleListener.NOTFOUND, null);
		return res;
	}
	
	public static List<Movie> cached() {
		List<Movie> res = new ArrayList<Movie>();
		Object mov;
		for (String k: cache.getKeys()) {
			mov = cache.get(k);
			if (mov != null)
				res.add((Movie) mov);
		}
		return res;
	}
	
	protected void load(Element xml) {
		dispatch(OnTitleListener.WORKING, null);
		
		boolean modified = false;
		String chk;
		chk = Commons.XML.attrText(xml, "title", "Title");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(name) || !name.equals(chk))) {
			name = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "plot");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) || !plot.equals(chk))) {
			plot = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "year");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != year) {
					year = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.attrText(xml, "poster");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "genre");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\s*")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(genres, lst)) {
				genres = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.attrText(xml, "language");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(language) || !language.equals(chk))) {
			language = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "director");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(director) || !director.equals(chk))) {
			director = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "writer");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\s*")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(writers, lst)) {
				writers = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.attrText(xml, "actors");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\s*")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(actors, lst)) {
				actors = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.attrText(xml, "country");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(country) || !country.equals(chk))) {
			country = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "released");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("dd MMM yyyy").parse(chk).getTime();
				if (t > 0) {
					released = t;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.attrText(xml, "runtime");
		if (!TextUtils.isEmpty(chk)) {
			if (chk.contains(" min"))
				chk = chk.split(" ")[0];
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != runtime) {
					runtime = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.attrText(xml, "rated");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(rated) || !rated.equals(chk))) {
			rated = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "awards");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(awards) || !awards.equals(chk))) {
			awards = chk;
			modified = true;
		}
		chk = Commons.XML.attrText(xml, "metascore");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != metascore) {
					metascore = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.attrText(xml, "imdbRating");
		if (!TextUtils.isEmpty(chk)) {
			try {
				double r = Double.parseDouble(chk);
				if (r > 0 && (imdbRating == null || r != imdbRating)) {
					imdbRating = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.attrText(xml, "imdbVotes");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != imdbVotes) {
					imdbVotes = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		//
		if (modified)
			save(true);
		
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void load(Cursor cr) {
		dispatch(OnTitleListener.WORKING, null);
		
		Log.v(TAG, "Loading movie " + imdb_id);
		
		int ci;
		imdb_id = cr.getString(cr.getColumnIndex("imdb_id")); // it's already set btw...
		name = cr.getString(cr.getColumnIndex("name"));
		year = cr.getInt(cr.getColumnIndex("year"));
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		language = cr.getString(cr.getColumnIndex("language"));
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("country");
		if (!cr.isNull(ci))
			country = cr.getString(ci);
		ci = cr.getColumnIndex("released");
		if (!cr.isNull(ci))
			released = cr.getLong(ci);
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
		ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
		ci = cr.getColumnIndex("awards");
		if (!cr.isNull(ci))
			awards = cr.getString(ci);
		ci = cr.getColumnIndex("metascore");
		if (!cr.isNull(ci))
			metascore = cr.getInt(ci);
		ci = cr.getColumnIndex("imdbRating");
		if (!cr.isNull(ci))
			imdbRating = cr.getDouble(ci);
		ci = cr.getColumnIndex("imdbVotes");
		if (!cr.isNull(ci))
			imdbVotes = cr.getInt(ci);
		ci = cr.getColumnIndex("rating");
		if (!cr.isNull(ci))
			rating = cr.getInt(ci);
		ci = cr.getColumnIndex("tags");
		if (!cr.isNull(ci))
			tags = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void save(boolean metadata) {
		dispatch(OnTitleListener.WORKING, null);
		
		Log.v(TAG, "Saving movie " + imdb_id);
		
		ContentValues cv = new ContentValues();
		
		cv.put("imdb_id", imdb_id);
		
		if (!TextUtils.isEmpty(name))
			cv.put("name", name);
		else
			cv.putNull("name");
		
		if (year > 0)
			cv.put("year", year);
		else
			cv.putNull("year");
		
		if (!TextUtils.isEmpty(plot))
			cv.put("plot", plot);
		else
			cv.putNull("plot");
		
		if (!TextUtils.isEmpty(poster))
			cv.put("poster", poster);
		else
			cv.putNull("poster");
		
		if (!genres.isEmpty())
			cv.put("genres", TextUtils.join(",", genres));
		else
			cv.putNull("genres");
		
		if (!TextUtils.isEmpty(language))
			cv.put("language", language);
		else
			cv.putNull("language");
		
		if (!TextUtils.isEmpty(director))
			cv.put("director", director);
		else
			cv.putNull("director");
		
		if (!writers.isEmpty())
			cv.put("writers", TextUtils.join(",", writers));
		else
			cv.putNull("writers");
		
		if (!actors.isEmpty())
			cv.put("actors", TextUtils.join(",", actors));
		else
			cv.putNull("actors");
		
		if (!TextUtils.isEmpty(country))
			cv.put("country", country);
		else
			cv.putNull("country");
		
		if (released > 0)
			cv.put("released", released);
		else
			cv.putNull("released");
		
		if (runtime > 0)
			cv.put("runtime", runtime);
		else
			cv.putNull("runtime");
		
		if (!TextUtils.isEmpty(rated))
			cv.put("rated", rated);
		else
			cv.putNull("rated");
		
		if (!TextUtils.isEmpty(awards))
			cv.put("awards", awards);
		else
			cv.putNull("awards");
		
		if (metascore > 0)
			cv.put("metascore", metascore);
		else
			cv.putNull("metascore");
		
		if (imdbRating != null && imdbRating > 0)
			cv.put("imdbRating", imdbRating);
		else
			cv.putNull("imdbRating");
		
		if (imdbVotes > 0)
			cv.put("imdbVotes", imdbVotes);
		else
			cv.putNull("imdbVotes");
		
		if (rating > 0)
			cv.put("rating", rating);
		else
			cv.putNull("rating");
		
		if (!tags.isEmpty())
			cv.put("tags", TextUtils.join(",", tags));
		else
			cv.putNull("tags");
		
		if (!subtitles.isEmpty())
			cv.put("subtitles", TextUtils.join(",", subtitles));
		else
			cv.putNull("subtitles");
		
		cv.put("watchlist", watchlist);
		cv.put("collected", collected);
		cv.put("watched", watched);
		
		boolean isnew = timestamp <= 0;
		if (isnew || metadata) {
			timestamp = System.currentTimeMillis();
			cv.put("timestamp", timestamp);
		}
		if (isnew)
			session.getDB().insertOrThrow(TABLE, null, cv);
		else
			session.getDB().update(TABLE, cv, "imdb_id=?", new String[] { imdb_id });
		
		Log.i(TAG, "Saved movie " + imdb_id);
		
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void delete() {
		Log.v(TAG, "Deleting movie " + imdb_id);
		dispatch(OnTitleListener.WORKING, null);
		session.getDB().delete(TABLE, "imdb_id=?", new String[] { imdb_id });
		dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void reload() {
		//dispatch(OnTitleListener.WORKING, null);
		Cursor cur = session.getDB().query(TABLE, null, "imdb_id=?", new String[] { imdb_id },
			null, null, null);
		try {
			if (cur.moveToFirst())
				load(cur);
		} finally {
			cur.close();
		}
		//dispatch(OnTitleListener.READY, null);
	}
	
	public void refresh(boolean force) {
		if (Title.ongoingServiceOperation)
			return;
		if (isOld() || force) {
			Intent si = new Intent(session.getContext(), Service.class);
			si.setAction(Service.REFRESH_MOVIE);
			si.putExtra("imdb_id", imdb_id);
			WakefulIntentService.sendWakefulWork(session.getContext(), si);
		}
	}
	
	public boolean isValid() {
		return !(TextUtils.isEmpty(imdb_id) || TextUtils.isEmpty(name));
	}
	
	public boolean isOld() {
		if (timestamp > 0) {
			if (timestamp == 1)
				return true;
			long now = System.currentTimeMillis();
			long ageLocal = now - timestamp;
			if (released > 0) {
				long ageAired = Math.abs(now - released);
				if (ageAired < Commons.weekLong)
					return ageLocal > Commons.dayLong;
				if (ageAired > Commons.yearLong)
					return ageLocal > Commons.monthLong;
			}
			return ageLocal > Commons.weekLong;
		}
		return false;
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public boolean inCollection() {
		return collected;
	}
	
	public boolean isWatched() {
		return watched;
	}
	
	public int getRating() {
		return rating;
	}
	
	public List<String> getTags() {
		return new ArrayList<String>(tags);
	}
	
	public boolean hasTags() {
		return !tags.isEmpty();
	}
	
	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "watchlist", Boolean.toString(watchlist));
			String msg = session.getRes().getString(watchlist ? R.string.msg_wlst_add_mov : R.string.msg_wlst_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void setCollected(boolean value) {
		if (value != collected) {
			collected = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "collected", Boolean.toString(collected));
			String msg = session.getRes().getString(collected ? R.string.msg_coll_add_mov : R.string.msg_coll_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void setWatched(boolean value) {
		if (value != watched) {
			watched = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "watched", Boolean.toString(watched));
			String msg = session.getRes().getString(watched ? R.string.msg_seen_add_mov : R.string.msg_seen_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void setRating(int value) {
		if (value != rating) {
			watchlist = false;
			watched = true;
			rating = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "rating", Integer.toString(rating));
		}
	}
	
	public void setTags(String[] values) {
		tags = new ArrayList<String>(Arrays.asList(values));
		if (isValid())
			save(false);
		else
			refresh(true);
		session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "tags", TextUtils.join(",", tags));
	}
	
	public void addTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (!hasTag(tag)) {
			tags.add(tag);
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "tags", TextUtils.join(",", tags));
			String msg = String.format(session.getRes().getString(R.string.msg_tags_add), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void delTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (hasTag(tag)) {
			tags.remove(tag);
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "tags", TextUtils.join(",", tags));
			String msg = String.format(session.getRes().getString(R.string.msg_tags_del), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public String imdbUrl() {
		return "http://www.imdb.com/title/" + imdb_id;
	}
}