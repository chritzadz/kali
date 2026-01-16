package kali;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kali.Expr.Assign;
import kali.Expr.Binary;
import kali.Expr.Grouping;
import kali.Expr.Literal;
import kali.Expr.Logical;
import kali.Expr.Unary;
import kali.Expr.UnaryPost;
import kali.Expr.Variable;
import kali.Stmt.Block;
import kali.Stmt.Class;
import kali.Stmt.Expression;
import kali.Stmt.If;
import kali.Stmt.Print;
import kali.Stmt.Var;
import kali.Stmt.While;

public class TypeChecker implements Expr.Visitor<Object>, Stmt.Visitor<Void>  {
  private Environment environment = new Environment();
  private Object currentReturnType = DataType.VOID;
  private String currentSuperClassType = null;

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
    else if (stmt.type.type == TokenType.IDENTIFIER) {
      try {
        declaredType = environment.get(stmt.type); //get the declared type
      } catch (RuntimeError error) {
        throw new CompilationError(stmt.type, "Unknown class type '" + stmt.type.lexeme + "'.");
      }
    }

    //check invalid assignment
    if (stmt.initializer != null) {
      Object initializerType = evaluate(stmt.initializer);
      //check inheritance
      if (declaredType != initializerType && !checkInheritance(declaredType, initializerType)) {
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

    if (value != type && !checkInheritance(type, value)){
      throw new CompilationError(expr.name, "Variable '" + expr.name.lexeme + "' is of type " + type + " but assigned " + value + ".");
    }

    return value;
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof KaliClass) {
      KaliClass klass = (KaliClass) object;
      
      Object fieldType = klass.findField(expr.name.lexeme);
      if (fieldType != null) return fieldType;

      KaliFunction method = klass.findMethod(expr.name.lexeme);
      if (method != null) return method;
      
      throw new CompilationError(expr.name, "Undefined property '" + expr.name.lexeme + "'.");
    }
    
    throw new CompilationError(expr.name, "Only instances have properties.");
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);
    if (object instanceof KaliClass) {
      KaliClass klass = (KaliClass) object;
      Object valueType = evaluate(expr.value);
      
      Object expectedType = klass.findField(expr.name.lexeme);
      
      if (expectedType == null) {
          throw new CompilationError(expr.name, "Undefined field '" + expr.name.lexeme + "'.");
      }

      if (valueType != expectedType && !checkInheritance(expectedType, valueType)){
        throw new CompilationError(expr.name, "Field '" + expr.name.lexeme + "' is of type " + expectedType + " but assigned " + valueType + ".");
      }
      
      return null;
    }
    throw new CompilationError(expr.name, "Only instances have fields.");
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    try {
      return environment.get(expr.keyword);
    } catch (RuntimeError error) {
      throw new CompilationError(expr.keyword, error.getMessage());
    }
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    try {
      return environment.get(expr.keyword);
    } catch (RuntimeError error) {
      throw new CompilationError(expr.keyword, error.getMessage());
    }
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
      default:
        return DataType.NIL;
    }
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
      default:
        return DataType.NIL;
    }
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    if (callee instanceof KaliClass) {
      KaliClass klass = (KaliClass)callee;
      if (expr.arguments.size() != klass.arity()) {
        throw new CompilationError(expr.paren, "Expected " + klass.arity() + " arguments but got " + expr.arguments.size() + ".");
      }
      return klass;
    }

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
        Object expectedType = DataType.NIL;
        if (paramTypeToken.type == TokenType.TYPE_NUMBER) expectedType = DataType.NUMBER;
        else if (paramTypeToken.type == TokenType.TYPE_STRING) expectedType = DataType.STRING;
        else if (paramTypeToken.type == TokenType.TYPE_BOOLEAN) expectedType = DataType.BOOLEAN;
        else if (paramTypeToken.type == TokenType.IDENTIFIER) {
            try {
                expectedType = environment.get(paramTypeToken);
            } catch (RuntimeError e) {
                throw new CompilationError(paramTypeToken, "Unknown param type '" + paramTypeToken.lexeme + "'.");
            }
        }

        if (argType != expectedType && !checkInheritance(expectedType, argType)) {
            throw new CompilationError(expr.paren, "Argument " + (i+1) + " expects " + expectedType + " but got " + argType + ".");
        }
    }

    //return statement must be the same.
    Token returnTypeToken = function.declaration.type;
    if (returnTypeToken.type == TokenType.TYPE_NUMBER) return DataType.NUMBER;
    else if (returnTypeToken.type == TokenType.TYPE_VOID) return DataType.VOID;
    else if (returnTypeToken.type == TokenType.TYPE_STRING) return DataType.STRING;
    else if (returnTypeToken.type == TokenType.TYPE_BOOLEAN) return DataType.BOOLEAN;
    else if (returnTypeToken.type == TokenType.IDENTIFIER) {
      try {
        return environment.get(returnTypeToken);
      } catch (RuntimeError e) {
        throw new CompilationError(returnTypeToken, "Unknown return type '" + returnTypeToken.lexeme + "'.");
      }
    }
    
    return DataType.NIL;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt){
    KaliFunction function = new KaliFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);

    checkFunction(stmt);
    return null;
  }

  private void checkFunction(Stmt.Function stmt) {
    //since it is a tree-like structure, save the "parent" type to ref
    Object enclosingFunctionType = currentReturnType;
    
    if (stmt.type.type == TokenType.TYPE_VOID) currentReturnType = DataType.VOID;
    else if (stmt.type.type == TokenType.TYPE_NUMBER) currentReturnType = DataType.NUMBER;
    else if (stmt.type.type == TokenType.TYPE_STRING) currentReturnType = DataType.STRING;
    else if (stmt.type.type == TokenType.TYPE_BOOLEAN) currentReturnType = DataType.BOOLEAN;
    else if (stmt.type.type == TokenType.IDENTIFIER) {
      try {
        currentReturnType = environment.get(stmt.type);
      } catch (RuntimeError error) {
        throw new CompilationError(stmt.type, "Unknown class type '" + stmt.type.lexeme + "'.");
      }
    }
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
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    //handling return statement mismatch
    Object valueType = DataType.NIL;
    if (stmt.value != null) valueType = evaluate(stmt.value);

    if (valueType != currentReturnType && !checkInheritance(currentReturnType, valueType)){
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

  @Override
  public Void visitClassStmt(Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null){
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof KaliClass)){
        throw new CompilationError(stmt.superclass.name, "Superclass must be a class.");
      }
      currentSuperClassType = ((KaliClass)superclass).name;
    }

    environment.define(stmt.name.lexeme, null);

    Map<String, KaliFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      boolean isInitializer = method.name.lexeme.equals(stmt.name.lexeme);
      KaliFunction function = new KaliFunction(method, environment, isInitializer);
      methods.put(method.name.lexeme, function);
    }

    Map<String, Object> fields = new HashMap<>();
    for (Stmt.Var field : stmt.fields) {
      Object type = DataType.NIL;
      if (field.type.type == TokenType.TYPE_NUMBER) type = DataType.NUMBER;
      else if (field.type.type == TokenType.TYPE_STRING) type = DataType.STRING;
      else if (field.type.type == TokenType.TYPE_BOOLEAN) type = DataType.BOOLEAN;
      else if (field.type.type == TokenType.IDENTIFIER) {
         try {
            type = environment.get(field.type); 
         } catch (RuntimeError error) {
            throw new CompilationError(field.type, "Unknown type.");
         }
      }
      fields.put(field.name.lexeme, type);
    }

    KaliClass klass = new KaliClass(stmt.name.lexeme, (KaliClass)superclass, methods, fields);
    environment.assign(stmt.name, klass);

    // Create a new scope for the class to define 'this'
    Environment previous = this.environment;
    this.environment = new Environment(environment);
    this.environment.define("this", klass);

    try {
      for (Stmt.Function method : stmt.methods) {
        checkFunction(method);
      }
    } finally {
      this.environment = previous;
    }

    return null;
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

  // private Object checkStringOperands(Token operator, Object left, Object right) throws CompilationError {
  //   if (left == DataType.STRING && right == DataType.STRING) return DataType.STRING;
  //   throw new CompilationError(operator, "Operands must be strings.");
  // }

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

  private boolean checkInheritance(Object declaredType, Object initializerType){
    if (declaredType instanceof KaliClass && initializerType instanceof KaliClass){
      KaliClass current = (KaliClass)initializerType;
      //this seems dirty but this is the most i could think of how to implement. for the chaining process
      while(current != null){
        if (current.equals(declaredType)) return true;
        current = current.superclass;
      }
    }
    return false;
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
