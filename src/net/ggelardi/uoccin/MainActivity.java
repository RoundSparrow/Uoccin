package net.ggelardi.uoccin;

import net.ggelardi.uoccin.adapters.DrawerAdapter;
import net.ggelardi.uoccin.adapters.DrawerAdapter.DrawerItem;
import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class MainActivity extends ActionBarActivity implements BaseFragment.OnFragmentListener {
	private static final String TAG = "MainActivity";
	private static final int REQUEST_ACCOUNT_PICKER = 1;
	private static final int REQUEST_AUTHORIZATION = 2;
	
	private Session session;
    private Toolbar toolbar;
    private DrawerLayout drawer;
	private DrawerAdapter drawerData;
	private ListView drawerList;
	private ProgressBar progressBar;
	private CharSequence lastTitle;
	private String lastView;
	private int pbCount = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		session = Session.getInstance(this);
		
		toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //toolbar.setNavigationIcon(R.drawable.ic_drawer);
        toolbar.setNavigationIcon(R.drawable.ic_navigation_menu);
        
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        drawer.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int state) {
			}
			@Override
			public void onDrawerSlide(View view, float offset) {
			}
			@Override
			public void onDrawerOpened(View view) {
				supportInvalidateOptionsMenu();
			}
			@Override
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
			}
		});
        
        drawerData = new DrawerAdapter(this);
        
		drawerList = (ListView) findViewById(R.id.drawer_list);
		drawerList.setAdapter(drawerData);
		drawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				drawer.closeDrawer(Gravity.START);
				openDrawerItem(drawerData.getItem(position));
			}
		});
		
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
		lastTitle = getTitle();
		
		lastView = session.getPrefs().getString(PK.STARTUPV, "sernext");
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView", lastView);
		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && action.equals(Commons.SN.CONNECT_FAIL))
			checkDrive();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && action.equals(Commons.SN.CONNECT_FAIL))
			checkDrive();
	}
	
	@Override
	public void onBackPressed() {
		if (drawer.isDrawerOpen(Gravity.START))
			drawer.closeDrawer(Gravity.START);
		else
			super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		dropHourGlass();
		
		if (getSupportFragmentManager().findFragmentByTag(BaseFragment.ROOT_FRAGMENT) == null)
			openDrawerItem(drawerData.findItem(lastView));
		
		if (session.driveEnabled() && TextUtils.isEmpty(session.driveUserAccount())) {
			Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
				new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},true,null,null,null,null) ;
			startActivityForResult(googlePicker, REQUEST_ACCOUNT_PICKER);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView", session.getPrefs().getString(PK.STARTUPV, "sernext"));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("lastView", lastView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		//
		if (drawer.isDrawerOpen(Gravity.START)) {
			getMenuInflater().inflate(R.menu.global, menu);
			actionBar.setTitle(R.string.app_name);
		} else {
			getMenuInflater().inflate(R.menu.main, menu);
			actionBar.setTitle(lastTitle);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case android.R.id.home:
				if (drawer.isDrawerOpen(Gravity.START))
					drawer.closeDrawer(Gravity.START);
				else
					drawer.openDrawer(Gravity.START);
				return true;
			case R.id.action_search:
				searchDialog();
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case R.id.action_test_tvdb:
				Intent t1 = new Intent(this, Service.class);
				t1.setAction(Service.CHECK_TVDB_RSS);
				WakefulIntentService.sendWakefulWork(this, t1);
				return true;
			case R.id.action_test_drive:
				Intent t2 = new Intent(this, Service.class);
				t2.setAction(Service.GDRIVE_CHECK);
				WakefulIntentService.sendWakefulWork(this, t2);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		lastTitle = title;
		getSupportActionBar().setTitle(lastTitle);
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == RESULT_OK)
			session.setDriveUserAccount(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
	}
	
	public void openDrawerItem(DrawerItem selection) {
		if (selection.id.equals("settings")) {
			startActivity(new Intent(this, SettingsActivity.class));
			return;
		}
		dropHourGlass();
		lastView = selection.id;
		drawerList.setItemChecked(selection.position, true);
		BaseFragment f = null;
		if (selection.type.equals(DrawerItem.SERIES)) {
			f = SeriesListFragment.newQuery(selection.label, selection.query, selection.details,
				(String[]) null);
		} else if (selection.type.equals(DrawerItem.MOVIE)) {
			f = MovieListFragment.newQuery(selection.label, selection.query, (String[]) null);
		}
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.ROOT_FRAGMENT).commit();
	}
	
	@Override
	public void fragmentAttached(BaseFragment fragment) {
		dropHourGlass();
		// anything else?
	}
	
	@Override
	public void showHourGlass(boolean value) {
		if (value)
			pbCount++;
		else
			pbCount--;
		progressBar.setVisibility(pbCount > 0 ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public void openMovieInfo(String imdb_id) {
		/*
		BaseFragment f = MovieInfoFragment.newInstance(imdb_id);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
		*/
	}
	
	@Override
	public void openSeriesInfo(String tvdb_id) {
		BaseFragment f = SeriesInfoFragment.newInstance(tvdb_id);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
	
	@Override
	public void openSeriesSeason(String tvdb_id, int season) {
		BaseFragment f = EpisodeListFragment.newList(tvdb_id, season);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
	
	@Override
	public void openSeriesEpisode(String tvdb_id, int season, int episode) {
		BaseFragment f = EpisodeInfoFragment.newInstance(tvdb_id, season, episode);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
	
	private void dropHourGlass() {
		pbCount = 0;
		if (progressBar != null)
			progressBar.setVisibility(View.GONE);
	}
	
	private void checkDrive() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new GSA(MainActivity.this).getFolder(true);
				} catch (UserRecoverableAuthIOException e) {
					startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
				} catch (Exception err) {
					Log.e(TAG, "checkDrive", err);
					Toast.makeText(MainActivity.this, err.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			}
		});
		t.start();
	}
	
	@SuppressLint("InflateParams")
	private void searchDialog() {
		LayoutInflater inflater = getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_search, null);
		final EditText edt = (EditText) view.findViewById(R.id.edt_search_text);
		final RadioGroup grp = (RadioGroup) view.findViewById(R.id.grp_search_type);
		final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(R.string.search_dialog).setIcon(
			R.drawable.ic_action_search).setPositiveButton(R.string.dlg_btn_ok, null).setNegativeButton(
			R.string.dlg_btn_cancel, null).setView(view).create();
		edt.setTextIsSelectable(true);
		dlg.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
		    @Override
		    public void onShow(DialogInterface dialog) {
		        Button btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
		        btn.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	String text = edt.getText().toString();
		                if (TextUtils.isEmpty(text)) {
		                	Toast.makeText(dlg.getContext(), R.string.search_no_text, Toast.LENGTH_SHORT).show();
		                	return;
		                }
		                if (grp.getCheckedRadioButtonId() == R.id.rbt_search_series)
		            		searchSeries(text);
		            	else
		            		searchMovies(text);
		                dlg.dismiss();
		            }
		        });
		    }
		});
		dlg.show();
	}
	
	private void searchSeries(String text) {
		BaseFragment f = SeriesListFragment.newSearch(text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
	
	private void searchMovies(String text) {
		BaseFragment f = MovieListFragment.newSearch(text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
}