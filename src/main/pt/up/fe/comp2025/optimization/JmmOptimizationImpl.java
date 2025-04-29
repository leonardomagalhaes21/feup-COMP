package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.CompilerConfig;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.*;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmOptimizationImpl implements JmmOptimization {

    ArrayList<Kind> assignments;

    public JmmOptimizationImpl() {
        assignments = new ArrayList<>();
        assignments.add(ASSIGN_STMT);
        assignments.add(ARRAY_ASSIGN_STMT);
        assignments.add(FIELD_ASSIGN_STMT);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        var config = ollirResult.getConfig();
        int n = CompilerConfig.getRegisterAllocation(config);

        if (n == -1) {
            return ollirResult;
        } else {
            optimizeRegisters(ollirResult);
            int mx = 0;

            for (var method : ollirResult.getOllirClass().getMethods()) {
                var VT = method.getVarTable();
                for (var reg : VT.keySet()) {
                    int val = VT.get(reg).getVirtualReg();
                    mx = Math.max(mx, val);
                }
            }

            if (mx + 1 > n) {
                var reports = ollirResult.getReports();
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.OPTIMIZATION,
                        -1,
                        -1,
                        "Need at least " + (mx + 1) + " registers.\n"
                ));
            }
        }
        return ollirResult;
    }

    private void optimizeRegisters(OllirResult OR) {
        OR.getOllirClass().buildCFGs();
        for (var method : OR.getOllirClass().getMethods()) {
            optMethodReg(method);
        }
    }

    private void optMethodReg(Method m) {
        var insts = m.getInstructions();
        var sz = insts.size();

        // Initialize sets for each instruction
        Set<String>[] IN = new Set[sz];
        Set<String>[] OUT = new Set[sz];
        Set<String>[] DEF = new Set[sz];

        for (int i = 0; i < sz; i++) {
            IN[i] = new TreeSet<>();
            OUT[i] = new TreeSet<>();
            DEF[i] = defs(m.getInstr(i));
        }

        // Calculate live-ins and live-outs using standard data flow analysis
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = sz - 1; i >= 0; i--) { // Process backwards for faster convergence
                var inst = m.getInstr(i);

                // Calculate new IN set: IN = USE ∪ (OUT - DEF)
                TreeSet<String> newIn = new TreeSet<>(uses(inst));
                TreeSet<String> outMinusDef = new TreeSet<>(OUT[i]);
                outMinusDef.removeAll(DEF[i]);
                newIn.addAll(outMinusDef);

                // Calculate new OUT set: OUT = ⋃(successor IN sets)
                TreeSet<String> newOut = new TreeSet<>();
                for (var suc : inst.getSuccessors()) {
                    int id = suc.getId() - 1;
                    if (id < 0 || id >= sz) continue;
                    newOut.addAll(IN[id]);
                }

                // Check if anything changed
                if (!equalSets(newIn, IN[i]) || !equalSets(newOut, OUT[i])) {
                    changed = true;
                    IN[i] = newIn;
                    OUT[i] = newOut;
                }
            }
        }

        // Build the interference graph
        Set<String> variables = new TreeSet<>();
        for (int i = 0; i < sz; i++) {
            variables.addAll(IN[i]);
            variables.addAll(OUT[i]);
            variables.addAll(DEF[i]);
        }

        // Remove 'this' and parameters from the variables that need register allocation
        variables.remove("this");
        for (var param : m.getParams()) {
            if (param instanceof org.specs.comp.ollir.Operand) {
                variables.remove(((org.specs.comp.ollir.Operand) param).getName());
            }
        }

        // Create the interference graph
        HashMap<String, Set<String>> graph = new HashMap<>();
        for (String var : variables) {
            graph.put(var, new TreeSet<>());
        }

        // Fill the interference graph - variables live at the same time interfere with each other
        for (int i = 0; i < sz; i++) {
            Set<String> liveOut = new TreeSet<>(OUT[i]);
            for (String defVar : DEF[i]) {
                for (String outVar : liveOut) {
                    if (!defVar.equals(outVar) && graph.containsKey(defVar) && graph.containsKey(outVar)) {
                        graph.get(defVar).add(outVar);
                        graph.get(outVar).add(defVar);
                    }
                }
            }
        }

        // Allocate registers using a simple greedy algorithm
        HashMap<String, Integer> colors = new HashMap<>();

        // Start register numbering after parameters
        int nextReg = 1;  // Start at 1 because register 0 is for 'this'
        if (m.isStaticMethod()) {
            nextReg = 0;  // Static methods don't have 'this'
        }
        nextReg += m.getParams().size();

        // Sort variables by degree (number of interferences)
        List<String> sortedVars = new ArrayList<>(variables);
        sortedVars.sort((a, b) -> {
            if (!graph.containsKey(a) || !graph.containsKey(b)) return 0;
            return Integer.compare(graph.get(b).size(), graph.get(a).size());
        });

        // Assign registers
        for (String var : sortedVars) {
            if (!graph.containsKey(var)) continue;

            // Find the smallest available register
            Set<Integer> usedColors = new TreeSet<>();
            for (String neighbor : graph.get(var)) {
                if (colors.containsKey(neighbor)) {
                    usedColors.add(colors.get(neighbor));
                }
            }

            int color = nextReg;
            while (usedColors.contains(color)) {
                color++;
            }

            colors.put(var, color);
        }

        // Update the variable table with assigned registers
        var VT = m.getVarTable();
        for (String var : variables) {
            if (colors.containsKey(var)) {
                VT.put(var, new org.specs.comp.ollir.Descriptor(colors.get(var)));
            }
        }
    }

    private boolean equalSets(Set<String> a, Set<String> b) {
        if (a.size() != b.size()) return false;
        for (var s : a) {
            if (!b.contains(s)) return false;
        }
        return true;
    }

    private Set<String> uses(Instruction inst) {
        Set<String> ret = new TreeSet<>();
        if (inst instanceof AssignInstruction ai) {
            if (ai.getRhs() instanceof BinaryOpInstruction bi) {
                if (bi.getLeftOperand() instanceof Operand lop) {
                    ret.add(lop.getName());
                }
                if (bi.getRightOperand() instanceof Operand rop) {
                    ret.add(rop.getName());
                }
            } else if (ai.getRhs() instanceof SingleOpInstruction si) {
                if (si.getSingleOperand() instanceof Operand op) {
                    ret.add(op.getName());
                }
            }
        } else if (inst instanceof GetFieldInstruction gf) {
            ret.add(gf.getField().getName());
        }
        return ret;
    }

    private Set<String> defs(Instruction inst) {
        Set<String> ret = new TreeSet<>();
        if (inst instanceof AssignInstruction ai) {
            ret.add(((Operand) ai.getDest()).getName());
        } else if (inst instanceof PutFieldInstruction pf) {
            ret.add(pf.getField().getName());
        }
        return ret;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        var config = semanticsResult.getConfig();
        var optimize = CompilerConfig.getOptimize(config);

        if (optimize) {
            optimizeConstantPropAndFold(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
        }
        inPlaceArrayForVarArgs(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
        return semanticsResult;
    }

    private void optimizeConstantPropAndFold(JmmNode node, SymbolTable table) {
        while (propagate(node, table) || fold(node, table)) ;
    }

    private boolean propagate(JmmNode node, SymbolTable table) {
        var cl = node.getChildren(CLASS_DECL.getNodeName()).get(0);
        var methods = cl.getChildren(METHOD_DECL.getNodeName());

        boolean ret = false;

        for (var method : methods) {
            ret |= propagateInMethod(method, table);
        }

        return false;
    }

    private boolean propagateInMethod(JmmNode node, SymbolTable table) {
        HashMap<String, Integer> integers = new HashMap<>();
        HashMap<String, Boolean> booleans = new HashMap<>();

        boolean ret = false;

        for (var stmt : node.getChildren()) {
            try {
                Kind kind = Kind.fromString(stmt.getKind());
                if (!Kind.STATEMENTS.contains(kind)) continue;

                if (kind == ASSIGN_STMT || kind == ARRAY_ASSIGN_STMT) {
                    var var = stmt.getChild(0).get("name");
                    var expr = stmt.getChild(stmt.getNumChildren() - 1);
                    ret |= addAndReplace(var, expr, integers, booleans);
                }
                if (stmt.getKind().equals(RETURN_STMT.getNodeName())) {
                    var expr = stmt.getChild(0);
                    ret |= tryReplace(expr, integers, booleans);
                }
                if (stmt.getKind().equals(IF_STMT.getNodeName())) {
                    var expr = stmt.getChild(0);
                    ret |= checkIfStmt(stmt, integers, booleans);
                    ret |= tryReplace(expr, integers, booleans);
                }
                if (stmt.getKind().equals(WHILE_STMT.getNodeName())) {
                    var expr = stmt.getChild(0);
                    ret |= checkWhileStmt(stmt, integers, booleans);
                    ret |= tryReplace(expr, integers, booleans);
                }
            } catch (Exception ignored) {
                // Kind.fromString can throw exception, ignore and continue
            }
        }

        return false;
    }

    private boolean checkWhileStmt(JmmNode stmt, HashMap<String, Integer> integers, HashMap<String, Boolean> booleans) {
        boolean ret = false;
        var expr = stmt.getChild(0);
        var body = stmt.getChild(1);

        for (var key : integers.keySet()) {
            boolean updated = varUpdated(key, body);
            if (!updated) {
                for (var child : body.getChildren()) {
                    HashMap<String, Integer> mi = new HashMap<>();
                    mi.put(key, integers.get(key));
                    HashMap<String, Boolean> mb = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            } else {
                integers.remove(key);
            }
        }

        for (var key : booleans.keySet()) {
            boolean updated = varUpdated(key, body);
            if (!updated) {
                for (var child : body.getChildren()) {
                    HashMap<String, Boolean> mb = new HashMap<>();
                    mb.put(key, booleans.get(key));
                    HashMap<String, Integer> mi = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            } else {
                booleans.remove(key);
            }
        }

        return ret;
    }

    private boolean checkIfStmt(JmmNode stmt, HashMap<String, Integer> integers, HashMap<String, Boolean> booleans) {
        var expr = stmt.getChild(0);
        var ifBody = stmt.getChild(1);
        var elseBody = stmt.getChild(2);

        boolean ret = false;

        for (var key : integers.keySet()) {
            boolean updated = varUpdated(key, ifBody);
            if (!updated) {
                for (var child : ifBody.getChildren()) {
                    HashMap<String, Integer> mi = new HashMap<>();
                    mi.put(key, integers.get(key));
                    HashMap<String, Boolean> mb = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            }

            updated |= varUpdated(key, elseBody);
            if (!updated) {
                for (var child : elseBody.getChildren()) {
                    HashMap<String, Integer> mi = new HashMap<>();
                    mi.put(key, integers.get(key));
                    HashMap<String, Boolean> mb = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            } else {
                integers.remove(key);
            }
        }

        for (var key : booleans.keySet()) {
            boolean updated = varUpdated(key, ifBody);
            if (!updated) {
                for (var child : ifBody.getChildren()) {
                    HashMap<String, Boolean> mb = new HashMap<>();
                    mb.put(key, booleans.get(key));
                    HashMap<String, Integer> mi = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            }

            updated |= varUpdated(key, elseBody);
            if (!updated) {
                for (var child : elseBody.getChildren()) {
                    HashMap<String, Boolean> mb = new HashMap<>();
                    mb.put(key, booleans.get(key));
                    HashMap<String, Integer> mi = new HashMap<>();
                    ret |= tryReplace(child, mi, mb);
                }
            } else {
                booleans.remove(key);
            }
        }

        return ret;
    }

    private boolean varUpdated(String key, JmmNode stmt) {
        if (stmt.getKind().equals(BLOCK_STMT.getNodeName())) {
            boolean ret = false;
            for (var child : stmt.getChildren()) {
                ret |= varUpdated(key, child);
            }
            return ret;
        }

        try {
            Kind kind = Kind.fromString(stmt.getKind());

            if (kind == ASSIGN_STMT || kind == ARRAY_ASSIGN_STMT) {
                var name = stmt.getChild(0).get("name");
                var expr = stmt.getChild(stmt.getNumChildren() - 1);
                return name.equals(key) && !canEvaluate(expr);
            }
        } catch (Exception ignored) {
            return true;
        }

        return false;
    }

    private boolean tryReplace(JmmNode expr, HashMap<String, Integer> integers, HashMap<String, Boolean> booleans) {
        if (expr.getKind().equals(PAREN_EXPR.getNodeName())) {
            return tryReplace(expr.getChild(0), integers, booleans);
        }
        if (expr.getKind().equals(UNARY_EXPR.getNodeName())) {
            return tryReplace(expr.getChild(0), integers, booleans);
        }
        if (expr.getKind().equals(BINARY_EXPR.getNodeName())) {
            return tryReplace(expr.getChild(0), integers, booleans) ||
                    tryReplace(expr.getChild(1), integers, booleans);
        }
        if (expr.getKind().equals(VAR_REF_EXPR.getNodeName())) {
            return replaceVar(expr, integers, booleans);
        }

        boolean ret = false;
        for (var child : expr.getChildren()) {
            ret |= tryReplace(child, integers, booleans);
        }

        return ret;
    }

    private boolean replaceVar(JmmNode expr, HashMap<String, Integer> integers, HashMap<String, Boolean> booleans) {
        JmmNode parent = expr.getParent();
        String name = expr.get("name");
        JmmNode add;

        if (integers.containsKey(name)) {
            add = new JmmNodeImpl(Arrays.asList(INTEGER_LITERAL.getNodeName()));
            add.put("value", integers.get(name).toString());
        } else if (booleans.containsKey(name)) {
            add = new JmmNodeImpl(Arrays.asList(BOOLEAN_LITERAL.getNodeName()));
            add.put("value", booleans.get(name).toString());
        } else {
            return false;
        }

        int idx = parent.getChildren().indexOf(expr);
        parent.removeChild(expr);
        parent.add(add, idx);
        return true;
    }

    private boolean addAndReplace(String var, JmmNode expr, HashMap<String, Integer> integers, HashMap<String, Boolean> booleans) {
        if (expr.getKind().equals(INTEGER_LITERAL.getNodeName())) {
            integers.put(var, Integer.parseInt(expr.get("value")));
            return false;   // did not replace constant
        }
        if (expr.getKind().equals(BOOLEAN_LITERAL.getNodeName())) {
            booleans.put(var, Boolean.parseBoolean(expr.get("value")));
            return false;   // did not replace constant
        }
        if (expr.getKind().equals(PAREN_EXPR.getNodeName())) {
            return addAndReplace(var, expr.getChild(0), integers, booleans);
        }
        if (expr.getKind().equals(UNARY_EXPR.getNodeName())) {
            return addAndReplace(var, expr.getChild(0), integers, booleans);
        }
        if (expr.getKind().equals(BINARY_EXPR.getNodeName())) {
            return addAndReplace(var, expr.getChild(0), integers, booleans) ||
                    addAndReplace(var, expr.getChild(1), integers, booleans);
        }
        if (expr.getKind().equals(VAR_REF_EXPR.getNodeName())) {
            // if something like i = 1; i = i + 1; cannot propagate i;
            if (var.equals(expr.get("name"))) return false;
            return replaceVar(expr, integers, booleans);
        }

        return false;
    }

    private boolean fold(JmmNode node, SymbolTable table) {
        boolean ret = false;

        try {
            Kind kind = Kind.fromString(node.getKind());
            if (assignments.contains(kind)) {
                ret |= foldExpr(node, table);
            }
        } catch (Exception e) {
            // thrown by Kind.fromString, ignore
        }

        for (var child : node.getChildren()) {
            ret |= fold(child, table);
        }

        return ret;
    }

    private boolean foldExpr(JmmNode node, SymbolTable table) {
        JmmNode expr = node.getChild(node.getNumChildren() - 1);

        if (expr.getKind().equals(INTEGER_LITERAL.getNodeName()) ||
                expr.getKind().equals(BOOLEAN_LITERAL.getNodeName())) return false;
        if (!canEvaluate(expr)) return false;

        int value = evaluateExpr(expr);
        String result = String.valueOf(value);

        Type type = new TypeUtils(table).getExprType(expr);
        JmmNode add;
        if (TypeUtils.isBoolean(type)) {
            result = value == 1 ? "true" : "false";
            add = new JmmNodeImpl(Arrays.asList(BOOLEAN_LITERAL.getNodeName()));
        } else {
            add = new JmmNodeImpl(Arrays.asList(INTEGER_LITERAL.getNodeName()));
        }
        add.put("value", result);

        node.removeChild(expr);
        node.add(add);

        return true;
    }

    private int evaluateExpr(JmmNode node) {
        if (node.getKind().equals(INTEGER_LITERAL.getNodeName()))
            return Integer.parseInt(node.get("value"));
        if (node.getKind().equals(BOOLEAN_LITERAL.getNodeName()))
            return node.get("value").equals("true") ? 1 : 0;
        if (node.getKind().equals(PAREN_EXPR.getNodeName()))
            return evaluateExpr(node.getChild(0));
        if (node.getKind().equals(UNARY_EXPR.getNodeName()))
            return 1 - evaluateExpr(node.getChild(0));
        if (node.getKind().equals(BINARY_EXPR.getNodeName())) {
            int left = evaluateExpr(node.getChild(0));
            int right = evaluateExpr(node.getChild(1));

            String operator = node.get("op");
            if ("+-*/".contains(operator)) {
                return switch (operator) {
                    case "+" -> left + right;
                    case "-" -> left - right;
                    case "*" -> left * right;
                    case "/" -> left / right;
                    default -> 0;
                };
            } else if (operator.equals("&&") || operator.equals("||")) {
                boolean res = switch (operator) {
                    case "&&" -> left == 1 && right == 1;
                    case "||" -> left == 1 || right == 1;
                    default -> false;
                };

                return res ? 1 : 0;
            } else {
                boolean res = switch (operator) {
                    case "<" -> left < right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case ">=" -> left >= right;
                    case "==" -> left == right;
                    default -> false;
                };

                return res ? 1 : 0;
            }
        }
        return 0;
    }

    private boolean canEvaluate(JmmNode node) {
        if (node.getKind().equals(INTEGER_LITERAL.getNodeName()))
            return true;
        if (node.getKind().equals(BOOLEAN_LITERAL.getNodeName()))
            return true;
        if (node.getKind().equals(PAREN_EXPR.getNodeName()))
            return canEvaluate(node.getChild(0));
        if (node.getKind().equals(BINARY_EXPR.getNodeName()))
            return canEvaluate(node.getChild(0)) && canEvaluate(node.getChild(1));
        if (node.getKind().equals(UNARY_EXPR.getNodeName()))
            return canEvaluate(node.getChild(0));
        return false;
    }

    // this pass will transform calls to varargs methods with calls with arrays
    // example:
    // public int foo(int... a) { return a[0]; }
    // this.foo(1, 2, 3); -> this.foo([1,2,3]);
    private void inPlaceArrayForVarArgs(JmmNode node, SymbolTable table) {
        dfsReplaceVarArgCalls(node, table);
    }

    private void dfsReplaceVarArgCalls(JmmNode node, SymbolTable table) {
        if (node.getKind().equals(FUNC_EXPR.getNodeName())) {
            var methods = table.getMethods();
            for (var method : methods) {
                if (node.get("methodname").equals(method)) {
                    checkVarArgsAndReplace(node, table);
                }
            }
        }

        // if not func expr continue visit
        for (var child : node.getChildren()) {
            dfsReplaceVarArgCalls(child, table);
        }
    }

    private void checkVarArgsAndReplace(JmmNode node, SymbolTable table) {
        var params = table.getParameters(node.get("methodname"));
        if (params.isEmpty()) return;

        Symbol lastParam = params.get(params.size() - 1);
        JmmNode lastChild = node.getChild(node.getNumChildren() - 1);

        // last argument is not array, no change needed
        if (!lastParam.getType().isArray()) return;

        // last argument is array but last child is also array, no need to change
        if (lastParam.getType().isArray() && lastChild.getKind().equals(ARRAY_EXPR.getNodeName())) return;

        // from now on we know lastParam is varargs and we must alter tree
        int start = params.size();
        ArrayList<JmmNode> arrayExprs = new ArrayList<>();

        for (int i = start; i < node.getNumChildren(); i++) {
            JmmNode child = node.getChild(i);
            arrayExprs.add(child);
        }

        // Remove all children that will go into the array
        for (JmmNode child : arrayExprs) {
            node.removeChild(child);
        }

        // Create new array expression node
        JmmNode arrayExpr = new JmmNodeImpl(Arrays.asList(ARRAY_EXPR.getNodeName()));

        // Add all collected expressions as children to the array expression
        for (JmmNode child : arrayExprs) {
            arrayExpr.add(child);
        }

        // Add the array expression as the last parameter
        node.add(arrayExpr);
    }
}