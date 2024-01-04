package com.eliotlash.molang.functions.strings;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;

public class Length extends Function {
    public Length(String name) {
        super(name);
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        return ctx.getEvaluator().evaluateString(arguments[0]).length();
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }
}
