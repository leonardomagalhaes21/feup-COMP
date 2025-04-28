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
            String labelEval  = OptUtils.getLabel("andEval");
            String labelFalse = OptUtils.getLabel("andFalse");
            String labelTrue  = OptUtils.getLabel("andTrue");
            String labelEnd   = OptUtils.getLabel("andEnd");

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
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" false").append(resOllirType).append(END_STMT);
            computation.append("goto ").append(labelEnd).append(END_STMT);

            // True label: assign true
            computation.append(labelTrue).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" true").append(resOllirType).append(END_STMT);

            // End label
            computation.append(labelEnd).append(":\n");

            return new OllirExprResult(tempName + resOllirType, computation.toString());
        } else if (op.equals("||")) {
            String labelEval  = OptUtils.getLabel("orEval");
            String labelFalse = OptUtils.getLabel("orFalse");
            String labelTrue  = OptUtils.getLabel("orTrue");
            String labelEnd   = OptUtils.getLabel("orEnd");

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
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" false").append(resOllirType).append(END_STMT);
            computation.append("goto ").append(labelEnd).append(END_STMT);

            // True label: assign true
            computation.append(labelTrue).append(":\n");
            computation.append(tempName).append(resOllirType).append(" :=").append(resOllirType).append(" true").append(resOllirType).append(END_STMT);

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
        var id = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        String code = id + ollirType;
        return new OllirExprResult(code);
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
                .append(END_STMT);

        // Invoke the constructor
        computation.append("invokespecial(").append(tempVar).append(typeString)
                .append(", \"<init>\").V").append(END_STMT);

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
                .append(END_STMT);

        return new OllirExprResult(tempVar + resultType, computation);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void dummy) {
        String methodName = node.get("methodname");
        OllirExprResult objResult = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder(objResult.getComputation());

        // Process arguments
        List<OllirExprResult> args = new ArrayList<>();
        for (int i = 1; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChild(i));
            computation.append(arg.getComputation());
            args.add(arg);
        }

        // Build arguments string
        String argsString = args.stream()
                .map(OllirExprResult::getCode)
                .collect(Collectors.joining(", "));

        // Determine return type
        Type returnType = types.getExprType(node);
        String returnTypeStr = ollirTypes.toOllirType(returnType);

        // Generate invocation
        String tempVar = ollirTypes.nextTemp();
        String tempWithType = tempVar + returnTypeStr;

        computation.append(tempWithType)
                .append(" :=").append(returnTypeStr)
                .append(" invokevirtual(")
                .append(objResult.getCode()).append(", \"")
                .append(methodName).append("\"")
                .append(argsString.isEmpty() ? "" : ", " + argsString)
                .append(")").append(returnTypeStr)
                .append(";\n");

        return new OllirExprResult(tempWithType, computation);
    }

    private OllirExprResult visitFuncExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        // Get method name
        String methodName = node.get("methodname");

        // Build arguments list
        List<OllirExprResult> args = new ArrayList<>();
        for (int i = 0; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChild(i));
            args.add(arg);
            computation.append(arg.getComputation());
        }

        // Check if this is a method call on an imported class
        boolean isImported = table.getImports().stream()
                .anyMatch(imp -> imp.equals(methodName) || imp.endsWith("." + methodName));

        String target = isImported ? methodName : "this";
        String invokeType = isImported ? "invokestatic" : "invokevirtual";

        // Generate a temp var to store the result if needed
        Type returnType = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(returnType);
        String resultVar = ollirTypes.nextTemp();

        // Build the arguments string
        String argsStr = args.stream()
                .map(OllirExprResult::getCode)
                .collect(Collectors.joining(", "));

        // Handle the method call differently based on whether it's a static (imported) method or instance method
        if (isImported) {
            // For static methods on imported classes (like io.println)
            computation.append(resultVar).append(ollirType)
                    .append(" :=").append(ollirType).append(" ")
                    .append(invokeType).append("(").append(target);

            if (!args.isEmpty()) {
                computation.append(", ").append(argsStr);
            }

            computation.append(")").append(ollirType).append(";\n");
        } else {
            // For instance methods (calling a method on this or another object)
            // This part needs to be adjusted based on your specific requirements
            computation.append(resultVar).append(ollirType)
                    .append(" :=").append(ollirType).append(" ")
                    .append(invokeType).append("(").append(target).append(", \"")
                    .append(methodName).append("\"");

            if (!args.isEmpty()) {
                computation.append(", ").append(argsStr);
            }

            computation.append(")").append(ollirType).append(";\n");
        }

        return new OllirExprResult(resultVar + ollirType, computation);
    }
}