package eu.veldsoft.vitosha.trade;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Voting widget preference activity.
 *
 * @author Todor Balabanov
 */
public class VotingWidgetConfigureActivity extends Activity {

    /** */
    private static final String PREFS_NAME = "eu.veldsoft.vitosha.trade.VotingWidget";

    /** */
    private static final String PREF_PREFIX_KEY = "appwidget_";

    /** */
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    /** */
    EditText mAppWidgetText;

    /** */
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = VotingWidgetConfigureActivity.this;

            String widgetText = mAppWidgetText.getText().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            VotingWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    /** */
    public VotingWidgetConfigureActivity() {
        super();
    }

    /**
     *
     * @param context
     * @param appWidgetId
     * @param text
     */
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    /**
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return "";
        }
    }

    /**
     *
     * @param context
     * @param appWidgetId
     */
    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
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
        mAppWidgetText = (EditText) findViewById(R.id.appwidget_text);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mAppWidgetText.setText(loadTitlePref(VotingWidgetConfigureActivity.this, mAppWidgetId));
    }
}
