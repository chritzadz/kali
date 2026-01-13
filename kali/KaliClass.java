package kali;

import java.util.List;
import java.util.Map;

class KaliClass {
  final String name;

  KaliClass(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
