package kali;

import java.util.List;

import kali.Expr.Assign;
import kali.Expr.Binary;
import kali.Expr.Grouping;
import kali.Expr.Literal;
import kali.Expr.Unary;
import kali.Expr.UnaryPost;
import kali.Expr.Variable;
import kali.Stmt.Block;
import kali.Stmt.Expression;
import kali.Stmt.If;
import kali.Stmt.Print;
import kali.Stmt.Var;

public class TypeChecker implements Expr.Visitor<Object>, Stmt.Visitor<Void>  {
  private Environment environment = new Environment();

  void check(List<Stmt> statements){
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (CompilationError error){
      Kali.compilationError(error);
    }
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Print stmt) {
    evaluate(stmt.expression);
    return null; //interpreter will  handle the System logs
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object declaredType = DataType.NIL;
    if (stmt.type.type == TokenType.TYPE_NUMBER) declaredType = DataType.NUMBER;
    else if (stmt.type.type == TokenType.TYPE_STRING) declaredType = DataType.STRING;
    else if (stmt.type.type == TokenType.TYPE_BOOLEAN) declaredType = DataType.BOOLEAN;

    //check invalid assignment
    if (stmt.initializer != null) {
      Object initializerType = evaluate(stmt.initializer);
      if (declaredType != initializerType) {
        throw new CompilationError(stmt.name, "Variable '" + stmt.name.lexeme + "' declared as " + stmt.type.lexeme + " but initialized with " + initializerType + ".");
      }
    }
    
    environment.define(stmt.name.lexeme, declaredType);
    return null;
  }

  @Override
  public Void visitIfStmt(If stmt) {
    Object condition = evaluate(stmt.condition);
    if (condition != DataType.BOOLEAN) {
      throw new RuntimeException("Condition must be a boolean."); 
    }
    
    execute(stmt.thenBranch);
    if (stmt.elseBranch != null) execute(stmt.elseBranch);
    return null;
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value); // is the value type since this is a type checker
    Object type;

    try {
      type = environment.get(expr.name);
    } catch (RuntimeError error){
      throw new CompilationError(error.token, error.getMessage());
    }

    if (value != type){
      throw new CompilationError(expr.name, "Variable '" + expr.name.lexeme + "' is of type " + type + " but assigned " + value + ".");
    }

    return value;
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        return DataType.BOOLEAN;
      case GREATER_EQUAL:
        return DataType.BOOLEAN;
      case LESS:
        return DataType.BOOLEAN;
      case LESS_EQUAL:
        return DataType.BOOLEAN;
      case MINUS:
        return checkNumberOperands(expr.operator, left, right);
      case SLASH:
        return checkNumberOperands(expr.operator, left, right);
      case STAR:
        return checkNumberOperands(expr.operator, left, right);
      case PLUS:
        return checkStringNumberOperands(expr.operator, left, right);
      case BANG_EQUAL:
      case EQUAL_EQUAL:
        if (left != right) {
          throw new CompilationError(expr.operator, "Operands must be the same type.");
        }
        return DataType.BOOLEAN;
    }

      // Unreachable.
      return DataType.NIL;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    if (expr.value instanceof Double) return DataType.NUMBER;
    if (expr.value instanceof String) return DataType.STRING; 
    if (expr.value instanceof Boolean) return DataType.BOOLEAN;
    return DataType.NIL;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
      case DOUBLE_PLUS:
      case DOUBLE_MINUS:
        return checkNumberOperand(expr.operator, right);
      case BANG:
        return DataType.BOOLEAN;
    }

    return DataType.NIL;
  }

  @Override
  public Object visitUnaryPostExpr(UnaryPost expr) {
    Object left = evaluate(expr.left);

    if (left != DataType.NUMBER) {
      throw new CompilationError(expr.operator, "Operand must be a number.");
    }

    return DataType.NUMBER;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    try {
      return environment.get(expr.name);
    } catch (RuntimeError error) {
      throw new CompilationError(error.token, error.getMessage());
    }
  }
  
  private void execute(Stmt stmt){
    stmt.accept(this);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private Object checkNumberOperand(Token operator, Object left){
    if (left == DataType.NUMBER) return DataType.NUMBER;
    throw new CompilationError(operator, "Operand must be a number.");
  }

  private Object checkNumberOperands(Token operator, Object left, Object right) throws CompilationError {
    if (left == DataType.NUMBER && right == DataType.NUMBER) return DataType.NUMBER;
    throw new CompilationError(operator, "Operands must be numbers.");
  }

  private Object checkStringOperands(Token operator, Object left, Object right) throws CompilationError {
    if (left == DataType.STRING && right == DataType.STRING) return DataType.STRING;
    throw new CompilationError(operator, "Operands must be strings.");
  }

  private Object checkStringNumberOperands(Token operator, Object left, Object right) throws CompilationError {
    if (left == DataType.STRING && right == DataType.STRING)
      return DataType.STRING;
    else if (left == DataType.NUMBER && right == DataType.NUMBER){
      return DataType.NUMBER;
    }
    throw new CompilationError(operator, "Operands must be strings.");
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
}
