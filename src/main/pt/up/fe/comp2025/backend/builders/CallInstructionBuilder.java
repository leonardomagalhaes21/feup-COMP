package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.inst.CallInstruction;
import org.specs.comp.ollir.inst.InvokeVirtualInstruction;
import org.specs.comp.ollir.inst.InvokeSpecialInstruction;
import org.specs.comp.ollir.inst.InvokeStaticInstruction;
import org.specs.comp.ollir.inst.NewInstruction;
import org.specs.comp.ollir.type.Type;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.type.ArrayType;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2025.backend.JasminUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import org.specs.comp.ollir.tree.TreeNode;

/**
 * Builder for generating Jasmin code for call instructions.
 */
public class CallInstructionBuilder {
    private static final String NL = "\n";

    private final CallInstruction instruction;
    private final OllirResult ollirResult;
    private final JasminUtils types;
    private final FunctionClassMap<TreeNode, String> generators;
    private final StringBuilder codeBuffer;

    public CallInstructionBuilder(CallInstruction instruction, OllirResult ollirResult,
            JasminUtils types, FunctionClassMap<TreeNode, String> generators) {
        this.instruction = instruction;
        this.ollirResult = ollirResult;
        this.types = types;
        this.generators = generators;
        this.codeBuffer = new StringBuilder();
    }

    public CallInstructionBuilder withOperandLoading() {
        if (instruction instanceof InvokeVirtualInstruction || instruction instanceof InvokeSpecialInstruction) {
            loadInstanceMethodOperands();
        } else if (instruction instanceof InvokeStaticInstruction) {
            loadStaticMethodOperands();
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction();
        }
        return this;
    }

    public CallInstructionBuilder withMethodInvocation() {
        if (instruction instanceof InvokeVirtualInstruction || instruction instanceof InvokeSpecialInstruction) {
            generateInstanceMethodCall();
        } else if (instruction instanceof InvokeStaticInstruction) {
            generateStaticMethodCall();
        } else if (instruction instanceof NewInstruction) {
            generateNewObjectCall();
        }
        return this;
    }

    public String buildCallInstruction() {
        return codeBuffer.toString();
    }

    private void loadInstanceMethodOperands() {
        Element caller = instruction.getOperands().get(0);
        codeBuffer.append(generators.apply(caller));

        // Load method arguments (skip caller and method name)
        for (int i = 2; i < instruction.getOperands().size(); i++) {
            Element arg = instruction.getOperands().get(i);
            codeBuffer.append(generators.apply(arg));
        }
    }

    private void loadStaticMethodOperands() {
        // Load method arguments (skip class and method name)
        for (int i = 2; i < instruction.getOperands().size(); i++) {
            Element arg = instruction.getOperands().get(i);
            codeBuffer.append(generators.apply(arg));
        }
    }

    private void handleNewInstruction() {
        Type returnType = instruction.getReturnType();
        if (returnType instanceof ArrayType) {
            if (instruction.getOperands().size() > 0) {
                Element sizeElem = instruction.getOperands().get(0);
                codeBuffer.append(generators.apply(sizeElem));
            }
        }
    }

    private void generateInstanceMethodCall() {
        String invokeType = instruction instanceof InvokeVirtualInstruction ? "invokevirtual" : "invokespecial";
        String className = resolveInstanceClassName();
        String methodName = resolveMethodName();
        String parameterSignature = buildParameterSignature();
        String returnType = types.getJasminType(instruction.getReturnType());

        codeBuffer.append(invokeType).append(" ")
                .append(className.replace(".", "/")).append("/")
                .append(methodName).append("(")
                .append(parameterSignature).append(")")
                .append(returnType).append(NL);
    }

    private void generateStaticMethodCall() {
        String className = resolveStaticClassName();
        String methodName = resolveMethodName();
        String parameterSignature = buildParameterSignature();
        String returnType = types.getJasminType(instruction.getReturnType());

        codeBuffer.append("invokestatic ").append(className.replace(".", "/")).append("/")
                .append(methodName).append("(")
                .append(parameterSignature).append(")")
                .append(returnType).append(NL);
    }

    private void generateNewObjectCall() {
        Type returnType = instruction.getReturnType();
        if (returnType instanceof ClassType classType) {
            codeBuffer.append("new ").append(classType.getName().replace(".", "/")).append(NL);
            codeBuffer.append("dup").append(NL);
        } else if (returnType instanceof ArrayType) {
            codeBuffer.append("newarray int").append(NL);
        }
    }

    private String resolveInstanceClassName() {
        Element caller = instruction.getOperands().get(0);
        String className;

        if (caller instanceof Operand operand) {
            Type callerType = operand.getType();
            if (callerType instanceof ClassType classType) {
                className = classType.getName();
                if (className.equals("this")) {
                    className = ollirResult.getOllirClass().getClassName();
                }
            } else if (callerType instanceof ArrayType) {
                className = "java/lang/Object";
            } else {
                className = ollirResult.getOllirClass().getClassName();
            }
        } else {
            className = ollirResult.getOllirClass().getClassName();
        }

        // Handle special constructor calls
        if (instruction instanceof InvokeSpecialInstruction invokeSpecial) {
            if (invokeSpecial.getSuperClass().isPresent()) {
                className = invokeSpecial.getSuperClass().get();
            }
        }

        return className;
    }

    private String resolveStaticClassName() {
        Element classNameElem = instruction.getOperands().get(0);
        if (classNameElem instanceof Operand) {
            return ((Operand) classNameElem).getName();
        } else {
            return ollirResult.getOllirClass().getClassName();
        }
    }

    private String resolveMethodName() {
        Element methodNameElem = instruction.getOperands().get(1);
        if (methodNameElem instanceof LiteralElement) {
            return ((LiteralElement) methodNameElem).getLiteral();
        } else {
            return "toString"; // Default fallback
        }
    }

    private String buildParameterSignature() {
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 2; i < instruction.getOperands().size(); i++) {
            Element arg = instruction.getOperands().get(i);
            paramsBuilder.append(getParameterTypeDescriptor(arg));
        }
        return paramsBuilder.toString();
    }

    private String getParameterTypeDescriptor(Element arg) {
        if (arg instanceof org.specs.comp.ollir.ArrayOperand || arg.getType() instanceof ArrayType) {
            return "[" + types.getJasminType(
                    arg.getType() instanceof ArrayType ? ((ArrayType) arg.getType()).getElementType()
                            : new org.specs.comp.ollir.type.BuiltinType(org.specs.comp.ollir.type.BuiltinKind.INT32));
        }
        return types.getJasminType(arg.getType());
    }
}
