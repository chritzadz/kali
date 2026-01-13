package kali;

import java.util.ArrayList;
import java.util.List;

import kali.Expr.Assign;
import kali.Expr.Binary;
import kali.Expr.Grouping;
import kali.Expr.Literal;
import kali.Expr.Logical;
import kali.Expr.Unary;
import kali.Expr.UnaryPost;
import kali.Expr.Variable;
import kali.Stmt.Block;
import kali.Stmt.Expression;
import kali.Stmt.If;
import kali.Stmt.Print;
import kali.Stmt.Var;
import kali.Stmt.While;

public class TypeChecker implements Expr.Visitor<Object>, Stmt.Visitor<Void>  {
  private Environment environment = new Environment();
  private DataType currentReturnType = DataType.NIL;

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
  public Object visitLogicalExpr(Logical expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    return checkBooleanOperands(expr.operator, left, right);
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitWhileStmt(While stmt) {
    Object conditionType = evaluate(stmt.condition);
    checkConditionBoolean(conditionType);
    execute(stmt.body);
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
    
    if (environment.hasCurrent(stmt.name.lexeme)) { //stricter tule in for functions as well.
      throw new CompilationError(stmt.name, "Variable '" + stmt.name.lexeme + "' already declared in this scope.");
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
      case GREATER_EQUAL:
      case LESS:
      case LESS_EQUAL:
        return checkSameOperands(expr.operator, left, right);
      case MINUS:
        return checkNumberOperands(expr.operator, left, right);
      case SLASH:
        return checkNumberOperands(expr.operator, left, right);
      case STAR:
        return checkStringOrNumberOperands(expr.operator, left, right);
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
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    if (!(callee instanceof KaliFunction)) {
      // since the function name will be set as an identifier, making sure that an identifier that have callee == null is just a variable declaration. cannot be called.
      throw new CompilationError(expr.paren, "Can only call functions.");
    }

    KaliFunction function = (KaliFunction)callee;
    //mismatch arity handling
    if (expr.arguments.size() != function.declaration.params.size()) {
      throw new CompilationError(expr.paren, "Expected " + function.declaration.params.size() + " arguments but got " + expr.arguments.size() + ".");
    }

    for (int i = 0; i < expr.arguments.size(); i++) {
        Object argType = evaluate(expr.arguments.get(i));
        Token paramTypeToken = function.declaration.paramTypes.get(i);
        
        //check dataytype of each parameter to its expected parent.
        DataType expectedType = DataType.NIL;
        if (paramTypeToken.type == TokenType.TYPE_NUMBER) expectedType = DataType.NUMBER;
        else if (paramTypeToken.type == TokenType.TYPE_STRING) expectedType = DataType.STRING;
        else if (paramTypeToken.type == TokenType.TYPE_BOOLEAN) expectedType = DataType.BOOLEAN;

        if (argType != expectedType) {
            throw new CompilationError(expr.paren, "Argument " + (i+1) + " expects " + expectedType + " but got " + argType + ".");
        }
    }

    //return statement must be the same.
    Token returnTypeToken = function.declaration.type;
    if (returnTypeToken.type == TokenType.TYPE_NUMBER) return DataType.NUMBER;
    else if (returnTypeToken.type == TokenType.TYPE_STRING) return DataType.STRING;
    else if (returnTypeToken.type == TokenType.TYPE_BOOLEAN) return DataType.BOOLEAN;
    
    return DataType.NIL;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt){
    KaliFunction function = new KaliFunction(stmt);
    environment.define(stmt.name.lexeme, function);

    //since it is a tree-like structure, save the "parent" type to ref
    DataType enclosingFunctionType = currentReturnType;
    
    if (stmt.type.type == TokenType.TYPE_NUMBER) currentReturnType = DataType.NUMBER;
    else if (stmt.type.type == TokenType.TYPE_STRING) currentReturnType = DataType.STRING;
    else if (stmt.type.type == TokenType.TYPE_BOOLEAN) currentReturnType = DataType.BOOLEAN;
    else currentReturnType = DataType.NIL;

    Environment previous = this.environment;
    this.environment = new Environment(environment);

    try {
        //type checking.
        for (int i = 0; i < stmt.params.size(); i++) {
            Token paramName = stmt.params.get(i);
            Token paramType = stmt.paramTypes.get(i);
            
            DataType dataType = DataType.NIL;
            if (paramType.type == TokenType.TYPE_NUMBER) dataType = DataType.NUMBER;
            else if (paramType.type == TokenType.TYPE_STRING) dataType = DataType.STRING;
            else if (paramType.type == TokenType.TYPE_BOOLEAN) dataType = DataType.BOOLEAN;
            
            environment.define(paramName.lexeme, dataType);
        }

        executeBlock(stmt.body, this.environment);
    } finally {
        this.environment = previous;
        currentReturnType = enclosingFunctionType;
    }
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    //handling return statement mismatch
    Object valueType = DataType.NIL;
    if (stmt.value != null) valueType = evaluate(stmt.value);

    if (valueType != currentReturnType){
      throw new CompilationError(stmt.keyword, "Return type mismatch. Expected " + currentReturnType + " but got " + valueType + ".");
    }

    return null;
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

  private Object checkSameOperands(Token operator, Object left, Object right) throws CompilationError {
    if (left == DataType.NUMBER && right == DataType.NUMBER) return DataType.BOOLEAN;
    if (left == DataType.STRING && right == DataType.STRING) return DataType.BOOLEAN;
    if (left == DataType.BOOLEAN && right == DataType.BOOLEAN) return DataType.BOOLEAN;
    throw new CompilationError(operator, "Operands must be the same type.");
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
    throw new CompilationError(operator, "Both operands must be strings or numbers");
  }

  private Object checkStringOrNumberOperands(Token operator, Object left, Object right) throws CompilationError {
    if (left == DataType.STRING && right == DataType.STRING)
      return DataType.STRING;
    else if (left == DataType.NUMBER && right == DataType.NUMBER){
      return DataType.NUMBER;
    }
    else if (left == DataType.NUMBER && right == DataType.STRING){
      return DataType.STRING;
    }
    else if (right == DataType.NUMBER && left == DataType.STRING){
      return DataType.STRING;
    }
    throw new CompilationError(operator, "Operands must be strings or numbers");
  }

  private Object checkBooleanOperands(Token operator, Object left, Object right) throws CompilationError{
    if (left == DataType.BOOLEAN && right == DataType.BOOLEAN) return DataType.BOOLEAN;
    throw new CompilationError(operator, "Operands must be booleans.");
  }

  private Object checkConditionBoolean(Object condition) throws CompilationError{
    if (condition == DataType.BOOLEAN) return null;
    throw new CompilationError(null, "Condition must be booleans.");
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
