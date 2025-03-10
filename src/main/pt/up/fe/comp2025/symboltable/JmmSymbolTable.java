package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class JmmSymbolTable extends AJmmSymbolTable {

    private final String className;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;
    private final List<String> imports;
    private final String superClass;
    private final List<Symbol> fields;


    // Atualizar o construtor para aceitar imports, superClass e fields
    public JmmSymbolTable(String className,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals,
                          List<String> imports,
                          String superClass,
                          List<Symbol> fields) {

        this.className = className;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.imports = imports;
        this.superClass = superClass;
        this.fields = fields;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.getOrDefault(methodSignature, TypeUtils.newIntType());
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.get(methodSignature);
    }
    public Type getVarType(String varName) {
        // Verificar se é um campo da classe
        for (var field : fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        // Verificar se é um parâmetro ou variável local em algum método
        for (var method : methods) {
            var localsList = locals.get(method);
            if (localsList != null) {
                for (var symbol : localsList) {
                    if (symbol.getName().equals(varName)) {
                        return symbol.getType();
                    }
                }
            }

            var paramsList = params.get(method);
            if (paramsList != null) {
                for (var symbol : paramsList) {
                    if (symbol.getName().equals(varName)) {
                        return symbol.getType();
                    }
                }
            }
        }

        // Se a variável não for encontrada
        throw new IllegalArgumentException("Variable " + varName + " not found.");
    }



    @Override
    public String toString() {
        return print();
    }

}
