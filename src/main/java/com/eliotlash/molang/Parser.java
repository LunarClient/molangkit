package com.eliotlash.molang;

import com.eliotlash.molang.ast.Assignable;
import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.ast.Operator;
import com.eliotlash.molang.ast.Stmt;
import com.eliotlash.molang.lexer.Keyword;
import com.eliotlash.molang.lexer.Token;
import com.eliotlash.molang.lexer.TokenType;
import com.eliotlash.molang.utils.ParserUtils;

import java.util.ArrayList;
import java.util.List;

import static com.eliotlash.molang.lexer.TokenType.*;

public class Parser {

    private final List<Token> input;
    private int current = 0;
    private final CompileConstants constants = new CompileConstants();

    public Parser(List<Token> input) {
        this.input = input;
    }

    /**
     * Resets the parser to the beginning of the input.
     */
    public void reset() {
        current = 0;
    }

    /**
     * Parses the input, expecting it to be a single expression.
     *
     * @return The parsed expression.
     */
    public Expr parseExpression() {
        Expr expr = expression();
        consume(EOF, "Expect end of expression");
        return expr;
    }

    /**
     * Parses the input, expecting it to be a single statement.
     *
     * @return The parsed statement.
     */
    public Stmt parseStatement() {
        Stmt expr = statement();
        consume(EOF, "Expect end of statement");
        return expr;
    }

    /**
     * Parses the input, expecting it to be a list of statements.
     *
     * @return The parsed list of statements.
     */
    public List<Stmt> parse() {
        var statements1 = new ArrayList<Stmt>();

        while (!isAtEnd()) {
            statements1.add(statement());
        }
        List<Stmt> statements = statements1;

        consume(EOF, "Expect end of expression.");

        return statements;
    }

    private Stmt statement() {
        if (matchKeyword(Keyword.RETURN)) return returnStatement();
        if (matchKeyword(Keyword.BREAK)) return breakStatement();
        if (matchKeyword(Keyword.CONTINUE)) return continueStatement();
        if (matchKeyword(Keyword.LOOP)) return loopStatement();
        if (matchKeyword(Keyword.IF)) return ifStatement();

        return expressionStatement();
    }

    private Stmt.Expression expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    private Stmt breakStatement() {
        consume(SEMICOLON, "Expect ';' after break statement");
        return new Stmt.Break();
    }

    private Stmt continueStatement() {
        consume(SEMICOLON, "Expect ';' after continue statement");
        return new Stmt.Continue();
    }

    private Stmt loopStatement() {
        consume(OPEN_PAREN, "Expect '(' for loop args");

        List<Expr> arguments = arguments();

        if (arguments.size() != 2) {
            throw error(peek(), "Expect 2 arguments for loop");
        }

        consume(SEMICOLON, "Expect ';' after loop statement");

        Expr count = arguments.get(0);
        Expr expr = arguments.get(1);

        return new Stmt.Loop(count, expr);
    }


    private Stmt ifStatement() {
        consume(OPEN_PAREN, "Expect '(' after 'if'");

        List<Expr> arguments = arguments();

        if (arguments.size() != 1) {
            throw error(peek(), "Expect condition for if statement");
        }

        Expr expr = expression();
        if (expr instanceof Expr.Block block) {
            List<Stmt.If> elifStmts = new ArrayList<>();
            Expr.Block elseBlock = null;

            if (matchKeyword(Keyword.ELSE_IF)) elifStmts = elifStatements();
            if(matchKeyword(Keyword.ELSE)) elseBlock = elseStatement();

            return new Stmt.If(arguments.get(0), block, elifStmts, elseBlock);
        }

        throw error(peek(), "Expect block after if statement");
    }

    private List<Stmt.If> elifStatements() {
        consume(OPEN_PAREN, "Expect '(' after 'elif'");

        List<Expr> arguments = arguments();

        if (arguments.size() != 1) {
            throw error(peek(), "Expect condition for elif statement");
        }

        Expr expr = expression();
        List<Stmt.If> elifStmts = new ArrayList<>();
        if (expr instanceof Expr.Block block) {

            //capture all elifs
            if (matchKeyword(Keyword.ELSE_IF)) elifStmts = elifStatements();

            //insert at 0th index so order is kept
            elifStmts.add(0, new Stmt.If(arguments.get(0), block, new ArrayList<>(), null));
            return elifStmts;
        }

        throw error(peek(), "Expect block after elif statement");
    }

    private Expr.Block elseStatement() {
        Expr expr = expression();
        if (expr instanceof Expr.Block block) {
            return block;
        }
        throw error(peek(), "Expect block after else statement");
    }

    private Stmt returnStatement() {
        Expr value = expression();

        consume(SEMICOLON, "Expect ';' after return statement");
        return new Stmt.Return(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = coalesce();

        if (match(EQUALS)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Assignable v) {
                return new Expr.Assignment(v, value);
            }

            throw error(equals, "Invalid assignment target");
        }

        return expr;
    }

    private Expr disjunction() {
        var expr = conjunction();

        while (match(OR)) {
            Expr right = conjunction();
            expr = new Expr.BinOp(Operator.OR, expr, right);
        }
        return expr;
    }

    private Expr conjunction() {
        var expr = equality();

        while (match(AND)) {
            Expr right = equality();
            expr = new Expr.BinOp(Operator.AND, expr, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.BinOp(Operator.from(operator), expr, right);
        }

        return expr;
    }

    private Expr comparison() {
        var expr = term();

        while (match(GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.BinOp(Operator.from(operator), expr, right);
        }

        return expr;
    }

    private Expr coalesce() {
        var expr = ternary();

        while (match(COALESCE)) {
            Expr right = ternary();
            expr = new Expr.Coalesce(expr, right);
        }

        return expr;
    }

    private Expr term() {
        var expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.BinOp(Operator.from(operator), expr, right);
        }
        return expr;
    }

    private Expr factor() {
        var expr = exponentiation();
        while (match(STAR, SLASH, PERCENT)) {
            Token operator = previous();
            Expr right = exponentiation();
            expr = new Expr.BinOp(Operator.from(operator), expr, right);
        }
        return expr;
    }

    private Expr exponentiation() {
        var expr = unary();
        while (match(CARET)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.BinOp(Operator.from(operator), expr, right);
        }
        return expr;
    }

    private Expr ternary() {
        Expr access = disjunction();
        if (match(QUESTION)) {
            Expr left = expression();
            if (match(COLON)) {
                Expr right = expression();
                return new Expr.Ternary(access, left, right);
            } else {
                return new Expr.Conditional(access, left);
            }
        }
        return access;
    }

    private Expr unary() {
        if (match(NOT)) {
            return new Expr.Not(unary());
        }
        if (match(MINUS)) {
            return new Expr.Negate(unary());
        }
        Expr access = access();
        if (match(ARROW)) {
            Expr right = expression();
            if (access instanceof Expr.Access acc) {
                return new Expr.SwitchContext(acc, right);
            }
            throw error(previous(), "Arrow operator must be used on an ACCESS target");
        }
        return access;
    }

    private Expr access() {
        Expr expr = primary();

        if (match(DOT)) {
            Token dot = previous();

            if (!(expr instanceof Expr.Variable v)) {
                throw error(dot, "Invalid access target");
            }

            if (match(IDENTIFIER)) {
                return finishAccess(v);
            } else {
                throw error(dot, "Expect identifier after '.'");
            }
        } else {
            return expr;
        }
    }

    private Expr finishAccess(Expr.Variable v) {
        Token name = previous();

        if (match(OPEN_PAREN)) {
            List<Expr> arguments = arguments();

            return new Expr.Call(v, name.lexeme(), arguments);
        }
        else if (match(DOT)) {
            Expr.Struct parentStruct = new Expr.Struct(v, null, new ArrayList<>());

            //child's child's child's...child
            Expr.Struct infant = finishStruct(parentStruct);
            return new Expr.Access(infant, infant.target().name());
        }
        else {
            return new Expr.Access(v, name.lexeme());
        }
    }

    private Expr.Struct finishStruct(Expr.Struct parentStruct) {
        if(match(IDENTIFIER)) {
            Expr.Struct child = new Expr.Struct(ParserUtils.createVariableFromString(previous().lexeme()), parentStruct, new ArrayList<>());
            parentStruct.children().add(child);
            if(!match(DOT)) return child;
            parentStruct = finishStruct(child);
        }
        return parentStruct;
    }

    private List<Expr> arguments() {
        List<Expr> arguments = new ArrayList<>();
        if (!check(CLOSE_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(COMMA));
        }

        consume(CLOSE_PAREN, "Expect ')' after arguments");
        return arguments;
    }

    private Expr primary() {
        if (match(OPEN_BRACE)) {
            return block();
        }

        if (match(IDENTIFIER)) {
            String lexeme = previous().lexeme();

            //enforce lowercase variable names
            if (!lexeme.toLowerCase().equals(lexeme)) {
                throw error(previous(), "Variable names must be lowercase");
            }
            Expr constant = constants.get(lexeme);
            return constant == null ? ParserUtils.createVariableFromString(lexeme) : constant;
        }

        if (match(NUMERAL)) {
            return new Expr.Constant(Double.parseDouble(previous().lexeme()));
        }

        if (match(STRING)) {
            return new Expr.Str(previous().lexeme());
        }

        if (match(OPEN_PAREN)) {
            Expr expr = expression();
            consume(CLOSE_PAREN, "Expect ')' after expression");
            return new Expr.Group(expr);
        }

        throw error(peek(), "Expect expression");
    }

    private Expr.Block block() {
        var statements = new ArrayList<Stmt>();

        while (!isAtEnd() && !check(CLOSE_BRACE)) {
            statements.add(statement());
        }
        consume(CLOSE_BRACE, "Expect '}' after block");
        return new Expr.Block(statements);
    }

    private Token consume(TokenType required, String error) {
        if (check(required)) {
            return advance();
        }

        throw error(peek(), error);
    }

    private boolean matchKeyword(Keyword... value) {
        for (Keyword keyword : value) {
            if (check(IDENTIFIER) && keyword.matches(peek().lexeme())) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private ParseException error(Token faulty, String message) {
        return new ParseException(faulty, message);
    }

    private boolean isAtEnd() {
        return peek().tokenType() == EOF;
    }

    private boolean check(TokenType type) {
        return peek().tokenType() == type;
    }

    private boolean checkNext(TokenType type) {
        return peekNext().tokenType() == type;
    }

    private Token advance() {
        return input.get(current++);
    }

    private Token peek() {
        if (current >= input.size()) return input.get(input.size() - 1);
        return input.get(current);
    }

    private Token peekNext() {
        if (current + 1 >= input.size()) return input.get(input.size() - 1);
        return input.get(current + 1);
    }

    private Token previous() {
        return input.get(current - 1);
    }

}
