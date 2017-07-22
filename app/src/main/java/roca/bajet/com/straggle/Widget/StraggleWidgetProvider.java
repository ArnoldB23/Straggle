package roca.bajet.com.straggle.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import roca.bajet.com.straggle.MainActivity;
import roca.bajet.com.straggle.R;

/**
 * Created by Arnold on 6/7/2017.
 */

public class StraggleWidgetProvider extends AppWidgetProvider {

    private final static String LOG_TAG = "StockWidgetProvider";
    public final static String ACTION_LOCATION_DATA_UPDATED = "roca.bajet.com.straggle.ACTION_LOCATION_DATA_UPDATED";
    private BroadcastReceiver mSleepWakeBroadcastReceiver;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {


        Log.d(LOG_TAG, "updateAppWidget...");
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list);

        Intent i = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        views.setOnClickPendingIntent(R.id.widget_top_bar, pendingIntent);

        views.setRemoteAdapter(R.id.widget_listview,
                new Intent(context, StraggleRemoteViewService.class));


        Intent clickIntentTemplate = new Intent(context, MainActivity.class);
        PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(clickIntentTemplate)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setPendingIntentTemplate(R.id.widget_listview, clickPendingIntentTemplate);
        views.setEmptyView(R.id.widget_listview, R.id.widget_empty_textview);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        WidgetLocationService.createPeriodicLocationTask(context);


    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(LOG_TAG, "onUpdate");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        Log.d(LOG_TAG, "onReceive, Got Broadcast: " + intent.getAction());
        if (ACTION_LOCATION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));


            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_listview);
            Log.d(LOG_TAG, "onReceive, ACTION_LOCATION_DATA_UPDATED!");
        }


    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(LOG_TAG, "onEnabled...");


        super.onEnabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(LOG_TAG, "onDeleted...");


        WidgetLocationService.stopPeriodicLocationTask(context);


        super.onDeleted(context, appWidgetIds);
    }


    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(LOG_TAG, "onDisabled...");


    }
}
