package kali;

import java.util.List;
import java.util.Map;

class KaliClass implements KaliCallable{
  final String name;
  final Map<String, KaliFunction> methods;

  KaliClass(String name, Map<String, KaliFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  public KaliFunction findMethod(String name){
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    KaliInstance instance = new KaliInstance(this);
    KaliFunction initializer = findMethod(name); // check constructor should be the name of the function.
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }
    return instance;
  }

  @Override
  public int arity() {
    KaliFunction initializer = findMethod(name);
    if (initializer == null) return 0;
    return initializer.arity();
  }
}
