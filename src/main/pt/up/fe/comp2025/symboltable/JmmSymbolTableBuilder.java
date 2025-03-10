package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmSymbolTableBuilder {

    private List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }

    public JmmSymbolTable build(JmmNode root) {

        reports = new ArrayList<>();

        var classDecl = root.getChild(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var imports = buildImports(classDecl);
        var fields = buildFields(classDecl);
        var superClassName = classDecl.get("superclass");

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, imports, superClassName, fields);
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var returnType = getType(method.get("returnType"));
            map.put(name, returnType);
        }

        return map;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var params = method.getChildren(PARAM).stream()
                    .map(param -> new Symbol(getType(param.get("type")), param.get("name")))
                    .toList();

            map.put(name, params);
        }

        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        var map = new HashMap<String, List<Symbol>>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var locals = method.getChildren(VAR_DECL).stream()
                    .map(varDecl -> new Symbol(getType(varDecl.get("type")), varDecl.get("name")))
                    .toList();

            map.put(name, locals);
        }

        return map;
    }

    private List<String> buildMethods(JmmNode classDecl) {

        var methods = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();

        return methods;
    }

    private List<String> buildImports(JmmNode classDecl) {
        List<String> imports = new ArrayList<>();
        for (var imp : classDecl.getChildren(IMPORT)) {
            imports.add(imp.get("name"));
        }
        return imports;
    }



    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();
        for (var field : classDecl.getChildren(FIELD_DECL)) {
            var fieldName = field.get("name");
            var fieldType = getType(field.get("type"));
            fields.add(new Symbol(fieldType, fieldName));
        }
        return fields;
    }


    private Type getType(String type) {
        switch (type) {
            case "int":
                return TypeUtils.newIntType();
            case "boolean":
                return TypeUtils.newBoolType();
            case "string":
                return TypeUtils.newStringType();
            case "void":
                return TypeUtils.newVoidType();
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

}
