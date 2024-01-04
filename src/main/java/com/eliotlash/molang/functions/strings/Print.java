package com.eliotlash.molang.functions.strings;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.variables.ExecutionContext;

public class Print extends Function {
    public Print(String name) {
        super(name);
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        if (arguments[0] instanceof Expr.Str) {
            System.out.println(ctx.getEvaluator().evaluateString(arguments[0]));
        } else {
            System.out.println(this.evaluateArgument(arguments, ctx, 0));
        }
        return 1.0;
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }
}
