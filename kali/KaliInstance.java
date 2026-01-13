package kali;

import java.util.HashMap;
import java.util.Map;

class KaliInstance {
  private KaliClass klass;

  KaliInstance(KaliClass klass) {
    this.klass = klass;
  }

  @Override
  public String toString() {
    return klass.name + " instance";
  }
}