package com.eliotlash.molang.functions.classic;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;

/**
 * Arc sine function
 */
public class AsinDegrees extends Function {

    public AsinDegrees(String name) {
        super(name);
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        double a = this.evaluateArgument(arguments, ctx, 0) / 180 * Math.PI;
        if (Math.abs(a) > 1) {
            return 0;
        }
        return Math.asin(a);
    }
}
