package kali;

import java.util.List;
import java.util.Map;

class KaliClass implements KaliCallable{
  final String name;
  final KaliClass superclass;
  final Map<String, KaliFunction> methods;
  final Map<String, Object> fields;

  KaliClass(String name, KaliClass superclass, Map<String, KaliFunction> methods, Map<String, Object> fields) {
    this.name = name;
    this.superclass = superclass;
    this.methods = methods;
    this.fields = fields;
  }

  /**
   * here by importance  precedence check the current method first.
   * @param name
   * @return
   */
  public KaliFunction findMethod(String name){
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
    if (superclass != null) {
      return superclass.findMethod(name); //check if it exists
    }
    return null;
  }
  
  public Object findField(String name) {
    if (fields != null && fields.containsKey(name)) {
      return fields.get(name);
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
