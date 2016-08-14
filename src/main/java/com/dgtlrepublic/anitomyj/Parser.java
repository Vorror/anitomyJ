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
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeTitle;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeType;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementAnimeYear;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodeNumber;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodePrefix;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementEpisodeTitle;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementFileChecksum;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseGroup;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementReleaseVersion;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementUnknown;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVideoResolution;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVolumeNumber;
import static com.dgtlrepublic.anitomyj.Element.ElementCategory.kElementVolumePrefix;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagEnclosed;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNone;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotDelimiter;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotEnclosed;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagUnknown;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.dgtlrepublic.anitomyj.Element.ElementCategory;
import com.dgtlrepublic.anitomyj.KeywordManager.KeywordOptions;
import com.dgtlrepublic.anitomyj.Token.Result;
import com.dgtlrepublic.anitomyj.Token.TokenFlag;

/**
 * Class to classify {@link Token}s.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class Parser {
    private final List<Element> elements;
    private final Options options;
    private final List<Token> tokens;
    private final ParserHelper parserHelper;
    private final ParserNumber parserNumber;
    private boolean isEpisodeKeywordsFound = false;

    /**
     * Constructs a new token parser.
     *
     * @param elements the list where parsed elements will be added
     * @param options  the parser options
     * @param tokens   the list of tokens.
     */
    public Parser(List<Element> elements, Options options, List<Token> tokens) {
        this.elements = Objects.requireNonNull(elements);
        this.options = Objects.requireNonNull(options);
        this.tokens = Objects.requireNonNull(tokens);
        this.parserHelper = new ParserHelper(this);
        this.parserNumber = new ParserNumber(this);
    }

    /** Returns the list of elements. */
    public List<Element> getElements() {
        return elements;
    }

    /** Returns the list of tokens. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** Returns the parser helper. */
    public ParserHelper getParserHelper() {
        return parserHelper;
    }

    /** Returns the number parser. */
    public ParserNumber getParserNumber() {
        return parserNumber;
    }

    /** Returns whether or not episode keywords were found. */
    public boolean isEpisodeKeywordsFound() {
        return isEpisodeKeywordsFound;
    }

    /** Begins the parsing process */
    public boolean parse() {
        searchForKeywords();
        searchForIsolatedNumbers();

        if (options.parseEpisodeNumber) {
            SearchForEpisodeNumber();
        }

        searchForAnimeTitle();

        if (options.parseReleaseGroup && empty(kElementReleaseGroup)) {
            searchForReleaseGroup();
        }

        if (options.parseEpisodeTitle && !empty(kElementEpisodeNumber)) {
            searchForEpisodeTitle();
        }

        validateElements();
        return empty(kElementAnimeTitle);
    }

    /** Search for anime keywords. */
    private void searchForKeywords() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getCategory() != kUnknown) continue;

            String word = token.getContent();
            word = StringHelper.trimAny(word, " -");
            if (word.isEmpty()) continue;

            // Don't bother if the word is a number that cannot be CRC
            if (word.length() != 8 && StringHelper.isNumericString(word)) continue;

            String keyword = KeywordManager.normalzie(word);
            AtomicReference<ElementCategory> category = new AtomicReference<>(kElementUnknown);
            AtomicReference<KeywordOptions> options = new AtomicReference<>(new KeywordOptions());

            if (KeywordManager.getInstance().findAndSet(keyword, category, options)) {
                if (!this.options.parseReleaseGroup && category.get() == kElementReleaseGroup)
                    continue;
                if (!ParserHelper.isElementCategorySearchable(category.get()) || !options.get().isSearchable())
                    continue;
                if (ParserHelper.isElementCategorySingular(category.get()) && !empty(category.get()))
                    continue;
                if (category.get() == kElementAnimeSeasonPrefix) {
                    parserHelper.checkAndSetAnimeSeasonKeyword(token, i);
                    continue;
                } else if (category.get() == kElementEpisodePrefix) {
                    if (options.get().isValid()) {
                        parserHelper.checkExtentKeyword(kElementEpisodeNumber, i, token);
                        continue;
                    }
                } else if (category.get() == kElementReleaseVersion) {
                    word = StringUtils.substring(word, 1);
                } else if (category.get() == kElementVolumePrefix) {
                    parserHelper.checkExtentKeyword(kElementVolumeNumber, i, token);
                    continue;
                }
            } else {
                if (empty(kElementFileChecksum) && ParserHelper.isCrc32(word)) {
                    category.set(kElementFileChecksum);
                } else if (empty(kElementVideoResolution) && ParserHelper.isResolution(word)) {
                    category.set(kElementVideoResolution);
                }
            }

            if (category.get() != kElementUnknown) {
                elements.add(new Element(category.get(), word));
                if (options.get() != null && options.get().isIdentifiable()) {
                    token.setCategory(kIdentifier);
                }
            }
        }
    }

    /** Search for episode number. */
    private void SearchForEpisodeNumber() {
        // List all unknown tokens that contain a number
        List<Result> tokens = new ArrayList<>();
        for (int i = 0; i < this.tokens.size(); i++) {
            Token token = this.tokens.get(i);
            if (token.getCategory() == kUnknown && ParserHelper.indexOfFirstDigit(token.getContent()) != -1) {
                tokens.add(new Result(token, i));
            }
        }

        if (tokens.isEmpty()) return;

        isEpisodeKeywordsFound = !empty(kElementEpisodeNumber);

        // If a token matches a known episode pattern, it has to be the episode number
        if (parserNumber.searchForEpisodePatterns(tokens)) return;

        // We have previously found an episode number via keywords
        if (!empty(kElementEpisodeNumber)) return;

        // From now on, we're only interested in numeric tokens
        tokens.removeIf(r -> !StringHelper.isNumericString(r.token.getContent()));

        // e.g. "01 (176)", "29 (04)"
        if (parserNumber.searchForEquivalentNumbers(tokens)) return;

        // e.g. " - 08"
        if (parserNumber.searchForSeparatedNumbers(tokens)) return;

        // e.g. "[12]", "(2006)"
        if (parserNumber.searchForIsolatedNumbers(tokens)) return;

        // Consider using the last number as a last resort
        parserNumber.searchForLastNumber(tokens);
    }

    /** Search for anime title. */
    private void searchForAnimeTitle() {
        boolean enclosed_title = false;

        Result token_begin = Token.findToken(tokens, kFlagNotEnclosed, kFlagUnknown);

        // If that doesn't work, find the first unknown token in the second enclosed
        // group, assuming that the first one is the release group
        if (token_begin.token == null) {
            token_begin = new Result(null, 0);
            enclosed_title = true;
            boolean skipped_previous_group = false;

            do {
                token_begin = Token.findToken(tokens, token_begin, kFlagUnknown);
                if (token_begin.token == null) break;

                // Ignore groups that are composed of non-Latin characters
                if (StringHelper.isMostlyLatinString(token_begin.token.getContent()) && skipped_previous_group) {
                    break;
                }

                // Get the first unknown token of the next group
                token_begin = Token.findToken(tokens, token_begin, kFlagBracket);
                token_begin = Token.findToken(tokens, token_begin, kFlagUnknown);
                skipped_previous_group = true;
            } while (token_begin.token != null);
        }

        if (token_begin.token == null) return;

        // Continue until an identifier (or a bracket, if the title is enclosed)
        // is found
        Result token_end = Token.findToken(tokens,
                                           token_begin,
                                           kFlagIdentifier,
                                           enclosed_title ? kFlagBracket : kFlagNone);

        // If within the interval there's an open bracket without its matching pair,
        // move the upper endpoint back to the bracket
        if (!enclosed_title) {
            int end = token_end.pos != null ? token_end.pos : tokens.size();
            Result last_bracket = token_end;
            boolean bracket_open = false;
            for (int i = token_begin.pos; i < end; i++) {
                Token token = tokens.get(i);
                if (token.getCategory() == kBracket) {
                    last_bracket = new Result(token, i);
                    bracket_open = !bracket_open;
                }
            }
            if (bracket_open) token_end = last_bracket;
        }

        // If the interval ends with an enclosed group (e.g. "Anime Title [Fansub]"),
        // move the upper endpoint back to the beginning of the group. We ignore
        // parentheses in order to keep certain groups (e.g. "(TV)") intact.
        if (!enclosed_title) {
            int end = token_end.pos != null ? token_end.pos : tokens.size();
            Result token = Token.findPrevToken(tokens, end, kFlagNotDelimiter);

            while (ParserHelper.isTokenCategory(token.token, kBracket)
                    && token.token.getContent().charAt(0) != ')') {

                token = Token.findPrevToken(tokens, token, kFlagBracket);
                if (token.pos != null) {
                    token_end = token;
                    token = Token.findPrevToken(tokens, token_end, kFlagNotDelimiter);
                }
            }
        }

        int end = tokens.size();
        if (token_end.token != null) end = Math.min(token_end.pos, end);
        parserHelper.buildElement(kElementAnimeTitle, false, tokens.subList(token_begin.pos, end));
    }

    /** Search for release group. */
    private void searchForReleaseGroup() {
        for (Result token_begin = new Result(null, 0), token_end = token_begin;
             token_begin.pos != null && token_begin.pos < tokens.size(); ) {

            // Find the first enclosed unknown token
            token_begin = Token.findToken(tokens, token_end, kFlagEnclosed, kFlagUnknown);
            if (token_begin.token == null) return;

            // Continue until a bracket or identifier is found
            token_end = Token.findToken(tokens, token_begin, kFlagBracket, kFlagIdentifier);
            if (token_end.token == null || token_end.token.getCategory() != kBracket) continue;

            // Ignore if it's not the first non-delimiter token in group
            Result prevToken = Token.findPrevToken(tokens, token_begin, TokenFlag.kFlagNotDelimiter);
            if (prevToken.token != null && prevToken.token.getCategory() != kBracket) continue;

            int end = tokens.size();
            end = Math.min(token_end.pos, end);
            parserHelper.buildElement(kElementReleaseGroup, true, tokens.subList(token_begin.pos, end));
            return;
        }
    }

    /** Search for episode title. */
    private void searchForEpisodeTitle() {
        // Find the first non-enclosed unknown token
        Result token_begin = Token.findToken(tokens, kFlagNotEnclosed, kFlagUnknown);
        if (token_begin.token == null) return;

        // Continue until a bracket or identifier is found
        Result token_end = Token.findToken(tokens, token_begin, kFlagBracket, kFlagIdentifier);

        int end = tokens.size();
        if (token_end.pos != null) end = Math.min(token_end.pos, end);
        parserHelper.buildElement(kElementEpisodeTitle, false, tokens.subList(token_begin.pos, end));
    }

    /** Search for isolated numbers. */
    private void searchForIsolatedNumbers() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getCategory() != kUnknown
                    || !StringHelper.isNumericString(token.getContent()) || !parserHelper.isTokenIsolated(i)) {
                continue;
            }

            int number = StringHelper.stringToInt(token.getContent());

            // Anime year
            if (number >= ParserNumber.kAnimeYearMin && number <= ParserNumber.kAnimeYearMax) {
                if (empty(kElementAnimeYear)) {
                    elements.add(new Element(kElementAnimeYear, token.getContent()));
                    token.setCategory(kIdentifier);
                    continue;
                }
            }

            // Video resolution
            if (number == 480 || number == 720 || number == 1080) {
                // If these numbers are isolated, it's more likely for them to be the
                // video resolution rather than the episode number. Some fansub groups use these without the "p" suffix.
                if (empty(kElementVideoResolution)) {
                    elements.add(new Element(kElementVideoResolution, token.getContent()));
                    token.setCategory(kIdentifier);
                }
            }
        }
    }

    /** Validate Elements. */
    private void validateElements() {
        if (!empty(kElementAnimeType) && !empty(kElementEpisodeTitle)) {
            String episode_title = get(kElementEpisodeTitle);

            for (int i = 0; i < elements.size(); ) {
                Element el = elements.get(i);

                if (el.getCategory() == kElementAnimeType) {
                    if (StringUtils.contains(episode_title, el.getValue())) {
                        if (episode_title.length() == el.getValue().length()) {
                            elements.removeIf(element -> element.getCategory() == kElementEpisodeTitle); // invalid episode title
                        } else {
                            String keyword = KeywordManager.normalzie(el.getValue());
                            if (KeywordManager.getInstance().contains(kElementAnimeType, keyword)) {
                                i = erase(el);  // invalid anime type
                                continue;
                            }
                        }
                    }
                }
                ++i;
            }
        }
    }

    /** Returns whether or not the parser contains this category. */
    private boolean empty(ElementCategory category) {
        return !elements.stream().anyMatch(element -> element.getCategory() == category);
    }

    /** Returns the value of a particular category. */
    private String get(ElementCategory category) {
        Element foundElement = elements.stream()
                .filter(element -> element.getCategory() == category)
                .findAny()
                .orElse(null);

        if (foundElement == null) {
            Element e = new Element(category, "");
            elements.add(e);
            foundElement = e;
        }

        return foundElement.getValue();
    }

    /** Deletes the first element with the same {@code element.category} and returns the deleted elements position. */
    private int erase(Element element) {
        int removedIdx = -1;
        for (ListIterator<Element> itr = elements.listIterator(); itr.hasNext(); ) {
            int idx = itr.nextIndex();
            Element curE = itr.next();
            if (element.getCategory() == curE.getCategory()) {
                removedIdx = idx;
                itr.remove();
                break;
            }
        }

        return removedIdx;
    }
}
