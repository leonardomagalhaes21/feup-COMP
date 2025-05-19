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
            // Example: handle CALL instructions
            if (instruction.getInstType() == InstructionType.CALL) {
                CallInstruction call = (CallInstruction) instruction;
                // Generate code for loading arguments, then:
                jasminCode.append("    invokevirtual ArrayAsArg/yourMethod([I)I\n");
            }
            // Handle other instruction types similarly


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