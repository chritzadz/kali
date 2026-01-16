package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A command-line tool to generate the Abstract Syntax Tree (AST) classes for the Kali language.
 *
 * This tool automates the creation of the boilerplate code required for the AST, including:
 * 1. The base abstract classes (Expr, Stmt).
 * 2. The standard Visitor interface for each tree.
 * 3. The specific subclasses for each AST node (e.g., Binary, Grouping, Literal).
 *
 * Usage:
 *   java tool.GenerateAst <output_directory>
 */
public class GenerateAst {

  /**
   * Main entry point for the AST generation tool.
   *
   * @param args Command line arguments. Expects exactly one argument: the output directory path.
   * @throws IOException If there is an error writing the files.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    // Define the Expression AST (Expr.java)
    defineAst(outputDir, "Expr", Arrays.asList(
      "Assign    : Token name, Expr value",
      "Binary    : Expr left, Token operator, Expr right",
      "Call      : Expr callee, Token paren, List<Expr> arguments",
      "Get       : Expr object, Token name",
      "Set       : Expr object, Token name, Expr value",
      "This      : Token keyword",
      "Super     : Token keyword, Token method",
      "Grouping  : Expr expression",
      "Literal   : Object value",
      "Logical   : Expr left, Token operator, Expr right",
      "Unary     : Token operator, Expr right",
      "UnaryPost : Expr left, Token operator",
      "Variable  : Token name"
    ));

    // Define the Statement AST (Stmt.java)
    defineAst(outputDir, "Stmt", Arrays.asList(
      "Block      : List<Stmt> statements",
      "Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods, List<Var> fields",
      "Expression : Expr expression",
      "Function   : Token name, Token type, List<Token> params, List<Token> paramTypes, List<Stmt> body",
      "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
      "Print      : Expr expression",
      "Return     : Token keyword, Expr value",
      "Var        : Token name, Token type, Expr initializer",
      "While      : Expr condition, Stmt body"
    ));
  }

  /**
   * Generates a complete AST file (Expr.java or Stmt.java) including the base class, visitor, and subclasses.
   *
   * @param outputDir The directory where the file should be generated.
   * @param baseName  The name of the base class (e.g., "Expr" or "Stmt").
   * @param types     A list of strings defining the types and their fields (e.g., "Binary : Expr left, Token operator...").
   * @throws IOException If the file cannot be written.
   */
  private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package kali;");
    writer.println();
    writer.println("import kali.Token;");
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    defineVisitor(writer, baseName, types);

    // The AST classes.
    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }

    // The base accept method for the Visitor pattern.
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    writer.println("}");
    writer.close();
  }

  /**
   * Generates the Visitor interface within the base class.
   *
   * @param writer   The PrintWriter to write to.
   * @param baseName The base class name (e.g., "Expr").
   * @param types    The list of types to generate visit methods for.
   */
  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");

    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" +
        typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");
  }

  /**
   * Generates the subclass for a specific AST node type.
   *
   * @param writer    The PrintWriter to write to.
   * @param baseName  The base class name which this subclass extends.
   * @param className The name of the subclass/node (e.g., "Binary").
   * @param fieldList The string representation of fields (e.g., "Expr left, Token operator, Expr right").
   */
  private static void defineType(
    PrintWriter writer, String baseName,
    String className, String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");

    // Constructor.
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store parameters in fields.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    // Visitor pattern implementation.
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");

    // Fields.
    writer.println();
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("  }");
  }
}
