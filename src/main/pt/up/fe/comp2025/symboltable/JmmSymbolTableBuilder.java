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
        var imports = buildImports(root.getChildren("ImportDeclaration"));
        var index = imports.isEmpty() ? 0 : imports.size();
        if (index >= root.getNumChildren()) {
            reports.add(newError(root, "Expected at least one class declaration"));
            throw new RuntimeException("Expected at least one class declaration");
        }
        var classDecl = root.getChild(index);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);


        String className = classDecl.get("name");
        var superClass = classDecl.hasAttribute("superClass") ? classDecl.get("superClass") : "";
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);


        return new JmmSymbolTable(
                className,
                methods,
                returnTypes,
                params,
                locals,
                imports,
                superClass,
                fields
        );
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        var methods = classDecl.getChildren("Method");

        methods.forEach(method -> map.put(
                method.get("name"),
                parseType(method.getObject("typename", JmmNode.class))));

        return map;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren("Method")) {
            var name = method.get("name");
            var params = method.getChildren(PARAM).stream()
                    .map(param -> new Symbol(getType(param.get("typename")), param.get("name")))
                    .toList();

            map.put(name, params);
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> localsMap = new HashMap<>();

        for (var method : classDecl.getChildren("Method")) {
            var methodName = method.get("name");
            var localSymbols = method.getChildren("Variable").stream()
                    .map(JmmSymbolTableBuilder::newSymbol)
                    .toList();
            localsMap.put(methodName, localSymbols);
        }

        return localsMap;
    }

    private static Symbol newSymbol(JmmNode node) {
        var typeNode = node.getObject("typename", JmmNode.class);
        var type = new Type(typeNode.get("name"), "true".equals(typeNode.get("isArray")));
        var name = node.get("name");
        return new Symbol(type, name);
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren("Method").stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private List<String> buildImports(List<JmmNode> importList) {
        return importList.stream().map(node -> node.get("ID")).toList();
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren("Variable").stream()
                .map(this::createSymbol)
                .toList();
    }

    private Symbol createSymbol(JmmNode node) {
        return new Symbol(
                parseType(node.getObject("typename", JmmNode.class)),
                node.get("name")
        );
    }
    private static Type parseType(JmmNode node) {
        return new Type(
                node.get("name"),
                node.get("isArray").equals("true")
        );
    }


    private Type getType(String type) {
        switch (type) {
            case "int":
                return TypeUtils.newIntType();

            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

}
