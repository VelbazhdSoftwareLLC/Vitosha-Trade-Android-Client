package eu.veldsoft.vitosha.trade;

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
public class WallpaperConfigureActivity extends PreferenceActivity {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.wallpaper_configure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(WallpaperConfigureActivity.this);

        /*
         * Remove our wallpaper.
         */
        if (preferences.getBoolean("set_wallpaper", false) == false) {
            stopService(new Intent(WallpaperConfigureActivity.this,
                    VitoshaTradeWallpaperService.class));
            startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            WallpaperConfigureActivity.this.finish();

            return;
        }

        /*
         * Run wallpaper service.
         */
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(WallpaperConfigureActivity.this,
                        VitoshaTradeWallpaperService.class));
        startActivity(intent);
        WallpaperConfigureActivity.this.finish();
    }
}
