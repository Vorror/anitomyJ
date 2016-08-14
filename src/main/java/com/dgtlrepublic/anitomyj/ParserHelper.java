/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeSeason;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotDelimiter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.dgtlrepublic.anitomyj.Element.ElementCategory;
import com.dgtlrepublic.anitomyj.Token.Result;
import com.dgtlrepublic.anitomyj.Token.TokenCategory;

/**
 * Utility class to assist in the parsing.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class ParserHelper {
    public static final String kDashes = "-\u2010\u2011\u2012\u2013\u2014\u2015";
    public static final String kDashesWithSpace = " -\u2010\u2011\u2012\u2013\u2014\u2015";
    public static final Map<String, String> ordinals = Collections.unmodifiableMap(new HashMap<String, String>() {{
        //@formatter:off
        put("1st", "1"); put("First", "1");
        put("2nd", "2"); put("Second", "2");
        put("3rd", "3"); put("Third", "3");
        put("4th", "4"); put("Fourth", "4");
        put("5th", "5"); put("Fifth", "5");
        put("6th", "6"); put("Sixth", "6");
        put("7th", "7"); put("Seventh", "7");
        put("8th", "8"); put("Eighth", "8");
        put("9th", "9"); put("Ninth", "9");
        //@formatter:on
    }});

    private final Parser parser;
    
    public ParserHelper(Parser parser) {
        this.parser = parser;
    }

    /** Returns whether or not the {@code result} matches the {@code category}. */
    public static boolean isTokenCategory(Result result, TokenCategory category) {
        return result != null && result.token != null && result.token.getCategory() == category;
    }

    /*** Returns whether or not the {@code token} matches the {@code category}. */
    public static boolean isTokenCategory(Token token, TokenCategory category) {
        return token != null && token.getCategory() == category;
    }

    /** Returns whether or not the {@code string} is a CRC string. */
    public static boolean isCrc32(String string) {
        return string != null && string.length() == 8 && StringHelper.isHexadecimalString(string);
    }

    /** Returns whether or not the {@code character} is a dash character. */
    public static boolean isDashCharacter(char c) {
        return StringUtils.containsAny(String.valueOf(c), kDashes);
    }

    /** Returns a number from an original(e.g 2nd). */
    public static String getNumberFromOrdinal(String string) {
        if (StringUtils.isEmpty(string)) return "";
        String foundString = ordinals.get(string);
        return foundString != null ? foundString : "";
    }

    /** Returns the index of the first digit in the {@code string}; -1 otherwise. */
    public static int indexOfFirstDigit(String string) {
        if (StringUtils.isEmpty(string)) return -1;
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return i;
            }
        }
        
        return -1;
    }

    /** Returns whether or not the {@code string} is a resolution. */
    public static boolean isResolution(String string) {
        if (StringUtils.isEmpty(string)) return false;
        int minWidthSize = 3;
        int minHeightSize = 3;

        // *###x###*
        if (string.length() >= minWidthSize + 1 + minHeightSize) {
            int pos = StringUtils.indexOfAny(string, "xX\u00D7");
            if (pos != -1 && pos >= minWidthSize && pos <= string.length() - (minHeightSize + 1)) {
                for (int i = 0; i < string.length(); i++) {
                    if (i != pos && !Character.isDigit(string.charAt(i))) return false;
                }

                return true;
            }

            // *###p
        } else if (string.length() >= minHeightSize + 1) {
            if (Character.toLowerCase(string.charAt(string.length() - 1)) == 'p') {
                for (int i = 0; i < string.length() - 1; i++) {
                    if (!Character.isDigit(string.charAt(i))) return false;
                }

                return true;
            }
        }

        return false;
    }

    /** Returns whether or not the {@code category} is searchable. */
    public static boolean isElementCategorySearchable(ElementCategory category) {
        switch (category) {
            case kElementAnimeSeasonPrefix:
            case kElementAnimeType:
            case kElementAudioTerm:
            case kElementDeviceCompatibility:
            case kElementEpisodePrefix:
            case kElementFileChecksum:
            case kElementLanguage:
            case kElementOther:
            case kElementReleaseGroup:
            case kElementReleaseInformation:
            case kElementReleaseVersion:
            case kElementSource:
            case kElementSubtitles:
            case kElementVideoResolution:
            case kElementVideoTerm:
            case kElementVolumePrefix:
                return true;
        }

        return false;
    }

    /** Returns whether the {@code category} is singular. */
    public static boolean isElementCategorySingular(ElementCategory category) {
        switch (category) {
            case kElementAnimeSeason:
            case kElementAnimeType:
            case kElementAudioTerm:
            case kElementDeviceCompatibility:
            case kElementEpisodeNumber:
            case kElementLanguage:
            case kElementOther:
            case kElementReleaseInformation:
            case kElementSource:
            case kElementVideoTerm:
                return false;
        }

        return true;
    }

    /** Returns whether or not a token at the current {@code pos} is isolated(surrounded by braces). */
    public boolean isTokenIsolated(int pos) {
        Result prevToken = Token.findPrevToken(parser.getTokens(), pos, kFlagNotDelimiter);
        if (!isTokenCategory(prevToken, kBracket)) return false;
        Result nextToken = Token.findNextToken(parser.getTokens(), pos, kFlagNotDelimiter);
        return isTokenCategory(nextToken, kBracket);
    }

    /** Finds ands sets the anime season keyword. */
    public boolean checkAndSetAnimeSeasonKeyword(Token token, int currentTokenPos) {
        TriConsumer<Token, Token, String> setAnimeSeason = (first, second, content) -> {
            parser.getElements().add(new Element(kElementAnimeSeason, content));
            first.setCategory(kIdentifier);
            second.setCategory(kIdentifier);
        };

        Result previousToken = Token.findPrevToken(parser.getTokens(), currentTokenPos, kFlagNotDelimiter);
        if (previousToken.token != null) {
            String number = getNumberFromOrdinal(previousToken.token.getContent());
            if (!number.isEmpty()) {
                setAnimeSeason.accept(previousToken.token, token, number);
                return true;
            }
        }

        Result nextToken = Token.findNextToken(parser.getTokens(), currentTokenPos, kFlagNotDelimiter);
        if (nextToken.token != null && StringHelper.isNumericString(nextToken.token.getContent())) {
            setAnimeSeason.accept(token, nextToken.token, nextToken.token.getContent());
            return true;
        }

        return false;
    }

    /**
     * A method to find the correct volume/episode number when prefixed(i.e Vol.4).
     *
     * @param category        the category we're searching for.
     * @param currentTokenPos the current token position
     * @param token           the token
     * @return true if we found the volume/episode number
     */
    public boolean checkExtentKeyword(ElementCategory category, int currentTokenPos, Token token) {
        Result nToken = Token.findNextToken(parser.getTokens(), currentTokenPos, kFlagNotDelimiter);
        if (isTokenCategory(nToken.token, kUnknown)) {
            if (indexOfFirstDigit(nToken.token.getContent()) == 0) {
                switch (category) {
                    case kElementEpisodeNumber:
                        if (!parser.getParserNumber().matchEpisodePatterns(nToken.token.getContent(), nToken.token)) {
                            parser.getParserNumber().setEpisodeNumber(nToken.token.getContent(), nToken.token, false);
                        }
                        break;
                    case kElementVolumeNumber:
                        if (!parser.getParserNumber().matchVolumePatterns(nToken.token.getContent(), nToken.token)) {
                            parser.getParserNumber().setVolumeNumber(nToken.token.getContent(), nToken.token, false);
                        }
                        break;
                    default:
                        return false;
                }

                token.setCategory(kIdentifier);
                return true;
            }
        }

        return false;
    }

    /**
     * Builds an element an adds it to the internal element list.
     *
     * @param category       the element category
     * @param keepDelimiters delimiters to keep in the element content.
     * @param tokens         the tokens used to create the element content value.
     */
    public void buildElement(ElementCategory category, boolean keepDelimiters, List<Token> tokens) {
        StringBuilder element = new StringBuilder();

        for (ListIterator<Token> iter = tokens.listIterator(); iter.hasNext(); ) {
            Token token = iter.next();
            switch (token.getCategory()) {
                case kUnknown:
                    element.append(token.getContent());
                    token.setCategory(kIdentifier);
                    break;
                case kBracket:
                    element.append(token.getContent());
                    break;
                case kDelimiter: {
                    String delimiter = "";
                    if (StringUtils.isNotEmpty(token.getContent())) {
                        delimiter = StringUtils.substring(token.getContent(), 0, 1);
                    }

                    if (keepDelimiters) {
                        element.append(delimiter);
                    } else if (iter.hasPrevious() && iter.hasNext()) {
                        switch (delimiter) {
                            case ",":
                            case "&":
                                element.append(delimiter);
                                break;
                            default:
                                element.append(' ');
                                break;
                        }
                    }
                    break;
                }
            }
        }

        if (!keepDelimiters) {
            element = new StringBuilder(StringHelper.trimAny(element.toString(), kDashesWithSpace));
        }

        if (!element.toString().isEmpty()) {
            parser.getElements().add(new Element(category, element.toString()));
        }
    }
}
