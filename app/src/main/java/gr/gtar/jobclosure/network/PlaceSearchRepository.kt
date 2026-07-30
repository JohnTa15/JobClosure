package gr.gtar.jobclosure.network

import gr.gtar.jobclosure.data.MapsProvider

/** [name] is the first, comma-separated part of the match (e.g. a church's own name when a named
 *  place matched); [fullText] is the complete formatted result, suitable to drop straight into an
 *  address field. */
data class PlaceSuggestion(val name: String, val fullText: String)

private const val MIN_QUERY_LENGTH = 3
private const val MAX_SUGGESTIONS = 5

/**
 * Backs the address/venue-name autocomplete fields: as the user types, suggests matching places
 * so they can pick a church/venue by name and get its address filled in automatically, instead of
 * typing the full address by hand.
 */
class PlaceSearchRepository(
    private val nominatimApi: NominatimApi,
    private val placesApi: PlacesApi,
) {
    suspend fun suggest(query: String, provider: MapsProvider, googleApiKey: String): List<PlaceSuggestion> {
        if (query.trim().length < MIN_QUERY_LENGTH) return emptyList()

        return try {
            when (provider) {
                MapsProvider.OPENSTREETMAP -> suggestOpenStreetMap(query)
                MapsProvider.GOOGLE -> if (googleApiKey.isBlank()) emptyList() else suggestGoogle(query, googleApiKey)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun suggestOpenStreetMap(query: String): List<PlaceSuggestion> =
        nominatimApi.search(query = query, limit = MAX_SUGGESTIONS)
            .filter { it.displayName.isNotBlank() }
            .map { toOsmSuggestion(it) }

    private suspend fun suggestGoogle(query: String, apiKey: String): List<PlaceSuggestion> =
        placesApi.autocomplete(input = query, apiKey = apiKey).predictions
            .take(MAX_SUGGESTIONS)
            .map { toSuggestion(it.description) }

    private fun toSuggestion(fullText: String) =
        PlaceSuggestion(name = fullText.substringBefore(",").trim(), fullText = fullText)

    /**
     * Nominatim's display_name spells out Greece's full administrative hierarchy (κοινότητα,
     * δήμος, περιφερειακή ενότητα, περιφέρεια, αποκεντρωμένη διοίκηση, ...) which is far more than
     * an address field needs. Building a short address from the structured `address` breakdown
     * instead keeps just street + settlement + postcode + country - enough precision to still
     * re-geocode correctly later for travel-time lookups, without the noise.
     */
    private fun toOsmSuggestion(result: NominatimResult): PlaceSuggestion {
        val name = result.displayName.substringBefore(",").trim()
        val address = result.address
        val concise = address?.let {
            listOfNotNull(
                listOfNotNull(it.road, it.houseNumber).joinToString(" ").ifBlank { null },
                it.village ?: it.town ?: it.city ?: it.municipality,
                it.postcode,
                it.country,
            ).joinToString(", ").ifBlank { null }
        } ?: result.displayName
        return PlaceSuggestion(name = name, fullText = concise)
    }
}
