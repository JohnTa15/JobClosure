package gr.gtar.jobclosure.shared.changelog

/** One update's worth of "what's new" bullets, identified by a strictly increasing [id]. */
data class ChangelogEntry(val id: Int, val items: List<String>)

/**
 * Every changelog entry ever shipped, oldest first. Add a new [ChangelogEntry] (with the next id)
 * whenever there's something worth telling the user about - never edit an old one, since both
 * platforms persist the last id they've shown and rely on ids only ever going up. Each platform
 * shows entries with id greater than the last one it displayed as the "what's new" popup after an
 * update, and the full list on demand from a "changelog history" button.
 */
val CHANGELOG_HISTORY: List<ChangelogEntry> = listOf(
    ChangelogEntry(
        id = 1,
        items = listOf(
            "Η διεύθυνση της εκκλησίας ενημερώνεται πλέον σωστά αφού την επεξεργαστείς και αποθηκεύσεις.",
            "Νέο κουμπί για συμπλήρωση της διεύθυνσης σπιτιού με την τρέχουσα τοποθεσία σου.",
            "Ο καιρός για το Drone υπολογίζεται πλέον για την πραγματική ημερομηνία της δουλειάς, όχι για σήμερα.",
            "Πιο σύντομες προτάσεις διευθύνσεων στην αναζήτηση, χωρίς την πλήρη διοικητική ιεραρχία.",
            "Νέο εικονίδιο εφαρμογής.",
            "Η σύνδεση στο desktop δεν κολλάει πια αν ακυρωθεί το login στο Google.",
            "Στα Windows: συντόμευση στο μενού Έναρξη και σωστή απεγκατάσταση από τις Ρυθμίσεις.",
        ),
    ),
    ChangelogEntry(
        id = 2,
        items = listOf(
            "Ο καιρός (θερμοκρασία + προειδοποίηση για βροχή) εμφανίζεται πλέον σε κάθε δουλειά, όχι μόνο με Drone.",
            "Νέα πεδία: τηλέφωνο πελάτη (με κλήση με ένα άγγιγμα) και τιμή δουλειάς, με ενδεικτικό υπολογισμό για τη δεξίωση.",
            "Νέος μικρός χάρτης (OpenStreetMap) στο κάτω μέρος της δουλειάς, για γρήγορη προεπισκόπηση της τοποθεσίας.",
            "Μπορείς τώρα να ξαναδείς παλιότερες ενημερώσεις από τις Ρυθμίσεις.",
        ),
    ),
)

/** The most recent changelog id - a platform is "up to date" on changelogs once it has shown this. */
val CURRENT_CHANGELOG_ID: Int = CHANGELOG_HISTORY.maxOf { it.id }
