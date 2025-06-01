# Java Minus Minus (Jmm) Compiler

## Overview

This project implements the **frontend phase** of a Java Minus Minus (Jmm) compiler. This is the first delivery of the project, focusing solely on processing Jmm source code through multiple stages, including lexical analysis, parsing, and semantic analysis. Future phases will include code optimization, intermediate representation (IR) generation, and backend processing.

## Features

- **Lexical Analysis:** Tokenization of Jmm source code using ANTLR to break down input into meaningful tokens.
- **Parsing:** Context-free grammar definition (`Javamm.g4`) for syntactic analysis and Abstract Syntax Tree (AST) generation.
- **Semantic Analysis:** Type checking, expression validation, scope resolution, and error detection to ensure code correctness.
- **Modular Analysis Passes:** Implementation of multiple analysis passes to validate expressions, detect type errors, and enforce language rules.
- **Symbol Table:** Construction of a symbol table to store variable and function declarations, enabling efficient scope and type resolution.

## OLLIR Generation

- **Code Generation:** The compiler generates OLLIR (Optimized Low-Level Intermediate Representation) code from the Abstract Syntax Tree (AST) using a visitor pattern.
- **Expression Handling:** Handles complex expressions, including binary operations, method calls, and array accesses, while ensuring type correctness.
- **Control Flow:** Generates OLLIR code for control flow constructs like `if` statements and `while` loops.
- **Field and Method Management:** Supports field access, method declarations, and invocations, including static and instance methods.

## Optimizations

- **Constant Propagation and Folding:** Identifies and replaces constant expressions to simplify the code and reduce runtime computations.
- **Register Allocation:** Implements a register allocation strategy to minimize the number of registers required during execution.
- **Dead Code Elimination:** Removes unused variables and unreachable code to optimize the generated OLLIR.
- **Array Handling:** Transforms calls to varargs methods into array-based calls for better compatibility and performance.
- **Control Flow Graphs:** Builds control flow graphs (CFGs) for methods to analyze and optimize live variable usage.

## Jasmin Code Generation

- **Assembly Generation:** The compiler translates the optimized OLLIR code into Jasmin assembly language.
- **Instruction Mapping:** Implements comprehensive mapping from OLLIR instructions to corresponding Jasmin instructions, including arithmetic operations, method calls, and control flow.
- **Stack Management:** Handles JVM stack operations efficiently, ensuring proper stack depth management and type consistency throughout code generation.
- **Method and Class Structure:** Generates complete Jasmin class files with proper method signatures, field declarations, and inheritance relationships.

## Group Members

- João Santos (202205794) - 33%
- Leonardo Teixeira (202208726) - 33%
- Tiago Pinto (202206280) - 33%

## Self-Assessment

We successfully implemented a complete compiler pipeline including the frontend (lexical analysis, parsing, and semantic analysis), intermediate representation generation (OLLIR), and backend code generation (Jasmin). Additionally, we implemented various optimizations and added extra components and tests to improve functionality and validation.

