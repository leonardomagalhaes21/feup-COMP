package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(NEW_EXPR, this::visitNewExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArray);
        addVisit(ARRAY_ACCESS_EXPR, this::visitArrayAccess);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(LENGTH_EXPR, this::visitArrayLength);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(FUNC_EXPR, this::visitFuncExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        var boolType = TypeUtils.newType(TypeName.BOOLEAN, false);
        String ollirBoolType = ollirTypes.toOllirType(boolType);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newType(TypeName.INT, false);
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));
        String op = node.get("op");

        StringBuilder computation = new StringBuilder();

        // Get the result type and its OLLIR equivalent
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);
        // temporary to store the result
        String tempName = ollirTypes.nextTemp();

        if (op.equals("&&")) {
            String labelEval = OptUtils.getLabel("andEval");
            String labelFalse = OptUtils.getLabel("andFalse");
            String labelTrue = OptUtils.getLabel("andTrue");
            String labelEnd = OptUtils.getLabel("andEnd");

            // If lhs is true, evaluate rhs, otherwise go to false
            computation.append(lhs.getComputation());
            computation.append("if (").append(lhs.getCode()).append(") goto ").append(labelEval).append(END_STMT);
            computation.append("goto ").append(labelFalse).append(END_STMT);

            // Evaluate rhs
            computation.append(labelEval).append(":\n");
            computation.append(rhs.getComputation());
            computation.append("if (").append(rhs.getCode()).append(") goto ").append(labelTrue).append(END_STMT);
            computation.append("goto ").append(labelFalse).append(END_STMT);

            // False label: assign false
            computation.append(labelFalse).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" false")
                    .append(resOllirType).append(END_STMT);
            computation.append("goto ").append(labelEnd).append(END_STMT);

            // True label: assign true
            computation.append(labelTrue).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" true")
                    .append(resOllirType).append(END_STMT);

            // End label
            computation.append(labelEnd).append(":\n");

            return new OllirExprResult(tempName + resOllirType, computation.toString());
        } else if (op.equals("||")) {
            String labelEval = OptUtils.getLabel("orEval");
            String labelFalse = OptUtils.getLabel("orFalse");
            String labelTrue = OptUtils.getLabel("orTrue");
            String labelEnd = OptUtils.getLabel("orEnd");

            // If lhs is true, go to true, otherwise evaluate rhs
            computation.append(lhs.getComputation());
            computation.append("if (").append(lhs.getCode()).append(") goto ").append(labelTrue).append(END_STMT);
            computation.append("goto ").append(labelEval).append(END_STMT);

            // Evaluate rhs
            computation.append(labelEval).append(":\n");
            computation.append(rhs.getComputation());
            computation.append("if (").append(rhs.getCode()).append(") goto ").append(labelTrue).append(END_STMT);
            computation.append("goto ").append(labelFalse).append(END_STMT);

            // False label: assign false
            computation.append(labelFalse).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" false")
                    .append(resOllirType).append(END_STMT);
            computation.append("goto ").append(labelEnd).append(END_STMT);

            // True label: assign true
            computation.append(labelTrue).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" true")
                    .append(resOllirType).append(END_STMT);

            // End label
            computation.append(labelEnd).append(":\n");

            return new OllirExprResult(tempName + resOllirType, computation.toString());
        } else {
            // Default handling for other operators
            computation.append(lhs.getComputation());
            computation.append(rhs.getComputation());

            String tempWithType = tempName + resOllirType;
            computation.append(tempWithType)
                    .append(" :=")
                    .append(resOllirType)
                    .append(" ")
                    .append(lhs.getCode())
                    .append(" ")
                    .append(op)
                    .append(resOllirType)
                    .append(" ")
                    .append(rhs.getCode())
                    .append(END_STMT);

            return new OllirExprResult(tempWithType, computation.toString());
        }
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String name = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);

        // Check if this is referring to a local variable first (including parameters)
        JmmNode methodNode = node.getAncestor(METHOD_DECL).orElse(null);
        if (methodNode != null) {
            String methodName = methodNode.get("name");
            boolean isLocalVar = table.getLocalVariables(methodName).stream()
                    .anyMatch(var -> var.getName().equals(name));
            boolean isParam = table.getParameters(methodName).stream()
                    .anyMatch(param -> param.getName().equals(name));

            // If it's a local variable or parameter, use direct reference
            if (isLocalVar || isParam) {
                return new OllirExprResult(name + ollirType);
            }
        }

        // Check if this is a field access
        boolean isField = table.getFields().stream()
                .anyMatch(field -> field.getName().equals(name));

        if (isField) {
            // For fields, use getfield instruction
            String tempVar = ollirTypes.nextTemp();
            StringBuilder computation = new StringBuilder();
            computation.append(tempVar).append(ollirType)
                    .append(" :=").append(ollirType)
                    .append(" getfield(this, ").append(name).append(ollirType).append(")").append(ollirType)
                    .append(";\n");

            return new OllirExprResult(tempVar + ollirType, computation);
        } else {
            // Regular variable
            return new OllirExprResult(name + ollirType);
        }
    }

    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return OllirExprResult.EMPTY;
    }

    private OllirExprResult visitNewExpr(JmmNode node, Void dummy) {
        // Get the class name from the node
        String className = node.get("classname");

        // Create a new temp variable
        String tempVar = ollirTypes.nextTemp();
        String typeString = "." + className;

        // Build the OLLIR code for object instantiation
        StringBuilder computation = new StringBuilder();
        computation.append(tempVar).append(typeString)
                .append(" :=").append(typeString)
                .append(" new(").append(className).append(")")
                .append(typeString)
                .append(";\n");

        // Invoke the constructor
        computation.append("invokespecial(").append(tempVar).append(typeString)
                .append(", \"<init>\").V")
                .append(";\n");

        return new OllirExprResult(tempVar + typeString, computation);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void u) {
        String type = ollirTypes.toOllirType(TypeUtils.newType(TypeName.INT, true)); // array always int

        String tmp = ollirTypes.nextTemp() + type;

        var expr = visit(node.getChild(0));

        StringBuilder computation = new StringBuilder();

        computation.append(expr.getComputation());
        computation.append(tmp).append(" :=").append(type);
        computation.append(" new(array, ").append(expr.getCode());
        computation.append(")").append(type).append(END_STMT);

        return new OllirExprResult(tmp, computation);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void u) {
        String type = ollirTypes.toOllirType(TypeUtils.newType(TypeName.INT, false));

        String tmp = ollirTypes.nextTemp() + type;

        var v = visit(node.getChild(0));
        var expr = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        computation.append(v.getComputation()).append(expr.getComputation());
        computation.append(tmp).append(" :=").append(type);
        computation.append(" ").append(v.getCode());
        computation.append("[").append(expr.getCode()).append("]");
        computation.append(type).append(END_STMT);

        return new OllirExprResult(tmp, computation);
    }

    private OllirExprResult visitArrayLength(JmmNode node, Void dummy) {
        OllirExprResult arrayExpr = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder(arrayExpr.getComputation());

        String tempVar = ollirTypes.nextTemp();
        String resultType = ".i32";

        computation.append(tempVar).append(resultType)
                .append(" :=").append(resultType)
                .append(" arraylength(").append(arrayExpr.getCode()).append(")")
                .append(resultType)
                .append(END_STMT);

        return new OllirExprResult(tempVar + resultType, computation);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void dummy) {
        String methodName = node.get("methodname");
        StringBuilder computation = new StringBuilder();

        // Process arguments first, preserving their evaluation order
        List<OllirExprResult> args = new ArrayList<>();
        for (int i = 0; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChild(i));
            // Add computation first to preserve evaluation order
            computation.append(arg.getComputation());
            args.add(arg);
        }

        // Determine return type
        Type returnType = types.getExprType(node);
        String returnTypeStr = ollirTypes.toOllirType(returnType);

        // Check if this is an imported class
        boolean isImported = false;
        String className = methodName; // Default to method name

        // If the method is called on something (e.g., io.println), extract the class
        // name
        if (!args.isEmpty()) {
            String firstArg = args.get(0).getCode();
            if (firstArg.contains(".")) {
                className = firstArg.substring(0, firstArg.indexOf("."));
            }

            // Check if the class is imported
            for (String imp : table.getImports()) {
                if (imp.equals(className) || imp.endsWith("." + className)) {
                    isImported = true;
                    break;
                }
            }
        }

        // Create temp var for result if needed (non-void return type)
        String tempVar = "";
        if (!returnTypeStr.equals(".V")) {
            tempVar = ollirTypes.nextTemp();
        }

        // Generate appropriate invocation based on whether it's imported (static) or
        // not
        if (isImported) {
            if (returnTypeStr.equals(".V")) {
                // Void static method call
                computation.append("invokestatic(")
                        .append(className).append(", \"")
                        .append(methodName).append("\"");

                if (args.size() > 1) {
                    // Skip the first arg which is the class name
                    String remainingArgs = args.stream()
                            .skip(1)
                            .map(OllirExprResult::getCode)
                            .collect(Collectors.joining(", "));

                    if (!remainingArgs.isEmpty()) {
                        computation.append(", ").append(remainingArgs);
                    }
                }

                computation.append(")")
                        .append(returnTypeStr)
                        .append(";\n");

                return new OllirExprResult("", computation);
            } else {
                // Non-void static method call with return value
                String tempWithType = tempVar + returnTypeStr;

                computation.append(tempWithType)
                        .append(" :=").append(returnTypeStr)
                        .append(" invokestatic(")
                        .append(className).append(", \"")
                        .append(methodName).append("\"");

                if (args.size() > 1) {
                    // Skip the first arg which is the class name
                    String remainingArgs = args.stream()
                            .skip(1)
                            .map(OllirExprResult::getCode)
                            .collect(Collectors.joining(", "));

                    if (!remainingArgs.isEmpty()) {
                        computation.append(", ").append(remainingArgs);
                    }
                }

                computation.append(")")
                        .append(returnTypeStr)
                        .append(";\n");

                return new OllirExprResult(tempWithType, computation);
            }
        } else {
            // Regular instance method call
            if (returnTypeStr.equals(".V")) {
                // Void instance method call
                computation.append("invokevirtual(")
                        .append(args.get(0).getCode()).append(", \"")
                        .append(methodName).append("\"");

                if (args.size() > 1) {
                    // Skip the first arg which is the object
                    String remainingArgs = args.stream()
                            .skip(1)
                            .map(OllirExprResult::getCode)
                            .collect(Collectors.joining(", "));

                    if (!remainingArgs.isEmpty()) {
                        computation.append(", ").append(remainingArgs);
                    }
                }

                computation.append(")")
                        .append(returnTypeStr)
                        .append(";\n");

                return new OllirExprResult("", computation);
            } else {
                // Non-void instance method call with return value
                String tempWithType = tempVar + returnTypeStr;

                computation.append(tempWithType)
                        .append(" :=").append(returnTypeStr)
                        .append(" invokevirtual(")
                        .append(args.get(0).getCode()).append(", \"")
                        .append(methodName).append("\"");

                if (args.size() > 1) {
                    // Skip the first arg which is the object
                    String remainingArgs = args.stream()
                            .skip(1)
                            .map(OllirExprResult::getCode)
                            .collect(Collectors.joining(", "));

                    if (!remainingArgs.isEmpty()) {
                        computation.append(", ").append(remainingArgs);
                    }
                }

                computation.append(")")
                        .append(returnTypeStr)
                        .append(";\n");

                return new OllirExprResult(tempWithType, computation);
            }
        }
    }

    private OllirExprResult visitFuncExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        // Get method name
        String methodName = node.get("methodname");

        // Special case for array length expression
        if (methodName.equals("length") &&
                node.getNumChildren() > 0 &&
                node.getChild(0).getKind().equals(VAR_REF_EXPR.getNodeName())) {

            OllirExprResult arrayExpr = visit(node.getChild(0));

            // Check if this is an array type
            Type type = types.getExprType(node.getChild(0));
            if (type != null && type.isArray()) {
                computation.append(arrayExpr.getComputation());

                String tempVar = ollirTypes.nextTemp();
                String resultType = ".i32";

                computation.append(tempVar).append(resultType)
                        .append(" :=").append(resultType)
                        .append(" arraylength(").append(arrayExpr.getCode()).append(")")
                        .append(resultType)
                        .append(END_STMT);

                return new OllirExprResult(tempVar + resultType, computation);
            }
        }

        // Build arguments list
        List<OllirExprResult> args = new ArrayList<>();
        for (int i = 0; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChild(i));
            args.add(arg);
            computation.append(arg.getComputation());
        }

        // Check if the first child is a reference to an imported class (for static
        // method calls)
        boolean isImported = false;
        String importedClass = "";

        if (node.getNumChildren() > 0 && node.getChild(0).getKind().equals(VAR_REF_EXPR.getNodeName())) {
            String className = node.getChild(0).get("name");

            // Check if the class is imported
            for (String imp : table.getImports()) {
                if (imp.equals(className) || imp.endsWith("." + className)) {
                    isImported = true;
                    importedClass = className;
                    break;
                }
            }
        }

        // Determine return type
        Type returnType = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(returnType);

        // Generate a temp var to store the result if needed (for non-void methods)
        String resultVar = ollirTypes.nextTemp();
        String resultWithType = resultVar + ollirType;

        if (isImported) {
            // This is a static method call to an imported class like io.println
            if (ollirType.equals(".V")) {
                // Void methods don't need assignment
                computation.append("invokestatic(")
                        .append(importedClass).append(", \"")
                        .append(methodName).append("\"");

                // Add arguments, skipping the first one (which is the class name)
                if (args.size() > 1) {
                    for (int i = 1; i < args.size(); i++) {
                        if (i == 1) {
                            computation.append(", ");
                        } else {
                            computation.append(", ");
                        }
                        computation.append(args.get(i).getCode());
                    }
                }

                computation.append(")")
                        .append(ollirType)
                        .append(";\n");

                return new OllirExprResult("", computation);
            } else {
                // Non-void methods need assignment to store result
                computation.append(resultWithType)
                        .append(" :=").append(ollirType).append(" ")
                        .append("invokestatic(")
                        .append(importedClass).append(", \"")
                        .append(methodName).append("\"");

                // Add arguments, skipping the first one (which is the class name)
                if (args.size() > 1) {
                    for (int i = 1; i < args.size(); i++) {
                        if (i == 1) {
                            computation.append(", ");
                        } else {
                            computation.append(", ");
                        }
                        computation.append(args.get(i).getCode());
                    }
                }

                computation.append(")")
                        .append(ollirType)
                        .append(";\n");

                return new OllirExprResult(resultWithType, computation);
            }
        } else {
            // Regular instance method call
            if (args.isEmpty()) {
                // No target object provided
                return OllirExprResult.EMPTY;
            }

            String target = args.get(0).getCode();

            if (ollirType.equals(".V")) {
                // Void method
                computation.append("invokevirtual(")
                        .append(target).append(", \"")
                        .append(methodName).append("\"");

                // Add the rest of the arguments
                if (args.size() > 1) {
                    for (int i = 1; i < args.size(); i++) {
                        computation.append(", ").append(args.get(i).getCode());
                    }
                }

                computation.append(")")
                        .append(ollirType)
                        .append(";\n");

                return new OllirExprResult("", computation);
            } else {
                // Non-void method
                computation.append(resultWithType)
                        .append(" :=").append(ollirType).append(" ")
                        .append("invokevirtual(")
                        .append(target).append(", \"")
                        .append(methodName).append("\"");

                // Add the rest of the arguments
                if (args.size() > 1) {
                    for (int i = 1; i < args.size(); i++) {
                        computation.append(", ").append(args.get(i).getCode());
                    }
                }

                computation.append(")")
                        .append(ollirType)
                        .append(";\n");

                return new OllirExprResult(resultWithType, computation);
            }
        }
    }
}