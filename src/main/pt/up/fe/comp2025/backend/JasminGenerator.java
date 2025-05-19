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
        // Check if there's already a constructor
        boolean hasConstructor = false;
        for (Method method : classUnit.getMethods()) {
            if (method.isConstructMethod()) {
                hasConstructor = true;
                break;
            }
        }

        if (!hasConstructor) {
            jasminCode.append("; Standard constructor\n");
            jasminCode.append(".method public <init>()V\n");
            jasminCode.append("    aload_0\n");
            jasminCode.append("    invokespecial ");

            String superClass = classUnit.getSuperClass() != null ? classUnit.getSuperClass() : "java/lang/Object";
            jasminCode.append(superClass.replace(".", "/")).append("/<init>()V\n");
            jasminCode.append("    return\n");
            jasminCode.append(".end method\n\n");
        }
    }

    private void buildMethods() {
        for (Method method : classUnit.getMethods()) {
            buildMethod(method);
        }
    }

    private void buildMethod(Method method) {
        currentMethod = method;
        maxStackSize = 0;
        currentStackSize = 0;

        // Method signature
        String accessModifier = jasminUtils.getModifier(method.getMethodAccessModifier());
        String methodName = method.isConstructMethod() ? "<init>" : method.getMethodName();
        String returnType = jasminUtils.getJasminType(method.getReturnType());

        jasminCode.append(".method ").append(accessModifier).append(methodName).append("(");

        // Parameters
        for (Element param : method.getParams()) {
            jasminCode.append(jasminUtils.getJasminType(param.getType()));
        }

        jasminCode.append(")").append(returnType).append("\n");

        // Calculate limits before processing instructions
        int limitLocals = calculateLimitLocals(method);

        // Process instructions to calculate stack size
        calculateStackSize(method);

        // Add the limits
        jasminCode.append("    .limit stack ").append(maxStackSize).append("\n");
        jasminCode.append("    .limit locals ").append(limitLocals).append("\n");

        // Method instructions
        generateInstructions(method);

        jasminCode.append(".end method\n\n");
    }

    private void generateInstructions(Method method) {
        for (Instruction instruction : method.getInstructions()) {
            switch (instruction.getInstType()) {
                case CALL -> generateCallInstruction((CallInstruction) instruction);
                case ASSIGN -> generateAssignInstruction((AssignInstruction) instruction);
                case RETURN -> generateReturnInstruction((ReturnInstruction) instruction);
                case BINARYOPER -> generateBinaryOperInstruction((BinaryOpInstruction) instruction);
                // ... other cases ...
                default -> jasminCode.append("    // TODO: handle ").append(instruction.getInstType()).append("\n");
            }
        }
    }

    private void generateCallInstruction(CallInstruction call) {
        // Get method name safely
        String methodName;
        Element methodElem = call.getMethodName();
        if (methodElem instanceof Operand operand) {
            methodName = operand.getName();
        } else if (methodElem instanceof LiteralElement literal) {
            methodName = literal.getLiteral();
        } else {
            jasminCode.append("    // TODO: handle unknown method name type\n");
            return;
        }

        // Get class name from first operand
        Element firstOperand = call.getOperands().get(0);
        String className = ((ClassType) firstOperand.getType()).getName();
        List<Element> args = call.getOperands().subList(1, call.getOperands().size());

        // Load object reference (for instance methods)
        jasminCode.append("    aload_0\n");

        // Load arguments (use variable table for correct indices)
        for (Element arg : args) {
            if (arg instanceof Operand operand) {
                int index = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                if (arg.getType() instanceof ArrayType) {
                    jasminCode.append("    aload_").append(index).append("\n");
                } else if (arg.getType() instanceof BuiltinType builtinType) {
                    switch (builtinType.getKind()) {
                        case INT32, BOOLEAN -> jasminCode.append("    iload_").append(index).append("\n");
                        default -> jasminCode.append("    // TODO: handle type\n");
                    }
                }
            }
            else if (arg instanceof LiteralElement literal) {
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
                    // Not a number, handle as string or skip
                    jasminCode.append("    ldc \"").append(lit).append("\"\n");
                }
            }
            else {
                jasminCode.append("    // TODO: handle arg type\n");
            }
        }

        // Build method signature
        StringBuilder sig = new StringBuilder();
        sig.append("(");
        for (Element arg : args) {
            sig.append(jasminUtils.getJasminType(arg.getType()));
        }
        sig.append(")");
        sig.append(jasminUtils.getJasminType(call.getReturnType()));

        jasminCode.append("    invokevirtual ")
                .append(className)
                .append("/")
                .append(methodName)
                .append(sig)
                .append("\n");
    }

    private void generateAssignInstruction(AssignInstruction assign) {
        // Left: variable name
        String varName = ((Operand) assign.getDest()).getName();
        int varIndex = currentMethod.getVarTable().get(varName).getVirtualReg();;

        // Right: SingleOpInstruction or BinaryOpInstruction
        Instruction rhs = assign.getRhs();
        if (rhs instanceof SingleOpInstruction singleOp) {
            Element elem = singleOp.getSingleOperand();
            if (elem instanceof LiteralElement literal) {
                int value = Integer.parseInt(literal.getLiteral());
                if (value >= -1 && value <= 5) {
                    jasminCode.append("    iconst_").append(value).append("\n");
                } else if (value >= -128 && value <= 127) {
                    jasminCode.append("    bipush ").append(value).append("\n");
                } else if (value >= -32768 && value <= 32767) {
                    jasminCode.append("    sipush ").append(value).append("\n");
                } else {
                    jasminCode.append("    ldc ").append(value).append("\n");
                }
            } else if (elem instanceof Operand operand) {
                int srcIndex = currentMethod.getVarTable().get(varName).getVirtualReg();
                if (elem.getType() instanceof ArrayType) {
                    jasminCode.append("    aload_").append(srcIndex).append("\n");
                    jasminCode.append("    astore_").append(varIndex).append("\n");
                    return;
                } else {
                    jasminCode.append("    iload_").append(srcIndex).append("\n");
                }
            }
            // Store result
            if (assign.getDest().getType() instanceof ArrayType) {
                jasminCode.append("    astore_").append(varIndex).append("\n");
            } else {
                jasminCode.append("    istore_").append(varIndex).append("\n");
            }
        } else if (rhs instanceof BinaryOpInstruction bin) {
            generateBinaryOperInstruction(bin);
            jasminCode.append("    istore_").append(varIndex).append("\n");
        } else {
            jasminCode.append("    // TODO: handle assign rhs type\n");
        }
    }

    private void generateReturnInstruction(ReturnInstruction ret) {
        if (ret.getOperand().isPresent()) {
            Element elem = ret.getOperand().get();
            if (elem instanceof Operand operand) {
                int index = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                if (elem.getType() instanceof ArrayType) {
                    jasminCode.append("    aload_").append(index).append("\n    areturn\n");
                } else if (elem.getType() instanceof BuiltinType builtinType) {
                    switch (builtinType.getKind()) {
                        case INT32, BOOLEAN -> jasminCode.append("    iload_").append(index).append("\n    ireturn\n");
                        default -> jasminCode.append("    // TODO: handle return type\n");
                    }
                }
            } else if (elem instanceof LiteralElement literal) {
                int value = Integer.parseInt(literal.getLiteral());
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
            }
        } else {
            jasminCode.append("    return\n");
        }
    }

    private void generateBinaryOperInstruction(BinaryOpInstruction bin) {
        // Get left and right operands
        Element left = bin.getLeftOperand();
        Element right = bin.getRightOperand();

        // Load left operand
        if (left instanceof Operand operand) {
            int index = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            jasminCode.append("    iload_").append(index).append("\n");
        } else if (left instanceof LiteralElement literal) {
            int value = Integer.parseInt(literal.getLiteral());
            if (value >= -1 && value <= 5) {
                jasminCode.append("    iconst_").append(value).append("\n");
            } else if (value >= -128 && value <= 127) {
                jasminCode.append("    bipush ").append(value).append("\n");
            } else if (value >= -32768 && value <= 32767) {
                jasminCode.append("    sipush ").append(value).append("\n");
            } else {
                jasminCode.append("    ldc ").append(value).append("\n");
            }
        }

        // Load right operand
        if (right instanceof Operand operand) {
            int index = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            jasminCode.append("    iload_").append(index).append("\n");
        } else if (right instanceof LiteralElement literal) {
            int value = Integer.parseInt(literal.getLiteral());
            if (value >= -1 && value <= 5) {
                jasminCode.append("    iconst_").append(value).append("\n");
            } else if (value >= -128 && value <= 127) {
                jasminCode.append("    bipush ").append(value).append("\n");
            } else if (value >= -32768 && value <= 32767) {
                jasminCode.append("    sipush ").append(value).append("\n");
            } else {
                jasminCode.append("    ldc ").append(value).append("\n");
            }
        }

        // Emit operation
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
            // Add more as needed
            default -> jasminCode.append("    // TODO: handle binary op\n");
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
        // Reset stack counters
        maxStackSize = 0;
        currentStackSize = 0;

        // We'll analyze each instruction to determine the stack effect
        for (Instruction instruction : method.getInstructions()) {
            analyzeInstructionStack(instruction);
        }
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

    }

    private void analyzeCallStack(CallInstruction instruction) {
        
    }

    private void analyzeBinaryOperStack(BinaryOpInstruction instruction) {
    }

    private void analyzeUnaryOperStack(UnaryOpInstruction instruction) {
        
    }

    private void analyzeGetFieldStack(GetFieldInstruction instruction) {
        
    }

    private void analyzePutFieldStack(PutFieldInstruction instruction) {
        
    }

    private void analyzeAssignStack(AssignInstruction instruction) {
        
    }

    private void analyzeBranchStack(CondBranchInstruction instruction) {
        
    }



    private void updateStackSize(int delta) {
        currentStackSize += delta;
        if (currentStackSize < 0) {
            currentStackSize = 0; // Stack can't be negative
        }
        maxStackSize = Math.max(maxStackSize, currentStackSize);
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