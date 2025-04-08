package me.jcodeDeveloper;

import org.jetbrains.annotations.ApiStatus.Obsolete;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public @Obsolete class TreeBuilder {
    @NotNull
    @Contract(pure = true)
    public static String build(@NotNull List<Lexer.Token> lexed) {
        StringBuilder builder = new StringBuilder();
        builder.append("{START");

        int offset = 1;
        List<Lexer.Token> modifiedTokens = getTokens(lexed);

        for (Lexer.Token token : modifiedTokens) {
            builder.append(",\n").append(getIndent(offset)).append("{");
            offset++;
            builder
                    .append("\n")
                    .append(getIndent(offset))
                    .append("Token type: ").append(token.TYPE()).append(",\n")
                    .append(getIndent(offset))
                    .append("Value: ").append(token.VAL_OR_PARAM()).append(",\n")
                    .append(getIndent(offset))
                    .append("Start: ").append(token.START_POSITION()).append(",\n")
                    .append(getIndent(offset))
                    .append("End: ").append(token.END_POSITION()).append(",\n");
            offset--;
            builder.append("\n").append(getIndent(offset)).append("}");
        }
        builder.append(",\n").append(getIndent(offset)).append("Tokens: ").append(modifiedTokens.size());
        builder.append(",\n").append(getIndent(offset)).append("EOF Token: ").append(Lexer.TokenType.EOF);
        return builder.append("\n}").toString().strip().replace("{START,", "{");
    }

    private static @NotNull List<Lexer.Token> getTokens(@NotNull List<Lexer.Token> lexed) {
        boolean IGNORE_MODE = false;
        StringBuilder IGNORE_TEXT = new StringBuilder();
        int IGNORE_START = -1;
        int IGNORE_END = -1;

        List<Lexer.Token> modifiedTokens = new ArrayList<>();

        for (Lexer.Token token : lexed) {
            if (token.TYPE() == Lexer.TokenType.IGNORED_TEXT) {
                if (!IGNORE_MODE) {
                    IGNORE_MODE = true;
                    IGNORE_START = token.START_POSITION();
                }
                IGNORE_TEXT.append(token.VAL_OR_PARAM());
                IGNORE_END = token.END_POSITION();
                continue;
            }

            if (IGNORE_MODE) {
                modifiedTokens.add(new Lexer.Token(IGNORE_TEXT.toString(), Lexer.TokenType.IGNORED_TEXT, IGNORE_START, IGNORE_END));
                IGNORE_MODE = false;
                IGNORE_TEXT.setLength(0);
                IGNORE_START = -1;
                IGNORE_END = -1;
            }

            modifiedTokens.add(token);
        }
        return modifiedTokens;
    }

    @NotNull
    private static String getIndent(int offset) {
        return "  ".repeat(Math.max(0, offset));
    }
}
