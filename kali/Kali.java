package kali;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The main entry point for the Kali language interpreter.
 * Handles script execution, REPL mode, and the core compilation/execution pipeline.
 */
public class Kali {
  // Error state flags
  static boolean hadError = false;
  static boolean hadRuntimeError = false;
  static boolean hadCompilationError = false;

  // Language subsystems
  private static final Interpreter interpreter = new Interpreter();
  private static final TypeChecker typeChecker = new TypeChecker();

  /**
   * Main entry point.
   * @param args Command line arguments. [0] is the path to a script file.
   * @throws IOException If reading input fails.
   */
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: kali [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /**
   * Reads and executes a source file.
   * @param path Path to the .kali file.
   * @throws IOException If file reading fails.
   */
  private static void runFile(String path) throws IOException {
    if (!path.endsWith(".kali")) {
      System.err.println("Error: source file must end with .kali");
      System.exit(65);
    }

    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
    if (hadCompilationError) System.exit(70);
  }

  /**
   * Starts the interactive REPL (Read-Eval-Print Loop).
   * @throws IOException If input reading fails.
   */
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }

  /**
   * Core pipeline: Scan -> Parse -> Resolve -> Type Check -> Interpret.
   * @param source The source code string.
   */
  private static void run(String source) {
    // 1. Scanning (Lexical Analysis)
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    // 2. Parsing (AST Generation)
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();

    // Stop if scanning or parsing failed
    if (hadError) return;

    // 3. Resolution (Variable binding)
    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    // Stop if resolution failed
    if (hadError) return;

    // 4. Type Checking (Static Analysis)
    typeChecker.check(statements);
    if (hadCompilationError) return;

    // 5. Interpretation (Execution)
    interpreter.interpret(statements);
  }

  // --- Error Reporting Utils ---

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

  static void compilationError(CompilationError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadCompilationError = true;
  }

  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}
