package com.antyzero.smoksmog.ui.screen.start;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.antyzero.smoksmog.R;
import com.antyzero.smoksmog.SmokSmogApplication;
import com.antyzero.smoksmog.error.ErrorReporter;
import com.antyzero.smoksmog.eventbus.EventBusModule;
import com.antyzero.smoksmog.eventbus.RxBus;
import com.antyzero.smoksmog.firebase.FirebaseEvents;
import com.antyzero.smoksmog.settings.SettingsHelper;
import com.antyzero.smoksmog.ui.BaseDragonActivity;
import com.antyzero.smoksmog.ui.dialog.AboutDialog;
import com.antyzero.smoksmog.ui.dialog.InfoDialog;
import com.antyzero.smoksmog.ui.screen.ActivityModule;
import com.antyzero.smoksmog.ui.screen.order.OrderActivity;
import com.antyzero.smoksmog.ui.screen.settings.SettingsActivity;
import com.antyzero.smoksmog.ui.screen.start.fragment.StationFragment;
import com.antyzero.smoksmog.ui.screen.start.model.StationIdList;
import com.antyzero.smoksmog.ui.typeface.TypefaceProvider;
import com.antyzero.smoksmog.ui.view.ViewPagerIndicator;
import com.trello.rxlifecycle.ActivityEvent;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.OnClick;
import pl.malopolska.smoksmog.SmokSmog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import smoksmog.logger.Logger;

/**
 * Base activity for future
 */
public class StartActivity extends BaseDragonActivity implements ViewPager.OnPageChangeListener {

    private static final String KEY_LAST_PAGE = "lastSelectedPagePosition";
    private static final int PAGE_LIMIT = 5;

    @Inject
    SmokSmog smokSmog;
    @Inject
    Logger logger;
    @Inject
    ErrorReporter errorReporter;
    @Inject
    SettingsHelper settingsHelper;
    @Inject
    RxBus rxBus;
    @Inject
    TypefaceProvider typefaceProvider;
    @Inject
    FirebaseEvents firebaseEvents;
    @Inject
    @Named(EventBusModule.EVENT_BUS_ERROR)
    Action1<Throwable> eventBusError;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.viewPager)
    ViewPager viewPager;
    @Bind(R.id.viewPagerIndicator)
    ViewPagerIndicator viewPagerIndicator;
    @Bind(R.id.viewSwitcher)
    ViewSwitcher viewSwitcher;
    @Bind(R.id.buttonAddStation)
    View buttonAddStation;

    private List<Long> stationIds;
    private StationSlideAdapter stationSlideAdapter;
    private int lastPageSelected = 0;
    private PageSave pageSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageSave = new PageSave(this);

        SmokSmogApplication.get(this).getAppComponent()
                .plus(new ActivityModule(this))
                .inject(this);

        stationIds = new StationIdList(this);

        setContentView(R.layout.activity_start);
        setSupportActionBar(toolbar);

        stationSlideAdapter = new StationSlideAdapter(getFragmentManager(), stationIds);

        viewPager.setAdapter(stationSlideAdapter);
        viewPager.setOffscreenPageLimit(PAGE_LIMIT);
        viewPager.addOnPageChangeListener(this);
        viewPager.addOnPageChangeListener(viewPagerIndicator);
        viewPager.setCurrentItem(pageSave.restorePage());

        viewPagerIndicator.setStationIds(stationIds);

        correctTitleMargin();

        // Listen for info dialog calls
        rxBus.toObserverable()
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event instanceof InfoDialog.Event) {
                        InfoDialog.show(getFragmentManager(), (InfoDialog.Event) event);
                    }
                }, eventBusError);

        // Listen for title updates
        rxBus.toObserverable()
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event instanceof TitleUpdateEvent) {
                        updateTitleWithStation();
                    }
                }, eventBusError);

        if (savedInstanceState != null) {
            lastPageSelected = savedInstanceState.getInt(KEY_LAST_PAGE, 0);
            viewPager.setCurrentItem(lastPageSelected, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewPagerIndicator.setStationIds(stationIds);
        stationSlideAdapter.notifyDataSetChanged();
        updateTitleWithStation();

        if (stationIds.isEmpty()) {
            visibleNoStations();
        } else {
            visibleStations();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    /**
     * Because it's not aligned with main layout margin
     */
    private void correctTitleMargin() {
        toolbar.setContentInsetsAbsolute(16, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                SettingsActivity.start(this);
                break;
            case R.id.action_order:
                OrderActivity.start(this);
                break;
            case R.id.action_facebook:
                goToFacebookSite();
                break;
            case R.id.action_beta:
                goToBetaLogin();
                break;
            case R.id.action_about:
                rxBus.send(new InfoDialog.Event<>(AboutDialog.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void goToBetaLogin() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/apps/testing/pl.malopolska.smoksmog")));
    }

    private void goToFacebookSite() {
        String facebookUrl = "https://fb.com/SmokSmog";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
        try {
            if (getPackageManager().getPackageInfo("com.facebook.katana", 0) != null) {
                intent.setData(Uri.parse("fb://page/714218922054053"));
            }
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LAST_PAGE, lastPageSelected);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // do nothing
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageSelected(int position) {
        updateTitleWithStation(position);
        lastPageSelected = position;
        pageSave.savePage(position);
        firebaseEvents.logStationCardInView(stationSlideAdapter.getItem(position).getStationId());
    }

    @OnClick(R.id.buttonAddStation)
    void buttonClickAddStation() {
        OrderActivity.start(this, true);
    }

    protected void updateTitleWithStation() {
        if (stationSlideAdapter.getCount() > 0) {
            updateTitleWithStation(viewPager.getCurrentItem());
        }
    }

    protected void updateTitleWithStation(int position) {
        WeakReference<StationFragment> reference = stationSlideAdapter.getFragmentReference(position);
        if (reference != null) {
            StationFragment stationFragment = reference.get();
            if (stationFragment != null) {
                String title = stationFragment.getTitle();
                if (title != null) {
                    toolbar.setTitle(title);
                    toolbar.setSubtitle(stationFragment.getSubtitle());
                    changeSubtitleTypeface();
                }
            }
        }
    }

    private void visibleStations() {
        viewSwitcher.setDisplayedChild(0);
    }

    private void visibleNoStations() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setSubtitle(null);
        }
        viewSwitcher.setDisplayedChild(1);
    }

    /**
     * This is messy, Calligraphy should handle this but for some reason it's the only TextView
     * not updated with default font.
     * <p>
     * TODO fix with Calligraphy in future
     */
    private void changeSubtitleTypeface() {
        for (int i = 1; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                textView.setTypeface(typefaceProvider.getDefault());
            }
        }
    }

    /**
     * Ask to check title and update is possible
     */
    public static class TitleUpdateEvent {

    }
}
