package pt.up.fe.comp2025.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.specs.comp.ollir.ArrayOperand;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.ArrayLengthInstruction;
import org.specs.comp.ollir.inst.AssignInstruction;
import org.specs.comp.ollir.inst.BinaryOpInstruction;
import org.specs.comp.ollir.inst.CallInstruction;
import org.specs.comp.ollir.inst.CondBranchInstruction;
import org.specs.comp.ollir.inst.GetFieldInstruction;
import org.specs.comp.ollir.inst.GotoInstruction;
import org.specs.comp.ollir.inst.Instruction;
import org.specs.comp.ollir.inst.InvokeSpecialInstruction;
import org.specs.comp.ollir.inst.InvokeStaticInstruction;
import org.specs.comp.ollir.inst.InvokeVirtualInstruction;
import org.specs.comp.ollir.inst.NewInstruction;
import org.specs.comp.ollir.inst.PutFieldInstruction;
import org.specs.comp.ollir.inst.ReturnInstruction;
import org.specs.comp.ollir.inst.SingleOpInstruction;
import org.specs.comp.ollir.inst.UnaryOpInstruction;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.BuiltinKind;
import org.specs.comp.ollir.type.BuiltinType;
import org.specs.comp.ollir.type.Type;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2025.backend.builders.AssignInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.BinaryOpInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.CallInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.CondBranchInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.ControlFlowInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.FieldAccessInstructionBuilder;
import pt.up.fe.comp2025.backend.builders.UnaryOpInstructionBuilder;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {
    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;
    private final JasminUtils types;
    private final FunctionClassMap<TreeNode, String> generators;

    List<Report> reports;
    String code;
    Method currentMethod;

    private int labelCounter = 0;
    private int maxStack = 0;
    private int stack = 0;

    private final Map<String, Integer> methodMaxStack = new HashMap<>();
    private final Map<String, Integer> methodMaxLocals = new HashMap<>();

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.reports = new ArrayList<>();
        this.types = new JasminUtils(ollirResult);
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, obj -> generateClassUnit((ClassUnit) obj));
        generators.put(Method.class, obj -> generateMethod((Method) obj));
        generators.put(AssignInstruction.class, obj -> generateAssign((AssignInstruction) obj));
        generators.put(SingleOpInstruction.class, obj -> generateSingleOp((SingleOpInstruction) obj));
        generators.put(LiteralElement.class, obj -> generateLiteral((LiteralElement) obj));
        generators.put(Operand.class, obj -> generateOperand((Operand) obj));
        generators.put(BinaryOpInstruction.class, obj -> generateBinaryOp((BinaryOpInstruction) obj));
        generators.put(ReturnInstruction.class, obj -> generateReturn((ReturnInstruction) obj));
        generators.put(CallInstruction.class, obj -> generateCall((CallInstruction) obj));
        generators.put(CondBranchInstruction.class, obj -> generateCondBranch((CondBranchInstruction) obj));
        generators.put(GotoInstruction.class, obj -> generateGoto((GotoInstruction) obj));
        generators.put(ArrayOperand.class, obj -> generateArrayOperand((ArrayOperand) obj));
        generators.put(GetFieldInstruction.class, obj -> generateGetField((GetFieldInstruction) obj));
        generators.put(PutFieldInstruction.class, obj -> generatePutField((PutFieldInstruction) obj));
        generators.put(UnaryOpInstruction.class, obj -> generateUnaryOp((UnaryOpInstruction) obj));
        generators.put(NewInstruction.class, obj -> generateNew((NewInstruction) obj));
        generators.put(InvokeSpecialInstruction.class, obj -> generateCall((InvokeSpecialInstruction) obj));
        generators.put(InvokeVirtualInstruction.class, obj -> generateCall((InvokeVirtualInstruction) obj));
        generators.put(InvokeStaticInstruction.class, obj -> generateCall((InvokeStaticInstruction) obj));
        generators.put(ArrayLengthInstruction.class, obj -> generateArrayLength((ArrayLengthInstruction) obj));
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }
        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {
        ClassStructureBuilder structureBuilder = new ClassStructureBuilder();

        // Build complete class structure
        structureBuilder.withClassDeclaration(ollirResult.getOllirClass().getClassName())
                .withInheritance(resolveParentClassName(classUnit))
                .withFieldDeclarations(classUnit.getFields())
                .withDefaultConstructor()
                .withMethodImplementations(filterNonConstructorMethods(ollirResult.getOllirClass().getMethods()));

        return structureBuilder.buildJasminCode();
    }

    // Inner class for building Jasmin class structure
    private class ClassStructureBuilder {
        private StringBuilder codeBuffer;
        private String parentClass;

        public ClassStructureBuilder() {
            this.codeBuffer = new StringBuilder();
        }

        public ClassStructureBuilder withClassDeclaration(String className) {
            codeBuffer.append(".class ").append(className).append(NL).append(NL);
            return this;
        }

        public ClassStructureBuilder withInheritance(String superClass) {
            this.parentClass = superClass;
            codeBuffer.append(".super ").append(superClass).append(NL).append(NL);
            return this;
        }

        public ClassStructureBuilder withFieldDeclarations(List<Field> fields) {
            generateFieldDeclarations(fields);
            return this;
        }

        public ClassStructureBuilder withDefaultConstructor() {
            codeBuffer.append(createConstructorCode(parentClass)).append(NL);
            return this;
        }

        public ClassStructureBuilder withMethodImplementations(List<Method> methods) {
            for (Method method : methods) {
                calculateMethodLimits(method);
                codeBuffer.append(generators.apply(method));
            }
            return this;
        }

        public String buildJasminCode() {
            return codeBuffer.toString();
        }

        private void generateFieldDeclarations(List<Field> fields) {
            for (Field field : fields) {
                codeBuffer.append(".field ")
                        .append(types.getModifier(field.getFieldAccessModifier()))
                        .append(field.getFieldName())
                        .append(" ")
                        .append(types.getJasminType(field.getFieldType()))
                        .append(NL);
            }

            if (!fields.isEmpty()) {
                codeBuffer.append(NL);
            }
        }

        private String createConstructorCode(String superClassName) {
            return String.format("""
                    ; default constructor
                    .method public <init>()V
                       aload_0
                       invokespecial %s/<init>()V
                       return
                    .end method
                    """, superClassName);
        }
    }

    private String resolveParentClassName(ClassUnit classUnit) {
        String parentClass = classUnit.getSuperClass();
        return (parentClass != null && !parentClass.isEmpty()) ? parentClass : "java/lang/Object";
    }

    private List<Method> filterNonConstructorMethods(List<Method> methods) {
        return methods.stream()
                .filter(method -> !method.isConstructMethod())
                .collect(java.util.stream.Collectors.toList());
    }

    // Removed old helper methods - replaced by ClassStructureBuilder

    private void calculateMethodLimits(Method method) {
        MethodAnalyzer analyzer = new MethodAnalyzer(method);
        methodMaxLocals.put(method.getMethodName(), analyzer.computeMaxLocals());
        methodMaxStack.put(method.getMethodName(), analyzer.computeMaxStackSize());
    }

    // Inner class for analyzing method requirements
    private class MethodAnalyzer {
        private final Method method;

        public MethodAnalyzer(Method method) {
            this.method = method;
        }

        public int computeMaxLocals() {
            int maxLocals = method.getParams().size() + 1;
            for (var entry : method.getVarTable().entrySet()) {
                Descriptor descriptor = entry.getValue();
                int virtualReg = descriptor.getVirtualReg();
                maxLocals = Math.max(maxLocals, virtualReg + 1);
            }
            return maxLocals;
        }

        public int computeMaxStackSize() {
            StackCalculator calculator = new StackCalculator();
            return calculator.calculateForMethod(method);
        }
    }

    // Separate class for stack size calculations
    private class StackCalculator {
        public int calculateForMethod(Method method) {
            int maxStack = 0;
            int currentStack = 0;

            for (Instruction inst : method.getInstructions()) {
                int stackDelta = analyzeStackChange(inst);
                currentStack += stackDelta;
                maxStack = Math.max(maxStack, currentStack);

                // Ensure stack doesn't go negative
                if (currentStack < 0) {
                    currentStack = 0;
                }
            }

            int minimumRequired = calculateMinimumRequiredStack(method);
            return Math.max(maxStack, minimumRequired);
        }

        private int calculateMinimumRequiredStack(Method method) {
            int minStack = 1; // Base requirement

            for (Instruction inst : method.getInstructions()) {
                minStack = Math.max(minStack, getMinStackForInstruction(inst));
            }
            return minStack;
        }

        private int getMinStackForInstruction(Instruction inst) {
            if (inst instanceof CallInstruction call) {
                int argsCount = call.getOperands().size() - 2;
                if (call instanceof InvokeVirtualInstruction || call instanceof InvokeSpecialInstruction) {
                    return 1 + argsCount + 2; // object ref + args + buffer
                } else if (call instanceof InvokeStaticInstruction) {
                    return argsCount + 2; // args + buffer
                } else if (call instanceof NewInstruction) {
                    return 3; // new + dup + args
                }
            } else if (inst instanceof BinaryOpInstruction) {
                return 3; // two operands + result
            } else if (inst instanceof AssignInstruction assign) {
                if (assign.getDest() instanceof ArrayOperand) {
                    return 4; // array ref + index + value + buffer
                }
            }
            return 1;
        }

        private int analyzeStackChange(Instruction inst) {
            return getStackChange(inst);
        }
    }

    private int getStackChange(Instruction inst) {
        if (inst instanceof AssignInstruction assign) {
            Element lhs = assign.getDest();
            Instruction rhs = assign.getRhs();
            if (lhs instanceof ArrayOperand) {
                return getStackChange(rhs) + 1 - 1;
            } else {
                return getStackChange(rhs) - 1;
            }
        }
        if (inst instanceof BinaryOpInstruction) {
            return 1;
        }
        if (inst instanceof UnaryOpInstruction) {
            return 0;
        }
        if (inst instanceof CallInstruction call) {
            int argsCount = call.getOperands().size() - 2;
            boolean hasReturnValue = !(call.getReturnType() instanceof BuiltinType) ||
                    ((BuiltinType) call.getReturnType()).getKind() != BuiltinKind.VOID;
            if (call instanceof InvokeVirtualInstruction || call instanceof InvokeSpecialInstruction) {
                return (1 + argsCount) - (hasReturnValue ? 0 : 1 + argsCount);
            } else if (call instanceof InvokeStaticInstruction) {
                return argsCount - (hasReturnValue ? argsCount - 1 : argsCount);
            } else if (call instanceof NewInstruction) {
                Type returnType = call.getReturnType();
                if (returnType instanceof ArrayType) {
                    return 1 - 1 + 1;
                } else {
                    return 2;
                }
            }
        }
        if (inst instanceof ReturnInstruction) {
            return 0;
        }
        if (inst instanceof CondBranchInstruction) {
            return 1;
        }
        if (inst instanceof SingleOpInstruction) {
            return 1;
        }
        if (inst instanceof ArrayLengthInstruction) {
            return 1 - 1 + 1;
        }
        if (inst instanceof GetFieldInstruction) {
            return 1 - 1 + 1;
        }
        if (inst instanceof PutFieldInstruction) {
            return 1;
        }
        if (inst instanceof GotoInstruction) {
            return 0;
        }
        return 0;
    }

    // Utility methods for code generation
    private void updateStackSize(int change) {
        stack += change;
        maxStack = Math.max(maxStack, stack);
        if (currentMethod != null) {
            methodMaxStack.put(currentMethod.getMethodName(), maxStack);
        }
    }

    private String getNextLabel() {
        return "L" + (labelCounter++);
    }

    private String generateMethod(Method method) {
        MethodBuilder methodBuilder = new MethodBuilder(method);
        return methodBuilder.withSignature()
                .withLimits()
                .withInstructionProcessing()
                .buildJasminMethod();
    }

    // Inner class for building complete method structures
    private class MethodBuilder {
        private final Method method;
        private final StringBuilder codeBuffer;
        private final String methodName;

        public MethodBuilder(Method method) {
            this.method = method;
            this.methodName = method.getMethodName();
            this.codeBuffer = new StringBuilder();
        }

        public MethodBuilder withSignature() {
            String methodSignature = buildMethodSignature();
            codeBuffer.append(methodSignature);
            return this;
        }

        public MethodBuilder withLimits() {
            int stackLimit = methodMaxStack.getOrDefault(methodName, 5);
            int localsLimit = methodMaxLocals.getOrDefault(methodName, 99);

            codeBuffer.append(TAB).append(".limit stack ").append(stackLimit).append(NL);
            codeBuffer.append(TAB).append(".limit locals ").append(localsLimit).append(NL);
            return this;
        }

        public MethodBuilder withInstructionProcessing() {
            initializeMethodContext();
            processInstructionSequence();
            finalizeMethod();
            return this;
        }

        public String buildJasminMethod() {
            return codeBuffer.toString();
        }

        private String buildMethodSignature() {
            String modifier = determineMethodModifier();
            String parameterSignature = buildParameterSignature();
            String returnTypeSignature = types.getJasminType(method.getReturnType());

            return String.format("\n.method %s%s(%s)%s%s",
                    modifier, methodName, parameterSignature, returnTypeSignature, NL);
        }

        private String determineMethodModifier() {
            if (methodName.equals("main")) {
                return "public static ";
            }
            return types.getModifier(method.getMethodAccessModifier());
        }

        private String buildParameterSignature() {
            StringBuilder paramsBuilder = new StringBuilder();
            for (Element param : method.getParams()) {
                paramsBuilder.append(generateParameterType(param));
            }
            return paramsBuilder.toString();
        }

        private String generateParameterType(Element param) {
            if (param.getType() instanceof ArrayType) {
                if (methodName.equals("main") && param.equals(method.getParams().get(0))) {
                    return "[Ljava/lang/String;";
                }
                Type elemType = ((ArrayType) param.getType()).getElementType();
                return "[" + types.getJasminType(elemType);
            } else if (param instanceof ArrayOperand) {
                Type elemType = ((ArrayOperand) param).getType();
                if (elemType instanceof ArrayType) {
                    elemType = ((ArrayType) elemType).getElementType();
                }
                return "[" + types.getJasminType(elemType);
            } else {
                return types.getJasminType(param.getType());
            }
        }

        private void initializeMethodContext() {
            currentMethod = method;
            maxStack = 0;
            stack = 0;
        }

        private void processInstructionSequence() {
            for (var inst : method.getInstructions()) {
                processLabelsForInstruction(inst);
                processInstructionCode(inst);
            }
        }

        private void processLabelsForInstruction(Instruction inst) {
            if (method.getLabels(inst) != null && !method.getLabels(inst).isEmpty()) {
                for (String label : method.getLabels(inst)) {
                    codeBuffer.append(label).append(":").append(NL);
                }
            }
        }

        private void processInstructionCode(Instruction inst) {
            String instCode = generators.apply(inst);
            String[] lines = instCode.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    codeBuffer.append(TAB).append(line).append(NL);
                }
            }
        }

        private void finalizeMethod() {
            codeBuffer.append(".end method").append(NL);
            currentMethod = null;
        }
    }

    private String generateAssign(AssignInstruction assign) {
        AssignInstructionBuilder builder = new AssignInstructionBuilder(
                assign, currentMethod, types, generators);

        return builder.withAssignmentGeneration()
                .buildAssignInstruction();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        ConstantValueHandler valueHandler = new ConstantValueHandler(literal);
        return valueHandler.generateOptimalInstruction();
    }

    // Inner class for handling constant value optimizations
    private class ConstantValueHandler {
        private final String value;
        private final Type type;

        public ConstantValueHandler(LiteralElement literal) {
            this.value = literal.getLiteral();
            this.type = literal.getType();
        }

        public String generateOptimalInstruction() {
            if (isIntegerType()) {
                return generateIntegerInstruction();
            } else if (isBooleanType()) {
                return generateBooleanInstruction();
            }
            return generateDefaultInstruction();
        }

        private boolean isIntegerType() {
            return type instanceof BuiltinType builtinType &&
                    builtinType.getKind() == BuiltinKind.INT32;
        }

        private boolean isBooleanType() {
            return type instanceof BuiltinType builtinType &&
                    builtinType.getKind() == BuiltinKind.BOOLEAN;
        }

        private String generateIntegerInstruction() {
            try {
                int intValue = Integer.parseInt(value);
                return selectOptimalIntegerInstruction(intValue);
            } catch (NumberFormatException e) {
                return generateDefaultInstruction();
            }
        }

        private String selectOptimalIntegerInstruction(int intValue) {
            if (intValue >= -1 && intValue <= 5) {
                return "iconst_" + (intValue == -1 ? "m1" : intValue) + NL;
            } else if (intValue >= -128 && intValue <= 127) {
                return "bipush " + intValue + NL;
            } else if (intValue >= -32768 && intValue <= 32767) {
                return "sipush " + intValue + NL;
            }
            return generateDefaultInstruction();
        }

        private String generateBooleanInstruction() {
            if (value.equals("1")) {
                return "iconst_1" + NL;
            } else if (value.equals("0")) {
                return "iconst_0" + NL;
            }
            return generateDefaultInstruction();
        }

        private String generateDefaultInstruction() {
            return "ldc " + value + NL;
        }
    }

    private String generateOperand(Operand operand) {
        var descriptor = currentMethod.getVarTable().get(operand.getName());
        Type type = operand.getType();
        if (descriptor == null) {
            if (type instanceof ArrayType) {
                return "aload_1" + NL;
            } else {
                return "aload_1" + NL;
            }
        }
        int reg = descriptor.getVirtualReg();
        return types.getLoadInstruction(type, reg, false) + NL;
    }

    private String generateArrayOperand(ArrayOperand arrayOp) {
        var code = new StringBuilder();
        if (arrayOp.isParameter()) {
            String name = arrayOp.getName();
            var descriptor = currentMethod.getVarTable().get(name);
            if (descriptor == null) {
                code.append("aload_1").append(NL);
            } else {
                int reg = descriptor.getVirtualReg();
                code.append(types.getLoadInstruction(arrayOp.getType(), reg, true)).append(NL);
            }
            return code.toString();
        }
        String name = arrayOp.getName();
        var descriptor = currentMethod.getVarTable().get(name);
        if (descriptor == null) {
            code.append("aload_2").append(NL);
        } else {
            int reg = descriptor.getVirtualReg();
            code.append(types.getLoadInstruction(arrayOp.getType(), reg, true)).append(NL);
        }
        if (arrayOp.getIndexOperands().isEmpty()) {
            return code.toString();
        }
        code.append(generators.apply(arrayOp.getIndexOperands().get(0)));
        Type arrayType = arrayOp.getType();
        Type elemType = null;
        if (arrayType instanceof ArrayType) {
            elemType = ((ArrayType) arrayType).getElementType();
        } else {
            elemType = new BuiltinType(BuiltinKind.INT32);
        }
        code.append(types.getArrayLoadPrefix(elemType)).append(NL);
        return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        BinaryOpInstructionBuilder builder = new BinaryOpInstructionBuilder(
                binaryOp, generators, this::getNextLabel);

        String result = builder.withOperandAnalysis()
                .buildBinaryOperation();

        updateStackSize(-1);
        return result;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        UnaryOpInstructionBuilder builder = new UnaryOpInstructionBuilder(
                unaryOp, generators, this::getNextLabel);

        return builder.withOperandLoading()
                .withOperationGeneration()
                .buildUnaryOpInstruction();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        ControlFlowInstructionBuilder builder = new ControlFlowInstructionBuilder(
                generators, types);

        return builder.withReturnGeneration(returnInst)
                .buildControlFlowInstruction();
    }

    private String generateCall(CallInstruction call) {
        CallInstructionBuilder builder = new CallInstructionBuilder(
                call, ollirResult, types, generators);

        return builder.withOperandLoading()
                .withMethodInvocation()
                .buildCallInstruction();
    }

    private String generateCondBranch(CondBranchInstruction condBranch) {
        CondBranchInstructionBuilder builder = new CondBranchInstructionBuilder(
                condBranch, generators);

        return builder.withConditionAnalysis()
                .buildCondBranchInstruction();
    }

    private String generateGoto(GotoInstruction gotoInst) {
        ControlFlowInstructionBuilder builder = new ControlFlowInstructionBuilder(
                generators, types);

        return builder.withGotoGeneration(gotoInst)
                .buildControlFlowInstruction();
    }

    private String generateGetField(GetFieldInstruction getField) {
        FieldAccessInstructionBuilder builder = new FieldAccessInstructionBuilder(
                generators, ollirResult, types);

        return builder.withGetFieldGeneration(getField)
                .buildFieldAccessInstruction();
    }

    private String generatePutField(PutFieldInstruction putField) {
        FieldAccessInstructionBuilder builder = new FieldAccessInstructionBuilder(
                generators, ollirResult, types);

        return builder.withPutFieldGeneration(putField)
                .buildFieldAccessInstruction();
    }

    private String generateNew(NewInstruction newInst) {
        var code = new StringBuilder();
        Type returnType = newInst.getReturnType();
        if (returnType instanceof ArrayType) {
            if (newInst.getOperands().size() > 0) {
                Element sizeElem = newInst.getOperands().get(1);
                code.append(generators.apply(sizeElem));
                code.append("newarray int");
                code.append(NL);
                return code.toString();
            } else {
                code.append("newarray int").append(NL);
                return code.toString();
            }
        } else {
            return generateCall(newInst);
        }
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLength) {
        ControlFlowInstructionBuilder builder = new ControlFlowInstructionBuilder(
                generators, types);

        return builder.withArrayLengthGeneration(arrayLength)
                .buildControlFlowInstruction();
    }

}