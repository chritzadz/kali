package kali;

import java.util.ArrayList;
import java.util.List;

public class Parser {
	private static class ParseError extends RuntimeException {}
	private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  // --- Statements ---

  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(TokenType.EQUAL)) {
      initializer = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt statement() {
    if (match(TokenType.IF)) return ifStatement();
    if (match(TokenType.PRINT)) return printStatement();
    if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  private Stmt ifStatement(){
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition");
    
    Stmt thenBranch = statement(); //here we wanna detect whether there is a block, since it is handled in statement() simply call it
    Stmt elseBranch = null;
    //after if block expect an else but not mandatory right
    if (match(TokenType.ELSE)){
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  // --- Expressions ---

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    Expr expr = equality();

    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target."); 
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS, TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    Expr expr = primary();

    if (match(TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS)) {
      Token operator = previous();
      return new Expr.UnaryPost(expr, operator); //we count this as special Unary where the operator comes after an expression
    }

    return expr;
  }

  private Expr primary() {
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  // --- Helpers ---

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  /**
   * Match type of the current, if any of the type match, then it will advance and return true
   * @param types list of type that is needed for checking
   * @return true or false of the current token type
   */
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  /**
   * Consumes the next token, check the types to and advances.
   * @param type type to be compared with the current token
   * @param message message of error if consume, if not matching
   * @return
   */
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  /**
   * Check whether the next type is as or not, does not advances
   * @return
   */
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  /**
   * returns the previous token, and advances
   * @return the current token
   */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  /**
   * Check whether it is at the EOF
   * @return
   */
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  /**
   * peek the next token without advancing
   * @return
   */
  private Token peek() {
    return tokens.get(current);
  }

  /**
   * peek the previous token without going backward
   * @return
   */
  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Kali.error(token, message);
    return new ParseError();
  }

}
