package eu.veldsoft.vitosha.trade;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import eu.veldsoft.vitosha.trade.dummy.InputData;

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

        //TODO Information should be taken from other source.
        views.setTextViewText(R.id.symbol_ticker, InputData.SYMBOL);
        views.setTextViewText(R.id.current_value, ""+InputData.OPEN[InputData.OPEN.length-1]);

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
