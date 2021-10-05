package eu.veldsoft.vitosha.trade;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.logging.Logger;

/**
 * Voting widget preference activity.
 *
 * @author Todor Balabanov
 */
public class VotingWidgetConfigureActivity extends Activity {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /** Default remote server URL. */
    private static final String PREFS_NAME = "eu.veldsoft.vitosha.trade.VotingWidget";

    /** Preferences key prefix. */
    private static final String PREF_PREFIX_KEY = "appwidget_";

    /** Widget identifier. */
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    /** Widget text. */
    private EditText widgetText;

    /** On click event handler. */
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        /**
         * On click event.
         *
         * @param view Event sender.
         */
        public void onClick(View view) {
            final Context context = VotingWidgetConfigureActivity.this;

            String widgetText = VotingWidgetConfigureActivity.this.widgetText.getText().toString();
            saveTitlePref(context, widgetId, widgetText);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            VotingWidget.updateAppWidget(context, appWidgetManager, widgetId);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    /**
     * Widget configuration constructor.
     */
    public VotingWidgetConfigureActivity() {
        super();
    }

    /**
     * Save preferences title.
     *
     * @param context Preferences context.
     * @param widgetId Widget identifier.
     * @param text Title text.
     */
    static void saveTitlePref(Context context, int widgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + widgetId, text);
        prefs.apply();
    }

    /**
     * Load preferences title.
     *
     * @param context Preferences context.
     * @param widgetId Widget identifier.
     * @return Title text.
     */
    static String loadTitlePref(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + widgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return "";
        }
    }

    /**
     * Delete preferences title.
     *
     * @param context Preferences context.
     * @param widgetId Widget identifier.
     */
    static void deleteTitlePref(Context context, int widgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + widgetId);
        prefs.apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.voting_widget_configure);
        widgetText = (EditText) findViewById(R.id.appwidget_text);
        findViewById(R.id.add_button).setOnClickListener(onClickListener);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        widgetText.setText(loadTitlePref(VotingWidgetConfigureActivity.this, widgetId));
    }
}
