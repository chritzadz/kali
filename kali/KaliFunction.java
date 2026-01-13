package kali;

import java.util.List;

public class KaliFunction implements KaliCallable {
  final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer; //forconstructor

  KaliFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.declaration = declaration;
  }

  KaliFunction bind(KaliInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);//this refers to the current instnace the "BINDING" process
    return new KaliFunction(declaration, environment, isInitializer);
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
      if (isInitializer) return closure.getAt(0, "this");
      return returnValue.value;
    }

    if (isInitializer) return closure.getAt(0, "this");
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
