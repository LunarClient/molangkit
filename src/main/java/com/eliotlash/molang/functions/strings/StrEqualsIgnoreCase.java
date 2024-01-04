package com.eliotlash.molang.functions.strings;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.utils.MolangUtils;
import com.eliotlash.molang.variables.ExecutionContext;

public class StrEqualsIgnoreCase extends Function {
    public StrEqualsIgnoreCase(String name) {
        super(name);
    }

    @Override
    public double _evaluate(Expr[] arguments, ExecutionContext ctx) {
        final String first = ctx.getEvaluator().evaluateString(arguments[0]);
        final String second = ctx.getEvaluator().evaluateString(arguments[1]);
        return MolangUtils.booleanToFloat(first.equalsIgnoreCase(second));
    }

    @Override
    public int getRequiredArguments() {
        return 2;
    }
}
