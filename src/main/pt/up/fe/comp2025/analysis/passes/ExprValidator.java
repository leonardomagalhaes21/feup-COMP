package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Objects;

public class ExprValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
        addVisit(Kind.MEMBER_EXPR, this::visitMemberExpr);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpr);
        addVisit(Kind.NEW_EXPR, this::visitNewExpr);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(Kind.PAREN_EXPR, this::visitParenExpr);
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.EXPR, this::visitExpr);
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

        // Special case for length method on arrays
        if ("length".equals(methodName) && funcExpr.getNumChildren() > 0) {
            TypeUtils typeUtils = new TypeUtils(table);
            var caller = funcExpr.getChildren().getFirst();
            var callerType = typeUtils.getExprType(caller);

            if (!callerType.isArray()) {
                var message = "The 'length' method can only be called on arrays.";
                addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
            }
            return null;
        }

        // Check if the method exists in the current class
        if (table.getMethods().contains(methodName)) {
            // Method exists in this class, verify argument types if any
            if (funcExpr.getNumChildren() > 1) { // First child is caller, rest are arguments
                var parameters = table.getParameters(methodName);
                var arguments = funcExpr.getChildren().subList(1, funcExpr.getNumChildren());
                boolean isVarargs = parameters.size() > 0 &&
                        parameters.get(parameters.size() - 1).getType().isArray();

                if (!isVarargs && arguments.size() != parameters.size()) {
                    var message = "Method '" + methodName + "' expects " + parameters.size() +
                            " arguments, but " + arguments.size() + " were provided.";
                    addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
                    return null;
                }

                TypeUtils typeUtils = new TypeUtils(table);
                for (int i = 0; i < parameters.size(); i++) {
                    var paramType = parameters.get(i).getType();
                    var argType = typeUtils.getExprType(arguments.get(i));

                    if (!isVarargs && !typeUtils.isAssignable(paramType, argType)) {
                        var message = "Incompatible argument type for parameter '" + parameters.get(i).getName() +
                                "'. Expected '" + paramType.getName() + (paramType.isArray() ? "[]" : "") +
                                "', but got '" + argType.getName() + (argType.isArray() ? "[]" : "") + "'.";
                        addReport(Report.newError(Stage.SEMANTIC, arguments.get(i).getLine(),
                                arguments.get(i).getColumn(), message, null));
                    }
                }
            }
            return null;
        }

        // If there's no caller (like a.foo()), it's an invalid call
        if (funcExpr.getNumChildren() == 0) {
            var message = "Method '" + methodName + "' is not defined.";
            addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
            return null;
        }

        // There's a caller, check if method might exist in caller's type
        TypeUtils typeUtils = new TypeUtils(table);
        var caller = funcExpr.getChildren().getFirst();
        var callerType = typeUtils.getExprType(caller);

        // If caller is "this", check class methods
        if (Kind.THIS_EXPR.check(caller) && table.getMethods().contains(methodName)) {
            return null;
        }

        // Check if caller type is the current class
        if (callerType.getName().equals(table.getClassName()) && table.getMethods().contains(methodName)) {
            return null;
        }

        // Check if method might be from superclass
        var superClass = table.getSuper();
        if (superClass != null &&
                (callerType.getName().equals(superClass) || callerType.getName().equals(table.getClassName()))) {
            return null; // Assume method exists in superclass
        }

        // Check if method might be from imported class
        var imports = table.getImports();
        for (String importName : imports) {
            if (importName.endsWith("." + callerType.getName()) || importName.equals(callerType.getName())) {
                return null; // Assume method exists in imported class
            }
        }

        var message = "Method '" + methodName + "' is not defined in type '" + callerType.getName() + "'.";
        addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
        return null;
    }

    private Void visitMemberExpr(JmmNode memberExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode objectExpr = memberExpr.getChildren().getFirst();
        String memberName = memberExpr.get("member");

        // Obter o tipo da expressão do objeto
        var objectType = typeUtils.getExprType(objectExpr);

        // Verificar se o membro existe na classe ou suas superclasses
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
        if(imports.contains(table.getSuper())){
            return null;
        }

        if (!Objects.equals(table.getSuper(), "") && !imports.contains(table.getSuper())) {
            var message = "Super '" + table.getSuper() + "' is not imported." + "Imports: " + imports;
            addReport(Report.newError(Stage.SEMANTIC, newExpr.getLine(), newExpr.getColumn(), message, null));
            return null;
        }





        return null;
    }
    private Void visitNewArrayExpr(JmmNode newArrayExpr, SymbolTable table){
        return null;
    }

    private Void visitParenExpr(JmmNode parenExpr, SymbolTable table) {
        // Implement validation logic for parenthesis expressions
        return null;
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode expr = unaryExpr.getChildren().getFirst();

        var exprType = typeUtils.getExprType(expr);

        // Verificar se o tipo da expressão é compatível com a operação unária
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
        // Implement validation logic for 'this' expressions
        return null;
    }
    private Void visitExpr(JmmNode expr, SymbolTable table) {
        return null;
    }
}


