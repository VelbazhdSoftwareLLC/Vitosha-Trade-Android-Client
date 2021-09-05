package eu.veldsoft.vitosha.trade;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * User voting widget.
 *
 * @author Todor Balabanov
 */
public class VotingWidget extends AppWidgetProvider {

    /**
     *
     * @param context
     * @param appWidgetManager
     * @param appWidgetId
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = VotingWidgetConfigureActivity.loadTitlePref(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.voting_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            VotingWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnabled(Context context) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisabled(Context context) {
    }
}
