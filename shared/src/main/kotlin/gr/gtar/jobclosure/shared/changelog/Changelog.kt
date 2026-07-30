package gr.gtar.jobclosure.shared.changelog

/**
 * A short "what's new" note shown once after an update, on both Android and desktop. Bump
 * [CURRENT_CHANGELOG_ID] and update [CURRENT_CHANGELOG_ITEMS] whenever there's something worth
 * telling the user about; each platform persists the last id it has shown and compares that
 * against [CURRENT_CHANGELOG_ID] on launch to decide whether to show the dialog again.
 */
const val CURRENT_CHANGELOG_ID = 1

val CURRENT_CHANGELOG_ITEMS: List<String> = listOf(
    "Η διεύθυνση της εκκλησίας ενημερώνεται πλέον σωστά αφού την επεξεργαστείς και αποθηκεύσεις.",
    "Νέο κουμπί για συμπλήρωση της διεύθυνσης σπιτιού με την τρέχουσα τοποθεσία σου.",
    "Ο καιρός για το Drone υπολογίζεται πλέον για την πραγματική ημερομηνία της δουλειάς, όχι για σήμερα.",
    "Πιο σύντομες προτάσεις διευθύνσεων στην αναζήτηση, χωρίς την πλήρη διοικητική ιεραρχία.",
    "Νέο εικονίδιο εφαρμογής.",
    "Η σύνδεση στο desktop δεν κολλάει πια αν ακυρωθεί το login στο Google.",
    "Στα Windows: συντόμευση στο μενού Έναρξη και σωστή απεγκατάσταση από τις Ρυθμίσεις.",
)
