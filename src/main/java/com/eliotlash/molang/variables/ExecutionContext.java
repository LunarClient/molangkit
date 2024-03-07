package com.eliotlash.molang.variables;

import com.eliotlash.molang.ConstantFunctions;
import com.eliotlash.molang.ast.Assignable;
import com.eliotlash.molang.ast.Evaluator;
import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.functions.FunctionDefinition;
import com.eliotlash.molang.functions.classic.*;
import com.eliotlash.molang.functions.limit.Clamp;
import com.eliotlash.molang.functions.limit.Max;
import com.eliotlash.molang.functions.limit.Min;
import com.eliotlash.molang.functions.rounding.Ceil;
import com.eliotlash.molang.functions.rounding.Floor;
import com.eliotlash.molang.functions.rounding.Round;
import com.eliotlash.molang.functions.rounding.Trunc;
import com.eliotlash.molang.functions.strings.Length;
import com.eliotlash.molang.functions.strings.Print;
import com.eliotlash.molang.functions.strings.StrEquals;
import com.eliotlash.molang.functions.strings.StrEqualsIgnoreCase;
import com.eliotlash.molang.functions.utility.*;

import com.eliotlash.molang.functions.utility.Random;
import com.eliotlash.molang.utils.MolangUtils;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;

public class ExecutionContext {

    private static final Map<FunctionDefinition, Function> BUILTIN_FUNCTIONS;

    private final Evaluator evaluator;

    public final Stack<Expr.Access> contextStack = new Stack<>();
    public final Map<VariableFlavor, List<Pair<RuntimeVariable, Expr.Access>>> flavorCache = new HashMap<>();
    public final Object2DoubleMap<Assignable> assignableMap = new Object2DoubleOpenHashMap<>();
    public Object2DoubleMap<Assignable> functionScopedArguments = new Object2DoubleOpenHashMap<>();

    private final Map<FunctionDefinition, Function> functionMap = new HashMap<>();
    private final Object2DoubleMap<RuntimeVariable> variableMap = new Object2DoubleOpenHashMap<>();
    private final Map<String, RuntimeVariable> variableCache = new HashMap<>();
    private final Object2DoubleMap<Expr.Struct> structMap = new Object2DoubleOpenHashMap<>();

    public ExecutionContext(Evaluator evaluator) {
        this.evaluator = evaluator;

        registerFunctions(BUILTIN_FUNCTIONS);
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public Object2DoubleMap<RuntimeVariable> getVariableMap() {
        return variableMap;
    }

    public Object2DoubleMap<Expr.Struct> getStructMap() {
        return structMap;
    }

    /**
     * Call with "query.something"
     */
    public RuntimeVariable getCachedVariable(String var) {
        return variableCache.computeIfAbsent(var, name -> {
            RuntimeVariable runtimeVariable = parseRuntimeVariable(name, null);
            return runtimeVariable;
        });
    }

    public RuntimeVariable parseRuntimeVariable(String name, Expr.Access access) {
        String[] split = name.split("\\.", 2);

        VariableFlavor flavor = VariableFlavor.parse(split[0]);
        RuntimeVariable runtimeVariable;
        if (split.length == 2 && flavor != null) {
            runtimeVariable = new RuntimeVariable(split[1], flavor);
        } else {
            runtimeVariable = new RuntimeVariable(name, null);
        }

        if (access != null) {
            Pair<RuntimeVariable, Expr.Access> pair = Pair.of(runtimeVariable, access);
            if (!flavorCache.containsKey(flavor)) {
                flavorCache.put(flavor, new ArrayList<>());
            }
            List<Pair<RuntimeVariable, Expr.Access>> list = flavorCache.get(flavor);
            if (!list.contains(pair)) {
                list.add(pair);
            }
        }
        return runtimeVariable;
    }

    public void setVariable(String var, double value) {
        RuntimeVariable cachedVariable = getCachedVariable(var);
        variableMap.put(cachedVariable, value);
    }

    public void setVariable(String var, boolean val) {
        setVariable(var, MolangUtils.booleanToFloat(val));
    }

    public void registerFunction(String target, Function function) {
        registerFunction(asFunctionDefinition(target, function), function);
    }

    public void registerFunction(FunctionDefinition definition, Function function) {
        functionMap.putIfAbsent(definition, function);
        if (function.isConstant()) {
            ConstantFunctions.addConstantFunction(definition);
        }
    }

    public void registerFunctions(Map<FunctionDefinition, Function> map) {
        map.forEach(this::registerFunction);
    }

    public Function getFunction(FunctionDefinition definition) {
        return this.functionMap.get(definition);
    }

    private static FunctionDefinition asFunctionDefinition(String target, Function function) {
        return new FunctionDefinition(new Expr.Variable(target), function.getName());
    }

    private static void addFunction(Map<FunctionDefinition, Function> map, String target, Function func) {
        map.put(asFunctionDefinition(target, func), func);
    }

    static {
        Map<FunctionDefinition, Function> map = new HashMap<>();
        addFunction(map, "math", new Abs("abs"));
        addFunction(map, "math", new CosDegrees("cos"));
        addFunction(map, "math", new Cos("cosradians"));
        addFunction(map, "math", new SinDegrees("sin"));
        addFunction(map, "math", new Sin("sinradians"));
        addFunction(map, "math", new Sign("sign"));
        addFunction(map, "math", new AsinDegrees("asin"));
        addFunction(map, "math", new AcosDegrees("acos"));
        addFunction(map, "math", new AtanDegrees("atan"));
        addFunction(map, "math", new Atan2("atan2"));
        addFunction(map, "math", new Exp("exp"));
        addFunction(map, "math", new Ln("ln"));
        addFunction(map, "math", new Mod("mod"));
        addFunction(map, "math", new Pow("pow"));
        addFunction(map, "math", new Sqrt("sqrt"));
        addFunction(map, "math", new Clamp("clamp"));
        addFunction(map, "math", new Max("max"));
        addFunction(map, "math", new Min("min"));
        addFunction(map, "math", new Ceil("ceil"));
        addFunction(map, "math", new Floor("floor"));
        addFunction(map, "math", new Round("round"));
        addFunction(map, "math", new Trunc("trunc"));
        addFunction(map, "math", new Lerp("lerp"));
        addFunction(map, "math", new LerpRotate("lerprotate"));
        addFunction(map, "math", new MinAngle("min_angle"));
        addFunction(map, "math", new Random("random"));
        addFunction(map, "math", new RandomInteger("random_integer"));
        addFunction(map, "math", new DiceRoll("dice_roll"));
        addFunction(map, "math", new DiceRollInteger("dice_roll_integer"));
        addFunction(map, "system", new Print("print"));
        addFunction(map, "string", new StrEquals("equals"));
        addFunction(map, "string", new StrEqualsIgnoreCase("equalsIgnoreCase"));
        addFunction(map, "string", new Length("length"));
        BUILTIN_FUNCTIONS = Map.copyOf(map);
    }
}
