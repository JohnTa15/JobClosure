package gr.gtar.jobclosure.dagr

import org.json.JSONArray
import org.json.JSONObject

/** What an autofill pass actually managed to do. */
data class DagrFillReport(
    val filled: List<String> = emptyList(),
    val missed: List<String> = emptyList(),
    val error: String? = null,
) {
    val total: Int get() = filled.size + missed.size
    val isEmpty: Boolean get() = total == 0 && error == null
}

/**
 * Builds the JavaScript that fills DAGR's forms, and parses back what it managed to do.
 *
 * Two things shape this. First, the page's markup is not published and the site is bilingual, so
 * fields are found by matching every name a field goes by on screen - its label, name, id,
 * placeholder, aria-label and the text immediately before it - instead of by fixed selectors.
 * Second, the result is always reported: a fill that quietly matched three of eleven fields and
 * left the rest blank is far more dangerous on a flight request than one that says so, because the
 * form would look filled in at a glance.
 *
 * Nothing here presses submit. The script only ever writes into fields; sending the request to the
 * authority stays a deliberate act by the pilot, on DAGR's own button, after reading the form.
 */
object DagrAutofill {

    fun loginScript(username: String, password: String): String =
        SCRIPT_PRELUDE + """
        (function () {
          try {
            var user = findField(['username', 'user', 'email', 'login', 'όνομα χρήστη', 'ονομα χρηστη', 'χρήστης', 'χρηστης'], ['password']);
            var pass = findField(['password', 'pass', 'κωδικός', 'κωδικος', 'συνθηματικό', 'συνθηματικο'], []);
            var filled = [], missed = [];
            if (user) { setValue(user, ${username.toJs()}); filled.push('Όνομα χρήστη'); } else { missed.push('Όνομα χρήστη'); }
            if (pass) { setValue(pass, ${password.toJs()}); filled.push('Κωδικός'); } else { missed.push('Κωδικός'); }
            return JSON.stringify({ filled: filled, missed: missed });
          } catch (e) {
            return JSON.stringify({ filled: [], missed: [], error: String(e) });
          }
        })();
        """.trimIndent()

    fun fillScript(fields: List<DagrField>): String {
        val spec = JSONArray()
        fields.forEach { field ->
            spec.put(
                JSONObject()
                    .put("key", field.key)
                    .put("label", field.label)
                    .put("value", field.value)
                    .put("matches", JSONArray(field.matches)),
            )
        }
        return SCRIPT_PRELUDE + """
        (function () {
          try {
            var spec = ${spec.toString().toJs()};
            var fields = JSON.parse(spec);
            var filled = [], missed = [];
            for (var i = 0; i < fields.length; i++) {
              var f = fields[i];
              var el = findField(f.matches, []);
              if (el) { setValue(el, f.value); filled.push(f.label); } else { missed.push(f.label); }
            }
            return JSON.stringify({ filled: filled, missed: missed });
          } catch (e) {
            return JSON.stringify({ filled: [], missed: [], error: String(e) });
          }
        })();
        """.trimIndent()
    }

    fun parseReport(rawJson: String?): DagrFillReport {
        // evaluateJavascript hands back a JSON *string literal* containing our JSON, or the bare
        // word null when the page navigated away mid-call.
        if (rawJson.isNullOrBlank() || rawJson == "null") {
            return DagrFillReport(error = "Η σελίδα δεν απάντησε - δοκίμασε ξανά αφού φορτώσει.")
        }
        return try {
            val unwrapped = if (rawJson.startsWith("\"")) JSONObject("{\"v\":$rawJson}").getString("v") else rawJson
            val json = JSONObject(unwrapped)
            DagrFillReport(
                filled = json.optJSONArray("filled").toStringList(),
                missed = json.optJSONArray("missed").toStringList(),
                error = json.optString("error").takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            DagrFillReport(error = "Δεν κατάλαβα την απάντηση της σελίδας: ${e.message}")
        }
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }

    /** JSON-quotes a value so it can be dropped into the script as a literal. */
    private fun String.toJs(): String = JSONObject.quote(this)

    /**
     * Shared helpers. [findField] scores every visible text input on the page against the names a
     * field goes by and takes the best, so a rename on DAGR's side degrades to "δεν βρέθηκε"
     * rather than to a value written into the wrong box.
     */
    private val SCRIPT_PRELUDE = """
        // Per-pass, deliberately not kept on window: a second attempt after the page has changed
        // must be free to match the same inputs again, or it would report them all as missed.
        var __jcUsed = [];

        function describe(el) {
          var parts = [el.name || '', el.id || '', el.placeholder || '',
                       el.getAttribute('aria-label') || '', el.getAttribute('title') || ''];
          if (el.labels) { for (var i = 0; i < el.labels.length; i++) parts.push(el.labels[i].innerText || ''); }
          var prev = el.previousElementSibling;
          if (prev && (prev.innerText || '').length <= 60) parts.push(prev.innerText || '');
          // Nearest ancestor small enough to be about this field alone. Taking any container would
          // pull in the whole form's text and let every field match every name.
          var node = el.parentElement, hops = 0;
          while (node && hops < 3) {
            var t = node.innerText || '';
            if (t.length > 0 && t.length <= 80) { parts.push(t); break; }
            node = node.parentElement; hops++;
          }
          return parts.join(' ').toLowerCase();
        }

        function isUsable(el) {
          if (el.disabled || el.readOnly) return false;
          var t = (el.type || '').toLowerCase();
          if (t === 'hidden' || t === 'submit' || t === 'button' || t === 'checkbox' || t === 'radio' || t === 'file') return false;
          var rect = el.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0;
        }

        function findField(matches, avoid) {
          var candidates = document.querySelectorAll('input, textarea, select');
          var best = null, bestScore = 0;
          for (var i = 0; i < candidates.length; i++) {
            var el = candidates[i];
            if (!isUsable(el) || __jcUsed.indexOf(el) !== -1) continue;
            var text = describe(el);
            var skip = false;
            for (var a = 0; a < avoid.length; a++) { if (text.indexOf(avoid[a]) !== -1) { skip = true; break; } }
            if (skip) continue;
            var score = 0;
            for (var m = 0; m < matches.length; m++) {
              if (text.indexOf(matches[m]) !== -1) score += matches[m].length;
            }
            if (score > bestScore) { bestScore = score; best = el; }
          }
          // A weak coincidental hit is worse than no hit: writing a date into "Σκοπός" looks filled
          // in but is wrong, whereas a miss is reported and typed by hand.
          if (bestScore < 4) return null;
          if (best) __jcUsed.push(best);
          return best;
        }

        function setValue(el, value) {
          if (el.tagName === 'SELECT') {
            for (var i = 0; i < el.options.length; i++) {
              var o = el.options[i];
              if (o.value === value || (o.text || '').trim() === value) { el.selectedIndex = i; break; }
            }
          } else {
            // Frameworks that own the input (React and friends) ignore a plain `el.value = x`, so go
            // through the native setter and then announce the change the way a keystroke would.
            var proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
            var setter = Object.getOwnPropertyDescriptor(proto, 'value');
            if (setter && setter.set) { setter.set.call(el, value); } else { el.value = value; }
          }
          el.dispatchEvent(new Event('input', { bubbles: true }));
          el.dispatchEvent(new Event('change', { bubbles: true }));
          el.dispatchEvent(new Event('blur', { bubbles: true }));
        }

    """.trimIndent() + "\n"
}
