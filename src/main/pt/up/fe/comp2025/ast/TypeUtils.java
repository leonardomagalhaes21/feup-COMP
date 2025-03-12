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
        return new Type("bool", false);
    }
    public static Type newVoidType() {
        return new Type("void", false);
    }

    public static String getIntType() {
        return "int";
    }
    public static String getBoolType() {
        return "bool";
    }
    public static String getVoidType() {
        return "void";
    }
    public static Type convertType(JmmNode typeNode) {
        var name = typeNode.get("name");
        var isArrayString = typeNode.get("isArray");
        var isArray = isArrayString != null && isArrayString.equals("true");
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

        return new Type("int", false);
    }


}
