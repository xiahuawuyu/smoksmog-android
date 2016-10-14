package com.antyzero.smoksmog;

import android.app.Application;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.antyzero.smoksmog.database.SmokSmokDb;
import com.antyzero.smoksmog.database.model.ListItemDb;
import com.antyzero.smoksmog.task.WidgetJobCreator;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import smoksmog.logger.Logger;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class SmokSmogApplication extends Application {

    private ApplicationComponent applicationComponent;

    @Inject
    Logger logger;
    @Inject
    SmokSmokDb smokSmokDb;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build(), new Answers());

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        applicationComponent.inject(this);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Lato-Light.ttf")
                .build());


        JobManager.create(this).addJobCreator(new WidgetJobCreator(this));

        new JobRequest.Builder("StationWidgetJob")
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.NOT_ROAMING)
                .setPersisted(true)
                .build().schedule();


        // TODO db testing code ignore and delete in future

        smokSmokDb.getList()
                .subscribe(listItemDb -> {
                    System.out.println(String.format(">>> id:%s | p:%s", listItemDb._id(), listItemDb.position()));
                });

        smokSmokDb.addToList(ListItemDb.FACTORY.marshal()._id(1));
        smokSmokDb.addToList(ListItemDb.FACTORY.marshal()._id(2));
        smokSmokDb.addToList(ListItemDb.FACTORY.marshal()._id(3));

    }

    public ApplicationComponent getAppComponent() {
        return applicationComponent;
    }

    /**
     * Kept to inject mocked components.
     *
     * @param applicationComponent for replace
     */
    @VisibleForTesting
    public void setAppComponent(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }

    /**
     * Get access to application instance
     *
     * @param context
     * @return
     */
    public static SmokSmogApplication get(Context context) {
        return (SmokSmogApplication) context.getApplicationContext();
    }
}
