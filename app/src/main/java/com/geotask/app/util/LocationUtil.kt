package com.geotask.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Small helpers around FusedLocationProvider and Geocoder, wrapped as coroutines. */
object LocationUtil {

    private const val TAG = "LocationUtil"

    sealed class LocationResult {
        data class Found(val location: Location) : LocationResult()
        object LocationServicesDisabled : LocationResult()
        object Unavailable : LocationResult()
    }

    /**
     * Caller must have already checked ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION.
     *
     * Play services' fused location can hang indefinitely with no success/failure callback
     * on some devices and emulator images, so this tries progressively cheaper/more direct
     * sources with a timeout on each, instead of waiting on a single call forever:
     *  1. fused "last location" (cached, near-instant when available)
     *  2. fused "current location" (a fresh fix, given a few seconds)
     *  3. the platform LocationManager's last known fix, as a final fallback
     */
    suspend fun getCurrentLocation(context: Context): LocationResult {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
            return LocationResult.LocationServicesDisabled
        }

        withTimeoutOrNullLogged("fused last location", 4_000) { getFusedLastLocation(context) }
            ?.let { return LocationResult.Found(it) }

        withTimeoutOrNullLogged("fused current location", 12_000) { getFusedCurrentLocation(context) }
            ?.let { return LocationResult.Found(it) }

        getPlatformLastKnownLocation(locationManager)
            ?.let { return LocationResult.Found(it) }

        return LocationResult.Unavailable
    }

    private suspend fun <T> withTimeoutOrNullLogged(label: String, timeoutMs: Long, block: suspend () -> T?): T? =
        try {
            withTimeoutOrNull(timeoutMs) { block() }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "$label failed", e)
            null
        }

    @SuppressLint("MissingPermission")
    private suspend fun getFusedLastLocation(context: Context): Location? =
        suspendCancellableCoroutine { continuation ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { e ->
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getFusedCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(10_000)
                .build()
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }

            client.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { e ->
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
        }

    @SuppressLint("MissingPermission")
    private fun getPlatformLastKnownLocation(locationManager: LocationManager?): Location? {
        if (locationManager == null) return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    suspend fun forwardGeocode(context: Context, query: String): Address? =
        forwardGeocodeSuggestions(context, query, maxResults = 1).firstOrNull()

    /**
     * Up to [maxResults] candidate addresses matching [query], for a live suggestions dropdown.
     *
     * The Tiramisu+ async `GeocodeListener` overload can hang forever with no callback and no
     * exception on some devices/emulator images when the backing network service stalls — the
     * same failure mode [getFusedCurrentLocation] has. So this is timeout-bounded too, rather
     * than leaving the caller's "Searching…" spinner stuck indefinitely.
     */
    suspend fun forwardGeocodeSuggestions(context: Context, query: String, maxResults: Int = 5): List<Address> {
        if (query.isBlank()) return emptyList()
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = withTimeoutOrNullLogged("forward geocode '$query'", 8_000) {
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocationName(query, maxResults) { results ->
                            if (continuation.isActive) continuation.resume(results)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, maxResults)
                }
            }
        }
        return results.orEmpty()
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): Address? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return withTimeoutOrNullLogged("reverse geocode $lat,$lng", 8_000) {
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(lat, lng, 1) { results ->
                            if (continuation.isActive) continuation.resume(results.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }
            }
        }
    }

    fun Address.displayName(): String =
        featureName ?: thoroughfare ?: locality ?: adminArea ?: countryName ?: "Selected location"

    /** A one-line formatted address for suggestion rows, e.g. "1600 Amphitheatre Pkwy, Mountain View, CA". */
    fun Address.formattedLine(): String {
        val lines = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }
        if (lines.isNotEmpty()) return lines.first()
        return listOfNotNull(featureName, locality, adminArea, countryName).distinct().joinToString(", ")
    }
}
