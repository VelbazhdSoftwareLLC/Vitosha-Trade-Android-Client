package eu.veldsoft.vitdiscomp;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Options screen.
 *
 * @author Todor Balabanov
 */
public class OptionsActivity extends PreferenceActivity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.activity_options);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences preferences = PreferenceManager
				  .getDefaultSharedPreferences(OptionsActivity.this);

		/*
		 * Remove our wallpaper.
		 */
		if (preferences.getBoolean("set_wallpaper", false) == false) {
			stopService(new Intent(OptionsActivity.this,
					  VitoshaTradeWallpaperService.class));
			startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
			OptionsActivity.this.finish();

			return;
		}

		/*
		 * Run wallpaper service.
		 */
		Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
		intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
				  new ComponentName(OptionsActivity.this,
							 VitoshaTradeWallpaperService.class));
		startActivity(intent);
		OptionsActivity.this.finish();
	}
}
