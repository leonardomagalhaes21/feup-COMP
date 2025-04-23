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

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);
        String code = ollirTypes.nextTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        // Handle different operators with proper OLLIR syntax
        computation.append(op).append(resOllirType).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
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

    private OllirExprResult visitNewArray(JmmNode node, Void dummy) {
        // Modified to ensure proper OLLIR syntax for array creation
        OllirExprResult sizeExpr = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder(sizeExpr.getComputation());

        String arrayType = ".array.i32";
        String tempVar = ollirTypes.nextTemp();

        // Generate array creation code with proper OLLIR syntax
        computation.append(tempVar).append(arrayType)
                .append(" :=").append(arrayType)
                .append(" new(array, ")
                .append(sizeExpr.getCode())
                .append(")")
                .append(END_STMT);

        return new OllirExprResult(tempVar + arrayType, computation);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void dummy) {
        OllirExprResult arrayExpr = visit(node.getChild(0));
        OllirExprResult indexExpr = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();
        computation.append(arrayExpr.getComputation());
        computation.append(indexExpr.getComputation());

        String tempVar = ollirTypes.nextTemp();
        String resultType = ".i32";

        computation.append(tempVar).append(resultType)
                .append(" :=").append(resultType)
                .append(" ").append(arrayExpr.getCode())
                .append("[").append(indexExpr.getCode()).append("]")
                .append(END_STMT);

        return new OllirExprResult(tempVar + resultType, computation);
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
}