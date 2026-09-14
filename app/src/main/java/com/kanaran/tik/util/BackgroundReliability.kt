package com.kanaran.tik.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Many Android skins — Vivo's FuntouchOS / OriginOS especially — pause or kill apps in the
 * background to save battery, well beyond stock Android's Doze. For a reminder app that shows
 * up as reminders arriving late or not at all, and no code inside the app can override it:
 * the user has to exempt tik in the phone's own settings. This works out whether the phone is
 * one of those, what to tell the user, and which settings screen to open.
 */
object BackgroundReliability {

    /** Brands whose battery management is known to stop background apps' alarms and receivers. */
    private val AGGRESSIVE_BRANDS = setOf(
        "vivo", "iqoo", "xiaomi", "redmi", "poco", "oppo", "realme", "oneplus",
        "huawei", "honor", "samsung", "meizu", "asus", "tecno", "infinix", "itel"
    )
    private val VIVO_BRANDS = setOf("vivo", "iqoo")

    fun isAggressive(manufacturer: String?, brand: String?): Boolean = matches(manufacturer, brand, AGGRESSIVE_BRANDS)

    fun isVivo(manufacturer: String?, brand: String?): Boolean = matches(manufacturer, brand, VIVO_BRANDS)

    private fun matches(manufacturer: String?, brand: String?, set: Set<String>) =
        listOfNotNull(manufacturer, brand).any { it.trim().lowercase() in set }

    /** What to tell the user, in order. Menu names drift between skin versions, so each step
     *  names the setting rather than relying on one exact path. */
    fun steps(vivo: Boolean): List<String> = if (vivo) {
        listOf(
            "Autostart: turn it ON for tik (Settings › Apps › Autostart, or i Manager › App manager › Autostart manager).",
            "Battery: allow tik high background power consumption (Settings › Battery › Background power consumption management › tik).",
            "Notifications: allow everything for tik, including lock screen and banners.",
            "Lock tik in Recents so \"clear all\" doesn't close it — open Recents and pull tik's card down, or tap its lock icon."
        )
    } else {
        listOf(
            "Battery: set tik to Unrestricted / Don't optimise (Settings › Apps › tik › Battery).",
            "If your phone has an Autostart or background-activity setting, allow it for tik.",
            "Lock tik in Recents so clearing apps doesn't close it."
        )
    }

    /** The phone we're on. Debug builds can pretend to be another brand (see MainActivity), so
     *  this card can be exercised on an emulator. */
    fun device(forcedBrand: String? = null): Pair<String, String> =
        if (!forcedBrand.isNullOrBlank()) forcedBrand to forcedBrand else Build.MANUFACTURER.orEmpty() to Build.BRAND.orEmpty()

    /** tik's own App info page — on Vivo, that's where its Battery and Notification settings live. */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Vivo's Autostart manager, if this phone has one we're allowed to open. These screens are
     * private to Vivo and move between OS versions, so each candidate is checked before use and
     * callers fall back to [appSettingsIntent]. Returns null off Vivo, or when none resolve.
     */
    fun autostartIntent(context: Context, vivo: Boolean): Intent? {
        if (!vivo) return null
        return VIVO_AUTOSTART_SCREENS.asSequence()
            .map { (pkg, cls) -> Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { intent ->
                val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                info?.activityInfo?.exported == true
            }
    }

    // Plain strings, turned into ComponentNames only when used — keeps this object loadable in
    // JVM unit tests, where Android framework constructors are stubs.
    private val VIVO_AUTOSTART_SCREENS = listOf(
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.PurviewTabActivity"
    )
}
