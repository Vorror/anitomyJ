/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.anitomyj;

import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kDelimiter;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kInvalid;
import static com.dgtlrepublic.anitomyj.Token.TokenCategory.kUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagDelimiter;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagEnclosed;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotBracket;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotDelimiter;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotEnclosed;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotIdentifier;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagNotValid;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagUnknown;
import static com.dgtlrepublic.anitomyj.Token.TokenFlag.kFlagValid;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.collections.CollectionUtils;

/**
 * An anime filename is tokenized into individual {@link Token}s. This class represents that individual token.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
class Token {
    /** The category of the token */
    enum TokenCategory {
        kUnknown,
        kBracket,
        kDelimiter,
        kIdentifier,
        kInvalid
    }

    /** TokenFlag, used for searching specific token categories. This allows granular searching of TokenCategories */
    enum TokenFlag {
        /** None */
        kFlagNone,

        /** Categories */
        kFlagBracket, kFlagNotBracket,
        kFlagDelimiter, kFlagNotDelimiter,
        kFlagIdentifier, kFlagNotIdentifier,
        kFlagUnknown, kFlagNotUnknown,
        kFlagValid, kFlagNotValid,

        /** Enclosed (Enclosed in some bracket (e.g [ ENCLOSED VALUE ] ) */
        kFlagEnclosed, kFlagNotEnclosed
    }

    /** Set of token category flags */
    private static final EnumSet<TokenFlag> kFlagMaskCategories = EnumSet.of(kFlagBracket, kFlagNotBracket,
                                                                             kFlagDelimiter, kFlagNotDelimiter,
                                                                             kFlagIdentifier, kFlagNotIdentifier,
                                                                             kFlagUnknown, kFlagNotUnknown,
                                                                             kFlagValid, kFlagNotValid);

    /** Set of token enclosed flags */
    private static final EnumSet<TokenFlag> kFlagMaskEnclosed = EnumSet.of(kFlagEnclosed, kFlagNotEnclosed);

    private TokenCategory category;
    private String content;
    private final boolean enclosed;

    /**
     * Constructs a new token.
     *
     * @param category the token category
     * @param content  the token content
     * @param enclosed whether or not the token is enclosed in braces
     */
    public Token(TokenCategory category, String content, boolean enclosed) {
        this.category = category;
        this.content = content;
        this.enclosed = enclosed;
    }

    /** Returns the token category. */
    public TokenCategory getCategory() {
        return category;
    }

    /** Sets the token category. */
    public void setCategory(TokenCategory category) {
        this.category = category;
    }

    /** Returns the token content. */
    public String getContent() {
        return content;
    }

    /** Sets the token content */
    public void setContent(String content) {
        this.content = content;
    }

    /** Returns whether or not the token is enclosed in braces. */
    public boolean isEnclosed() {
        return enclosed;
    }

    /**
     * Validates a token against the {@code flags}. The {@code flags} is used as a search parameter.
     *
     * @param token the token
     * @param flags the flags the token must conform against
     * @return true if the token conforms to the set of {@code flags}; false otherwise
     */
    public static boolean checkTokenFlags(Token token, EnumSet<TokenFlag> flags) {
        /** simple alias to check if flag is a part of the set */
        Function<TokenFlag, Boolean> checkFlag = flags::contains;

        /** make sure token is the correct closure */
        if (CollectionUtils.containsAny(flags, kFlagMaskEnclosed)) {
            boolean success = checkFlag.apply(kFlagEnclosed) == token.enclosed;
            if (!success) return false; /** not enclosed correctly (e.g enclosed when we're looking for non-enclosed) */
        }

        /** make sure token is the correct category */
        if (CollectionUtils.containsAny(flags, kFlagMaskCategories)) {
            AtomicBoolean success = new AtomicBoolean(false);
            TriConsumer<TokenFlag, TokenFlag, TokenCategory> checkCategory = (fe, fn, c) -> {
                if (!success.get()) {
                    boolean result = checkFlag.apply(fe) ? token.category == c :
                                     checkFlag.apply(fn) && token.category != c;
                    success.set(result);
                }
            };

            checkCategory.accept(kFlagBracket, kFlagNotBracket, kBracket);
            checkCategory.accept(kFlagDelimiter, kFlagNotDelimiter, kDelimiter);
            checkCategory.accept(kFlagIdentifier, kFlagNotIdentifier, kIdentifier);
            checkCategory.accept(kFlagUnknown, kFlagNotUnknown, kUnknown);
            checkCategory.accept(kFlagNotValid, kFlagValid, kInvalid);
            if (!success.get()) return false;
        }

        return true;
    }

    /**
     * Given a list of {@code tokens}, searches for any token that matches the list of {@code flags}.
     *
     * @param tokens the list of tokens
     * @param begin  the search starting position. <i>Inclusive</i> of {@code begin.pos}.
     * @param flags  the search flags
     * @return the search result
     */
    public static Result findToken(List<Token> tokens, Result begin, TokenFlag... flags) {
        if (begin == null || begin.pos == null) return Result.getEmptyResult();
        return findTokenBase(tokens, begin.pos, i -> i < tokens.size(), i -> i + 1, flags);
    }

    /**
     * Given a list of {@code tokens}, searches for any token that matches the list of {@code flags}.
     *
     * @param tokens the list of tokens
     * @param flags  the search flags
     * @return the search result
     */
    public static Result findToken(List<Token> tokens, TokenFlag... flags) {
        return findTokenBase(tokens, 0, i -> i < tokens.size(), i -> i + 1, flags);
    }

    /**
     * Given a list of {@code tokens}, searches for the <i>next</i> token in {@code tokens} that matches the list of
     * {@code flags}.
     *
     * @param tokens   the list of tokens
     * @param position the search starting position. <i>Exclusive</i>.
     * @param flags    the search flags
     * @return the search result
     */
    public static Result findNextToken(List<Token> tokens, int position, TokenFlag... flags) {
        return findTokenBase(tokens, ++position, i -> i < tokens.size(), i -> i + 1, flags);
    }

    /**
     * Given a list of {@code tokens}, searches for the <i>next</i> token in {@code tokens} that matches the list of
     * {@code flags}.
     *
     * @param tokens   the list of tokens
     * @param position the search starting position. <i>Exclusive of position.pos</i>.
     * @param flags    the search flags
     * @return the search result
     */
    public static Result findNextToken(List<Token> tokens, Result position, TokenFlag... flags) {
        return findTokenBase(tokens, position.pos + 1, i -> i < tokens.size(), i -> i + 1, flags);
    }

    /**
     * Given a list of {@code tokens}, searches for the <i>previous</i> token in {@code tokens} that matches the list of
     * {@code flags}.
     *
     * @param tokens   the list of tokens
     * @param position the search starting position. <i>Exclusive</i>.
     * @param flags    the search flags
     * @return the search result
     */
    public static Result findPrevToken(List<Token> tokens, int position, TokenFlag... flags) {
        return findTokenBase(tokens, --position, i -> i >= 0, i -> i - 1, flags);
    }

    /**
     * Given a list of {@code tokens}, searches for the <i>previous</i> token in {@code tokens} that matches the list of
     * {@code flags}.
     *
     * @param tokens   the list of tokens
     * @param position the search starting position. <i>Exclusive of position.pos</i>.
     * @param flags    the search flags
     * @return the search result
     */
    public static Result findPrevToken(List<Token> tokens, Result position, TokenFlag... flags) {
        return findTokenBase(tokens, position.pos - 1, i -> i >= 0, i -> i - 1, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token token = (Token) o;
        return enclosed == token.enclosed &&
                category == token.category &&
                Objects.equals(content, token.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, content, enclosed);
    }

    @Override
    public String toString() {
        return "Token{" +
                "category=" + category +
                ", content='" + content + '\'' +
                ", enclosed=" + enclosed +
                '}';
    }

    /************ P R I V A T E  A P I ********** */

    /**
     * Given a list of tokens finds the first token the passes {@link #checkTokenFlags(Token, EnumSet)}.
     *
     * @param tokens         the list of the tokens to search
     * @param startIdx       the start index of the search. Inclusive.
     * @param shouldContinue a function that returns whether or not we should continue searching
     * @param next           a function that returns the next search index
     * @param flags          the flags the each token should be validated against
     * @return the found token
     */
    private static Result findTokenBase(List<Token> tokens,
                                        int startIdx,
                                        Function<Integer, Boolean> shouldContinue,
                                        Function<Integer, Integer> next,
                                        TokenFlag... flags) {
        EnumSet<TokenFlag> find = EnumSet.noneOf(TokenFlag.class);
        find.addAll(Arrays.asList(flags));

        for (int i = startIdx; shouldContinue.apply(i); i = next.apply(i)) {
            Token token = tokens.get(i);
            if (checkTokenFlags(token, find)) {
                return new Result(token, i);
            }
        }

        return new Result(null, null);
    }

    /** Search result for finds */
    public static class Result {
        public Token token;

        public Integer pos;

        /**
         * Constructs a new search result.
         *
         * @param token       the found token
         * @param searchIndex the index the token was found
         */
        public Result(Token token, Integer searchIndex) {
            this.token = token;
            this.pos = searchIndex;
        }

        /** Returns an empty search result. */
        public static Result getEmptyResult() {
            return new Result(null, null);
        }

        @Override
        public String toString() {
            return "Result{" +
                    "token=" + token +
                    ", pos=" + pos +
                    '}';
        }
    }
}
