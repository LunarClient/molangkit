package com.eliotlash.molang.utils;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.variables.VariableFlavor;

public class ParserUtils {
    public static Expr.Variable createVariableFromString(String string) {
        String[] split = string.split("\\.", 2);
        if (split.length == 1) {
            return new Expr.Variable(null, split[0]);
        } else {
            return new Expr.Variable(VariableFlavor.parse(split[0]), split[1]);
        }
    }
}
