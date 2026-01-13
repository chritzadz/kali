package kali;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();
  final Environment enclosing;

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Object value){
    values.put(name, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)){
      return values.get(name.lexeme);
    }

    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    //if the nevironemtn is an inner (not global) then assign every global variable inside the
    if (enclosing != null) {
      enclosing.assign(name, value);
      return; //oops first bug
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
}