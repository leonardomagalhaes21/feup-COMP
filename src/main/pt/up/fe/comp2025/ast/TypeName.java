package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.Type;

public enum TypeName {
    INT("int"),
    BOOLEAN("boolean"),
    VOID("void"),
    ANY("any");

    private final String name;

    TypeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Type newType(TypeName typeName, boolean isArray) {
        return new Type(typeName.getName(), isArray);
    }

}