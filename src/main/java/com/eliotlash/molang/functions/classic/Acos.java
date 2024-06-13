package com.eliotlash.molang.functions.classic;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;

/**
 * Arc cosine function
 */
public class Acos extends Function {

    public Acos(String name) {
        super(name);
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        double a = this.evaluateArgument(arguments, ctx, 0);
        if (Math.abs(a) > 1) {
            return 0;
        }
        return Math.acos(a);
    }
}
