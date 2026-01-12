package kali;

class CompilationError extends RuntimeException {
  final Token token;

  CompilationError(Token token, String message) {
    super(message);
    this.token = token;
  }
}
