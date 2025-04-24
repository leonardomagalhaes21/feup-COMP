package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;
    private boolean needsReturnValue = true;
    private boolean hasReturnStatement = false;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(PARAMETERS, this::visitParameters);

        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(ARRAY_ACCESS_EXPR, this::visitComplexArrayAccess);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayElement);

        addVisit(IMPORT_DECL, this::visitImport);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Get condition expression
        JmmNode condExpr = node.getChild(0);
        OllirExprResult condition = exprVisitor.visit(condExpr);

        // Generate unique labels for then/else/end blocks
        String thenLabel = ollirTypes.nextTemp("then");
        String elseLabel = ollirTypes.nextTemp("else");
        String endLabel = ollirTypes.nextTemp("endif");

        // Add condition computation
        code.append(condition.getComputation());

        // Add conditional branch instruction
        code.append("if (").append(condition.getCode()).append(") goto ").append(thenLabel).append(END_STMT);

        // If there's an else block
        if (node.getNumChildren() > 2) {
            code.append("goto ").append(elseLabel).append(END_STMT);
        } else {
            code.append("goto ").append(endLabel).append(END_STMT);
        }

        // Add then label and then branch
        code.append(thenLabel).append(":").append(NL);
        code.append(visit(node.getChild(1)));
        code.append("goto ").append(endLabel).append(END_STMT);

        // Add else branch if it exists
        if (node.getNumChildren() > 2) {
            code.append(elseLabel).append(":").append(NL);
            code.append(visit(node.getChild(2)));
        }

        // Add end label
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Generate unique labels for condition/loop/end
        String condLabel = ollirTypes.nextTemp("whileCond");
        String loopLabel = ollirTypes.nextTemp("whileBody");
        String endLabel = ollirTypes.nextTemp("whileEnd");

        // Jump to condition evaluation
        code.append("goto ").append(condLabel).append(END_STMT);

        // Add condition label
        code.append(condLabel).append(":").append(NL);

        // Get condition expression
        JmmNode condExpr = node.getChild(0);
        OllirExprResult condition = exprVisitor.visit(condExpr);
        code.append(condition.getComputation());

        // Add conditional branch
        code.append("if (").append(condition.getCode()).append(") goto ").append(loopLabel).append(END_STMT);
        code.append("goto ").append(endLabel).append(END_STMT);

        // Add loop body
        code.append(loopLabel).append(":").append(NL);
        code.append(visit(node.getChild(1)));

        // Jump back to condition
        code.append("goto ").append(condLabel).append(END_STMT);

        // Add end label
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {
        // Imports don't generate code in OLLIR
        return "";
    }
    private String visitParameters(JmmNode node, Void unused) {
        StringBuilder params = new StringBuilder();

        // Visit each parameter and join them with commas
        for (int i = 0; i < node.getNumChildren(); i++) {
            params.append(visit(node.getChild(i)));
            if (i < node.getNumChildren() - 1) {
                params.append(", ");
            }
        }

        return params.toString();
    }
    private String defaultVisit(JmmNode node, Void unused) {
        StringBuilder result = new StringBuilder();

        // Visit all children and concatenate the results
        for (var child : node.getChildren()) {
            result.append(visit(child));
        }

        return result.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        // Reset state variables for each method
        hasReturnStatement = false;
        needsReturnValue = true;

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = node.getBoolean("isPublic", false);

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        var paramsCode = visit(node.getChild(1));
        code.append("(" + paramsCode + ")");

        // type - get the return type from the method's type node
        JmmNode typeNode = node.getChild(0);
        Type returnType = TypeUtils.convertType(typeNode);
        String retType = ollirTypes.toOllirType(returnType);
        code.append(retType);
        code.append(L_BRACKET);

        // rest of its children stmts
        var stmtsCode = node.getChildren(STMT).stream()
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));

        code.append(stmtsCode);

        // Handle return values if needed
        if (needsReturnValue && !hasReturnStatement) {
            // Add default return for void methods
            if (returnType.getName().equals("void")) {
                code.append("\n    ret.V");
            } else {
                // For non-void methods, add a default return value (like 0 for int)
                String defaultValue = "0" + ollirTypes.toOllirType(returnType);
                code.append("\n    ret").append(retType).append(" ").append(defaultValue);
            }
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    // Update visitReturn to set hasReturnStatement
    private String visitReturn(JmmNode node, Void unused) {
        JmmNode methodNode = node.getAncestor(METHOD_DECL).orElse(null);
        Type retType = methodNode != null ?
                TypeUtils.convertType(methodNode.getChild(0)) :
                TypeUtils.newType(TypeName.INT, false);

        StringBuilder code = new StringBuilder();
        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;

        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);
        code.append(expr.getCode());
        code.append(END_STMT);

        hasReturnStatement = true;

        return code.toString();
    }

    // Fix visitAssignStmt to properly get variable types
    private String visitAssignStmt(JmmNode node, Void unused) {
        JmmNode left = node.getChild(0);
        JmmNode right = node.getChild(1);
        StringBuilder code = new StringBuilder();

        if (left.getKind().equals(ARRAY_ACCESS_EXPR.getNodeName())) {
            // Handle array assignment separately
            return visitArrayElement(node, unused);
        }

        String name = left.get("name");
        Type type = types.getExprType(left); // Use getExprType instead of getVarType

        var rhs = exprVisitor.visit(right);
        code.append(rhs.getComputation());

        code.append(name).append(ollirTypes.toOllirType(type))
                .append(" :=").append(ollirTypes.toOllirType(type))
                .append(" ")
                .append(rhs.getCode())
                .append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }






    private String visitClass(JmmNode node, Void u) {
        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        var superClass = table.getSuper().isEmpty() ? "Object" : table.getSuper();
        code.append(" extends ").append(superClass).append(L_BRACKET);
        for (var field : table.getFields()) {
            code.append(".field public ").append(field.getName());
            code.append(ollirTypes.toOllirType(field.getType())).append(END_STMT);
        }

        code.append(buildConstructor());

        // instantiate methods
        for (var method : node.getChildren()) {
            if (method.getKind().equals("Method")) {
                code.append(visit(method));
            }
        }

        code.append(R_BRACKET);
        return code.toString();
    }


    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }



    private String visitComplexArrayAccess(JmmNode node, Void unused) {
        // This handles more complex array access like a[i+j] or a[b[i]]
        JmmNode arrayExpr = node.getChild(0);
        JmmNode indexExpr = node.getChild(1);

        // Generate code for array and index expressions
        OllirExprResult array = exprVisitor.visit(arrayExpr);
        OllirExprResult index = exprVisitor.visit(indexExpr);

        // Build the complete computation
        StringBuilder code = new StringBuilder();
        code.append(array.getComputation());
        code.append(index.getComputation());

        // Create a temporary for the result
        String tempVar = ollirTypes.nextTemp();
        String resultType = ".i32"; // Array element type is int

        // Generate the array access instruction with proper OLLIR syntax
        code.append(tempVar).append(resultType)
                .append(" :=").append(resultType)
                .append(" ").append(array.getCode())
                .append("[").append(index.getCode()).append("]")
                .append(END_STMT);

        return new OllirExprResult(tempVar + resultType, code.toString()).getComputation();
    }

    private String visitArrayElement(JmmNode node, Void unused) {
        // Handle array element assignment (a[i] = x)
        JmmNode arrayAccessNode = node.getChild(0);
        JmmNode valueExpr = node.getChild(1);

        // Generate array object and index expressions
        OllirExprResult arrayObj = exprVisitor.visit(arrayAccessNode.getChild(0));
        OllirExprResult indexExpr = exprVisitor.visit(arrayAccessNode.getChild(1));
        OllirExprResult value = exprVisitor.visit(valueExpr);

        // Define the result type for array elements (always i32 in this case)
        String resultType = ".i32";

        StringBuilder code = new StringBuilder();
        code.append(arrayObj.getComputation());
        code.append(indexExpr.getComputation());
        code.append(value.getComputation());

        // Generate the array assignment - CORRECT OLLIR SYNTAX FOR ARRAY ASSIGNMENT
        code.append(arrayObj.getCode())
                .append("[").append(indexExpr.getCode()).append("]")
                .append(resultType) // Type must appear here in OLLIR
                .append(" :=").append(resultType)
                .append(" ")
                .append(value.getCode())
                .append(END_STMT);

        return code.toString();
    }
}
