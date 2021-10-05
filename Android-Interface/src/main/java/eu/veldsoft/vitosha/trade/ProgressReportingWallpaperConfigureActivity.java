package eu.veldsoft.vitosha.trade;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.logging.Logger;

/**
 * Options screen.
 *
 * @author Todor Balabanov
 */
public class ProgressReportingWallpaperConfigureActivity extends PreferenceActivity {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

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
                .getDefaultSharedPreferences(ProgressReportingWallpaperConfigureActivity.this);

        /*
         * Remove our wallpaper.
         */
        if (preferences.getBoolean("set_wallpaper", false) == false) {
            stopService(new Intent(ProgressReportingWallpaperConfigureActivity.this,
                    ProgressReportingWallpaperService.class));
            startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            ProgressReportingWallpaperConfigureActivity.this.finish();

            return;
        }

        /*
         * Run wallpaper service.
         */
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(ProgressReportingWallpaperConfigureActivity.this,
                        ProgressReportingWallpaperService.class));
        startActivity(intent);
        ProgressReportingWallpaperConfigureActivity.this.finish();
    }
}
