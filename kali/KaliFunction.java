package kali;

import java.util.List;

public class KaliFunction implements KaliCallable {
  final Stmt.Function declaration;
  private final Environment closure;

  KaliFunction(Stmt.Function declaration, Environment closure) {
    this.closure = closure;
    this.declaration = declaration;
  }
  
  KaliFunction(Stmt.Function declaration) {
    this.closure = null;
    this.declaration = declaration;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) throws RuntimeException{
    Environment environment = new Environment(closure); // use closure!
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }

    return null;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }
  
  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
