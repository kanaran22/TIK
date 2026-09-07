package com.kanaran.tik.ui.editor

import android.content.Intent
import android.location.Address
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kanaran.tik.data.SavedPlace
import com.kanaran.tik.data.TriggerType
import com.kanaran.tik.ui.components.BrutalistToggleTag
import com.kanaran.tik.ui.components.SecondaryButton
import com.kanaran.tik.util.LocationUtil
import com.kanaran.tik.util.LocationUtil.displayName
import com.kanaran.tik.util.LocationUtil.formattedLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LocationPickerSection(
    locationName: String?,
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Float,
    triggerType: TriggerType,
    savedPlaces: List<SavedPlace> = emptyList(),
    onLocationPicked: (name: String?, lat: Double, lng: Double) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (TriggerType) -> Unit,
    onSavePlace: (name: String, lat: Double, lng: Double, radiusMeters: Float) -> Unit = { _, _, _, _ -> },
    onDeletePlace: (SavedPlace) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearchedQuery by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLocationSettingsHint by remember { mutableStateOf(false) }

    // Debounced live suggestions: fetch ~350ms after the user pauses typing.
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length < 3) {
            suggestions = emptyList()
            hasSearchedQuery = null
            return@LaunchedEffect
        }
        isSearching = true
        delay(350)
        suggestions = LocationUtil.forwardGeocodeSuggestions(context, searchQuery, maxResults = 5)
        isSearching = false
        hasSearchedQuery = searchQuery
    }

    fun pick(name: String?, lat: Double, lng: Double) {
        onLocationPicked(name, lat, lng)
        suggestions = emptyList()
        searchQuery = ""
        hasSearchedQuery = null
        errorMessage = null
        showLocationSettingsHint = false
    }

    fun pickSaved(place: SavedPlace) {
        pick(place.name, place.latitude, place.longitude)
        onRadiusChange(place.radiusMeters)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (savedPlaces.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SAVED PLACES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedPlaces.forEach { place ->
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                                .clickable { pickSaved(place) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                            Text(place.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Forget ${place.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeletePlace(place) }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search an address") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isSearching) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                Text("SEARCHING...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (hasSearchedQuery == searchQuery && suggestions.isEmpty() && searchQuery.trim().length >= 3) {
            Text(
                "No matches — try a shorter or different search, or use current location.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (suggestions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp).padding(vertical = 4.dp)) {
                    items(suggestions) { address ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pick(address.displayName(), address.latitude, address.longitude)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                address.formattedLine(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        SecondaryButton(
            text = if (isLocating) "LOCATING..." else "USE CURRENT LOCATION",
            icon = { Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp)) },
            onClick = {
                errorMessage = null
                showLocationSettingsHint = false
                isLocating = true
                scope.launch {
                    when (val result = LocationUtil.getCurrentLocation(context)) {
                        is LocationUtil.LocationResult.Found -> {
                            val location = result.location
                            val address = LocationUtil.reverseGeocode(context, location.latitude, location.longitude)
                            pick(address?.displayName() ?: "Current location", location.latitude, location.longitude)
                        }
                        LocationUtil.LocationResult.LocationServicesDisabled -> {
                            errorMessage = "Location services are turned off."
                            showLocationSettingsHint = true
                        }
                        LocationUtil.LocationResult.Unavailable -> {
                            errorMessage = "Couldn't get a location fix. Try search instead, or move somewhere with a clearer GPS/network signal."
                        }
                    }
                    isLocating = false
                }
            }
        )

        errorMessage?.let {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("! $it".uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                if (showLocationSettingsHint) {
                    SecondaryButton(
                        text = "OPEN LOCATION SETTINGS",
                        onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    )
                }
            }
        }

        if (latitude != null && longitude != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onBackground))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                locationName ?: "Selected location",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val alreadySaved = savedPlaces.any { it.latitude == latitude && it.longitude == longitude }
                        Icon(
                            if (alreadySaved) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (alreadySaved) "Already saved" else "Save this place",
                            tint = if (alreadySaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(enabled = !alreadySaved) {
                                    onSavePlace(locationName ?: "Saved place", latitude, longitude, radiusMeters)
                                }
                        )
                    }

                    Text("RADIUS: ${radiusMeters.roundToInt()} M", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = radiusMeters,
                        onValueChange = onRadiusChange,
                        valueRange = 50f..1000f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.onBackground,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Text("REMIND ME WHEN I:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrutalistToggleTag(
                            label = "Arrive",
                            selected = triggerType == TriggerType.ON_ARRIVE,
                            onClick = { onTriggerTypeChange(TriggerType.ON_ARRIVE) }
                        )
                        BrutalistToggleTag(
                            label = "Leave",
                            selected = triggerType == TriggerType.ON_LEAVE,
                            onClick = { onTriggerTypeChange(TriggerType.ON_LEAVE) }
                        )
                        BrutalistToggleTag(
                            label = "Both",
                            selected = triggerType == TriggerType.BOTH,
                            onClick = { onTriggerTypeChange(TriggerType.BOTH) }
                        )
                    }
                }
            }
        } else {
            Text(
                "No location chosen yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
