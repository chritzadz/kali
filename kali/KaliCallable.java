package kali;
import java.util.List;

interface KaliCallable {
  /**
   * Pass interpreter since the function creates a new environment within the block, needs interpreter as well.
   * @param interpreter
   * @param arguments
   * @return
   */
  Object call(Interpreter interpreter, List<Object> arguments);
  int arity();
}