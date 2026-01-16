package kali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Scanner (Lexer) for the Kali language.
 *
 * Responsible for transforming raw source code (String) into a list of Tokens.
 * It handles the first phase of the interpreter pipeline: Lexical Analysis.
 *
 * Works by iterating through the source character by character (linear scan)
 * and grouping them into meaningful lexemes (tokens) like keywords, literals, and operators.
 */
public class Scanner {
  /** The raw source code to be scanned. */
  private final String source;
  /** The list of tokens accumulating during the scan. */
  private final List<Token> tokens = new ArrayList<>();

  /** The start index of the lexeme currently being scanned. */
  private int start = 0;
  /** The current character index in the source code. */
  private int current = 0;
  /** The current line number, used for error reporting. */
  private int line = 1;

  /** Map of reserved words to their corresponding TokenTypes. */
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    TokenType.AND);
    keywords.put("class",  TokenType.CLASS);
    keywords.put("else",   TokenType.ELSE);
    keywords.put("false",  TokenType.FALSE);
    keywords.put("for",    TokenType.FOR);
    keywords.put("if",     TokenType.IF);
    keywords.put("nil",    TokenType.NIL);
    keywords.put("or",     TokenType.OR);
    keywords.put("print",  TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super",  TokenType.SUPER);
    keywords.put("this",   TokenType.THIS);
    keywords.put("true",   TokenType.TRUE);
    keywords.put("while",  TokenType.WHILE);
    keywords.put("string", TokenType.TYPE_STRING);
    keywords.put("number", TokenType.TYPE_NUMBER);
    keywords.put("boolean", TokenType.TYPE_BOOLEAN);
    keywords.put("void", TokenType.TYPE_VOID); // Void might be useful later, but sticking to requested types.
    keywords.put("extends", TokenType.EXTENDS);
  }

  public Scanner(String source) {
    this.source = source;
  }

  /**
   * Loops through the entire source code to generate tokens.
   * Three pointers are maintained to track state:
   * 1. start -> The beginning index of the current lexeme being scanned.
   * 2. current -> The current character index being inspected.
   * 3. line -> The current line number (for error reporting).
   *
   * Calls scanToken() iteratively to recognize and consume individual tokens.
   * @return A list of generated Tokens.
   */
  public List<Token> scanTokens(){
    while(!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  /**
   * Scans a single token by advancing the current character and matching it against known patterns.
   * If a character could start multiple token types, it checks subsequent characters.
   * Basically, it tokenizes in a left-associative way, reading from left to right to build an expression.
   */
  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(TokenType.LEFT_PAREN); break;
      case ')': addToken(TokenType.RIGHT_PAREN); break;
      case '{': addToken(TokenType.LEFT_BRACE); break;
      case '}': addToken(TokenType.RIGHT_BRACE); break;
      case ',': addToken(TokenType.COMMA); break;
      case '.': addToken(TokenType.DOT); break;
      case ';': addToken(TokenType.SEMICOLON); break;
      case '*': addToken(TokenType.STAR); break;
      case '-':
        addToken(match('-') ? TokenType.DOUBLE_MINUS : TokenType.MINUS);
        break;
      case '+':
        addToken(match('+') ? TokenType.DOUBLE_PLUS : TokenType.PLUS);
        break;
      case '!':
        addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
        break;
      case '=':
        addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
        break;
      case '<':
        addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        break;
      case '>':
        addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        break;
      case '/':
        if (match('/')){
          // A comment goes until the end of the line.
          while(peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(TokenType.SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      default:
        if (isDigit(c)) {
          number();
        }
        else if (isAlpha(c)){
          identifier();
        }
        else{
          Kali.error(line, "Unexpected character.");
        }
        break;
    }
  }

  // --- Token Scanning Methods ---

  /**
   * Scans an identifier or keyword.
   * Assumes that an underscore (_) or any alphabetic character starts an identifier.
   * Uses "Maximal Munch" to consume as many alphanumeric characters as possible.
   * Checks strictly reserved keywords first; if not found, defaults to a user-defined IDENTIFIER.
   */
  private void identifier() {
    while(isAlpha(peek()) || isDigit(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = TokenType.IDENTIFIER;
    addToken(type);
  }

  /**
   * Scans a number literal.
   * Uses "Maximal Munch" to consume the entire number.
   * Checks for a fractional part (floating point) and counts it as part of the number.
   * Note: Kali (following Lox) always represents numbers as Doubles internally, regardless of whether they have a decimal point.
   */
  private void number(){
    while (isDigit(peek())) advance();

    if (peek() == '.' && isDigit(peekNext())){
      advance();
      while (isDigit(peek())) advance();
    }

    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  /**
   * Scans a string literal.
   * This function is called after detecting the opening double-quote (").
   * It consumes characters until the closing quote is found, handling multiline strings by tracking line breaks.
   */
  private void string(){
    while(peek() != '"' && !isAtEnd()){
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()){
      Kali.error(line, "Unterminated string.");
      return;
    }

    advance();
    String value = source.substring(start+1, current-1);
    addToken(TokenType.STRING, value);
  }

  // --- Character Helper Methods ---

  /**
   * Consumes the current character if it matches the expected character.
   * This is a conditional advance: if the next character matches 'expected', we consume it and return true.
   * Otherwise, we leave the state unchanged and return false.
   * Useful for two-character tokens like '!=' or '>='.
   * @param expected The character to match.
   * @return true if matched and consumed, false otherwise.
   */
  private boolean match(char expected){
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  /**
   * Returns the current character without consuming it (lookahead).
   * Similar to advance(), but does not increment the 'current' pointer.
   * Used to check the context of the current scan without altering the state.
   * @return The current character, or '\0' if at the end of source.
   */
  private char peek(){
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext(){
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c){
    return (c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          c == '_';
  }

  private boolean isDigit(char c){
    return c >= '0' && c <= '9';
  }

  /**
   * whether the current is at the end or not
   * @return boolean expression of the current pointer should before the length.
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance(){
    return source.charAt(current++);
  }

  // --- Token Addition Helpers ---

  private void addToken(TokenType type){
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
