package eu.veldsoft.vitdiscomp;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

public class OptionsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.activity_options);

		getPreferenceScreen().findPreference("set_wallpaper")
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object value) {
						if (value == null) {
							return false;
						}

						if (value instanceof Boolean == false) {
							return false;
						}

						if (((Boolean) value) == false) {
							// TODO Remove our wall paper.
							return false;
						} else {
							((SwitchPreference) preference).setChecked(false);
						}

						Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
						intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
								new ComponentName(OptionsActivity.this, VitoshaTradeWallpaperService.class));
						startActivity(intent);

						OptionsActivity.this.finish();

						return false;
					}
				});
	}
}
