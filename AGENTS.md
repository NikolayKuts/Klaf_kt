# Project Conventions

- Keep file-level constants, companion objects, support objects, and helper data classes near the top of the file, before composable and regular functions.
- Keep class-level properties near the start of the class, after the constructor and before `init` blocks and methods. Group dependencies before mutable state.
- Keep local variables close to the code that uses them; the ordering rule applies to file-level and class-level declarations.
- Do not add new constants or support types at the bottom of a source file.

# Collaboration & Engineering Principles

- Do not be a sycophant or automatically agree with the user. The user's opinion is not the final technical truth.
- Actively warn the user against architectural, design, and technical mistakes, antipatterns, and hidden pitfalls.
- Provide objective, in-depth technical arguments, contrast trade-offs clearly, and defend solid engineering solutions.
- Respect that the final decision belongs to the user after they have heard and weighed the technical arguments (e.g., pragmatic MVP compromise vs ideal architecture).

# Independent Final Verification

- For a task that changes executable code or tests, delegate the final code review and final test/build verification to two separate subagents when subagents are available. The reviewer inspects the scoped diff for correctness, regressions, security, and missed edge cases; the tester independently chooses and runs relevant checks. They must not edit source files, commit, or push.
- Keep TDD in the implementation flow: write and run failing tests before the fix when the user requests TDD. Independent subagent verification happens after the implementation; it does not replace that red/green cycle.
- Give each subagent the task scope and the actual changed files, including changes in the private server submodule when relevant. Do not give the reviewer a desired conclusion. The main agent waits for both reports, fixes actionable findings itself, and reruns affected checks (and review when the fix is material) before declaring completion.
- If subagents are unavailable, perform the checks directly and say so. For documentation-only or explanatory tasks with no executable-code change, do not spawn these verification subagents unless the user specifically requests them.
