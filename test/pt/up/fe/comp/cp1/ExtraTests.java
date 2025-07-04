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
        TestUtils.mustFail(result);
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







    @Test
    public void testCorrectWhileIfConditions() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/CorrectWhileIfConditions.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testNegateIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/NegateIntToBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testNegateBoolToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/NegateBoolToBool.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testDuplicateImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/DuplicateImports.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testDuplicateVariables() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/DuplicateVariables.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testDuplicateMethods() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/DuplicateMethods.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testDuplicateParameters() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/DuplicateParameters.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testLengthAsVariable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/LengthAsVariable.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testMultipleReturns() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MultipleReturns.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testStatementsAfterReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StatementsAfterReturn.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testThisAsArgument() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ThisAsArgument.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testImportedClassInstantiation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ImportedClassInstantiation.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testVoidField() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VoidField.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testLengthAsMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/LengthAsMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testUndefinedMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/UndefinedMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testDefinedMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/DefinedMethod.jmm"));
        TestUtils.noErrors(result);
    }


    @Test
    public void testStatementsAfterReturn2(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StatementsAfterReturn2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testThisAsArgument2(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ThisAsArgument2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testImportedClassInstantiation2(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ImportedClassInstantiation2.jmm"));
        TestUtils.noErrors(result);
    }



    @Test
    public void testIncorrectArgumentCount() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/IncorrectArgumentCount.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testIncorrectArgumentType() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/IncorrectArgumentType.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testStaticMethodAccessingInstanceField() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StaticMethodAccessingInstanceField.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsAsField() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarargsAsField.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsAsLocalVariable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarargsAsLocalVariable.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsAsReturnType() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarargsAsReturnType.jmm"));
        TestUtils.mustFail(result);
    }


    @Test
    public void varargsIntArrayCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarargsIntArrayCall.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsBooleanAndIntArrayCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarargsBooleanAndIntArrayCall.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void stringArgumentToMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StringArgumentToMethod.jmm"));
        TestUtils.noErrors(result);
    }
}
