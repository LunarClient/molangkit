package com.eliotlash.molang;

import java.util.HashMap;
import java.util.Map;

import com.eliotlash.molang.ast.Expr;

public class CompileConstants {

	/**
	 * Known, constant values that can be used in expressions.
	 */
	public Map<String, Expr.Constant> constants = new HashMap<>();

	public CompileConstants() {
		// Bedrock Parity
		this.registerConstant("math.pi", Math.PI);
	}

	public void registerConstant(String name, double value) {
		this.constants.put(name, new Expr.Constant(value));
	}

	public Expr get(String name) {
		return this.constants.get(name);
	}
}
