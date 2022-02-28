package com.eliotlash.molang;

import com.eliotlash.molang.ast.Expr;

public class Molang {
	public static Expr parse(String expression) {
		return new Parser(Lexer.tokenize(expression)).parse();
	}
}