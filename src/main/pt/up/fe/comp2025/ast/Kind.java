package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Enum that mirrors the nodes that are supported by the AST.
 *
 * This enum allows to handle nodes in a safer and more flexible way that using strings with the names of the nodes.
 */
public enum Kind {
    PROGRAM,
    CLASS_DECL,
    VAR_DECL,
    TYPE,
    METHOD_DECL,
    PARAM,
    PARAMETERS,
    STMT,
    EXPR,
    BINARY_EXPR,

    VAR_REF_EXPR,
    FIELD_DECL,
    IMPORT_DECL,
    LENGTH_EXPR,


    //Expressions
    PAREN_EXPR,
    UNARY_EXPR,
    FUNC_EXPR,
    MEMBER_EXPR,
    BOOLEAN_LITERAL,
    ARRAY_ACCESS_EXPR,
    ARRAY_EXPR,
    NEW_EXPR,
    THIS_EXPR,
    NEW_ARRAY_EXPR,
    INTEGER_LITERAL,


    //Statements
    BLOCK_STMT,
    EXPR_STMT,
    IF_STMT,
    WHILE_STMT,
    ASSIGN_STMT,
    ARRAY_ASSIGN_STMT,
    RETURN_STMT;


    public static final Set<Kind> STATEMENTS = Set.of(
            BLOCK_STMT,
            EXPR_STMT,
            IF_STMT,
            WHILE_STMT,
            ASSIGN_STMT,
            ARRAY_ASSIGN_STMT,
            RETURN_STMT
    );


    private static final Set<Kind> EXPRESSIONS = Set.of(
            BINARY_EXPR,
            INTEGER_LITERAL,
            VAR_REF_EXPR,
            PAREN_EXPR,
            UNARY_EXPR,
            FUNC_EXPR,
            MEMBER_EXPR,
            BOOLEAN_LITERAL,
            ARRAY_EXPR,
            NEW_EXPR,
            THIS_EXPR,
            NEW_ARRAY_EXPR
    );


    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public static List<String> toNodeName(Kind firstKind, Kind... otherKinds) {
        var nodeNames = new ArrayList<String>();
        nodeNames.add(firstKind.getNodeName());

        for (Kind kind : otherKinds) {
            nodeNames.add(kind.getNodeName());
        }

        return nodeNames;
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.isInstance(this);
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
