package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.inst.ArrayLengthInstruction;
import org.specs.comp.ollir.inst.GotoInstruction;
import org.specs.comp.ollir.inst.ReturnInstruction;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp2025.backend.JasminUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

public class ControlFlowInstructionBuilder {
    private static final String NL = "\n";

    private final FunctionClassMap<TreeNode, String> generators;
    private final JasminUtils types;
    private final StringBuilder codeBuilder;

    public ControlFlowInstructionBuilder(FunctionClassMap<TreeNode, String> generators,
            JasminUtils types) {
        this.generators = generators;
        this.types = types;
        this.codeBuilder = new StringBuilder();
    }

    public ControlFlowInstructionBuilder withReturnGeneration(ReturnInstruction returnInst) {
        if (returnInst.hasReturnValue()) {
            // Load return value and generate typed return
            Element operand = returnInst.getOperand().get();
            codeBuilder.append(generators.apply(operand));
            Type type = operand.getType();
            codeBuilder.append(types.getReturnInstruction(type)).append(NL);
        } else {
            // Void return
            codeBuilder.append("return").append(NL);
        }

        return this;
    }

    public ControlFlowInstructionBuilder withGotoGeneration(GotoInstruction gotoInst) {
        String targetLabel = gotoInst.getLabel();
        codeBuilder.append("goto ").append(targetLabel).append(NL);

        return this;
    }

    public ControlFlowInstructionBuilder withArrayLengthGeneration(ArrayLengthInstruction arrayLength) {
        // Load array reference and get its length
        Element arrayRef = arrayLength.getOperands().get(0);
        codeBuilder.append(generators.apply(arrayRef));
        codeBuilder.append("arraylength").append(NL);

        return this;
    }

    public String buildControlFlowInstruction() {
        return codeBuilder.toString();
    }
}
