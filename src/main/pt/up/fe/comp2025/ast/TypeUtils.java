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

    public static Type newIntType() {
        return new Type("int", false);
    }
    public static Type newBoolType() {
        return new Type("boolean", false);
    }

    public static Type newStringType() {
        return new Type("string", false);
    }
    public static Type newVoidType() {
        return new Type("void", false);
    }


    public static Type convertType(JmmNode typeNode) {
        var name = typeNode.get("name");
        var isArray = false;

        if (name.endsWith("[]")) {
            isArray = true;
            name = name.substring(0, name.length() - 2); // Remover o "[]"
        }

        switch (name) {
            case "int":
                return new Type("int", isArray);
            case "boolean":
                return new Type("boolean", isArray);
            case "string":
                return new Type("string", isArray);
            case "void":
                return new Type("void", isArray);
            default:
                throw new IllegalArgumentException("Unsupported type: " + name);
        }
    }



    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public Type getExprType(JmmNode expr) {
        if (Kind.VAR_REF_EXPR.check(expr)) {
            String varName = expr.get("name");
            return table.getVarType(varName);
        }

        if (Kind.BINARY_EXPR.check(expr)) {
            JmmNode left = expr.getChild(0);
            JmmNode right = expr.getChild(1);
            Type leftType = getExprType(left);
            Type rightType = getExprType(right);

            if (leftType.getName().equals("int") && rightType.getName().equals("int")) {
                return new Type("int", false);
            }
        }

        if (Kind.INTEGER_LITERAL.check(expr)) {
            return new Type("int", false);
        }

        throw new IllegalArgumentException("Tipo de expressão desconhecido: " + expr.getKind());
    }




}
