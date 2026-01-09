package kali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to scan and tokenize the source code.
 */
public class Scanner {
  private final String source;
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private final List<Token> tokens = new ArrayList<>();
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    TokenType.AND);
    keywords.put("class",  TokenType.CLASS);
    keywords.put("else",   TokenType.ELSE);
    keywords.put("false",  TokenType.FALSE);
    keywords.put("for",    TokenType.FOR);
    keywords.put("fun",    TokenType.FUN);
    keywords.put("if",     TokenType.IF);
    keywords.put("nil",    TokenType.NIL);
    keywords.put("or",     TokenType.OR);
    keywords.put("print",  TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super",  TokenType.SUPER);
    keywords.put("this",   TokenType.THIS);
    keywords.put("true",   TokenType.TRUE);
    keywords.put("var",    TokenType.VAR);
    keywords.put("while",  TokenType.WHILE);
  }

  public Scanner(String source) {
    this.source = source;
  }

  /**
   * Loop thourgh every character. there will be three variables maintained
   * 1. start -> start of the pointer
   * 2. current -> current position of the pointer
   * 3. line -> how many lines
   * 
   * will call the scanToken() to evaluate,
   * @return
   */
  public List<Token> scanTokens(){
    while(!isAtEnd()) {
      start = current;
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
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

  private void addToken(TokenType type){
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }


  /**
   * For every character seen we advance() which return the current character and advances the pointer
   * Basically it tokenize in a left-associative way from left to riught of the token which is part of the expression.
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
      case '-': addToken(TokenType.MINUS); break;
      case '+': addToken(TokenType.PLUS); break;
      case ';': addToken(TokenType.SEMICOLON); break;
      case '*': addToken(TokenType.STAR); break; 
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
          while(peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(TokenType.SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
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
          //MAXIMAL MUNCH RESERVED KEYWORD TECHNIQUE assume a letter or underscore
          identifier();
        }
        else{
          Kali.error(line, "Unexpected character.");
        }
        break;
    }
  }

  public boolean isAlpha(char c){
    return (c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          c == '_';
  }

  /**
   * Assume that _ or any alphabet is a key to identifier.
   */
  public void identifier() {
    while(isAlpha(peek()) || isDigit(peek())) advance();

    //check whether it is a reserved keyword or no?
    // Reserved keyword is later used for later, since Scanner is mainly used for valu expression.
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = TokenType.IDENTIFIER;
    addToken(type);
  }

  public boolean isDigit(char c){
    return c > '0' && c <= '9';
  }

  /**
   * Maximal munch to consume number always check for floating andcount as number, Here Lox always represent number as a Double until it is all a digit is done ended in space.
   */
  public void number(){
    while (isDigit(peek())) advance();

    if (peek() == '.' && isDigit(peekNext())){
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  public char peekNext(){
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  /**
   * helps check the next character is there is a match with the expected c, then consume, else we revert back.
   * @param c
   * @return
   */
  public boolean match(char c){
    if (source.charAt(current++) == c){
      return true;
    } else {
      current--;
      return false;
    }
  }

  /**
   * similar to adavnce but does not consume, it merely checks the next character without popping it
   * @return
   */
  public char peek(){
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }


  /**
   * String is a literal, so this function is called after detecting the char "
   */
  public void string(){
    //this allows multiline string
    while(peek() != '"' && !isAtEnd()){
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()){
      //invalid string
      Kali.error(line, "Unterminated string.");
      return;
    }

    advance();
    String value = source.substring(start+1, current-1); //grab the string up to "
    addToken(TokenType.STRING, value); //add the value as token type string
  }
}
