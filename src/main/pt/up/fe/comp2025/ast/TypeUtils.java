package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

/**
 * Utility methods regarding types.
 */
public class TypeUtils {


    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newType(TypeName typeName, boolean isArray) {
        return new Type(typeName.getName(), isArray);
    }


    public static Type convertType(JmmNode typeNode) {

        // TODO: When you support new types, this must be updated
        var name = typeNode.get("name");
        var isArray = false;

        return new Type(name, isArray);
    }


    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public Type getExprType(JmmNode expr) {
        var kind = Kind.fromString(expr.getKind());

        return switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr);
            case FUNC_EXPR -> getFuncExprType(expr);
            case NEW_EXPR -> getNewExprType(expr);
            case ARRAY_EXPR, NEW_ARRAY_EXPR -> newType(TypeName.INT, true);   // Arrays are of type int[]
            case INTEGER_LITERAL, ARRAY_ACCESS_EXPR -> newType(TypeName.INT, false);
            case BOOLEAN_LITERAL -> newType(TypeName.BOOLEAN, false);
            default -> throw new UnsupportedOperationException("Unknown expression kind: " + kind);
        };
    }

    private Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "*", "/" -> newType(TypeName.INT, false);
            case "<", "<=", ">", ">=", "==", "!=", "&&", "||" -> newType(TypeName.BOOLEAN, false);
            default -> throw new RuntimeException("Unknown operator: " + operator);
        };
    }

    private Type getVarExprType(JmmNode varExpr) {
        var currentMethod = varExpr.getAncestor(Kind.METHOD_DECL).orElseThrow().get("name");
        var fields = table.getFields();
        var params = table.getParameters(currentMethod);
        var locals = table.getLocalVariables(currentMethod);

        var varName = varExpr.get("name");

        for (var field: fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        for (var param: params) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        for (var local: locals) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        return newType(TypeName.ANY, false);
    }

    private Type getFuncExprType(JmmNode funcExpr) {
        var funcName = funcExpr.get("methodname");
        var methods = table.getMethods();

        for (var method: methods) {
            if (method.equals(funcName)) {
                return table.getReturnType(method);
            }
        }

        return newType(TypeName.ANY, false);
    }

    private Type getNewExprType(JmmNode newExpr) {
        var className = newExpr.get("class");

        return new Type(className, false);
    }

    public boolean isAssignable(Type left, Type right) {
        if (left == null || right == null) return false;
        if (left.isArray() || right.isArray()) {
            return left.isArray() && right.isArray() && left.getName().equals(right.getName());
        }
        return left.equals(right) || left.getName().equals("any") || right.getName().equals("any");
    }
}
