package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.GetFieldInstruction;
import org.specs.comp.ollir.inst.PutFieldInstruction;
import org.specs.comp.ollir.type.Type;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2025.backend.JasminUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

public class FieldAccessInstructionBuilder {
    private static final String NL = "\n";

    private final FunctionClassMap<TreeNode, String> generators;
    private final OllirResult ollirResult;
    private final JasminUtils types;
    private final StringBuilder codeBuilder;

    public FieldAccessInstructionBuilder(FunctionClassMap<TreeNode, String> generators,
            OllirResult ollirResult,
            JasminUtils types) {
        this.generators = generators;
        this.ollirResult = ollirResult;
        this.types = types;
        this.codeBuilder = new StringBuilder();
    }

    public FieldAccessInstructionBuilder withGetFieldGeneration(GetFieldInstruction getField) {
        // Load object reference
        Element objectRef = getField.getOperands().get(0);
        codeBuilder.append(generators.apply(objectRef));

        // Generate getfield instruction
        String className = ollirResult.getOllirClass().getClassName();
        Operand fieldOp = (Operand) getField.getOperands().get(1);
        String fieldName = fieldOp.getName();
        Type fieldType = getField.getFieldType();

        codeBuilder.append("getfield ").append(className).append("/")
                .append(fieldName).append(" ")
                .append(types.getJasminType(fieldType))
                .append(NL);

        return this;
    }

    public FieldAccessInstructionBuilder withPutFieldGeneration(PutFieldInstruction putField) {
        // Load object reference
        Element objectRef = putField.getOperands().get(0);
        codeBuilder.append(generators.apply(objectRef));

        // Load value to store
        Element value = putField.getOperands().get(2);
        codeBuilder.append(generators.apply(value));

        // Generate putfield instruction
        String className = ollirResult.getOllirClass().getClassName();
        Operand fieldOp = (Operand) putField.getOperands().get(1);
        String fieldName = fieldOp.getName();
        Type fieldType = putField.getFieldType();

        codeBuilder.append("putfield ").append(className).append("/")
                .append(fieldName).append(" ")
                .append(types.getJasminType(fieldType))
                .append(NL);

        return this;
    }

    public String buildFieldAccessInstruction() {
        return codeBuilder.toString();
    }
}
