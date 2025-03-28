# Java Minus Minus (Jmm) Compiler

## Overview

This project implements the **frontend phase** of a Java Minus Minus (Jmm) compiler. This is the first delivery of the project, focusing solely on processing Jmm source code through multiple stages, including lexical analysis, parsing, and semantic analysis. Future phases will include code optimization, intermediate representation (IR) generation, and backend processing.

## Features

- **Lexical Analysis:** Tokenization of Jmm source code using ANTLR to break down input into meaningful tokens.
- **Parsing:** Context-free grammar definition (`Javamm.g4`) for syntactic analysis and Abstract Syntax Tree (AST) generation.
- **Semantic Analysis:** Type checking, expression validation, scope resolution, and error detection to ensure code correctness.
- **Modular Analysis Passes:** Implementation of multiple analysis passes to validate expressions, detect type errors, and enforce language rules.
- **Symbol Table:** Construction of a symbol table to store variable and function declarations, enabling efficient scope and type resolution.

## Group Members

- João Santos (202205794) - 33%
- Leonardo Teixeira (202208726) - 33%
- Tiago Pinto (202206280) - 33%

## Self-Assessment

We successfully implemented the compiler frontend and added extra components and tests to improve functionality and validation.

