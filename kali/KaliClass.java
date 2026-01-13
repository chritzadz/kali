package kali;

import java.util.List;

class KaliClass implements KaliCallable{
  final String name;

  KaliClass(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    KaliInstance instance = new KaliInstance(this);
    return instance;
  }

  @Override
  public int arity() {
    return 0; //for now
  }
}
