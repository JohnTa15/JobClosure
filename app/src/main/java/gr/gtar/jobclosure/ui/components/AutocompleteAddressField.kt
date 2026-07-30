package gr.gtar.jobclosure.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.PopupProperties
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.network.PlaceSearchRepository
import gr.gtar.jobclosure.network.PlaceSuggestion
import kotlinx.coroutines.delay

private const val DEBOUNCE_MILLIS = 400L

/**
 * Address/venue-name field with a live suggestions dropdown as the user types (debounced), so a
 * church or venue can be found by name instead of typing its full address by hand. Backed by
 * [PlaceSearchRepository], which itself picks Nominatim or Google Places depending on the maps
 * provider setting.
 *
 * [onSuggestionSelected] defaults to just filling the field with the suggestion's full text; pass
 * a custom one (e.g. for a "church name" field) to also fill a separate address field from the
 * same pick.
 */
@Composable
fun AutocompleteAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    provider: MapsProvider,
    googleApiKey: String,
    placeSearchRepository: PlaceSearchRepository,
    modifier: Modifier = Modifier,
    onSuggestionSelected: (PlaceSuggestion) -> Unit = { onValueChange(it.fullText) },
) {
    var suggestions by remember { mutableStateOf(emptyList<PlaceSuggestion>()) }
    var expanded by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    LaunchedEffect(value, provider, googleApiKey) {
        if (!isTyping) return@LaunchedEffect
        if (value.trim().length < 3) {
            suggestions = emptyList()
            expanded = false
            return@LaunchedEffect
        }
        delay(DEBOUNCE_MILLIS)
        suggestions = placeSearchRepository.suggest(value, provider, googleApiKey)
        expanded = suggestions.isNotEmpty()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                isTyping = true
                onValueChange(it)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = suggestion.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (suggestion.fullText != suggestion.name) {
                                Text(
                                    text = suggestion.fullText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                    },
                    onClick = {
                        isTyping = false
                        expanded = false
                        onSuggestionSelected(suggestion)
                    },
                )
            }
        }
    }
}
