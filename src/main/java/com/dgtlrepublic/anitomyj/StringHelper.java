/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

/**
 * A String helper class that's analogous to <i>string.cpp</i> of the original Anitomy C++ library.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class StringHelper {
    /** Returns whether or not the character is numeric. */
    public static boolean isAlphanumericChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z');
    }

    /** Returns whether or not the character is a hex character. */
    public static boolean isHexadecimalChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'F')
                || (c >= 'a' && c <= 'f');
    }

    /** Returns whether or not the character is a latin character. */
    public static boolean isLatinChar(char c) {
        // We're just checking until the end of Latin Extended-B block, rather than
        // all the blocks that belong to the Latin script.
        return c <= '\u024F';
    }

    /** Returns whether or not the {@code string} is a numeric string. */
    public static boolean isAlphanumericString(String string) {
        return StringUtils.isAlphanumeric(string);
    }

    /** Returns whether or not the {@code string} is a hex string. */
    public static boolean isHexadecimalString(String string) {
        return StringUtils.isNotEmpty(string) && string.chars().allMatch(value -> isHexadecimalChar((char) value));
    }

    /** Returns whether or not the {@code string} is mostly a latin string. */
    public static boolean isMostlyLatinString(String string) {
        double length = StringUtils.isNotEmpty(string) ? 1.0 : string.length();
        return IntStream.range(0, StringUtils.isEmpty(string) ? 0 : string.length())
                .filter(value -> isLatinChar(string.charAt(value)))
                .count() / length >= 0.5;
    }

    /** Returns whether or not the {@code string is a numeric string}. */
    public static boolean isNumericString(String string) {
        return StringUtils.isNumeric(string);
    }

    /** Returns the int value of the {@code string}; 0 otherwise. */
    public static int stringToInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NullPointerException | NumberFormatException e) {
            return 0;
        }
    }

    /** Returns the trimmed version of the string remove <i>any</i> of the {@code trimChars}. */
    public static String trimAny(String string, String trimChars) {
        int pos_begin = findFirstNotOfAny(string, trimChars); /** find the first char not in trimChars */
        int pos_end = findLastNotOfAny(string, trimChars); /** find the last char not in trimChars */
        if (pos_begin == -1 || pos_end == -1) return "";
        return string.substring(pos_begin, pos_end + 1);
    }

    /** Returns the index of the <i>first</i> character that's not one of {@code trimChars}; -1 otherwise. */
    public static int findFirstNotOfAny(String string, String trimChars) {
        if (StringUtils.isEmpty(string) || StringUtils.isEmpty(trimChars)) return -1;
        for (int i = 0; i < string.length(); i++) {
            if (!StringUtils.containsAny(String.valueOf(string.charAt(i)), trimChars)) {
                return i;
            }
        }

        return -1;
    }

    /** Returns the index of the <i>last</i> character that's not one of {@code trimChars}; -1 otherwise. */
    public static int findLastNotOfAny(String string, String trimChars) {
        if (StringUtils.isEmpty(string) || StringUtils.isEmpty(trimChars)) return -1;
        for (int i = string.length() - 1; i >= 0; i--) {
            if (!StringUtils.containsAny(String.valueOf(string.charAt(i)), trimChars)) {
                return i;
            }
        }

        return -1;
    }
}

