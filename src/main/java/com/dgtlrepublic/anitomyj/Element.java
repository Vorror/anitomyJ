/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import java.util.Objects;

/**
 * An {@code Element} represents an identified Anime {@link Token}. A single filename may contain multiple of the same
 * token(e.g {@link ElementCategory#kElementEpisodeNumber}).
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class Element {
    /** Element Categories */
    public enum ElementCategory {
        kElementAnimeSeason,
        kElementAnimeSeasonPrefix,
        kElementAnimeTitle,
        kElementAnimeType,
        kElementAnimeYear,
        kElementAudioTerm,
        kElementDeviceCompatibility,
        kElementEpisodeNumber,
        kElementEpisodeNumberAlt,
        kElementEpisodePrefix,
        kElementEpisodeTitle,
        kElementFileChecksum,
        kElementFileExtension,
        kElementFileName,
        kElementLanguage,
        kElementOther,
        kElementReleaseGroup,
        kElementReleaseInformation,
        kElementReleaseVersion,
        kElementSource,
        kElementSubtitles,
        kElementVideoResolution,
        kElementVideoTerm,
        kElementVolumeNumber,
        kElementVolumePrefix,
        kElementUnknown
    }

    private ElementCategory category;
    private final String value;

    /**
     * Constructs a new Element
     *
     * @param category the category of the element
     * @param value    the element's value
     */
    public Element(ElementCategory category, String value) {
        this.category = category;
        this.value = value;
    }

    /** Returns the element's category */
    public ElementCategory getCategory() {
        return category;
    }

    /** Returns the element's valve */
    public String getValue() {
        return value;
    }

    /** Sets the element's category */
    public void setCategory(ElementCategory category) {
        this.category = category;
    }

    @Override
    public int hashCode() {return Objects.hash(category);}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}
        final Element other = (Element) obj;
        return Objects.equals(this.category, other.category);
    }

    @Override
    public String toString() {
        return "Element{" +
                "category=" + category +
                ", value='" + value + '\'' +
                '}';
    }
}
