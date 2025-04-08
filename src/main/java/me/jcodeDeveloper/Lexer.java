package me.jcodeDeveloper;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    final List<Token> tokens;
    final String unparsed;

    private Lexer(String parse) {
        this.unparsed = parse;
        this.tokens = new ArrayList<>();
    }
    private void lex() {
        char[] decon = unparsed.toCharArray();

        int end = -1;
        boolean ignoreMode = false;
        for (int i = 0; i < decon.length; i++) {
            char currentToken = decon[i];
            char nextToken = (i+1 < unparsed.length()) ? decon[i+1] : ' ';
            // Skips over the current token if the text is between two ~ ~s
            if (ignoreMode && currentToken != '~') {
                tokens.add(new Token(String.valueOf(currentToken), TokenType.IGNORED_TEXT, i, i+1));
                continue;
            }
            switch (currentToken) {
                case '.' -> tokens.add(new Token("", TokenType.OUT, i, i+1));
                case ',' -> tokens.add(new Token("", TokenType.INPUT, i, i+1));
                case '+' -> tokens.add(new Token("", TokenType.ADD, i, i+1));
                case '-' -> tokens.add(new Token("", TokenType.SUB, i, i+1));
                case '>' -> tokens.add(new Token("", TokenType.MOV_RGT, i, i+1));
                case '<' -> tokens.add(new Token("", TokenType.MOV_LFT, i, i+1));
                case '~' -> {
                    ignoreMode = !ignoreMode;
                    tokens.add(new Token("", TokenType.TOG_COM, i, i+1));
                }
                case '[' -> tokens.add(new Token("", TokenType.CHK_0, i, i+1));
                case ']' -> tokens.add(new Token("", TokenType.CHK_N0, i, i+1));
                case '@' -> tokens.add(new Token("", TokenType.OUT_STACK, i, i+1));
                case '$' -> tokens.add(new Token("", TokenType.OUT_ALL_QUE, i, i+1));
                case '#' -> tokens.add(new Token("", TokenType.OUT_QUE, i, i+1));
                case '^' -> tokens.add(new Token("", TokenType.COP, i, i+1));
                case '&' -> tokens.add(new Token("", TokenType.PST, i, i+1));
                case '*' -> tokens.add(new Token("", TokenType.FIN, i, i+1));
                case '!' -> tokens.add(new Token("", TokenType.TERM, i, i+1));
                case '/' -> tokens.add(new Token("", TokenType.MOV_TP_R, i, i+1));
                case '\\' -> tokens.add(new Token("", TokenType.MOV_TP_L, i, i+1));
                case '|' -> tokens.add(new Token("", TokenType.NEW_TP, i, i+1));
                
                case '_' -> {
                    int start = i;
                    if (nextToken == '{') {
                        StringBuilder parameter = new StringBuilder();
                        i++;
                        while (currentToken != '}') {
                            i++;
                            currentToken = decon[i];

                            parameter.append(currentToken);
                        }
                        tokens.add(new Token(parameter.toString(), TokenType.STR, start, i));
                    }
                    else throw new Executor.UnexpectedTokenException(nextToken, '{');
                }
                case '%' -> {
                    int start = i-1;
                    if (nextToken == '{') {
                        StringBuilder parameter = new StringBuilder();
                        i++;
                        while (currentToken != '}') {
                            i++;
                            currentToken = decon[i];

                            parameter.append(currentToken);
                        }
                        tokens.add(new Token(parameter.toString(), TokenType.ASYNC_EXECUTE, start, i));
                    } else throw new Executor.UnexpectedTokenException(nextToken, '{');
                }
                case '"' -> tokens.add(new Token("", TokenType.ASYNC_COPY, i, i+1));
                case ' ', '\t', '\n' -> {}
                default -> tokens.add(new Token(String.valueOf(currentToken), TokenType.UNKNOWN, i, i+1));
            }
            end = i;
        }
        tokens.add(new Token("", TokenType.EOF, end, end+1));

    }

    public static List<Token> parse(String input, boolean tree) {
        Lexer lexer = new Lexer(input);
        lexer.lex();
        if (tree) System.out.println("Finished parsing. Parse tree:\n"+ TreeBuilder.build(lexer.tokens));

        return lexer.tokens;
    }

    public enum TokenType {
        ADD, SUB, // +, -
        MOV_LFT, MOV_RGT, // <, >
        OUT, OUT_STACK, OUT_QUE, OUT_ALL_QUE, INPUT, // ., @, $, #, `,`,
        TOG_COM, CHK_0, CHK_N0, FIN, TERM, // ~, [, ], *, !
        COP, PST, // ^, &
        STR, //_{...}
        MOV_TP_R, MOV_TP_L, NEW_TP, // /, \, |
        ASYNC_EXECUTE, ASYNC_COPY, // %{...}, `
        UNKNOWN, IGNORED_TEXT, // UNKNOWN represents tokens that are unknown (duh)
        // IGNORED_TEXT represents commented out text
        EOF
    }
    public record Token(String VAL_OR_PARAM, TokenType TYPE, int START_POSITION, int END_POSITION) {
        @Contract(pure = true)
        @Override
        public @NotNull String toString() {
            return "Token{" +
                    "VALUE='" + VAL_OR_PARAM + '\'' +
                    ", TYPE=" + TYPE +
                    ", START_POSITION=" + START_POSITION +
                    ", END_POSITION=" + END_POSITION +
                    "}";
        }
    }
}
