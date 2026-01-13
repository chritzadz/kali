package kali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kali.Expr.Logical;
import kali.Expr.UnaryPost;
import kali.Expr.Variable;
import kali.Stmt.While;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  private Environment environment = new Environment();
  final Environment globals = new Environment();
  private final Map<Expr, Integer> locals = new HashMap<>();

  //Lox implementation to show foreign/in-built methods
  Interpreter() {
    globals.define("clock", new KaliCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Kali.runtimeError(error);
    }
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    environment.define(stmt.name.lexeme, null);
    KaliClass klass = new KaliClass(stmt.name.lexeme);
    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; //uninitialized will be null
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof KaliCallable)) {
      throw new RuntimeError(expr.paren,
        "Can only call functions and classes.");
    }

    KaliCallable function = (KaliCallable)callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }
    return function.call(this, arguments);
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt){
    KaliFunction function = new KaliFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitLogicalExpr(Logical expr) {
    //i kinda get it know, we wanna implmement short circuit and AND OR Precendence
    Object left = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(While stmt) {
    while (isTruthy(evaluate(stmt.condition))){
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); 

    switch (expr.operator.type) {
      case GREATER:
        checkSameOperands(expr.operator, left, right);
        if (left instanceof Double && right instanceof Double){
          return (double)left > (double)right;
        }
        else if (left instanceof String && right instanceof String){
          return ((String)left).length() > ((String)right).length();
        }
        else {
          int leftVal = (Boolean) left ? 1 : 0;
          int rightVal = (Boolean) right ? 1 : 0;
          return leftVal > rightVal;
        }
      case GREATER_EQUAL:
        checkSameOperands(expr.operator, left, right);
        if (left instanceof Double && right instanceof Double){
          return (double)left >= (double)right;
        }
        else if (left instanceof String && right instanceof String){
          return ((String)left).length() >= ((String)right).length();
        }
        else {
          int leftVal = (Boolean) left ? 1 : 0;
          int rightVal = (Boolean) right ? 1 : 0;
          return leftVal >= rightVal;
        }
      case LESS:
        checkSameOperands(expr.operator, left, right);
        if (left instanceof Double && right instanceof Double){
          return (double)left < (double)right;
        }
        else if (left instanceof String && right instanceof String){
          return ((String)left).length() < ((String)right).length();
        }
        else {
          int leftVal = (Boolean) left ? 1 : 0;
          int rightVal = (Boolean) right ? 1 : 0;
          return leftVal < rightVal;
        }
      case LESS_EQUAL:
        checkSameOperands(expr.operator, left, right);
        if (left instanceof Double && right instanceof Double){
          return (double)left <= (double)right;
        }
        else if (left instanceof String && right instanceof String){
          return ((String)left).length() <= ((String)right).length();
        }
        else {
          int leftVal = (Boolean) left ? 1 : 0;
          int rightVal = (Boolean) right ? 1 : 0;
          return leftVal <= rightVal;
        }
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        if (left instanceof String && right instanceof Double) {

        }

        throw new RuntimeError(expr.operator,"Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkStarOperands(expr.operator, left, right);
        if (left instanceof Double) {
          if (right instanceof Double) {
            return (double)left * (double)right;
          } else if (right instanceof String){
            StringBuilder sb = new StringBuilder();
            int times = ((Double) left).intValue();
            for (int i = 0; i < times; i++) {
              sb.append((String) right);
            }
            return sb.toString();
          }
        } else if (right instanceof Double) {
          StringBuilder sb = new StringBuilder();
          int times = ((Double) right).intValue();
          for (int i = 0; i < times; i++) {
            sb.append((String) left);
          }
          return sb.toString();
        }
        
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
      case BANG:
        return !isTruthy(right);
      case DOUBLE_MINUS:
        checkNumberOperand(expr.operator, right);
        double decremented = (double)right - 1;
        if (expr.right instanceof Variable) {
          Token name = ((Variable) expr.right).name;
          environment.assign(name, decremented);
          return decremented;
        }
        throw new RuntimeError(expr.operator, "Invalid assignment targetcase DOUBLE_MINUS:"); // not variable error
      case DOUBLE_PLUS:
        checkNumberOperand(expr.operator, right);
        double incremented = (double)right + 1;
        if (expr.right instanceof Variable) {
          Token name = ((Variable) expr.right).name;
          environment.assign(name, incremented);
          return incremented;
        }
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitUnaryPostExpr(UnaryPost expr) {
    switch (expr.operator.type) {
      case DOUBLE_MINUS:
        Object left = evaluate(expr.left);
        checkNumberOperand(expr.operator, left);
        double decremented = (double)left - 1;
        if (expr.left instanceof Variable) { //here we check that post a increment only works for a variable. and we assigne after all of it is evaluated.
          Token name = ((Variable) expr.left).name;
          environment.assign(name, decremented);
        }
        return left;
      case DOUBLE_PLUS:
        Object left1 = evaluate(expr.left);
        checkNumberOperand(expr.operator, left1);
        double incremented = (double)left1 + 1;
        if (expr.left instanceof Variable) {
          Token name = ((Variable) expr.left).name;
          environment.assign(name, incremented);
        }
        return left1;
    }
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
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

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;

    return left.equals(right);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private void checkSameOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    if (left instanceof String && right instanceof String) return;
    if (left instanceof Boolean && right instanceof Boolean) return;
    throw new RuntimeError(operator, "Operands must be the same typoe.");
  }

  private void checkStarOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    else if (left instanceof Double && right instanceof String) return;
    else if (left instanceof String && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
