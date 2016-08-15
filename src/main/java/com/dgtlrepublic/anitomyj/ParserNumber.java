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
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeType;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodeNumber;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodeNumberAlt;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodePrefix;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseVersion;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVolumeNumber;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVolumePrefix;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kDelimiter;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagEnclosed;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotDelimiter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.dgtlrepublic.anitomyj.Element.ElementCategory;
import com.dgtlrepublic.anitomyj.KeywordManager.KeywordOptions;
import com.dgtlrepublic.anitomyj.Token.Result;
import com.dgtlrepublic.anitomyj.Token.TokenFlag;

/**
 * A Utility class to assist in number parsing.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class ParserNumber {
    public static final int kAnimeYearMin = 1900;
    public static final int kAnimeYearMax = 2050;
    public static final int kEpisodeNumberMax = kAnimeYearMin - 1;
    public static final int kVolumeNumberMax = 20;

    private final Parser parser;

    public ParserNumber(Parser parser) {
        this.parser = parser;
    }

    /** Returns whether or not a the {@code number} is a volume number. */
    public boolean isValidVolumeNumber(String number) {
        return StringHelper.stringToInt(number) <= kVolumeNumberMax;
    }

    /** Returns whether or not the {@code number} is a valid episode number. */
    public boolean isValidEpisodeNumber(String number) {
        try {
            return NumberFormat.getInstance().parse(number).doubleValue() <= kEpisodeNumberMax;
        } catch (ParseException | NullPointerException | NumberFormatException e) {
            return false;
        }
    }

    /************ S E T ********** */

    /** Sets the alternative episode number. */
    public boolean setAlternativeEpisodeNumber(String number, Token token) {
        parser.getElements().add(new Element(kElementEpisodeNumberAlt, number));
        token.setCategory(kIdentifier);
        return true;
    }

    /**
     * Sets the volume number.
     *
     * @param number   the number
     * @param token    the token which contains the volume number
     * @param validate true if we should check if it's a valid number; false to disable verification.
     * @return true if the volume number was set
     */
    public boolean setVolumeNumber(String number, Token token, boolean validate) {
        if (validate && !isValidVolumeNumber(number)) {
            return false;
        }

        parser.getElements().add(new Element(kElementVolumeNumber, number));
        token.setCategory(kIdentifier);
        return true;
    }

    /**
     * Sets the anime episode number.
     *
     * @param number   the episode number
     * @param token    the token which contains the volume number.
     * @param validate true if we should check if it's a valid episode number; false to disable validation
     * @return true if the episode number was set
     */
    public boolean setEpisodeNumber(String number, Token token, boolean validate) {
        if (validate && !isValidEpisodeNumber(number)) return false;
        token.setCategory(kIdentifier);
        ElementCategory category = kElementEpisodeNumber;

        /** Handle equivalent numbers */
        if (parser.isEpisodeKeywordsFound()) {
            for (Element element : parser.getElements()) {
                if (element.getCategory() != kElementEpisodeNumber) continue;

                /** The larger number gets to be the alternative one */
                int comparison = StringHelper.stringToInt(number) - StringHelper.stringToInt(element.getValue());
                if (comparison > 0) {
                    category = kElementEpisodeNumberAlt;
                } else if (comparison < 0) {
                    element.setCategory(kElementEpisodeNumberAlt);
                } else {
                    return false; /** No need to add the same number twice */
                }

                break;
            }
        }

        parser.getElements().add(new Element(category, number));
        return true;
    }

    /**
     * Checks if a number follows the specified {@code token}.
     *
     * @param category the category to set if a number follows the {@code token}.
     * @param token    the token
     * @return true if a number follows the token; false otherwise
     */
    public boolean numberComesAfterPrefix(ElementCategory category, Token token) {
        int number_begin = ParserHelper.indexOfFirstDigit(token.getContent());
        String prefix = StringUtils.substring(token.getContent(), 0, number_begin).toUpperCase(Locale.ENGLISH);
        if (KeywordManager.getInstance().contains(category, prefix)) {
            String number = StringUtils.substring(token.getContent(), number_begin, token.getContent().length());

            switch (category) {
                case kElementEpisodePrefix:
                    if (!matchEpisodePatterns(number, token))
                        setEpisodeNumber(number, token, false);
                    return true;
                case kElementVolumePrefix:
                    if (!matchVolumePatterns(number, token))
                        setVolumeNumber(number, token, false);
                    return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the the number precedes the word "of".
     *
     * @param token           the token
     * @param currentTokenIdx the index of the token.
     * @return true if the token precedes the word "of"
     */
    public boolean numberComesBeforeTotalNumber(Token token, int currentTokenIdx) {
        Result nextToken = Token.findNextToken(parser.getTokens(), currentTokenIdx, kFlagNotDelimiter);
        if (nextToken.token != null) {
            if (StringUtils.equalsIgnoreCase(nextToken.token.getContent(), "of")) {
                Result otherToken = Token.findNextToken(parser.getTokens(), nextToken, kFlagNotDelimiter);

                if (otherToken.token != null) {
                    if (StringHelper.isNumericString(otherToken.token.getContent())) {
                        setEpisodeNumber(token.getContent(), token, false);
                        nextToken.token.setCategory(kIdentifier);
                        otherToken.token.setCategory(kIdentifier);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /************ E P I S O D E  M A T C H E R S ********** */

    /**
     * Attempts to find an episode/season inside a {@code word}/
     *
     * @param word  the word
     * @param token the token
     * @return true if the word was matched to an episode/season number
     */
    public boolean matchEpisodePatterns(String word, Token token) {
        if (StringHelper.isNumericString(word)) return false;

        word = StringHelper.trimAny(word, " -");

        boolean numericFront = Character.isDigit(word.charAt(0));
        boolean numericBack = Character.isDigit(word.charAt(word.length() - 1));

        // e.g. "01v2"
        if (numericFront && numericBack)
            if (matchSingleEpisodePattern(word, token))
                return true;
        // e.g. "01-02", "03-05v2"
        if (numericFront && numericBack)
            if (matchMultiEpisodePattern(word, token))
                return true;
        // e.g. "2x01", "S01E03", "S01-02xE001-150"
        if (numericBack)
            if (matchSeasonAndEpisodePattern(word, token))
                return true;
        // e.g. "ED1", "OP4a", "OVA2"
        if (!numericFront)
            if (matchTypeAndEpisodePattern(word, token))
                return true;
        // e.g. "07.5"
        if (numericFront && numericBack)
            if (matchFractionalEpisodePattern(word, token))
                return true;
        // e.g. "4a", "111C"
        if (numericFront && !numericBack)
            if (matchPartialEpisodePattern(word, token))
                return true;
        // e.g. "#01", "#02-03v2"
        if (numericBack)
            if (matchNumberSignPattern(word, token))
                return true;
        // U+8A71 is used as counter for stories, episodes of TV series, etc.
        if (numericFront)
            if (matchJapaneseCounterPattern(word, token))
                return true;

        return false;
    }

    /**
     * Match a single episode pattern. e.g. "01v2".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchSingleEpisodePattern(String word, Token token) {
        String regexPattern = "(\\d{1,3})[vV](\\d)";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            setEpisodeNumber(matcher.group(1), token, false);
            parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(2)));
            return true;
        }

        return false;
    }

    /**
     * Match a multi episode pattern. e.g. "01-02", "03-05v2".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchMultiEpisodePattern(String word, Token token) {
        String regexPattern = "(\\d{1,3})(?:[vV](\\d))?[-~&+](\\d{1,3})(?:[vV](\\d))?";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            String lowerBound = matcher.group(1);
            String upperBound = matcher.group(3);

            /** Avoid matching expressions such as "009-1" or "5-2" */
            if (StringHelper.stringToInt(lowerBound) < StringHelper.stringToInt(upperBound)) {
                if (setEpisodeNumber(lowerBound, token, true)) {
                    setEpisodeNumber(upperBound, token, true);
                    if (StringUtils.isNotEmpty(matcher.group(2)))
                        parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(2)));
                    if (StringUtils.isNotEmpty(matcher.group(4)))
                        parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(4)));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Match season and episode patters. e.g. "2x01", "S01E03", "S01-02xE001-150".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchSeasonAndEpisodePattern(String word, Token token) {
        String regexPattern = "S?(\\d{1,2})(?:-S?(\\d{1,2}))?(?:x|[ ._-x]?E)(\\d{1,3})(?:-E?(\\d{1,3}))?";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            parser.getElements().add(new Element(kElementAnimeSeason, matcher.group(1)));
            if (StringUtils.isNotEmpty(matcher.group(2)))
                parser.getElements().add(new Element(kElementAnimeSeason, matcher.group(2)));
            setEpisodeNumber(matcher.group(3), token, false);
            if (StringUtils.isNotEmpty(matcher.group(4)))
                setEpisodeNumber(matcher.group(4), token, false);
            return true;
        }

        return false;
    }

    /**
     * Match type and episode. e.g. "2x01", "S01E03", "S01-02xE001-150".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchTypeAndEpisodePattern(String word, Token token) {
        int numberBegin = ParserHelper.indexOfFirstDigit(word);
        String prefix = StringUtils.substring(word, 0, numberBegin);

        AtomicReference<ElementCategory> category = new AtomicReference<>(kElementAnimeType);
        AtomicReference<KeywordOptions> options = new AtomicReference<>();

        if (KeywordManager.getInstance().findAndSet(KeywordManager.normalzie(prefix), category, options)) {
            parser.getElements().add(new Element(kElementAnimeType, prefix));
            String number = StringUtils.substring(word, numberBegin);
            if (matchEpisodePatterns(number, token) || setEpisodeNumber(number, token, true)) {
                int foundIdx = parser.getTokens().indexOf(token);
                if (foundIdx != -1) {
                    token.setContent(number);
                    parser.getTokens()
                            .add(foundIdx, new Token(options.get().isIdentifiable() ? kIdentifier
                                                                                    : kUnknown,
                                                     prefix,
                                                     token.isEnclosed()));
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Match fractional episodes. e.g. "07.5".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchFractionalEpisodePattern(String word, Token token) {
        if (StringUtils.isEmpty(word)) word = "";
        String regexPattern = "\\d+\\.5";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            if (setEpisodeNumber(word, token, true))
                return true;
        }

        return false;
    }

    /**
     * Match partial episodes episodes. "4a", "111C".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchPartialEpisodePattern(String word, Token token) {
        if (StringUtils.isEmpty(word)) return false;
        int foundIdx = IntStream.rangeClosed(0, word.length())
                .filter(value -> !Character.isDigit(word.charAt(value)))
                .findFirst()
                .orElse(word.length());
        int suffixLength = word.length() - foundIdx;

        Function<Integer, Boolean> isValidSuffix = c -> (c >= 'A' && c <= 'C') || (c >= 'a' && c <= 'c');

        if (suffixLength == 1 && isValidSuffix.apply((int) word.charAt(foundIdx)))
            if (setEpisodeNumber(word, token, true))
                return true;

        return false;
    }

    /**
     * Match partial episodes episodes. e.g. "#01", "#02-03v2".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchNumberSignPattern(String word, Token token) {
        if (StringUtils.isEmpty(word) || word.charAt(0) != '#') word = "";
        String regexPattern = "#(\\d{1,3})(?:[-~&+](\\d{1,3}))?(?:[vV](\\d))?";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            if (setEpisodeNumber(matcher.group(1), token, true)) {
                if (StringUtils.isNotEmpty(matcher.group(2)))
                    setEpisodeNumber(matcher.group(2), token, false);
                if (StringUtils.isNotEmpty(matcher.group(3)))
                    parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(3)));
                return true;
            }
        }

        return false;
    }

    /**
     * Match Japanese patterns. e.g. U+8A71 is used as counter for stories, episodes of TV series, etc.
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchJapaneseCounterPattern(String word, Token token) {
        if (StringUtils.isEmpty(word) || word.charAt(word.length() - 1) != '\u8A71') return false;
        String regexPattern = "(\\d{1,3})\u8A71";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            setEpisodeNumber(matcher.group(1), token, false);
            return true;
        }

        return false;
    }

    /************ V O L U M E  M A T C H E R S ********** */

    /**
     * Attempts to find an volume numbers inside a {@code word}.
     *
     * @param word  the word
     * @param token the token
     * @return true if the word was matched to an episode/season number
     */
    public boolean matchVolumePatterns(String word, Token token) {
        // All patterns contain at least one non-numeric character
        if (StringHelper.isNumericString(word)) return false;

        word = StringHelper.trimAny(word, " -");

        boolean numericFront = Character.isDigit(word.charAt(0));
        boolean numericBack = Character.isDigit(word.charAt(word.length() - 1));

        // e.g. "01v2"
        if (numericFront && numericBack)
            if (matchSingleVolumePattern(word, token))
                return true;
        // e.g. "01-02", "03-05v2"
        if (numericFront && numericBack)
            if (matchMultiVolumePattern(word, token))
                return true;

        return false;
    }

    /**
     * Match single volume. e.g. "01v2".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchSingleVolumePattern(String word, Token token) {
        if (StringUtils.isEmpty(word)) word = "";
        String regexPattern = "(\\d{1,2})[vV](\\d)";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            setVolumeNumber(matcher.group(1), token, false);
            parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(2)));
            return true;
        }

        return false;
    }

    /**
     * Match multi-volume. e.g. "01-02", "03-05v2".
     *
     * @param word  the word
     * @param token the token
     * @return true if the token matched
     */
    public boolean matchMultiVolumePattern(String word, Token token) {
        if (StringUtils.isEmpty(word)) word = "";
        String regexPattern = "(\\d{1,2})[-~&+](\\d{1,2})(?:[vV](\\d))?";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            String lowerBound = matcher.group(1);
            String upperBound = matcher.group(2);
            if (StringHelper.stringToInt(lowerBound) < StringHelper.stringToInt(upperBound)) {
                if (setVolumeNumber(lowerBound, token, true)) {
                    setVolumeNumber(upperBound, token, false);
                    if (StringUtils.isNotEmpty(matcher.group(3)))
                        parser.getElements().add(new Element(kElementReleaseVersion, matcher.group(3)));
                    return true;
                }
            }
        }

        return false;
    }

    /************ S E A R C H ********** */

    /**
     * Searches for isolated numbers in a list of {@code tokens}.
     *
     * @param tokens the list of tokens
     * @return true if an isolated number was found
     */
    public boolean searchForIsolatedNumbers(List<Result> tokens) {
        for (Result it : tokens) {
            if (!it.token.isEnclosed() || !parser.getParserHelper().isTokenIsolated(it.pos)) continue;
            if (setEpisodeNumber(it.token.getContent(), it.token, true)) return true;
        }

        return false;
    }

    /**
     * Searches for separated numbers in a list of {@code tokens}.
     *
     * @param tokens the list of tokens
     * @return true if a separated number was found
     */
    public boolean searchForSeparatedNumbers(List<Result> tokens) {
        for (Result it : tokens) {
            Result previousToken = Token.findPrevToken(parser.getTokens(), it, TokenFlag.kFlagNotDelimiter);

            // See if the number has a preceding "-" separator
            if (ParserHelper.isTokenCategory(previousToken.token, kUnknown)
                    && ParserHelper.isDashCharacter(previousToken.token.getContent().charAt(0))) {
                if (setEpisodeNumber(it.token.getContent(), it.token, true)) {
                    previousToken.token.setCategory(kIdentifier);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Searches for episode patterns in a list of {@code tokens}.
     *
     * @param tokens the list of tokens
     * @return true if an episode number was found
     */
    public boolean searchForEpisodePatterns(List<Result> tokens) {
        for (Result it : tokens) {
            boolean numericFront = it.token.getContent().length() > 0 && Character.isDigit(it.token.getContent()
                                                                                                   .charAt(0));

            if (!numericFront) {
                // e.g. "EP.1", "Vol.1"
                if (numberComesAfterPrefix(kElementEpisodePrefix, it.token))
                    return true;
                if (numberComesAfterPrefix(kElementVolumePrefix, it.token))
                    continue;
            } else {
                // e.g. "8 of 12"
                if (numberComesBeforeTotalNumber(it.token, it.pos))
                    return true;
            }

            // Look for other patterns
            if (matchEpisodePatterns(it.token.getContent(), it.token)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches for equivalent number in a list of {@code tokens}. e.g 08(114)
     *
     * @param tokens the list of tokens
     * @return true if an equivalent number was found
     */
    public boolean searchForEquivalentNumbers(List<Result> tokens) {
        for (Result it : tokens) {
            // find number must be isolated
            if (parser.getParserHelper().isTokenIsolated(it.pos) || !isValidEpisodeNumber(it.token.getContent())) {
                continue;
            }

            // Find the first enclosed, non-delimiter token
            Result nextToken = Token.findNextToken(parser.getTokens(), it.pos, kFlagNotDelimiter);
            if (!ParserHelper.isTokenCategory(nextToken, kBracket)) continue;
            nextToken = Token.findNextToken(parser.getTokens(), nextToken, kFlagEnclosed, kFlagNotDelimiter);
            if (!ParserHelper.isTokenCategory(nextToken, kUnknown)) continue;

            // Check if it's an isolated number
            if (!parser.getParserHelper().isTokenIsolated(nextToken.pos)
                    || !StringHelper.isNumericString(nextToken.token.getContent())
                    || !isValidEpisodeNumber(nextToken.token.getContent())) {
                continue;
            }

            List<Token> list = Arrays.asList(it.token, nextToken.token);
            list.sort((o1, o2) -> Integer.compare(StringHelper.stringToInt(o1.getContent()),
                                                  StringHelper.stringToInt(o2.getContent())));
            setEpisodeNumber(list.get(0).getContent(), list.get(0), false);
            setAlternativeEpisodeNumber(list.get(1).getContent(), list.get(1));
            return true;
        }

        return false;
    }

    /**
     * Searches for the last number token in a list of {@code tokens}.
     *
     * @param tokens the list of tokens
     * @return true if the last number token was found
     */
    public boolean searchForLastNumber(List<Result> tokens) {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            Result it = tokens.get(i);

            // Assuming that episode number always comes after the title, first token
            // cannot be what we're looking for
            if (it.pos == 0) continue;

            if (it.token.isEnclosed()) continue;

            // Ignore if it's the first non-enclosed, non-delimiter token
            if (parser.getTokens().subList(0, it.pos)
                    .stream().allMatch(r -> r.isEnclosed() || r.getCategory() == kDelimiter)) {
                continue;
            }

            // Ignore if the previous token is "Movie" or "Part"
            Result previousToken = Token.findPrevToken(parser.getTokens(), it, TokenFlag.kFlagNotDelimiter);
            if (ParserHelper.isTokenCategory(previousToken, kUnknown)) {
                if (StringUtils.equalsIgnoreCase(previousToken.token.getContent(), "Movie")
                        || StringUtils.equalsIgnoreCase(previousToken.token.getContent(), "Part")) {
                    continue;
                }
            }

            // We'll use this number after all
            if (setEpisodeNumber(it.token.getContent(), it.token, true))
                return true;
        }

        return false;
    }
}
