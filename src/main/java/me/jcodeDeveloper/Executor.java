package me.jcodeDeveloper;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.nio.BufferUnderflowException;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import java.util.Stack;

/// VSLang (a.k.a `_{BrainFuck}>.>.>.>.>.>.>.>.>.`) or Very Stupid Language is
/// a derivative of BrainFuck which uses an `n`-dimensional ({@link #tapes}) grid
/// of cells with multiple tapes, and an operation chain, as well as multiple tapes,
/// a tape pointer, async, and other utilities to improve on the popular language
/// BrainFuck.
///
/// The code, lexed by {@link Lexer}, and executed by the {@link Executor}, and then
/// is handled by this code.
///
/// To create a new VSLang script, go to the `/main/vslang` folder and create a
/// `.vslang` file with your code inside it.
///
/// > **BRAINFUCK SYNTAX:**<br>
/// > `+` Increment the pointed-to cell by 1<br>
/// > `-` Decrement the pointed-to cell by 1<br>
/// > `.` Print out the ASCII value of the current
///       cell<br>
/// > `,` Store the integer value of the inputted
///       char into the current cell<br>
/// > `[` If the cell is 0, jump to the corresponding
///       `]`<br>
/// > `]` If the cell is not 0, jump to the matching
///       `[`<br>
/// > **VSLANG ADDITIONS:**<br>
/// > `_{...}` Stores ... (A string) over the next
///            length of ... cells in integer forms<br>
/// > `~...~` Comments out the text (...) (New lines)
///           ARE allowed.<br>
/// > `@` Outputs the current tape.<br>
/// > `$` Outputs every tape.<br>
/// > `#` Outputs the current task queue<br>
/// > `^` Copies the current cell's value<br>
/// > `&` Pastes the copied cell (0 if no cell
///       is copied)'s value into the current
///       cell.<br>
/// > `*` Finish execution early.<br>
/// > `!` Terminate (Or disable all future operations.)<br>
/// > ` / ` Moves the tape pointer to the right.<br>
/// > ` \ ` Moves the tape pointer to the left.<br>
/// > `|` Creates a new empty tape.<br>
/// > `%{...}` Executes ... async and as if it was a different
///            file.<br>
/// > `"` Copies the main candidate {@link #mainCandidate}'s
///       data, values, tapes, etc, onto the aysnc task (MOST
///       LIKELY WILL CAUSE AN ERROR)
///
/// If you want to forcefully execute a string, you can use the
/// {@link Parser#execute(List)} where you pass the result of:
/// {@link Lexer#parse(String, boolean)} into the execute function.
///
/// WARNING ABOUT ASYNC: Async is obviously experimental and not recommended.
///
/// YOU CAN ALSO USE THE {@link TreeBuilder} TO BUILD AN AST OF THIS LANGUAGE
/// (NOTE: DUE TO HOW IT WORKS, THERE WILL BE NO NESTING AS ONLY A SELECT FEW
/// TOKENS EVEN HAVE 1 ATTRIBUTE.)
///
/// @see Lexer
/// @see Parser
/// @see TreeBuilder
///
/// @author Jason Pakman
/// @implNote If you are going to use this, be sure to include credits and
/// also make sure you are properly setting up VSLang.
public class Executor {
    /// Used for async execution. The first instance of the executor is
    /// considered the main candidate, meaning that other files' data can
    /// be transferred through files, causing errors, if the buggy `"` is
    /// called.
    @ApiStatus.Experimental
    public static Executor mainCandidate;

    /// Represents the pointer to the cell that operations will be performed on.
    /// Remember, there are **TWO** pointers (This and [#POINTED_TAPE]) which both
    /// have different functionality.
    private int pointer;
    /// The operations is a queue of lazy operations that will be ran when the script's
    /// {@link #EXECUTE_OPERATIONS()} is called (Either by the `*` or an EOF token)
    private final List<Runnable> operations;
    /// If the operation is terminated, the {@link #EXECUTE_OPERATIONS()} method will
    /// not run. This can only be toggled on, and a terminated script can not be
    /// restored. Scripts are auto-terminated once the parser reaches an EOF token.
    ///
    /// @see #TERMINATE()
    boolean terminated;
    /// Represents the cell copied by the `^` operation, and can be pasted using the
    /// `&` keyword.
    ///
    /// @see #COPY_CURR_CELL()
    /// @see #PASTE_COPIED_CELL()
    int copied = 0;

    /// Since there are `n` tapes *(where n represents a real number greater than 0,)*
    /// the tape pointer, (or this,) tells the script which tape to perform operations on
    ///
    /// @see #pointer
    /// @see #TAPE_LEFT()
    /// @see #TAPE_RIGHT()
    /// @see #NEW_TAPE()
    int POINTED_TAPE;
    /// Represents the grid of tapes (Note, tapes can be different sizes than eachother)
    List<Stack<Integer>> tapes;

    private Executor() {
        this.tapes = new ArrayList<>();
        this.pointer = 0;
        this.POINTED_TAPE = 0;
        this.operations = new ArrayList<>();
        this.terminated = false;
        if (mainCandidate == null) mainCandidate = this;

        // Create the primary track
        tapes.add(new Stack<>());

        tapes.get(POINTED_TAPE).push(0);
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    static Executor STATIC_START_EXEC() {
        return new Executor();
    }

    void TAPE_LEFT() {
        operations.add(() -> {
            if (POINTED_TAPE <= 0)
                throw new IndexOutOfBoundsException("Tape pointer tried to point to sub0 tape");
            POINTED_TAPE--;
            pointer = 0;
        });
    }

    void TAPE_RIGHT() {
        operations.add(() -> {
            if (POINTED_TAPE >= tapes.size())
                NEW_TAPE();
            POINTED_TAPE++;
            pointer = 0;
            CHECK_STACK();
        });
    }
    void NEW_TAPE() {
        operations.add(() -> {
            tapes.add(new Stack<>());
            tapes.get(tapes.size() - 1).push(0);
        });
    }

    void POINT_RIGHT() {
        operations.add(() -> {
            pointer++;
            CHECK_STACK();
        });
    }

    void POINT_LEFT() {
        operations.add(() -> {
            if (pointer > 0)
                pointer--;
            else throw new IndexOutOfBoundsException("Pointers can not be less than 0");
        });
    }

    void INCREMENT() {
        operations.add(() -> tapes.get(POINTED_TAPE).set(pointer, tapes.get(POINTED_TAPE).get(pointer)+1));
    }

    void DECREMENT() {
        operations.add(() -> {
            CHECK_STACK();
            if (tapes.get(POINTED_TAPE).get(pointer)-1 >= 0)
                tapes.get(POINTED_TAPE).set(pointer, tapes.get(POINTED_TAPE).get(pointer)-1);
            else throw new BufferUnderflowException();
        });
    }

    void OUTPUT_CURRENT_CELL() {
        operations.add(() -> System.out.print((char) (int) tapes.get(POINTED_TAPE).get(pointer)));
    }

    void OUTPUT_CURRENT_TAPE() {
        operations.add(() -> System.out.print(tapes.get(POINTED_TAPE)));
    }

    void OUTPUT_QUEUE() {
        operations.add(() -> System.out.print(operations));
    }

    void OUTPUT_ALL_TAPE() {
        operations.add(() -> tapes.forEach(System.out::println));
    }

    void READ_INPUT_CHAR() {
        operations.add(() -> tapes.get(POINTED_TAPE).set(pointer, new Scanner(System.in).nextInt()));
    }

    void CHECK_CELL_FOR_ZERO() {
        operations.add(() -> {
            CHECK_STACK();
            if (tapes.get(POINTED_TAPE).get(pointer) == 0) {
                int loop = 1;
                while (loop > 0) {
                    pointer++;
                    if (pointer < tapes.get(POINTED_TAPE).size()) {
                        if (tapes.get(POINTED_TAPE).get(pointer) == 0) {
                            loop--;
                        }
                    }
                }
            }
        });
    }

    void CHECK_CELL_FOR_NON_ZERO() {

        operations.add(() -> {
            CHECK_STACK();
            if (tapes.get(POINTED_TAPE).get(pointer) != 0) {
                int loop = 1;
                while (loop > 0) {
                    pointer--;
                    if (pointer < tapes.get(POINTED_TAPE).size()) {
                        if (tapes.get(POINTED_TAPE).get(pointer) != 0) {
                            loop--;
                        }
                    }
                }
            }
        });

    }

    void CHECK_STACK() {
        while (pointer >= tapes.get(POINTED_TAPE).size()) {
            tapes.get(POINTED_TAPE).add(0);
        }
    }

    void ASYNC_EXECUTE(List<Lexer.Token> candidates) {
        new Thread(() -> Parser.execute(candidates)).start();
    }

    void ASYNC_COPY_DATA() {
        if (mainCandidate == this)
            throw new RuntimeException("Tried to copy main candidate thread onto itself");

        this.pointer = mainCandidate.pointer;
        this.operations.clear();
        this.operations.addAll(mainCandidate.operations);
        this.terminated = mainCandidate.terminated;
        this.POINTED_TAPE = mainCandidate.POINTED_TAPE;
    }

    void EXECUTE_OPERATIONS() {
        if (terminated) return;
        for (Runnable op : operations) {
            op.run();
        }
    }

    void TERMINATE() {
        terminated = true;
    }

    void COPY_CURR_CELL() {
        operations.add(() -> copied = tapes.get(POINTED_TAPE).get(pointer));
    }

    void PASTE_COPIED_CELL() {
        operations.add(() -> tapes.get(POINTED_TAPE).set(pointer, copied));
    }

    void STORE_OVER_CELLS(String value) {
        operations.add(() -> {
            char[] deconstructed = value.toCharArray();
            int pos = pointer;
            for (char c : deconstructed) {
                pointer++;
                CHECK_STACK();
                if (tapes.get(POINTED_TAPE).get(pointer) != 0)
                    throw new RuntimeException("Free up cell #"+pointer+" to use the _ operator");
                tapes.get(POINTED_TAPE).set(pointer, (int) c);
            }
            pointer = pos;
        });
    }

    public static void main(String[] args) throws IOException {
        File dir = new File("src/main/vslang/");

        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Directory not found: " + dir.getAbsolutePath());
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".vslang"));

        if (files == null || files.length == 0) {
            System.out.println("No .vslang files found.");
            return;
        }

        for (File file : files) {
            System.out.println("Executing: " + file.getName());
            String content = readFile(file);

            Parser.execute(Lexer.parse(content, true));
        }
    }
    private static @NotNull String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }

    public static final class UnexpectedTokenException extends RuntimeException {
        public UnexpectedTokenException(char got, char expected) {
            super("Received unexpected token. Expected "+expected+". Got: "+got);
        }
    }
}
