package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.*;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2025.optimization.OptUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;
import static org.specs.comp.ollir.OperationType.*;


import java.util.*;
import java.util.stream.Collectors;

public class JasminGenerator {
    private static final String TAB = "    ";
    private static final String NL = "\n";
    private static final Set<OperationType> comparators = Set.of(
            LTE, LTH, GTH, GTE, EQ, NEQ
    );

    private final OllirResult ollirResult;
    private final Map<String, String> importFullNames = new HashMap<>();
    private final FunctionClassMap<TreeNode, String> generators;

    List<Report> reports;
    String code;
    Method currentMethod;

    private int stack = 0;
    private int maxStack = 0;
    private int maxLocals = 0;
    //private final ClassUnit classUnit;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        this.reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::getOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CondBranchInstruction.class, this::generateConditional);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(Instruction.class, this::generateInstruction);
    }

    private static String fieldClassAndName(Operand opClass, Operand field) {
        var className = opClass.toElement().getType();
        var name = className.toString();
        name = name.substring(name.lastIndexOf("(") + 1, name.length() - 1);
        return name + "/" + field.getName();
    }

    private void updateStack(int inc) {
        stack += inc;
        if (stack < 0) System.out.println("ERROR in stack size");
        maxStack = Math.max(maxStack, stack);
    }

    private void updateLocal(int vr) {
        maxLocals = Math.max(maxLocals, vr + 1);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return formatJasmin(code);
    }

    private String formatJasmin(String code) {
        var lines = code.split("\n");

        var formatted = new StringBuilder();

        var indent = 0;
        for (var line : lines) {
            if (line.startsWith(".end")) {
                indent--;
            }

            formatted.append(TAB.repeat(indent)).append(line).append(NL);

            if (line.startsWith(".method")) {
                indent++;
            }
        }

        System.out.println(formatted);
        return formatted.toString();
    }

    private void addImportFullNames(ClassUnit classUnit) {
        for (var i : classUnit.getImports()) {
            String importNonQualified = i.substring(i.lastIndexOf(".") + 1);
            i = i.replace(".", "/");
            importFullNames.put(importNonQualified, i);
        }
    }

    private String generateClassUnit(ClassUnit classUnit) {
        addImportFullNames(classUnit);

        var code = new StringBuilder();

        var modifier = classUnit.getClassAccessModifier() != AccessModifier.DEFAULT ?
                classUnit.getClassAccessModifier().name().toLowerCase() + " " : "";

        var className = classUnit.getClassName();
        code.append(".class ").append(modifier).append(className).append(NL);

        var superClass = getSuperClassName();
        code.append(".super ").append(superClass).append(NL).append(NL);

        for (var field : classUnit.getFields()) {
            code.append(getField(field)).append(NL);
        }

        for (var method : classUnit.getMethods()) {
            if (method.isConstructMethod()) {
                code.append(getConstructor(superClass));
            } else {
                code.append(generators.apply(method));
            }
            code.append(NL).append(NL);
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        // set method
        currentMethod = method;

        var code = new StringBuilder(getMethodHeader(method));
        updateLocal(method.getParams().size());
        var instructions = new StringBuilder();

        for (var inst : method.getInstructions()) {
            var labels = method.getLabels(inst);

            for (var label : labels) {
                instructions.append(label).append(":\n");
            }

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL, "", NL));

            instructions.append(instCode);

            while (this.stack > 0) {
                instructions.append("pop\n");
                this.stack--;
            }
        }

        code.append(".limit stack ").append(maxStack).append(NL);
        code.append(".limit locals ").append(maxLocals).append(NL);
        code.append(instructions);
        code.append(".end method");

        // unset method
        currentMethod = null;
        this.maxStack = 0;
        this.maxLocals = 0;

        return code.toString();
    }

    private String getMethodHeader(Method method) {
        var header = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " : "public ";

        if (method.isStaticMethod()) {
            modifier += "static ";
        }

        if (method.isFinalMethod()) {
            modifier += "final ";
        }

        header.append(".method ").append(modifier).append(method.getMethodName());

        // Add parameters
        var params = method.getParams().stream()
                .map(this::generateParam).toList();

        header.append("(").append(String.join("", params)).append(")");

        // Add return type
        var returnType = generateParam(method.getReturnType());
        header.append(returnType).append(NL);

        return header.toString();
    }

    private String generateGetField(GetFieldInstruction instruction) {
        StringBuilder code = new StringBuilder();

        // Retrieve the virtual register for the object
        int virtualRegister = currentMethod.getVarTable()
                .get(instruction.getObject().getName())
                .getVirtualReg();
        updateLocal(virtualRegister);

        // Determine the appropriate aload instruction
        String aloadInstruction = virtualRegister <= 3 ? "aload_" : "aload ";
        code.append(aloadInstruction).append(virtualRegister).append(NL);

        // Update the stack size
        updateStack(1);

        // Generate the getfield instruction
        code.append("getfield ")
                .append(fieldClassAndName(instruction.getObject(), instruction.getField()))
                .append(" ")
                .append(generateParam(instruction.getField()))
                .append(NL);

        return code.toString();
    }

    private String generateParam(Element element) {
        return generateType(element.getType());
    }

    private String generateParam(Type type) {
        return generateType(type);
    }

    private String generatePutField(PutFieldInstruction instruction) {
        var code = new StringBuilder();

        int vr = currentMethod.getVarTable().get(instruction.getObject().getName()).getVirtualReg();
        updateLocal(vr);

        String aload = vr <= 3 ? "aload_" : "aload ";

        // push object onto the stack
        code.append(aload).append(vr).append(NL);

        updateStack(1);

        // get value from the field
        code.append("getfield ");

        code.append(fieldClassAndName(instruction.getObject(), instruction.getField()));
        code.append(" ");

        // Add return type
        var returnType = generateType(instruction.getField().getType());
        code.append(returnType).append(NL);

        return code.toString();
    }

    private String generateCall(CallInstruction instruction) {

        String type = instruction.getInvocationKind();

        return switch (type) {
            case "InvokeStatic", "InvokeSpecial", "InvokeVirtual" -> generateInvoke(instruction);
            case "New" -> generateNew(instruction);
            case "ArrayLength" -> generateArrayLength(instruction);
            case "Ldc" -> generateLoadConstant(instruction);
            default -> throw new NotImplementedException(type);
        };
    }

    private String generateArrayLength(CallInstruction instruction) {
        var code = new StringBuilder();
        Operand caller = (Operand) instruction.getCaller();
        int vr = currentMethod.getVarTable().get(caller.getName()).getVirtualReg();

        String aload = vr <= 3 ? "aload_" : "aload ";
        // push array to the stack
        code.append(aload).append(vr).append(NL);
        // get array length
        code.append("arraylength\n");

        updateLocal(vr);
        updateStack(1);


        return code.toString();
    }

    private String generateLoadConstant(CallInstruction instruction) {
        var code = new StringBuilder();

        // generate code for calling method
        var caller = instruction.getCaller().toString();
        var name = caller.substring(caller.lastIndexOf("(") + 1, caller.length() - 1);
        code.append("ldc ").append(name).append(NL);
        updateStack(1);

        return code.toString();
    }

    private String generateNew(CallInstruction instruction) {
        Type returnType = instruction.getReturnType();

        if (returnType instanceof ArrayType) {
            return generateNewArray(instruction);
        }

        var code = new StringBuilder();

        var caller = instruction.getCaller().toString();
        var name = caller.substring(caller.lastIndexOf("(") + 1, caller.length() - 1);
        name = importFullNames.getOrDefault(name, name);
        code.append("   new ").append(name).append(NL);

        updateStack(1);

        return code.toString();
    }

    private String generateNewArray(CallInstruction instruction) {
        Element arg = instruction.getArguments().getFirst();

        updateStack(1);
        updateStack(-1);

        return getOperand(arg) + "newarray int\n";
    }


    private String getInvokeVirtual(CallInstruction instruction, Type type) {
        var code = new StringBuilder();
        var className = type.toString();
        className = className.substring(className.lastIndexOf("(") + 1, className.length() - 1);
        className = importFullNames.getOrDefault(className, className);

        code.append("invokevirtual ").append(className).append("/");

        var methodName = ((LiteralElement) instruction.getMethodName()).getLiteral();

        code.append(methodName);
        return code.toString();
    }

    private String getInvokeStatic(CallInstruction instruction, String caller) {
        var code = new StringBuilder();
        caller = importFullNames.getOrDefault(caller, caller);
        code.append("invokestatic ").append(caller).append("/");

        var methodName = ((LiteralElement) instruction.getMethodName()).getLiteral();
        //methodName = methodName.substring(1, methodName.length() - 1);

        code.append(methodName);
        return code.toString();
    }

    private String getInvokeSpecial(Type type) {

        var className = type.toString();
        className = className.substring(className.lastIndexOf("(") + 1, className.length() - 1);
        className = importFullNames.getOrDefault(className, className);

        return "invokespecial " + className + "/<init>";
    }

    private String generateInvoke(CallInstruction instruction) {
        var code = new StringBuilder();

        // push object onto the stack
        Operand caller = (Operand) instruction.getCaller();
        code.append(getOperand(caller));

        int numArgs = instruction.getArguments().size();
        // generate code for loading arguments
        for (var arg : instruction.getArguments()) {
            code.append(getOperand(arg));
        }

        updateStack(-numArgs);

        switch (instruction.getInvocationKind()) {
            case "InvokeStatic" -> code.append(getInvokeStatic(instruction, caller.getName()));
            case "InvokeSpecial" -> {
                code.append(getInvokeSpecial(caller.getType()));
                updateStack(-1);
            }
            case "InvokeVirtual"-> {
                code.append(getInvokeVirtual(instruction, caller.getType()));
                updateStack(-1);
            }
            default -> throw new NotImplementedException(instruction.getInvocationKind());
        }

        code.append("(");

        // generate code for loading arguments
        for (var arg : instruction.getArguments()) {
            code.append(generateParam(arg));
        }

        String returnType = generateParam(instruction.getReturnType());
        code.append(")").append(returnType);
        code.append(NL);

        if (!returnType.equals("V")) {
            updateStack(1);
        }

        return code.toString();
    }

    private String generateAssign(AssignInstruction assignInstruction) {
        var code = new StringBuilder();
        Element dest = assignInstruction.getDest();
        Instruction rhs = assignInstruction.getRhs();

        if (dest instanceof Operand lhsOperand && BuiltinType.is(lhsOperand.getType(), BuiltinKind.INT32)) {
            var variable = currentMethod.getVarTable().get(lhsOperand.getName());
            if (variable != null) {
                int varIndex = variable.getVirtualReg();
                String varName = lhsOperand.getName();

                if (rhs instanceof BinaryOpInstruction binOp && binOp.getOperation().getOpType() == OperationType.ADD) {
                    Operand varOp = null;
                    LiteralElement litOp = null;

                    if (binOp.getLeftOperand() instanceof Operand left && left.getName().equals(varName) && binOp.getRightOperand() instanceof LiteralElement rightLit) {
                        varOp = left;
                        litOp = rightLit;
                    } else if (binOp.getRightOperand() instanceof Operand right && right.getName().equals(varName) && binOp.getLeftOperand() instanceof LiteralElement leftLit) {
                        varOp = right;
                        litOp = leftLit;
                    }

                    if (varOp != null && litOp != null) {

                        try {
                            int incValue = Integer.parseInt(litOp.getLiteral());
                            if (incValue != 0 && incValue >= -128 && incValue <= 127) {
                                updateLocal(varIndex);
                                code.append("iinc ").append(varIndex).append(" ").append(incValue).append(NL);
                                return code.toString();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        String rhsCodeString = generators.apply(rhs);

        if (dest instanceof Operand lhsOperand) {
            code.append(rhsCodeString);
            var variable = currentMethod.getVarTable().get(lhsOperand.getName());
            if (variable == null) {
                reports.add(Report.newError(null, -1, -1, "Variable not found in var table for assignment: " + lhsOperand.getName(), null));
                return "; ERROR: Variable not found for assignment: " + lhsOperand.getName() + NL;
            }
            int reg = variable.getVirtualReg();
            Type type = lhsOperand.getType();
            updateLocal(reg);

            updateStack(-1);

            if (BuiltinType.is(type, BuiltinKind.INT32) ||
                    BuiltinType.is(type, BuiltinKind.BOOLEAN)) {
                String istore = reg <= 3 ? "istore_" : "istore ";
                code.append(istore).append(reg).append(NL);
            }

            // Assumindo que outros tipos (OBJECTREF, ARRAYREF, STRING, CLASS) são referências
            else {
                String astore = reg <= 3 ? "astore_" : "astore ";
                code.append(astore).append(reg).append(NL);
            }
        } else if (dest instanceof ArrayOperand arrayLhs) {
            var variableInfo = currentMethod.getVarTable().get(arrayLhs.getName());
            if (variableInfo == null) {
                reports.add(Report.newError(null, -1, -1, "Array variable not found for store: " + arrayLhs.getName(), null));
                return "; ERROR: Array variable not found for store: " + arrayLhs.getName() + NL;
            }
            int virtualRegister = variableInfo.getVirtualReg();
            updateLocal(virtualRegister);
            String loadInstruction = virtualRegister <= 3 ? "aload_" : "aload ";

            code.append(loadInstruction).append(virtualRegister).append(NL);
            updateStack(1);

            code.append(generators.apply(arrayLhs.getIndexOperands().getFirst()));

            code.append(rhsCodeString);
            if(rhs instanceof AssignInstruction assignRhs && assignRhs.getDest() instanceof Operand) {
                // Se o lado direito é uma atribuição simples, só empilha o valor
                code.append(generators.apply(assignRhs.getRhs()));
                code.append("iastore").append(NL);
            } else {
                code.append("aastore").append(NL);
            }

        } else {
            reports.add(Report.newError(null, -1, -1, "Unsupported destination type for assignment: " + dest.getClass().getName(), null));
            throw new NotImplementedException("Assign to destination of type: " + dest.getClass().getName());
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction instruction) {
        return generators.apply(instruction.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {

        updateStack(1);
        int n = Integer.parseInt(literal.getLiteral());

        if (n == -1) return "iconst_m1" + NL;
        if (n >= 0 && n <= 5) return "iconst_" + n + NL;
        if (n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE) return "bipush " + n + NL;
        if (n >= Short.MIN_VALUE && n <= Short.MAX_VALUE) return "sipush " + n + NL;

        return "ldc " + n + NL;
    }


    private String getOperand(Element operand) {
        if (operand instanceof ArrayOperand arrayOperand) {
            return getArrayOperand(arrayOperand);
        }
        if (operand instanceof Operand simpleOperand) {
            return resolveSimpleOperand(simpleOperand);
        }
        return generators.apply(operand);
    }

    private String getArrayOperand(ArrayOperand arrayOperand) {
        var variableInfo = currentMethod.getVarTable().get(arrayOperand.getName());
        int virtualRegister = variableInfo.getVirtualReg();
        StringBuilder codeBuilder = new StringBuilder();

        updateLocal(virtualRegister);
        updateStack(1);

        String loadInstruction = virtualRegister <= 3 ? "aload_" : "aload ";

        codeBuilder.append(loadInstruction).append(virtualRegister).append(NL);
        codeBuilder.append(getOperand(arrayOperand.getIndexOperands().getFirst()));
        codeBuilder.append("iaload").append(NL);

        updateStack(-1);

        return codeBuilder.toString();
    }

    private String getOperand(Operand operand) {
        if (operand instanceof ArrayOperand op) {
            return getArrayOperand(op);
        }

        var variable = currentMethod.getVarTable().get(operand.getName());

        if (variable == null) {
            return "";
        }

        var reg = variable.getVirtualReg();
        var type = operand.getType();

        updateLocal(reg);
        updateStack(1);

        // Check for primitive types (int, boolean)
        if (BuiltinType.is(type, BuiltinKind.INT32) ||
                BuiltinType.is(type, BuiltinKind.BOOLEAN)) {
            String iload = reg <= 3 ? "iload_" : "iload ";
            return iload + reg + NL;
        }
        // Check for reference types (String, objects, arrays)
        else if (BuiltinType.is(type, BuiltinKind.STRING) ||
                ClassType.is(type, ClassKind.OBJECTREF) ||
                ClassType.is(type, ClassKind.THIS) ||
                type instanceof ArrayType) {
            String aload = reg <= 3 ? "aload_" : "aload ";
            return aload + reg + NL;
        }

        throw new NotImplementedException(type.toString());
    }

    private String resolveSimpleOperand(Operand operand) {
        return generators.apply(operand);
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        OperationType type = binaryOp.getOperation().getOpType();
        // apply operation
        var op = switch (type) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB, LTE, LTH, GTH, GTE, EQ, NEQ -> "isub";
            case DIV -> "idiv";
            case XOR -> "ixor";
            case AND, ANDB, NOTB -> "iand";
            case OR, ORB -> "ior";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        if (comparators.contains(type)) {
            String ifInst = switch (type) {
                case LTE -> "ifle ";
                case LTH -> "iflt ";
                case GTE -> "ifge ";
                case GTH -> "ifgt ";
                case EQ -> "ifeq ";
                case NEQ -> "ifne ";
                default -> throw new NotImplementedException(type);
            };

            String trueLabel = OptUtils.getLabel("L_fact");
            String endLabel = OptUtils.getLabel("L_end");

            code.append(ifInst).append(trueLabel).append("\n");
            code.append("iconst_0\ngoto ").append(endLabel).append("\n");
            code.append(trueLabel).append(":\niconst_1\n");
            code.append(endLabel).append(":\n");
        }

        updateStack(-1);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction instruction) {
        var operand = instruction.getOperand();
        String operandCode = generators.apply(operand);

        updateStack(1);
        updateStack(-1);

        return operandCode + "iconst_1\nixor\n";
    }
    // todo: check later and fix
    private String generateReturn(ReturnInstruction instruction) {
        var returnType = instruction.getReturnType();
        StringBuilder code = new StringBuilder();

        // Check if return has a value (non-void)
        if (instruction.hasReturnValue()) {
            // Get the operand value from the Optional
            Element operand = instruction.getOperand().orElseThrow(() ->
                    new IllegalStateException("Return instruction claims to have value but operand is empty"));

            // Generate code for the operand based on its actual type
            code.append(generators.apply(operand));

            // Determine the appropriate return instruction based on type
            if (BuiltinType.is(returnType, BuiltinKind.INT32) ||
                    BuiltinType.is(returnType, BuiltinKind.BOOLEAN)) {
                code.append("ireturn").append(NL);
            }
            else if (BuiltinType.is(returnType, BuiltinKind.STRING) ||
                    returnType instanceof ArrayType ||
                    ClassType.is(returnType, ClassKind.OBJECTREF)) {
                code.append("areturn").append(NL);
            }
            else {
                throw new NotImplementedException(returnType.toString());
            }

            updateStack(-1); // Decrease stack by 1 for non-void returns
        }
        else {
            // Void return doesn't consume stack items
            code.append("return").append(NL);
        }

        return code.toString();
    }

    private String generateConditional(CondBranchInstruction instruction) {
        if (instruction instanceof OpCondInstruction opCondInstruction) {
            return generateOpCondInstruction(opCondInstruction);
        }

        return generateSingleOpCondInstruction((SingleOpCondInstruction) instruction);
    }

    private String generateOpCondInstruction(OpCondInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Instruction condition = instruction.getCondition();
        if (condition instanceof SingleOpInstruction) {
            code.append(generators.apply(((SingleOpInstruction) condition).getSingleOperand()));
        } else {
            code.append(generators.apply(condition));
        }

        updateStack(-1);
        code.append("ifne ").append(instruction.getLabel()).append(NL);
        return code.toString();
    }

    private String generateSingleOpCondInstruction(SingleOpCondInstruction inst) {
        StringBuilder code = new StringBuilder();
        Instruction condition = inst.getCondition();
        code.append(generators.apply(condition));
        updateStack(-1);
        code.append("ifne ").append(inst.getLabel());

        return code.toString();
    }


    private String generateGoto(GotoInstruction instruction) {
        String label = instruction.getLabel();

        return "goto " + label;
    }

    private String generateInstruction(Instruction instruction) {
        System.out.println("Instruction not implemented: " + instruction);
        return "";
    }

    public String getSuperClassName() {
        var superClass = ollirResult.getOllirClass().getSuperClass();

        if (superClass == null || superClass.equals("Object")) {
            superClass = "java/lang/Object";
        }

        superClass = importFullNames.getOrDefault(superClass, superClass);
        return superClass;
    }

    public String generateType(Type type) {
        // For BuiltinType
        if (BuiltinType.is(type, BuiltinKind.INT32)) return "I";
        if (BuiltinType.is(type, BuiltinKind.STRING)) return "Ljava/lang/String;";
        if (BuiltinType.is(type, BuiltinKind.BOOLEAN)) return "Z";
        if (BuiltinType.is(type, BuiltinKind.VOID)) return "V";

        // For ArrayType
        if (type.getClass().equals(ArrayType.class)) {
            ArrayType arrayType = (ArrayType) type;
            return "[" + generateType(arrayType.getElementType());
        }

        // For ClassType
        if (ClassType.is(type, ClassKind.OBJECTREF)) {
            ClassType classType = (ClassType) type;
            String name = classType.getName();
            return "L" + importFullNames.getOrDefault(name, name) + ";";
        }

        throw new NotImplementedException(type.toString());
    }

    public String getField(Field field) {
        var code = new StringBuilder();

        var modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " : "";

        if (field.isStaticField()) {
            modifier += "static ";
        }

        if (field.isFinalField()) {
            modifier += "final ";
        }

        code.append(".field ").append(modifier).append(field.getFieldName()).append(" ");

        var type = generateType(field.getFieldType());
        code.append(type).append(NL);

        return code.toString();
    }

    private String getConstructor(String superClass) {
        return """
                .method public <init>()V
                aload_0
                invokespecial %s/<init>()V
                return
                .end method
                """.formatted(superClass);
    }

}
