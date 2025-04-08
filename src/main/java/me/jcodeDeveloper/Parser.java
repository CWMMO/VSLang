package me.jcodeDeveloper;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Parser {
    @Contract(pure = true)
    public static void execute(@NotNull List<Lexer.Token> lexed) {
        Executor base = Executor.STATIC_START_EXEC();
        boolean commentMode = false;

        for (Lexer.Token token : lexed) {
            if (commentMode && token.TYPE() != Lexer.TokenType.TOG_COM) continue;

            switch (token.TYPE()) {
                case ADD -> base.INCREMENT();
                case SUB -> base.DECREMENT();

                case MOV_LFT -> base.POINT_LEFT();
                case MOV_RGT -> base.POINT_RIGHT();

                case OUT -> base.OUTPUT_CURRENT_CELL();
                case OUT_QUE -> base.OUTPUT_QUEUE();
                case OUT_ALL_QUE -> base.OUTPUT_ALL_TAPE();
                case OUT_STACK -> base.OUTPUT_CURRENT_TAPE();

                case INPUT -> base.READ_INPUT_CHAR();

                case TOG_COM -> commentMode = !commentMode;
                case COP -> base.COPY_CURR_CELL();
                case PST -> base.PASTE_COPIED_CELL();

                case CHK_0 -> base.CHECK_CELL_FOR_ZERO();
                case CHK_N0 -> base.CHECK_CELL_FOR_NON_ZERO();

                case FIN -> base.EXECUTE_OPERATIONS();
                case TERM -> base.TERMINATE();

                case STR -> base.STORE_OVER_CELLS(token.VAL_OR_PARAM());

                case NEW_TP -> base.NEW_TAPE();
                case MOV_TP_L -> base.TAPE_LEFT();
                case MOV_TP_R -> base.TAPE_RIGHT();

                case ASYNC_EXECUTE -> base.ASYNC_EXECUTE(Lexer.parse(token.VAL_OR_PARAM(), false));
                case ASYNC_COPY -> base.ASYNC_COPY_DATA();

                case EOF -> {
                    base.EXECUTE_OPERATIONS();
                    base.TERMINATE();

                }

                case IGNORED_TEXT, UNKNOWN -> {}
                default -> throw new RuntimeException("Unknown token: "+token);
            }
        }
    }
}
