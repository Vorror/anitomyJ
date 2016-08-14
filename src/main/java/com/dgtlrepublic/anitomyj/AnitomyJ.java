/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementFileExtension;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementFileName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

/**
 * A library capable of parsing Anime filenames.
 * <p>
 * This is a C++ to Java port of <a href="https://github.com/erengy/anitomy">Anitomy</a>.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class AnitomyJ {
    static { KeywordManager.getInstance(); }

    private AnitomyJ() {}

    /**
     * Parses an anime {@code filename} into its constituent elements.
     *
     * @param filename the anime file name
     * @return the list of parsed elements
     */
    public static List<Element> parse(String filename) {
        Options options = new Options();
        List<Element> elements = new ArrayList<>(32);
        List<Token> tokens = new ArrayList<>();

        /** remove/parse extension */
        AtomicReference<String> fname = new AtomicReference<>(filename);
        if (options.parseFileExtension) {
            AtomicReference<String> extension = new AtomicReference<>();
            if (removeExtensionFromFilename(fname, extension)) {
                elements.add(new Element(kElementFileExtension, extension.get()));
            }
        }

        /** set filename */
        if (fname.get() == null || fname.get().length() == 0) return elements;
        elements.add(new Element(kElementFileName, fname.get()));

        /** tokenize */
        boolean isTokenized = new Tokenizer(fname.get(), elements, options, tokens).tokenize();
        if (!isTokenized) return elements;
        new Parser(elements, options, tokens).parse();
        return elements;
    }

    /**
     * Removes the extension from the {@code filename}.
     *
     * @param filename  the ref that will be updated with the new filename
     * @param extension the ref that will be updated with the file extension
     * @return true if then extension was separated from the filename
     */
    private static boolean removeExtensionFromFilename(AtomicReference<String> filename,
                                                       AtomicReference<String> extension) {
        int position;
        if (StringUtils.isEmpty(filename.get()) || (position = filename.get().lastIndexOf('.')) == -1) return false;

        /** remove file extension */
        extension.set(filename.get().substring(position + 1));
        if (extension.get().length() > 4) return false;
        if (!StringHelper.isAlphanumericString(extension.get())) return false;

        /** check if valid anime extension */
        String keyword = KeywordManager.normalzie(extension.get());
        if (!KeywordManager.getInstance().contains(kElementFileExtension, keyword)) return false;

        filename.set(filename.get().substring(0, position));
        return true;
    }
}
