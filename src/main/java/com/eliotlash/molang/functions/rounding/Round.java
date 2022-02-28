package com.eliotlash.molang.functions.rounding;

import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;
import com.eliotlash.molang.ast.Expr;

public class Round extends Function {
	public Round(Expr[] values, String name) throws Exception {
		super(values, name);
	}

	@Override
	public int getRequiredArguments() {
		return 1;
	}

	@Override
	public double evaluate(ExecutionContext ctx) {
		return Math.round(this.evaluateArgument(ctx, 0));
	}
}