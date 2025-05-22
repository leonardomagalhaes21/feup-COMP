package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.type.*;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;



import java.util.*;

public class JasminGenerator {
    private final OllirResult ollirResult;
    private final JasminUtils jasminUtils;
    private final StringBuilder jasminCode;
    private final ClassUnit classUnit;
    private final List<Report> reports;
    private final Map<String, String> importFullNames;
    private static final String TAB = "    ";
    private static final String NL = "\n";

    // Track the current method being processed for limit calculation
    private Method currentMethod;
    private int maxStackSize;
    private int currentStackSize;
    private Map<String, Integer> labelCounter;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.jasminUtils = new JasminUtils(ollirResult);
        this.jasminCode = new StringBuilder();
        this.classUnit = ollirResult.getOllirClass();
        this.reports = new ArrayList<>();
        this.maxStackSize = 0;
        this.currentStackSize = 0;
        this.labelCounter = new HashMap<>();
        this.importFullNames = new HashMap<>();
    }

    public String build() {
        // Class declaration
        buildClassDecl();

        // Fields
        buildFields();

        // Constructor
        buildConstructor();

        // Methods
        buildMethods();

        return jasminCode.toString();
    }

    private void buildClassDecl() {
        // Class access modifier
        String classAccessModifier = jasminUtils.getModifier(classUnit.getClassAccessModifier());
        jasminCode.append(".class ").append(classAccessModifier).append(classUnit.getClassName()).append("\n");

        // Super class
        String superClass = classUnit.getSuperClass() != null ? classUnit.getSuperClass() : "java/lang/Object";
        jasminCode.append(".super ").append(superClass.replace(".", "/")).append("\n\n");
    }

    private void buildFields() {
        for (Field field : classUnit.getFields()) {
            String accessModifier = jasminUtils.getModifier(field.getFieldAccessModifier());
            String fieldType = jasminUtils.getJasminType(field.getFieldType());
            jasminCode.append(".field ").append(accessModifier).append(field.getFieldName())
                    .append(" ").append(fieldType).append("\n");
        }

        if (!classUnit.getFields().isEmpty()) {
            jasminCode.append("\n");
        }
    }

    private void buildConstructor() {
        boolean hasConstructor = false;
        for (Method method : classUnit.getMethods()) {
            if (method.isConstructMethod()) {
                hasConstructor = true;
                buildMethod(method); // Generate user-defined constructor
            }
        }
        if (!hasConstructor) {
            jasminCode.append(".method <init>()V\n");
            jasminCode.append("    .limit stack 1\n");
            jasminCode.append("    .limit locals 1\n");
            jasminCode.append("    aload_0\n");
            jasminCode.append("    invokespecial java/lang/Object/<init>()V\n");
            jasminCode.append("    return\n");
            jasminCode.append(".end method\n\n");
        }
    }


    private void buildMethods() {
        for (Method method : classUnit.getMethods()) {
            if (!method.isConstructMethod()) {
                buildMethod(method);
            }
        }
    }

    private String getMethodAccessModifier(Method method) {
        var modifier = method.getMethodAccessModifier();
        if (modifier == AccessModifier.DEFAULT) {
            return "public ";
        }
        return jasminUtils.getModifier(modifier);
    }




    private void buildMethod(Method method) {
        currentMethod = method;
        maxStackSize = 0;
        currentStackSize = 0;

        String accessModifier = getMethodAccessModifier(method);
        String staticModifier = method.isStaticMethod() ? "static " : "";
        String methodName = method.isConstructMethod() ? "<init>" : method.getMethodName();
        String returnType = jasminUtils.getJasminType(method.getReturnType());

        jasminCode.append(".method ")
                .append(accessModifier)
                .append(staticModifier)
                .append(methodName)
                .append("(");

        // Only append parameters if not a default constructor
        if (!(method.isConstructMethod() && method.getParams().isEmpty())) {
            for (Element param : method.getParams()) {
                jasminCode.append(jasminUtils.getJasminType(param.getType()));
            }
        }

        jasminCode.append(")").append(returnType).append("\n");

        int limitLocals = calculateLimitLocals(method);
        calculateStackSize(method);

        jasminCode.append("    .limit stack ").append(Math.max(2, maxStackSize)).append("\n");
        jasminCode.append("    .limit locals ").append(limitLocals).append("\n");

        // Special handling for default constructor
        if (method.isConstructMethod() && method.getParams().isEmpty()) {
            jasminCode.append("    aload_0\n");
            jasminCode.append("    invokespecial java/lang/Object/<init>()V\n");
            jasminCode.append("    return\n");
        } else {
            generateInstructions(method);
        }

        jasminCode.append(".end method\n\n");
    }


    private void addImportFullNames(ClassUnit classUnit) {
        for (var imp : classUnit.getImports()) {
            String importNonQualified = imp.substring(imp.lastIndexOf(".") + 1);
            imp = imp.replace(".", "/");
            importFullNames.put(importNonQualified, imp);
        }
    }



    private String formatJasmin(String code) {
        var lines = code.split("\n");
        var formatted = new StringBuilder();
        var indent = 0;
        for (var line : lines) {
            if (line.startsWith(".end")) {
                indent--;
            }
            formatted.append(TAB.repeat(Math.max(0, indent))).append(line).append(NL);
            if (line.startsWith(".method")) {
                indent++;
            }
        }
        System.out.println(formatted.toString());
        return formatted.toString();
    }


    private void generateInstructions(Method method) {
        boolean hasReturn = false;
        for (Instruction instruction : method.getInstructions()) {
            // Get labels from method's label map
            for (String label : method.getLabels(instruction)) {
                generateLabelInstruction(label);
            }
            if (instruction instanceof ReturnInstruction) {
                hasReturn = true;
            }

            switch (instruction.getInstType()) {
                case CALL -> generateCallInstruction((CallInstruction) instruction);
                case ASSIGN -> generateAssignInstruction((AssignInstruction) instruction);
                case RETURN -> generateReturnInstruction((ReturnInstruction) instruction);
                case BINARYOPER -> generateBinaryOperInstruction((BinaryOpInstruction) instruction);
                case BRANCH -> generateBranchInstruction((CondBranchInstruction) instruction);
                case GOTO -> generateGotoInstruction((GotoInstruction) instruction);
                case PUTFIELD -> generatePutFieldInstruction((PutFieldInstruction) instruction);
                case GETFIELD -> generateGetFieldInstruction((GetFieldInstruction) instruction);
                case NOPER -> { /* No operation */ }
                default -> jasminCode.append("    ; Unhandled instruction type: ").append(instruction.getInstType()).append("\n");
            }
        }
        if (!hasReturn
                && method.getReturnType() instanceof BuiltinType builtin
                && builtin.getKind() == BuiltinKind.VOID) {
            jasminCode.append("    return\n");
        }
    }



    private void generateCallInstruction(CallInstruction call) {
        String methodName;
        Element methodElem = call.getMethodName();
        if (methodElem instanceof Operand operand) {
            methodName = operand.getName();
        } else if (methodElem instanceof LiteralElement literal) {
            methodName = literal.getLiteral();
        } else {
            jasminCode.append("    ;Unknown method name type\n");
            return;
        }


        Element firstOperand = call.getOperands().get(0);
        List<Element> args = call.getOperands().subList(1, call.getOperands().size());

        // Detect static call to io.print/println
        boolean isStatic = firstOperand instanceof Operand
                && firstOperand.getType() instanceof ClassType
                && !methodName.equals("<init>");
        ClassType classType = isStatic ? (ClassType) firstOperand.getType() : null;

        if (isStatic && classType != null && classType.getName().equals("io")
                && (methodName.equals("print") || methodName.equals("println"))) {
            jasminCode.append("    getstatic java/lang/System/out Ljava/io/PrintStream;\n");
            generateElementCode(args.get(0));
            String argType = jasminUtils.getJasminType(args.get(0).getType());
            jasminCode.append("    invokevirtual java/io/PrintStream/")
                    .append(methodName)
                    .append("(")
                    .append(argType)
                    .append(")V\n");
            return;
        }

        // Load arguments
        if (!isStatic) {
            generateElementCode(firstOperand);
        }
        for (Element arg : args) {
            generateElementCode(arg);
        }

        // Build method signature
        StringBuilder sig = new StringBuilder();
        sig.append("(");
        for (Element arg : args) {
            sig.append(jasminUtils.getJasminType(arg.getType()));
        }
        sig.append(")").append(jasminUtils.getJasminType(call.getReturnType()));

        // Determine class name for method call
        String className;
        if (isStatic && classType != null) {
            className = classType.getName();
        } else if (methodName.equals("<init>")) {
            className = "java/lang/Object";
        } else {
            className = classUnit.getClassName();
        }

        // Generate appropriate invocation instruction
        if (isStatic) {
            jasminCode.append("    invokestatic ");
        } else if (methodName.equals("<init>")) {
            jasminCode.append("    invokespecial ");
        } else {
            jasminCode.append("    invokevirtual ");
        }

        jasminCode.append(className.replace('.', '/'))
                .append("/")
                .append(methodName)
                .append(sig)
                .append("\n");
    }

    private void generateBranchInstruction(CondBranchInstruction branch) {
        Instruction condition = branch.getCondition();

        // Generate condition code first
        if (condition instanceof SingleOpInstruction singleOp) {
            generateElementCode(singleOp.getSingleOperand());
        } else if (condition instanceof BinaryOpInstruction binOp) {
            generateBinaryOperInstruction(binOp);
        }

        // Generate branch
        String jumpLabel = branch.getLabel();
        jasminCode.append("    ifne ").append(jumpLabel).append("\n");
    }

    private void generateLabelInstruction(String label) {
        jasminCode.append(label).append(":\n");
    }

    private void generateGotoInstruction(GotoInstruction gotoInst) {
        jasminCode.append("    goto ").append(gotoInst.getLabel()).append("\n");
    }



    private void generateAssignInstruction(AssignInstruction assign) {
        String varName = ((Operand) assign.getDest()).getName();
        var varDesc = currentMethod.getVarTable().get(varName);

        // Check if destination is a field (not in var table)
        boolean isField = varDesc == null;

        Instruction rhs = assign.getRhs();

        if (isField) {
            // Field assignment: this.varName = <rhs>
            jasminCode.append("    aload_0\n");
            if (rhs instanceof SingleOpInstruction singleOp) {
                generateElementCode(singleOp.getSingleOperand());
            } else if (rhs instanceof BinaryOpInstruction bin) {
                generateBinaryOperInstruction(bin);
            }
            String fieldType = jasminUtils.getJasminType(assign.getDest().getType());
            jasminCode.append("    putfield ")
                    .append(classUnit.getClassName()).append("/")
                    .append(varName).append(" ")
                    .append(fieldType).append("\n");
        } else {
            int varIndex = varDesc.getVirtualReg();

            // Optimization: iinc for var = var + const
            if (rhs instanceof BinaryOpInstruction bin
                    && bin.getOperation().getOpType() == OperationType.ADD
                    && bin.getLeftOperand() instanceof Operand left
                    && bin.getRightOperand() instanceof LiteralElement rightLit
                    && left.getName().equals(varName)) {
                try {
                    int incValue = Integer.parseInt(rightLit.getLiteral());
                    jasminCode.append("    iinc ").append(varIndex).append(" ").append(incValue).append("\n");
                    return;
                } catch (NumberFormatException ignored) {
                    // Fallback to normal codegen if not a valid int
                }
            }

            if (rhs instanceof SingleOpInstruction singleOp) {
                Element elem = singleOp.getSingleOperand();
                generateElementCode(elem);
                if (assign.getDest().getType() instanceof ArrayType
                        || assign.getDest().getType() instanceof ClassType
                        || (assign.getDest().getType() instanceof BuiltinType bt && bt.getKind() == BuiltinKind.STRING)) {
                    jasminCode.append("    astore_").append(varIndex).append("\n");
                } else {
                    jasminCode.append("    istore_").append(varIndex).append("\n");
                }
            } else if (rhs instanceof BinaryOpInstruction bin) {
                generateBinaryOperInstruction(bin);
                jasminCode.append("    istore_").append(varIndex).append("\n");
            } else {
                jasminCode.append("    ;TODO: handle assign rhs type\n");
            }
        }
    }


    // Helper to load an element (literal, local, or field)
    private void generateElementCode(Element elem) {
        if (elem instanceof LiteralElement literal) {
            String lit = literal.getLiteral();
            try {
                int value = Integer.parseInt(lit);
                if (value >= -1 && value <= 5) {
                    jasminCode.append("    iconst_").append(value).append("\n");
                } else if (value >= -128 && value <= 127) {
                    jasminCode.append("    bipush ").append(value).append("\n");
                } else if (value >= -32768 && value <= 32767) {
                    jasminCode.append("    sipush ").append(value).append("\n");
                } else {
                    jasminCode.append("    ldc ").append(value).append("\n");
                }
            } catch (NumberFormatException e) {
                if ("null".equals(lit)) {
                    jasminCode.append("    aconst_null\n");
                } else {
                    jasminCode.append("    ldc \"").append(lit).append("\"\n");
                }
            }
        } else if (elem instanceof Operand operand) {
            var desc = currentMethod.getVarTable().get(operand.getName());
            if (desc != null) {
                int idx = desc.getVirtualReg();
                if (elem.getType() instanceof ArrayType
                        || elem.getType() instanceof ClassType
                        || (elem.getType() instanceof BuiltinType bt && bt.getKind() == BuiltinKind.STRING)) {
                    jasminCode.append("    aload_").append(idx).append("\n");
                } else {
                    jasminCode.append("    iload_").append(idx).append("\n");
                }
            } else {
                // Field access
                jasminCode.append("    aload_0\n")
                        .append("    getfield ")
                        .append(classUnit.getClassName()).append("/")
                        .append(operand.getName()).append(" ")
                        .append(jasminUtils.getJasminType(operand.getType()))
                        .append("\n");
            }
        }
    }

    private void generateGetFieldInstruction(GetFieldInstruction instruction) {
        // Load the object reference
        Element objectElem = instruction.getOperands().get(0);
        generateElementCode(objectElem);

        // Get the field name and type
        String fieldName = ((Operand) instruction.getOperands().get(1)).getName();
        Type fieldType = instruction.getFieldType();
        String fieldTypeStr = jasminUtils.getJasminType(fieldType);

        // Get class name
        String className = objectElem.getType() instanceof ClassType ?
                ((ClassType) objectElem.getType()).getName() :
                classUnit.getClassName();

        // Generate getfield instruction
        jasminCode.append("    getfield ")
                .append(className.replace('.', '/'))
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldTypeStr)
                .append("\n");
    }

    private void generatePutFieldInstruction(PutFieldInstruction instruction) {
        // Load object reference (first operand is the object)
        Element objectElem = instruction.getOperands().get(0);
        generateElementCode(objectElem);

        // Load value to store (second operand is the value)
        Element valueElem = instruction.getOperands().get(2);
        generateElementCode(valueElem);

        // Get field info
        String fieldName = ((Operand) instruction.getOperands().get(1)).getName();
        Type fieldType = instruction.getFieldType();
        String fieldTypeStr = jasminUtils.getJasminType(fieldType);

        // Get class name
        String className = objectElem.getType() instanceof ClassType ?
                ((ClassType) objectElem.getType()).getName() :
                classUnit.getClassName();

        // Generate putfield instruction
        jasminCode.append("    putfield ")
                .append(className.replace('.', '/'))
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldTypeStr)
                .append("\n");
    }


    private void generateReturnInstruction(ReturnInstruction ret) {
        if (ret.getOperand().isPresent()) {
            Element elem = ret.getOperand().get();
            if (elem instanceof Operand operand) {
                var desc = currentMethod.getVarTable().get(operand.getName());
                if (desc != null) {
                    int index = desc.getVirtualReg();
                    if (elem.getType() instanceof ArrayType) {
                        jasminCode.append("    aload_").append(index).append("\n    areturn\n");
                    } else if (elem.getType() instanceof BuiltinType builtinType) {
                        switch (builtinType.getKind()) {
                            case INT32, BOOLEAN -> jasminCode.append("    iload_").append(index).append("\n    ireturn\n");
                            case STRING -> jasminCode.append("    aload_").append(index).append("\n    areturn\n");
                            case VOID -> jasminCode.append("    return\n");
                        }
                    } else if (elem.getType() instanceof ClassType) {
                        jasminCode.append("    aload_").append(index).append("\n    areturn\n");
                    }
                } else {
                    // Could be a field or error, fallback to field access or error handling
                    jasminCode.append("    // Field or unknown variable: ").append(operand.getName()).append("\n");
                }
            } else if (elem instanceof LiteralElement literal) {
                String lit = literal.getLiteral();
                try {
                    int value = Integer.parseInt(lit);
                    if (value >= -1 && value <= 5) {
                        jasminCode.append("    iconst_").append(value).append("\n");
                    } else if (value >= -128 && value <= 127) {
                        jasminCode.append("    bipush ").append(value).append("\n");
                    } else if (value >= -32768 && value <= 32767) {
                        jasminCode.append("    sipush ").append(value).append("\n");
                    } else {
                        jasminCode.append("    ldc ").append(value).append("\n");
                    }
                    jasminCode.append("    ireturn\n");
                } catch (NumberFormatException e) {
                    if ("null".equals(lit)) {
                        jasminCode.append("    aconst_null\n    areturn\n");
                    } else {
                        jasminCode.append("    ldc \"").append(lit).append("\"\n    areturn\n");
                    }
                }
            }
        } else {
            jasminCode.append("    return\n");
        }
    }

    private void generateBinaryOperInstruction(BinaryOpInstruction bin) {
        Element left = bin.getLeftOperand();
        Element right = bin.getRightOperand();

        generateElementCode(left);
        generateElementCode(right);

        switch (bin.getOperation().getOpType()) {
            case ADD -> jasminCode.append("    iadd\n");
            case SUB -> jasminCode.append("    isub\n");
            case MUL -> jasminCode.append("    imul\n");
            case DIV -> jasminCode.append("    idiv\n");
            case AND -> jasminCode.append("    iand\n");
            case OR -> jasminCode.append("    ior\n");
            case LTH -> {
                String trueLabel = getNextLabel();
                String endLabel = getNextLabel();
                jasminCode.append("    if_icmplt ").append(trueLabel).append("\n");
                jasminCode.append("    iconst_0\n");
                jasminCode.append("    goto ").append(endLabel).append("\n");
                jasminCode.append(trueLabel).append(":\n");
                jasminCode.append("    iconst_1\n");
                jasminCode.append(endLabel).append(":\n");
            }
            case GTE -> {
                String trueLabel = getNextLabel();
                String endLabel = getNextLabel();
                jasminCode.append("    if_icmpge ").append(trueLabel).append("\n");
                jasminCode.append("    iconst_0\n");
                jasminCode.append("    goto ").append(endLabel).append("\n");
                jasminCode.append(trueLabel).append(":\n");
                jasminCode.append("    iconst_1\n");
                jasminCode.append(endLabel).append(":\n");
            }
            case LTE -> {
                String trueLabel = getNextLabel();
                String endLabel = getNextLabel();
                jasminCode.append("    if_icmple ").append(trueLabel).append("\n");
                jasminCode.append("    iconst_0\n");
                jasminCode.append("    goto ").append(endLabel).append("\n");
                jasminCode.append(trueLabel).append(":\n");
                jasminCode.append("    iconst_1\n");
                jasminCode.append(endLabel).append(":\n");
            }
            case EQ -> {
                String trueLabel = getNextLabel();
                String endLabel = getNextLabel();
                jasminCode.append("    if_icmpeq ").append(trueLabel).append("\n");
                jasminCode.append("    iconst_0\n");
                jasminCode.append("    goto ").append(endLabel).append("\n");
                jasminCode.append(trueLabel).append(":\n");
                jasminCode.append("    iconst_1\n");
                jasminCode.append(endLabel).append(":\n");
            }
            case NEQ -> {
                String trueLabel = getNextLabel();
                String endLabel = getNextLabel();
                jasminCode.append("    if_icmpne ").append(trueLabel).append("\n");
                jasminCode.append("    iconst_0\n");
                jasminCode.append("    goto ").append(endLabel).append("\n");
                jasminCode.append(trueLabel).append(":\n");
                jasminCode.append("    iconst_1\n");
                jasminCode.append(endLabel).append(":\n");
            }
            default -> {}
        }
    }
    private int calculateLimitLocals(Method method) {
        // If this is an instance method, register 0 is used for 'this'
        int count = method.isStaticMethod() ? 0 : 1;

        // Parameters
        count += method.getParams().size();

        // Local variables
        int maxVarIndex = -1;
        for (Descriptor descriptor : method.getVarTable().values()) {
            int varIndex = descriptor.getVirtualReg();
            maxVarIndex = Math.max(maxVarIndex, varIndex);
        }

        // The total number of locals is max index + 1
        return Math.max(count, maxVarIndex + 1);
    }

    private void calculateStackSize(Method method) {


        // Count operations that affect stack
        int stackNeeded = 0;
        for (Instruction instruction : method.getInstructions()) {
            if (instruction instanceof CallInstruction call) {
                // Method calls typically need operands + 1 for possible return value
                stackNeeded = Math.max(stackNeeded, call.getOperands().size() + 1);
            } else if (instruction instanceof BinaryOpInstruction) {
                // Binary operations need 2 operands and produce 1 result
                stackNeeded = Math.max(stackNeeded, 2);
            }
        }

        // Add safety margin
        maxStackSize = Math.max(2, stackNeeded);
    }

    private void analyzeInstructionStack(Instruction instruction) {
        InstructionType instType = instruction.getInstType();

        if (instType == InstructionType.ASSIGN) {
            analyzeAssignStack((AssignInstruction) instruction);
        } else if (instType == InstructionType.CALL) {
            analyzeCallStack((CallInstruction) instruction);
        } else if (instType == InstructionType.GOTO) {
            // No stack effect
        } else if (instType == InstructionType.BRANCH) {
            analyzeBranchStack((CondBranchInstruction) instruction);
        } else if (instType == InstructionType.RETURN) {
            analyzeReturnStack((ReturnInstruction) instruction);
        } else if (instType == InstructionType.PUTFIELD) {
            analyzePutFieldStack((PutFieldInstruction) instruction);
        } else if (instType == InstructionType.GETFIELD) {
            analyzeGetFieldStack((GetFieldInstruction) instruction);
        } else if (instType == InstructionType.UNARYOPER) {
            analyzeUnaryOperStack((UnaryOpInstruction) instruction);
        } else if (instType == InstructionType.BINARYOPER) {
            analyzeBinaryOperStack((BinaryOpInstruction) instruction);
        } else if (instType == InstructionType.NOPER) {
            // No operation, no stack effect
        }
    }

    private void analyzeReturnStack(ReturnInstruction instruction) {
        // Return instructions typically need 1 stack slot for the return value
        // (except for void returns)
        if (instruction.hasReturnValue()) {
            updateStackLimit(1);
        }
    }

    private void analyzeCallStack(CallInstruction instruction) {
        // Count operands (including target object for instance methods)
        int operandCount = instruction.getOperands().size();

        // If it's not a static call, we need an extra slot for 'this'
        Element firstOperand = instruction.getOperands().get(0);
        boolean isStatic = firstOperand instanceof Operand
                && firstOperand.getType() instanceof ClassType
                && !((Operand) instruction.getMethodName()).getName().equals("<init>");

        if (!isStatic) {
            operandCount++;
        }

        updateStackLimit(operandCount);

        // If the result is used, we need an extra slot
        if (!(instruction.getReturnType() instanceof BuiltinType) ||
                ((BuiltinType) instruction.getReturnType()).getKind() != BuiltinKind.VOID) {
            updateStackLimit(1);
        }
    }

    private void analyzeBinaryOperStack(BinaryOpInstruction instruction) {
        // Binary operations need 2 operands and produce 1 result
        updateStackLimit(2);
    }

    private void analyzeUnaryOperStack(UnaryOpInstruction instruction) {
        // Unary operations need 1 operand and produce 1 result
        updateStackLimit(1);
    }

    private void analyzeGetFieldStack(GetFieldInstruction instruction) {
        // Need 1 slot for the object reference, produces 1 result
        updateStackLimit(2);
    }

    private void analyzePutFieldStack(PutFieldInstruction instruction) {
        // Need 1 slot for the object reference and 1 for the value
        updateStackLimit(2);
    }

    private void analyzeAssignStack(AssignInstruction instruction) {
        // Analyze the right-hand side of the assignment
        Instruction rhs = instruction.getRhs();

        if (rhs instanceof SingleOpInstruction) {
            // Loading literals or variables needs 1 stack slot
            updateStackLimit(1);
        } else if (rhs instanceof BinaryOpInstruction) {
            // Binary operations need 2 operands
            analyzeBinaryOperStack((BinaryOpInstruction) rhs);
        } else if (rhs instanceof UnaryOpInstruction) {
            // Unary operations need 1 operand
            analyzeUnaryOperStack((UnaryOpInstruction) rhs);
        } else if (rhs instanceof CallInstruction) {
            analyzeCallStack((CallInstruction) rhs);
        }
    }
    private void analyzeBranchStack(CondBranchInstruction instruction) {
        // For conditional branches, analyze the condition
        Instruction condition = instruction.getCondition();

        if (condition instanceof SingleOpInstruction) {
            // Simple condition needs 1 stack slot
            updateStackLimit(1);
        } else if (condition instanceof BinaryOpInstruction) {
            // Binary condition needs 2 stack slots
            updateStackLimit(2);
        } else {
            // Other operations - safe default
            updateStackLimit(2);
        }
    }



    private void updateStackLimit(int requiredSlots) {
        if (requiredSlots > maxStackSize) {
            maxStackSize = requiredSlots;
        }
    }






    // Helper methods for label generation
    private String getNextLabel() {
        String prefix = "label";
        int count = labelCounter.getOrDefault(prefix, 0) + 1;
        labelCounter.put(prefix, count);
        return prefix + count;
    }

    // Return reports for errors/warnings
    public List<Report> getReports() {
        return reports;
    }
}