package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;


public class ExtraTests {

    //adicional tests
    @Test
    public void missingReturnPath() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MissingReturnPath.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void booleanOperations() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/BooleanOperations.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void voidReturnValue() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VoidReturnValue.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void nestedObjectAccess() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/NestedObjectAccess.jmm"));
        TestUtils.noErrors(result);
    }


    @Test
    public void stringConcatenation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StringConcatenation.jmm"));
        TestUtils.noErrors(result);
    }


    @Test
    public void typeNarrowing() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/TypeNarrowing.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void varDeclCantBeVarargs() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarDeclCantBeVarargs.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void objectExtendsTest() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ObjectExtendsTest.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void fieldDeclCantBeVarargs() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/FieldDeclCantBeVarargs.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayIndexIsBoolean() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ArrayIndexIsTypeIsBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void loopConditionsReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/CheckLoopConditionsReturns.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void loopConditionsReturn2() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/CheckLoopConditionsReturns2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void compareMethodTypes() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/CompareDeclaredMethodTypesWithCalledTypes.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void generalVarArgs() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/GeneralVarArgsCheck.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void generalVarArgs2() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/GeneralVarArgsCheck2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void generalVarArgs3() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/GeneralVarArgsCheck3.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void methodAcceptsVarArgsOrArray() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MethodAcceptVarArgsOrArray.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void methodDoesNotExistInExtendedClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MethodDoesNotExistButClassExtends.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void typeAssignments1() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/TypeAssignmentsCheck1.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void typeAssignments2() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/TypeAssignmentsCheck2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void methodCallFromImportedClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VerifyIfMethodCalledIsFromImportedClass.jmm"));
        TestUtils.mustFail(result);
    }
}
