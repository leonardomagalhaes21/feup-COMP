package pt.up.fe.comp2025.ast;

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
}