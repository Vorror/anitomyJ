/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagValid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.dgtlrepublic.anitomyj.Token.Result;
import com.dgtlrepublic.anitomyj.Token.TokenCategory;

/**
 * A class the will tokenize an anime filename.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class Tokenizer {
    private final String filename;
    private final List<Element> elements;
    private final Options options;
    private final List<Token> tokens;
    private static final List<Pair<String, String>> brackets;

    static {
        brackets = new ArrayList<>();
        brackets.add(Pair.of("(", ")")); // U+0028-U+0029 Parenthesis
        brackets.add(Pair.of("[", "]")); // U+005B-U+005D Square bracket
        brackets.add(Pair.of("{", "}")); // U+007B-U+007D Curly bracket
        brackets.add(Pair.of("\u300C", "\u300D"));  // Corner bracket
        brackets.add(Pair.of("\u300E", "\u300E"));  // White corner bracket
        brackets.add(Pair.of("\u3010", "\u3011")); // Black lenticular bracket
        brackets.add(Pair.of("\uFF08", "\uFF09")); // Fullwidth parenthesis
    }

    /**
     * Tokenize a filename into {@link Element}s.
     *
     * @param filename the filename
     * @param elements the list of elements where preidentified tokens will be added
     * @param options  the parser options
     * @param tokens   the list of tokens where tokens will be added.
     */
    public Tokenizer(String filename, List<Element> elements, Options options, List<Token> tokens) {
        this.filename = Objects.requireNonNull(filename);
        this.elements = Objects.requireNonNull(elements);
        this.options = Objects.requireNonNull(options);
        this.tokens = Objects.requireNonNull(tokens);
    }

    /** Returns true if tokenization was successful; false otherwise. */
    public boolean tokenize() {
        tokenizeByBrackets();
        return !tokens.isEmpty();
    }

    /**
     * Adds a token to the internal list of tokens.
     *
     * @param category the token category
     * @param enclosed whether or not the token is enclosed in braces
     * @param range    the token range
     */
    private void addToken(TokenCategory category, boolean enclosed, TokenRange range) {
        tokens.add(new Token(category,
                             StringUtils.substring(filename, range.getOffset(), range.getOffset() + range.getSize()),
                             enclosed));
    }

    /** Returns then delimiters found in a token range. */
    private String getDelimiters(TokenRange range) {
        final StringBuilder delimiters = new StringBuilder();

        Function<Integer, Boolean> isDelimiter = c -> {
            if (!StringHelper.isAlphanumericChar((char) c.intValue())) {
                if (StringUtils.containsAny(String.valueOf((char) c.intValue()), options.allowedDelimiters)) {
                    if (!StringUtils.containsAny(delimiters.toString(), String.valueOf((char) c.intValue()))) {
                        return true;
                    }
                }
            }

            return false;
        };

        IntStream.range(range.getOffset(), Math.min(filename.length(), range.getOffset() + range.getSize()))
                .filter(value -> isDelimiter.apply((int) filename.charAt(value)))
                .forEach(value -> delimiters.append(filename.charAt(value)));

        return delimiters.toString();
    }

    /** Tokenize by bracket. */
    private void tokenizeByBrackets() {
        /** a ref to the closing brace of a found opening brace. (e.g "}" if we found an "}") */
        AtomicReference<String> matchingBracket = new AtomicReference<>();

        /** function to find an opening brace */
        BiFunction<Integer, Integer, Integer> findFirstBracket = (start, end) -> {
            for (int i = start; i < end; i++) {
                for (Pair<String, String> bracket : brackets) {
                    if (String.valueOf(filename.charAt(i)).equals(bracket.getLeft())) {
                        matchingBracket.set(bracket.getRight());
                        return i;
                    }
                }
            }

            return -1;
        };

        boolean isBracketOpen = false;
        for (int i = 0; i < filename.length(); ) {
            int foundIdx;
            if (!isBracketOpen) {
                /** look for opening brace */
                foundIdx = findFirstBracket.apply(i, filename.length());
            } else {
                /** look for closing brace */
                foundIdx = filename.indexOf(matchingBracket.get(), i);
            }

            TokenRange range = new TokenRange(i, foundIdx == -1 ? filename.length() : foundIdx - i);
            if (range.getSize() > 0) {
                /** check if our range contains any known anime identifies */
                tokenizeByPreidentified(isBracketOpen, range);
            }

            if (foundIdx != -1) {
                /** mark as bracket */
                addToken(TokenCategory.kBracket, true, new TokenRange(range.getOffset() + range.getSize(), 1));
                isBracketOpen = !isBracketOpen;
                i = foundIdx + 1;
            } else {
                break;
            }
        }
    }

    /**
     * Tokenize by looking for known anime identifiers.
     *
     * @param enclosed whether or not the current {@code range} is enclosed in braces
     * @param range    the token range
     */
    private void tokenizeByPreidentified(boolean enclosed, TokenRange range) {
        List<TokenRange> preidentifiedTokens = new ArrayList<>();

        /** find known anime identifiers */
        KeywordManager.getInstance().peekAndAdd(filename, range, elements, preidentifiedTokens);

        int offset = range.getOffset();
        TokenRange subrange = new TokenRange(range.getOffset(), 0);
        while (offset < range.getOffset() + range.getSize()) {
            for (TokenRange preidentifiedToken : preidentifiedTokens) {
                if (offset == preidentifiedToken.getOffset()) {
                    if (subrange.getSize() > 0) {
                        tokenizeByDelimiters(enclosed, subrange);
                    }

                    addToken(TokenCategory.kIdentifier, enclosed, preidentifiedToken);
                    subrange.setOffset(preidentifiedToken.getOffset() + preidentifiedToken.getSize());
                    offset = subrange.getOffset() - 1;  /** It's going to be incremented below */
                    break;
                }
            }

            subrange.setSize(++offset - subrange.getOffset());
        }

        /** Either there was no preidentified token range, or we're now about to  process the tail of our current range. */
        if (subrange.getSize() > 0) {
            tokenizeByDelimiters(enclosed, subrange);
        }
    }

    /**
     * Tokenize by Delimiters allowed in {@link Options#allowedDelimiters}
     *
     * @param enclosed whether or not the current {@code range} is enclosed in braces
     * @param range    the token range
     */
    private void tokenizeByDelimiters(boolean enclosed, TokenRange range) {
        String delimiters = getDelimiters(range);

        if (delimiters.isEmpty()) {
            addToken(TokenCategory.kUnknown, enclosed, range);
            return;
        }

        for (int i = range.getOffset(), end = range.getOffset() + range.getSize(); i < end; ) {
            Integer found = IntStream.range(i, Math.min(end, filename.length()))
                    .filter(c -> StringUtils.containsAny(String.valueOf(filename.charAt(c)), delimiters))
                    .findFirst()
                    .orElse(end);

            TokenRange subrange = new TokenRange(i, found - i);
            if (subrange.getSize() > 0) {
                addToken(TokenCategory.kUnknown, enclosed, subrange);
            }

            if (found != end) {
                addToken(TokenCategory.kDelimiter,
                         enclosed,
                         new TokenRange(subrange.getOffset() + subrange.getSize(), 1));
                i = found + 1;
            } else {
                break;
            }
        }

        validateDelimiterTokens();
    }

    /** Validates tokens(e.g make sure certain words delimited by certain tokens aren't spit). */
    @SuppressWarnings("CodeBlock2Expr")
    private void validateDelimiterTokens() {
        Function<Result, Boolean> isDelimiterToken = r -> {
            return r != null && r.token != null && r.token.getCategory() == TokenCategory.kDelimiter;
        };

        Function<Result, Boolean> isUnknownToken = r -> {
            return r != null && r.token != null && r.token.getCategory() == TokenCategory.kUnknown;
        };

        Function<Result, Boolean> isSingleCharacterToken = r -> {
            return isUnknownToken.apply(r) && r.token.getContent().length() == 1 && !r.token
                    .getContent()
                    .equals("-");
        };

        BiConsumer<Token, Result> appendTokenTo = (src, dest) -> {
            dest.token.setContent(dest.token.getContent() + src.getContent());
            src.setCategory(TokenCategory.kInvalid); /** make dest as invalid so it's removed later */
        };

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getCategory() != TokenCategory.kDelimiter) continue;
            char delimiter = token.getContent().charAt(0);

            Result prevToken = Token.findPrevToken(tokens, i, kFlagValid);
            Result nextToken = Token.findNextToken(tokens, i, kFlagValid);

            // Check for single-character tokens to prevent splitting group names,
            // keywords, episode number, etc.
            if (delimiter != ' ' && delimiter != '_') {

                /** single character token */
                if (isSingleCharacterToken.apply(prevToken)) {
                    appendTokenTo.accept(token, prevToken);

                    while (isUnknownToken.apply(nextToken)) {
                        appendTokenTo.accept(nextToken.token, prevToken);

                        nextToken = Token.findNextToken(tokens, i, kFlagValid);
                        if (isDelimiterToken.apply(nextToken)
                                && nextToken.token.getContent().charAt(0) == delimiter) {
                            appendTokenTo.accept(nextToken.token, prevToken);
                            nextToken = Token.findNextToken(tokens, nextToken, kFlagValid);
                        }
                    }

                    continue;
                }

                if (isSingleCharacterToken.apply(nextToken)) {
                    appendTokenTo.accept(token, prevToken);
                    appendTokenTo.accept(nextToken.token, prevToken);
                    continue;
                }
            }

            /** Check for adjacent delimiters */
            if (isUnknownToken.apply(prevToken) && isDelimiterToken.apply(nextToken)) {
                char nextDelimiter = nextToken.token.getContent().charAt(0);
                if (delimiter != nextDelimiter && delimiter != ',') {
                    if (nextDelimiter == ' ' || nextDelimiter == '_') {
                        appendTokenTo.accept(token, prevToken);
                    }
                }
            }
        }

        /** remove invalid tokens */
        tokens.removeIf(token -> token.getCategory() == TokenCategory.kInvalid);
    }
}
