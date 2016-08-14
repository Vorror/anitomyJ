/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeSeasonPrefix;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeType;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAudioTerm;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementDeviceCompatibility;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodePrefix;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementFileExtension;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementLanguage;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementOther;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseGroup;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseInformation;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseVersion;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementSource;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementSubtitles;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementUnknown;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVideoResolution;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVideoTerm;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVolumePrefix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.dgtlrepublic.anitomyj.Element.ElementCategory;

/**
 * A class to manage the list of known anime keywords. This class is analogous to {@code keyword.cpp} of the original
 * library.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class KeywordManager {
    private final Map<String, Keyword> keys = new HashMap<>();
    private final Map<String, Keyword> file_extensions = new HashMap<>();
    private final List<Pair<ElementCategory, List<String>>> peekEntries;
    private static final KeywordManager instance = new KeywordManager();

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private KeywordManager() {
        KeywordOptions optionsDefault = new KeywordOptions();
        KeywordOptions optionsInvalid = new KeywordOptions(true, true, false);
        KeywordOptions optionsUnidentifiable = new KeywordOptions(false, true, true);
        KeywordOptions optionsUnidentifiableInvalid = new KeywordOptions(false, true, false);
        KeywordOptions optionsUnidentifiableUnsearchable = new KeywordOptions(false, false, true);

        add(kElementAnimeSeasonPrefix, optionsUnidentifiable, Arrays.asList("SAISON", "SEASON"));

        add(kElementAnimeType,
            optionsUnidentifiable,
            Arrays.asList("GEKIJOUBAN", "MOVIE", "OAD", "OAV", "ONA", "OVA", "SPECIAL", "SPECIALS", "TV")
        );

        add(kElementAnimeType,
            optionsUnidentifiableUnsearchable,
            Arrays.asList("SP"));  // e.g. "Yumeiro Patissiere SP Professional"

        add(kElementAnimeType,
            optionsUnidentifiableInvalid,
            Arrays.asList("ED", "ENDING", "NCED", "NCOP", "OP", "OPENING", "PREVIEW", "PV"));

        add(kElementAudioTerm, optionsDefault, Arrays.asList(
                // Audio channels
                "2.0CH", "2CH", "5.1", "5.1CH", "DTS", "DTS-ES", "DTS5.1",
                "TRUEHD5.1",
                // Audio codec
                "AAC", "AACX2", "AACX3", "AACX4", "AC3", "FLAC", "FLACX2",
                "FLACX3", "FLACX4", "LOSSLESS", "MP3", "OGG", "VORBIS",
                // Audio language
                "DUALAUDIO", "DUAL AUDIO"
        ));

        add(kElementDeviceCompatibility,
            optionsDefault,
            Arrays.asList("IPAD3", "IPHONE5", "IPOD", "PS3", "XBOX", "XBOX360"));

        add(kElementDeviceCompatibility, optionsUnidentifiable, Arrays.asList("ANDROID"));

        add(kElementEpisodePrefix, optionsDefault, Arrays.asList(
                "EP", "EP.", "EPS", "EPS.", "EPISODE", "EPISODE.", "EPISODES",
                "CAPITULO", "EPISODIO", "FOLGE"));

        add(kElementEpisodePrefix,
            optionsInvalid,
            Arrays.asList("E", "\\x7B2C"));  // single-letter episode keywords are not valid tokens

        add(kElementFileExtension, optionsDefault, Arrays.asList(
                "3GP", "AVI", "DIVX", "FLV", "M2TS", "MKV", "MOV", "MP4", "MPG",
                "OGM", "RM", "RMVB", "WEBM", "WMV"));

        add(kElementFileExtension, optionsInvalid, Arrays.asList(
                "AAC", "AIFF", "FLAC", "M4A", "MP3", "MKA", "OGG", "WAV", "WMA",
                "7Z", "RAR", "ZIP",
                "ASS", "SRT"));

        add(kElementLanguage, optionsDefault, Arrays.asList(
                "ENG", "ENGLISH", "ESPANO", "JAP", "PT-BR", "SPANISH", "VOSTFR"));

        add(kElementLanguage, optionsUnidentifiable, Arrays.asList("ESP", "ITA"));  // e.g. "Tokyo ESP", "Bokura ga Ita"

        add(kElementOther, optionsDefault, Arrays.asList(
                "REMASTER", "REMASTERED", "UNCENSORED", "UNCUT",
                "TS", "VFR", "WIDESCREEN", "WS"));

        add(kElementReleaseGroup, optionsDefault, Arrays.asList("THORA"));

        add(kElementReleaseInformation, optionsDefault, Arrays.asList("BATCH", "COMPLETE", "PATCH", "REMUX"));

        add(kElementReleaseInformation,
            optionsUnidentifiable,
            Arrays.asList("END", "FINAL"));  // e.g. "The End of Evangelion", "Final Approach"

        add(kElementReleaseVersion, optionsDefault, Arrays.asList("V0", "V1", "V2", "V3", "V4"));

        add(kElementSource, optionsDefault, Arrays.asList(
                "BD", "BDRIP", "BLURAY", "BLU-RAY",
                "DVD", "DVD5", "DVD9", "DVD-R2J", "DVDRIP", "DVD-RIP",
                "R2DVD", "R2J", "R2JDVD", "R2JDVDRIP",
                "HDTV", "HDTVRIP", "TVRIP", "TV-RIP",
                "WEBCAST", "WEBRIP"));

        add(kElementSubtitles, optionsDefault, Arrays.asList(
                "ASS", "BIG5", "DUB", "DUBBED", "HARDSUB", "RAW", "SOFTSUB",
                "SOFTSUBS", "SUB", "SUBBED", "SUBTITLED"));

        add(kElementVideoTerm, optionsDefault, Arrays.asList(
                // Frame rate
                "23.976FPS", "24FPS", "29.97FPS", "30FPS", "60FPS", "120FPS",
                // Video codec
                "8BIT", "8-BIT", "10BIT", "10BITS", "10-BIT", "10-BITS", "HI10P",
                "H264", "H265", "H.264", "H.265", "X264", "X265", "X.264",
                "AVC", "HEVC", "DIVX", "DIVX5", "DIVX6", "XVID",
                // Video format
                "AVI", "RMVB", "WMV", "WMV3", "WMV9",
                // Video quality
                "HQ", "LQ",
                // Video resolution
                "HD", "SD"));

        add(kElementVolumePrefix, optionsDefault, Arrays.asList("VOL", "VOL.", "VOLUME"));

        /** {@link #peekAndAdd(String, TokenRange, List, List)} entries */
        peekEntries = new ArrayList<Pair<ElementCategory, List<String>>>() {{
            add(Pair.of(kElementAudioTerm, Arrays.asList("Dual Audio")));
            add(Pair.of(kElementVideoTerm, Arrays.asList("H264", "H.264", "h264", "h.264")));
            add(Pair.of(kElementVideoResolution, Arrays.asList("480p", "720p", "1080p")));
            add(Pair.of(kElementSource, Arrays.asList("Blu-Ray")));
        }};
    }

    /** Return singleton instance. */
    public static KeywordManager getInstance() {
        return instance;
    }

    /** Returns a normalized string. */
    public static String normalzie(String word) {
        if (StringUtils.isEmpty(word)) return word;
        return word.toUpperCase(Locale.ENGLISH);
    }

    /** Returns whether or not {@code KeywordManager} contains {@code keyword}. */
    public boolean contains(ElementCategory category, String keyword) {
        Map<String, Keyword> keys = getKeywordContainer(category);
        Keyword foundEntry = keys.get(keyword);
        return foundEntry != null && foundEntry.getCategory() == category;
    }

    /**
     * Finds a particular {@code keyword}. If found sets {@code category} and {@code options} to the found search
     * result.
     *
     * @param keyword  the keyword to search for
     * @param category an atomic reference that will be set/changed to the found keyword category
     * @param options  an atomic reference that will be set/changed to the found keyword options
     * @return true if the keyword was found; false otherwise
     */
    public boolean findAndSet(String keyword,
                              AtomicReference<ElementCategory> category,
                              AtomicReference<KeywordOptions> options) {
        Map<String, Keyword> keys = getKeywordContainer(category.get());
        Keyword foundEntry = keys.get(keyword);
        if (foundEntry != null) {
            if (category.get() == kElementUnknown) {
                category.set(foundEntry.getCategory());
            } else if (foundEntry.getCategory() != category.get()) {
                return false;
            }

            options.set(foundEntry.getOptions());
            return true;
        }

        return false;
    }

    /**
     * Given a particular {@code filename} and {@code range} attempt to preidentify the token before we attempt the main
     * parsing logic.
     *
     * @param filename            the filename
     * @param range               the search range
     * @param elements            elements array that any pre-identified elements will be added to
     * @param preidentifiedTokens elements array that any pre-identified token ranges will be added to
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public void peekAndAdd(String filename,
                           TokenRange range,
                           List<Element> elements,
                           List<TokenRange> preidentifiedTokens) {
        int end_r = range.getOffset() + range.getSize();
        String search = filename.substring(range.getOffset(), end_r > filename.length() ? filename.length() : end_r);
        peekEntries.forEach(entry -> entry.getRight().forEach(keyword -> {
            int foundIdx = search.indexOf(keyword);
            if (foundIdx != -1) {
                foundIdx += range.getOffset();
                elements.add(new Element(entry.getKey(), keyword));
                preidentifiedTokens.add(new TokenRange(foundIdx, keyword.length()));
            }
        }));
    }

    /** P R I V A T E  A P I */

    /** Returns the appropriate keyword container. */
    private Map<String, Keyword> getKeywordContainer(ElementCategory category) {
        return category == kElementFileExtension ? file_extensions : keys;
    }

    /** Adds a {@code category}, {@code options} and {@code keywords} to the internal keywords list. */
    private void add(ElementCategory category, KeywordOptions options, List<String> keywords) {
        Map<String, Keyword> keys = getKeywordContainer(category);
        keywords.stream()
                .filter(StringUtils::isNotEmpty)
                .filter(s -> !keys.containsKey(s))
                .forEach(keyword -> keys.put(keyword, new Keyword(category, options)));
    }

    /**
     * Keyword options for a particular keyword.
     *
     * @author Paul Miller
     */
    public static class KeywordOptions {
        private final boolean identifiable;
        private final boolean searchable;
        private final boolean valid;

        public KeywordOptions() {
            this(true, true, true);
        }

        /**
         * Constructs a new keyword option
         *
         * @param identifiable if the token is identifiable
         * @param searchable   if the token is searchable
         * @param valid        if the token is valid
         */
        public KeywordOptions(boolean identifiable, boolean searchable, boolean valid) {
            this.identifiable = identifiable;
            this.searchable = searchable;
            this.valid = valid;
        }

        /** Returns whether or not a keyword is identifiable. */
        public boolean isIdentifiable() {
            return identifiable;
        }

        /** Returns whether or not a keyword is searchable */
        public boolean isSearchable() {
            return searchable;
        }

        /** Returns whether or not a keyword is valid */
        public boolean isValid() {
            return valid;
        }
    }

    /**
     * A Keyword
     *
     * @author Paul  Miller
     */
    public static class Keyword {
        private final ElementCategory category;
        private final KeywordOptions options;

        /**
         * Constructs a new Keyword.
         *
         * @param category the category of the keyword
         * @param options  the keyword's options
         */
        public Keyword(ElementCategory category, KeywordOptions options) {
            this.category = category;
            this.options = options;
        }

        /** Returns the keyword category. */
        public ElementCategory getCategory() {
            return category;
        }

        /** Returns the keyword options */
        public KeywordOptions getOptions() {
            return options;
        }
    }
}
