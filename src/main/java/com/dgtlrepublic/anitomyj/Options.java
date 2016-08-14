/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

/**
 * AnitomyJ search configuration options.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class Options {
    public final String allowedDelimiters;
    public final boolean parseEpisodeNumber;
    public final boolean parseEpisodeTitle;
    public final boolean parseFileExtension;
    public final boolean parseReleaseGroup;

    public Options() {
        this.allowedDelimiters = " _.&+,|";
        this.parseEpisodeNumber = true;
        this.parseEpisodeTitle = true;
        this.parseFileExtension = true;
        this.parseReleaseGroup = true;
    }
}
