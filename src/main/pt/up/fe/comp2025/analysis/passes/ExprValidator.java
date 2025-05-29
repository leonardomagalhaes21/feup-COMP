package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Objects;

public class ExprValidator extends AnalysisVisitor {

    private JmmNode currentClass;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
        addVisit(Kind.MEMBER_EXPR, this::visitMemberExpr);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpr);
        addVisit(Kind.NEW_EXPR, this::visitNewExpr);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(Kind.PAREN_EXPR, this::visitParenExpr);
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.EXPR, this::visitExpr);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        currentClass = classDecl;

        return null;
    }

    private Void visitArrayLengthExpr(JmmNode arrayLengthExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var array = arrayLengthExpr.getChildren().getFirst();
        var arrayType = typeUtils.getExprType(array);

        if (!arrayType.isArray()) {
            var message = "The 'length' method can only be called on arrays.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayLengthExpr.getLine(),
                    arrayLengthExpr.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitArrayExpr(JmmNode arrayExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var arrayType = typeUtils.getExprType(arrayExpr);
        for (var child : arrayExpr.getChildren()) {
            var childType = typeUtils.getExprType(child);
            if (!arrayType.getName().equals(childType.getName())) {
                var message = "Array elements must have the same type, but found '" + arrayType.getName() + "' and '" + childType.getName() + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        arrayExpr.getLine(),
                        arrayExpr.getColumn(),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode arrayExpr = arrayAccessExpr.getChildren().getFirst();
        JmmNode indexExpr = arrayAccessExpr.getChildren().get(1);

        var arrayType = typeUtils.getExprType(arrayExpr);
        var indexType = typeUtils.getExprType(indexExpr);

        if (!arrayType.isArray()) {
            var message = "Array access must be performed on an array, but found '" + arrayType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccessExpr.getLine(),
                    arrayAccessExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        if (!indexType.getName().equals(TypeName.INT.getName())) {
            var message = "Array index must be of type 'int', but found '" + indexType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccessExpr.getLine(),
                    arrayAccessExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        var methodName = funcExpr.get("methodname");
        var callArgs = funcExpr.getChildren().subList(1, funcExpr.getNumChildren());

        if (methodName.equals("length")) {
            return visitArrayLengthExpr(funcExpr, table);
        }

        if (!table.getMethods().contains(methodName)) {
            return null;
        }

        List<JmmNode> params = TypeUtils.getMethodParams(methodName, currentClass);
        List<Symbol> parameters = table.getParameters(methodName);

        boolean isVarargs = false;
        Type varargsType = null;
        int fixedParamCount = parameters.size();

        if (!params.isEmpty()) {
            JmmNode lastParam = params.get(params.size() - 1);
            if (!lastParam.getChildren().isEmpty() && lastParam.getChild(0).hasAttribute("isVarargs")) {
                isVarargs = lastParam.getChild(0).get("isVarargs").equals("true");
                varargsType = parameters.get(parameters.size() - 1).getType();
                if (isVarargs) {
                    fixedParamCount = parameters.size() - 1;
                }
            }
        }

        if (isVarargs) {
            if (callArgs.size() < fixedParamCount) {
                var message = "Method '" + methodName + "' expects at least " + fixedParamCount + " arguments, but " + callArgs.size() + " were provided.";
                addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
                return null;
            }
        } else {
            if (callArgs.size() != parameters.size()) {
                var message = "Method '" + methodName + "' expects " + parameters.size() +
                        " arguments, but " + callArgs.size() + " were provided.";
                addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
                return null;
            }
        }

        TypeUtils typeUtils = new TypeUtils(table);
        // Check fixed parameters
        for (int i = 0; i < fixedParamCount; i++) {
            var paramType = parameters.get(i).getType();
            var argType = typeUtils.getExprType(callArgs.get(i));
            if (!typeUtils.isAssignable(paramType, argType)) {
                var message = "Incompatible argument type for parameter '" + parameters.get(i).getName() +
                        "'. Expected '" + paramType.getName() + (paramType.isArray() ? "[]" : "") +
                        "', but got '" + argType.getName() + (argType.isArray() ? "[]" : "") + "'.";
                addReport(Report.newError(Stage.SEMANTIC, callArgs.get(i).getLine(),
                        callArgs.get(i).getColumn(), message, null));
            }
        }
        // Check varargs arguments
        if (isVarargs) {
            Type elementType = new Type(varargsType.getName(), false);
            for (int i = fixedParamCount; i < callArgs.size(); i++) {
                var argType = typeUtils.getExprType(callArgs.get(i));
                if (!(typeUtils.isAssignable(elementType, argType) ||
                        (argType.isArray() && typeUtils.isAssignable(varargsType, argType)))) {
                    var message = "Incompatible argument type for varargs parameter '" + parameters.get(parameters.size() - 1).getName() +
                            "'. Expected '" + elementType.getName() +
                            "', but got '" + argType.getName() + (argType.isArray() ? "[]" : "") + "'.";
                    addReport(Report.newError(Stage.SEMANTIC, callArgs.get(i).getLine(),
                            callArgs.get(i).getColumn(), message, null));
                }
            }
        }
        return null;
    }

    private Void visitMemberExpr(JmmNode memberExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode objectExpr = memberExpr.getChildren().getFirst();
        String memberName = memberExpr.get("member");

        // Get the type of the object expression
        var objectType = typeUtils.getExprType(objectExpr);

        // Verify if the member exists in the object type
        if (!table.getMethods().contains(memberName) && table.getFields().stream().noneMatch(field -> field.getName().equals(memberName))) {
            var message = "Member '" + memberName + "' does not exist in class '" + objectType.getName() + "'.";
            addReport(Report.newError(Stage.SEMANTIC, memberExpr.getLine(), memberExpr.getColumn(), message, null));
            return null;
        }

        return null;
    }

    private Void visitNewExpr(JmmNode newExpr, SymbolTable table) {
        if (!newExpr.hasAttribute("classname")) {
            var message = "Node NewExpr does not contain attribute 'classname'.";
            addReport(Report.newError(Stage.SEMANTIC, newExpr.getLine(), newExpr.getColumn(), message, null));
            return null;
        }

        var imports = table.getImports();
        if (imports.contains(table.getSuper())) {
            return null;
        }

        if (!Objects.equals(table.getSuper(), "") && !imports.contains(table.getSuper())) {
            var message = "Super '" + table.getSuper() + "' is not imported." + "Imports: " + imports;
            addReport(Report.newError(Stage.SEMANTIC, newExpr.getLine(), newExpr.getColumn(), message, null));
            return null;
        }


        return null;
    }

    private Void visitNewArrayExpr(JmmNode newArrayExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        // Check if the size expression is of type int
        if (newArrayExpr.getNumChildren() > 0) {
            JmmNode sizeExpr = newArrayExpr.getChildren().getFirst();
            Type sizeType = typeUtils.getExprType(sizeExpr);

            if (!sizeType.getName().equals(TypeName.INT.getName())) {
                var message = "Array size expression must be of type 'int', but found '" + sizeType.getName() + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        sizeExpr.getLine(),
                        sizeExpr.getColumn(),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    private Void visitParenExpr(JmmNode parenExpr, SymbolTable table) {

        // If the parenthesized expression has no children, that's an error
        if (parenExpr.getNumChildren() == 0) {
            var message = "Empty parenthesized expression.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    parenExpr.getLine(),
                    parenExpr.getColumn(),
                    message,
                    null)
            );
        } else if (parenExpr.getNumChildren() > 1) {
            // If there's more than one child, that's also an error
            var message = "Multiple expressions inside parentheses.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    parenExpr.getLine(),
                    parenExpr.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode expr = unaryExpr.getChildren().getFirst();

        var exprType = typeUtils.getExprType(expr);

        // Verify if the expression is of type 'int' or 'boolean'
        if (!exprType.getName().equals(TypeName.INT.getName()) && !exprType.getName().equals(TypeName.BOOLEAN.getName())) {
            var message = "Unary expression must be of type 'int' or 'boolean', but found '" + exprType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    unaryExpr.getLine(),
                    unaryExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitThisExpr(JmmNode thisExpr, SymbolTable table) {
        // Check if 'this' is being used in a static context
        boolean isInStaticMethod = thisExpr.getAncestor(Kind.METHOD_DECL)
                .map(method -> method.getOptional("isStatic")
                        .map(Boolean::parseBoolean)
                        .orElse(false))
                .orElse(false);

        if (isInStaticMethod) {
            var message = "Cannot use 'this' in a static context.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    thisExpr.getLine(),
                    thisExpr.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitExpr(JmmNode expr, SymbolTable table) {
        // For generic expressions, validate that the expression type is valid
        TypeUtils typeUtils = new TypeUtils(table);

        try {
            Type exprType = typeUtils.getExprType(expr);

            // If the type is unknown or invalid, report an error
            if (exprType.getName().equals(TypeName.ANY.getName()) &&
                    !expr.getChildren().isEmpty()) {
                var message = "Unable to determine type of expression.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        expr.getLine(),
                        expr.getColumn(),
                        message,
                        null)
                );
            }
        } catch (Exception e) {
            // If an exception occurs during type determination, report it
            var message = "Error analyzing expression: " + e.getMessage();
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    expr.getLine(),
                    expr.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitMethodCallExpr(JmmNode methodCallExpr, SymbolTable table) {
        String methodName = methodCallExpr.get("methodname");

        if (table.getMethods().contains(methodName)) {
            List<Symbol> parameters = table.getParameters(methodName);
            List<JmmNode> args = methodCallExpr.getChildren();
            if (args.size() != parameters.size()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodCallExpr.getLine(),
                        methodCallExpr.getColumn(),
                        "Method '" + methodName + "' expects " + parameters.size() + " arguments, but " + args.size() + " were provided.",
                        null));
            } else {
                TypeUtils typeUtils = new TypeUtils(table);
                for (int i = 0; i < args.size(); i++) {
                    Type argType = typeUtils.getExprType(args.get(i));
                    Type paramType = parameters.get(i).getType();
                    if (!typeUtils.isAssignable(paramType, argType)) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                methodCallExpr.getLine(),
                                methodCallExpr.getColumn(),
                                "Incorrect argument type for parameter " + (i + 1) + " in method '" + methodName + "'.",
                                null));
                    }

                }
            }
        }
        else {
            var message = "Method '" + methodName + "' is not defined.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    methodCallExpr.getLine(),
                    methodCallExpr.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }
}