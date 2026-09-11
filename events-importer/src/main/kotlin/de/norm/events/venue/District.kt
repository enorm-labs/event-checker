package de.norm.events.venue

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * One of Berlin's 23 districts as they stood from 1986 to 2000, which is the only thing
 * `venue.district` may hold.
 *
 * **The 23 pre-2001 districts, not the 12 boroughs that replaced them** (#1307). The 2001 reform
 * merged Friedrichshain with Kreuzberg and Prenzlauer Berg into Pankow, and nobody in Berlin says
 * either of the merged names. With the boroughs, 36 of 86 venues sat in one filter value. The old
 * districts are the level people name, and each is a union of today's *Ortsteile*, so the mapping is
 * a fixed table rather than a judgement.
 *
 * **One level, never a mix.** The field was a free-form `String?` and that is how two wrong values
 * got in (#329): two venues on the RAW-Gelände said `friedrichshain` while nine at the same postal
 * code said `friedrichshain-kreuzberg`, and a filter on either dropped the others without failing.
 * The cure was a closed set, and it still is. A borough is not a value here, and neither is an
 * Ortsteil: `moabit` is `tiergarten`, `gesundbrunnen` is `wedding`, `oberschoeneweide` is `koepenick`.
 *
 * **The wire form is the kebab-case name**, because that is what the database holds and what the
 * frontend sends. `V021` carries the matching CHECK constraint, so a hand-edited row cannot introduce
 * a twenty-fourth district either.
 *
 * **Here rather than in `events-core` beside `SourceLicence`, and that is not an oversight.** The
 * kebab-case wire form needs Jackson's `@JsonValue` to survive a round trip, and `events-core`
 * carries no Jackson. Only the importer writes a venue, so only the importer needs to parse one. The
 * BFF still filters on the raw column, which is why `V021` carries the constraint as well.
 */
enum class District(
    @get:JsonValue val value: String
) {
    CHARLOTTENBURG("charlottenburg"),
    FRIEDRICHSHAIN("friedrichshain"),
    HELLERSDORF("hellersdorf"),
    HOHENSCHOENHAUSEN("hohenschoenhausen"),
    KOEPENICK("koepenick"),
    KREUZBERG("kreuzberg"),
    LICHTENBERG("lichtenberg"),
    MARZAHN("marzahn"),
    MITTE("mitte"),
    NEUKOELLN("neukoelln"),
    PANKOW("pankow"),
    PRENZLAUER_BERG("prenzlauer-berg"),
    REINICKENDORF("reinickendorf"),
    SCHOENEBERG("schoeneberg"),
    SPANDAU("spandau"),
    STEGLITZ("steglitz"),
    TEMPELHOF("tempelhof"),
    TIERGARTEN("tiergarten"),
    TREPTOW("treptow"),
    WEDDING("wedding"),
    WEISSENSEE("weissensee"),
    WILMERSDORF("wilmersdorf"),
    ZEHLENDORF("zehlendorf");

    companion object {
        /**
         * Parses the wire form, rejecting anything else.
         *
         * **Unlike `SourceLicence.parseOrProhibited` there is no lenient fallback, because there
         * is no safe one.** A licence has a conservative answer to fall back
         * to. A district does not: guessing puts a venue in the wrong place on the map, or removes
         * it from a search, and both are worse than refusing the write.
         */
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): District =
            entries.find { it.value.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown district '$value'. Expected one of: ${entries.joinToString(", ") { it.value }}"
                )
    }
}
