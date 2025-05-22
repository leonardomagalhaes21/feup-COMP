package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    public JasminUtils(OllirResult ollirResult) {
        // Can be useful to have if you expand this class with more methods
        this.ollirResult = ollirResult;
    }


    public String getModifier(AccessModifier accessModifier) {
        return accessModifier != AccessModifier.DEFAULT ?
                accessModifier.name().toLowerCase() + " " :
                "";
    }
    public String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            // Only supports int[] and boolean[] for now
            var elemType = ((ArrayType) type).getElementType();
            if (elemType instanceof BuiltinType builtin) {
                switch (builtin.getKind()) {
                    case INT32: return "[I";
                    case BOOLEAN: return "[Z";
                    case STRING: return "[Ljava/lang/String;";
                    default: return "[Ljava/lang/Object;";
                }
            }
            return "[Ljava/lang/Object;";
        }
        if (type instanceof BuiltinType builtin) {
            switch (builtin.getKind()) {
                case INT32: return "I";
                case BOOLEAN: return "Z";
                case STRING: return "Ljava/lang/String;";
                case VOID: return "V";
            }
        }
        if (type instanceof ClassType classType) {
            return "L" + classType.getName().replace('.', '/') + ";";
        }
        return "Ljava/lang/Object;";
    }
}