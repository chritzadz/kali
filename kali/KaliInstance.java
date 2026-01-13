package kali;

import java.util.HashMap;
import java.util.Map;

class KaliInstance {
  private KaliClass klass;
  private final Map<String, Object> fields = new HashMap<>();

  KaliInstance(KaliClass klass) {
    this.klass = klass;
  }

  public Object get(Token name){
    if (fields.containsKey(name.lexeme)){
      return fields.get(name.lexeme);
    }

    KaliFunction method = klass.findMethod(name.lexeme);
    if (method != null) return method.bind(this);
    if (method != null) return method;

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return klass.name + " instance";
  }
}