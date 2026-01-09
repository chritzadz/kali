package kali;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Kali {
	static boolean hadError = false;
	static boolean hadRuntimeError = false;
	private static final Interpreter interpreter = new Interpreter();


	/**
	 * Iterate through each file only 'process' one file. or if there is no file args, then it is a prompt
	 * The file will act as the file with the code to run
	 * @param args is the number for files (each files is a long string)
	*/
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

	/**
	 * Parse to bytes, and then processed to a string by run()
	 * @param path is a string path to the file that will be interpreted by Lox
	*/
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		if (hadError) System.exit(65);
		if (hadRuntimeError) System.exit(70);
	}

	/**
	 * Accepts the commands given by user, and process
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
	 * First scanner will act to iterate thruogh the source string (code)
	 * Tokenize all of the code by the user with the Scanner class. and then calling scanToken()
	 * After being able to tokenize, parse using the Parse class to get an Expression of the whole code.
	 * @param source is the one liner string of all the code.
	 */
	private static void run(String source){
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();

		Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();

    if (hadError) return;
		interpreter.interpret(statements);
	}

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
    System.err.println(error.getMessage() +
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
}
