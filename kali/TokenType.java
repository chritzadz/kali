package kali;

public enum TokenType {
    //Single-character token
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    //One or tewo character token
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL, DOUBLE_PLUS, DOUBLE_MINUS,

    // Literals
    IDENTIFIER, STRING, NUMBER, VOID, BOOLEAN,

    // Type Keywords
    TYPE_NUMBER, TYPE_STRING, TYPE_BOOLEAN,

    //Reserved keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, WHILE,

    EOF
}
