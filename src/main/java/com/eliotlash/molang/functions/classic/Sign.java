package com.eliotlash.molang.functions.classic;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;

/**
 * Absolute value function
 */
public class Sign extends Function {

    public Sign(String name) {
        super(name);
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        return Math.signum(this.evaluateArgument(arguments, ctx, 0));
    }
}
