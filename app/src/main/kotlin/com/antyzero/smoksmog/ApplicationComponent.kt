package com.antyzero.smoksmog

import com.antyzero.smoksmog.domain.DomainModule
import com.antyzero.smoksmog.eventbus.EventBusModule
import com.antyzero.smoksmog.fabric.FabricModule
import com.antyzero.smoksmog.firebase.SmokSmogFirebaseInstanceIdService
import com.antyzero.smoksmog.i18n.I18nModule
import com.antyzero.smoksmog.job.JobModule
import com.antyzero.smoksmog.job.SmokSmogJobService
import com.antyzero.smoksmog.logger.LoggerModule
import com.antyzero.smoksmog.settings.SettingsModule
import com.antyzero.smoksmog.ui.dialog.AboutDialog
import com.antyzero.smoksmog.ui.dialog.FacebookDialog
import com.antyzero.smoksmog.ui.screen.ActivityComponent
import com.antyzero.smoksmog.ui.screen.ActivityModule
import com.antyzero.smoksmog.ui.screen.start.item.AirQualityViewHolder
import com.antyzero.smoksmog.ui.screen.start.item.ParticulateViewHolder
import com.antyzero.smoksmog.ui.widget.StationWidget
import com.antyzero.smoksmog.ui.widget.StationWidgetService
import com.antyzero.smoksmog.ui.widget.WidgetModule
import com.antyzero.smoksmog.user.UserModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        DomainModule::class,
        LoggerModule::class,
        FabricModule::class,
        SettingsModule::class,
        EventBusModule::class,
        I18nModule::class,
        UserModule::class,
        WidgetModule::class,
        JobModule::class))
interface ApplicationComponent {

    operator fun plus(activityModule: ActivityModule): ActivityComponent

    fun inject(smokSmogApplication: SmokSmogApplication)

    fun inject(airQualityViewHolder: AirQualityViewHolder)

    fun inject(aboutDialog: AboutDialog)

    fun inject(particulateViewHolder: ParticulateViewHolder)

    fun inject(facebookDialog: FacebookDialog)

    fun inject(stationWidget: StationWidget)

    fun inject(stationWidgetService: StationWidgetService)

    fun inject(smokSmogFirebaseInstanceIdService: SmokSmogFirebaseInstanceIdService)

    fun inject(smokSmogJobService: SmokSmogJobService)
}
