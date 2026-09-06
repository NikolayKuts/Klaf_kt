# Project Conventions

- Keep file-level constants, companion objects, support objects, and helper data classes near the top of the file, before composable and regular functions.
- Keep class-level properties near the start of the class, after the constructor and before `init` blocks and methods. Group dependencies before mutable state.
- Keep local variables close to the code that uses them; the ordering rule applies to file-level and class-level declarations.
- Do not add new constants or support types at the bottom of a source file.
