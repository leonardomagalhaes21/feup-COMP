package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp2025.ast.Kind;

import java.util.List;
import java.util.ArrayList;
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
        addVisit(METHOD_DECL, this::visitMethod);
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(PARAMETERS, this::visitParameters);

        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(FIELD_ASSIGN_STMT, this::visitFieldAssignStmt);
        addVisit(ARRAY_ACCESS_EXPR, this::visitComplexArrayAccess);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayElement);
        addVisit(EXPR_STMT, this::visitExprStmt);

        addVisit(IMPORT_DECL, this::visitImport);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitMethod(JmmNode node, Void u) {
        StringBuilder code = new StringBuilder(".method ");

        // Add modifiers
        if (node.getOptional("isPublic").map(Boolean::parseBoolean).orElse(false)) {
            code.append("public ");
        } else {
            code.append("private ");
        }

        if (node.getOptional("isStatic").map(Boolean::parseBoolean).orElse(false)) {
            code.append("static ");
        }

        // Add method name
        code.append(node.get("name"));

        // Add parameters
        JmmNode paramsNode = node.getChildren().stream()
                .filter(child -> child.getKind().equals(PARAMETERS.getNodeName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No parameters node found in method declaration"));

        String params = paramsNode.getChildren().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));

        code.append("(").append(params).append(")");

        // Add return type
        JmmNode typeNode = node.getChild(0);
        code.append(ollirTypes.toOllirType(typeNode)).append(" {\n");

        // Add method statements - find statements among children
        node.getChildren().stream()
                .filter(child -> STATEMENTS.contains(Kind.fromString(child.getKind())))
                .forEach(stmt -> code.append("    ").append(visit(stmt)).append("\n"));

        // If return is void and no return statement found, add default return
        Type returnType = TypeUtils.convertType(typeNode);
        boolean hasReturnStmt = node.getChildren().stream()
                .anyMatch(child -> child.getKind().equals(RETURN_STMT.getNodeName()));

        if (returnType.getName().equals("void") && !hasReturnStmt) {
            code.append("    ret.V;\n");
        }

        code.append("}\n");

        return code.toString();
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Get condition expression
        JmmNode condExpr = node.getChild(0);
        OllirExprResult condition = exprVisitor.visit(condExpr);

        // Generate unique labels for then/else/end blocks
        String thenLabel = OptUtils.getLabel("then");
        String elseLabel = OptUtils.getLabel("else");
        String endLabel = OptUtils.getLabel("endif");

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
            code.append("goto ").append(endLabel).append(END_STMT);
        }

        // Add end label
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Generate unique labels for condition/loop/end
        String condLabel = OptUtils.getLabel("whileCond");
        String loopLabel = OptUtils.getLabel("whileBody");
        String endLabel = OptUtils.getLabel("whileEnd");

        // Jump to condition evaluation
        code.append("goto ").append(condLabel).append(END_STMT);

        // Add loop body label
        code.append(loopLabel).append(":").append(NL);
        code.append(visit(node.getChild(1)));
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

        // Add end label
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {
        List<String> name = node.getObjectAsList("name", String.class);
        return "import " + String.join(".", name) + END_STMT;
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
        Type retType = methodNode != null ? TypeUtils.convertType(methodNode.getChild(0))
                : TypeUtils.newType(TypeName.INT, false);

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

    private String visitAssignStmt(JmmNode node, Void unused) {
        JmmNode left = node.getChild(0);
        JmmNode right = node.getChild(1);
        StringBuilder code = new StringBuilder();

        if (left.getKind().equals(ARRAY_ACCESS_EXPR.getNodeName())) {
            // Handle array assignment separately
            return visitArrayElement(node, unused);
        }

        String name = left.get("name");
        Type type = types.getExprType(left);
        String ollirType = ollirTypes.toOllirType(type);

        if (right.getKind().equals(BINARY_EXPR.getNodeName())
                && (right.get("op").equals("+")
                || right.get("op").equals("-")
                || right.get("op").equals("*")
                || right.get("op").equals("/")
                || right.get("op").equals("&&")
                || right.get("op").equals("||"))
                && right.getChild(0).getKind().equals(VAR_REF_EXPR.getNodeName())
                && right.getChild(0).get("name").equals(name)
                && right.getChild(1).getKind().equals(INTEGER_LITERAL.getNodeName())) {

            code.append(name).append(ollirType)
                    .append(" :=").append(ollirType)
                    .append(" ")
                    .append(name).append(ollirType)
                    .append(" ").append(right.get("op")).append(ollirType)
                    .append(" ")
                    .append(right.getChild(1).get("value")).append(ollirType)
                    .append(END_STMT);

            return code.toString();
        }

        var rhs = exprVisitor.visit(right);
        code.append(rhs.getComputation());

        // Check if this is a field assignment or a local variable
        // First check if we're in a method
        JmmNode methodNode = node.getAncestor(METHOD_DECL).orElse(null);
        boolean isLocalVar = false;

        if (methodNode != null) {
            String methodName = methodNode.get("name");
            // Check if it's a local variable in this method
            isLocalVar = table.getLocalVariables(methodName).stream()
                    .anyMatch(var -> var.getName().equals(name));

            // Also check if it's a parameter (which is also a local variable)
            if (!isLocalVar) {
                isLocalVar = table.getParameters(methodName).stream()
                        .anyMatch(param -> param.getName().equals(name));
            }
        }

        // Check if this is a field assignment - only if it's not a local var
        boolean isField = !isLocalVar && table.getFields().stream()
                .anyMatch(field -> field.getName().equals(name));

        if (isField) {
            // For fields, use putfield instruction
            code.append("putfield(this, ").append(name).append(ollirType)
                    .append(", ").append(rhs.getCode()).append(")")
                    .append(".V") // Add proper void return type for putfield
                    .append(END_STMT);
        } else {
            // For local variables, use normal assignment
            code.append(name).append(ollirType)
                    .append(" :=").append(ollirType)
                    .append(" ")
                    .append(rhs.getCode())
                    .append(END_STMT);
        }

        return code.toString();
    }

    private String visitFieldAssignStmt(JmmNode node, Void unused) {
        JmmNode field = node.getChild(0);
        JmmNode assignee = node.getChild(1);
        var expr = exprVisitor.visit(assignee);

        Type fieldType = null;
        for (var f : table.getFields()) {
            if (f.getName().equals(field.get("name"))) {
                fieldType = f.getType();
            }
        }

        assert fieldType != null;
        String type = ollirTypes.toOllirType(fieldType);

        return expr.getComputation() + "putfield(this, " + field.get("name") + type + ", " + expr.getCode() + ")" + ".V"
                + END_STMT;
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
                .append(resultType) // Add the type specification before END_STMT
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

        // Generate the array assignment - use correct OLLIR syntax
        code.append(arrayObj.getCode())
                .append("[").append(indexExpr.getCode()).append("]")
                .append(resultType)
                .append(" :=").append(resultType)
                .append(" ")
                .append(value.getCode())
                .append(END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        JmmNode exprNode = node.getChild(0);

        // Handle function calls (both static and instance methods)
        if (exprNode.getKind().equals(FUNC_EXPR.getNodeName()) ||
                exprNode.getKind().equals(METHOD_CALL_EXPR.getNodeName())) {

            String methodName = exprNode.get("methodname");
            StringBuilder code = new StringBuilder();
            boolean isStatic = false;
            String className = null;
            int startArgIndex = 0;

            // Process the target object/class
            if (exprNode.getKind().equals(FUNC_EXPR.getNodeName())) {
                JmmNode objNode = exprNode.getChild(0);

                // Check if this is a static method call to an imported class
                if (objNode.getKind().equals(VAR_REF_EXPR.getNodeName())) {
                    className = objNode.get("name");

                    // Check if the class is imported
                    for (String imp : table.getImports()) {
                        if (imp.equals(className) || imp.endsWith("." + className)) {
                            isStatic = true;
                            startArgIndex = 1; // Skip the class name argument
                            break;
                        }
                    }
                }
            }

            // Process arguments
            List<String> argCodes = new ArrayList<>();
            for (int i = startArgIndex; i < exprNode.getNumChildren(); i++) {
                OllirExprResult arg = exprVisitor.visit(exprNode.getChild(i));
                code.append(arg.getComputation());
                argCodes.add(arg.getCode());
            }

            // Get the return type of the method
            Type returnType = types.getExprType(exprNode);
            String returnTypeStr = ollirTypes.toOllirType(returnType);

            // Build the method call
            if (isStatic) {
                // Static method call to imported class
                code.append("invokestatic(")
                        .append(className)
                        .append(", \"")
                        .append(methodName)
                        .append("\"");
            } else {
                // Instance method call
                if (argCodes.isEmpty()) {
                    // No arguments provided
                    return code.toString();
                }

                // Check if this is a call to a static method in the current class
                final List<String> finalArgCodes = argCodes;
                boolean isStaticLocalMethod = table.getMethods().stream()
                        .anyMatch(m -> {
                            // Get method signature
                            String methodSig = m + "(" + finalArgCodes.size() + ")";
                            // Check if method exists and is static
                            return m.equals(methodName) &&
                                    table.getParameters(methodSig) != null &&
                                    table.getParameters(methodSig).size() == finalArgCodes.size();
                        });

                if (isStaticLocalMethod) {
                    // Static method call in current class
                    code.append("invokestatic(")
                            .append(table.getClassName())
                            .append(", \"")
                            .append(methodName)
                            .append("\"");
                } else {
                    // Instance method call (either local or inherited)
                    code.append("invokevirtual(")
                            .append(argCodes.get(0)) // First argument is the object
                            .append(", \"")
                            .append(methodName)
                            .append("\"");

                    // Remove the object from arguments list
                    argCodes = argCodes.subList(1, argCodes.size());
                }
            }

            // Add remaining arguments if any
            if (!argCodes.isEmpty()) {
                code.append(", ")
                        .append(String.join(", ", argCodes));
            }

            code.append(")").append(returnTypeStr)
                    .append(END_STMT);

            return code.toString();
        }

        // Default behavior for other expressions
        return exprVisitor.visit(exprNode).getComputation();
    }
}
