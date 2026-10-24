# Project Conventions

- Keep file-level constants, companion objects, support objects, and helper data classes near the top of the file, before composable and regular functions.
- Keep class-level properties near the start of the class, after the constructor and before `init` blocks and methods. Group dependencies before mutable state.
- Keep local variables close to the code that uses them; the ordering rule applies to file-level and class-level declarations.
- Do not add new constants or support types at the bottom of a source file.
- Order top-level functions by visibility: public, then internal, then private. Private helper and extension functions belong below public/internal entry points.

# Collaboration & Engineering Principles

- Do not be a sycophant or automatically agree with the user. The user's opinion is not the final technical truth.
- Actively warn the user against architectural, design, and technical mistakes, antipatterns, and hidden pitfalls.
- Provide objective, in-depth technical arguments, contrast trade-offs clearly, and defend solid engineering solutions.
- Respect that the final decision belongs to the user after they have heard and weighed the technical arguments (e.g., pragmatic MVP compromise vs ideal architecture).

# Mandatory Test-First Development

- TDD is mandatory for new or changed executable behavior, bug fixes, database migrations, and maintenance/cleanup scripts. The user does not need to request it separately. Only an explicit user instruction may waive this requirement.
- Before changing implementation, choose the appropriate test level (unit, integration, database/migration, UI, or end-to-end), write a behavioral test, and run it. Confirm that it fails because the required behavior is missing or incorrect, not because of an unrelated compilation, dependency, or environment problem.
- Only after the expected failing result, implement the smallest change needed and rerun the test until it passes. Then run relevant regression checks. Do not write implementation first and describe later tests as TDD.
- For behavior-preserving refactoring, establish and run characterization/regression coverage before editing implementation; do not manufacture a failing assertion when no behavior change is intended.
- Test destructive database work against disposable databases first, including preservation of data outside the authorized scope. Tests must never perform cleanup against the user's working database.
- If the required test cannot be run, report the concrete blocker and do not silently proceed with implementation. Record the actual red/green commands and results; distinguish tests that were written from tests that were executed.
- Documentation-only and formatting-only changes that do not affect executable behavior do not require artificial tests.

# Independent Final Verification

- For a task that changes executable code or tests, delegate the final code review and final test/build verification to two separate subagents when subagents are available. The reviewer inspects the scoped diff for correctness, regressions, security, and missed edge cases; the tester independently chooses and runs relevant checks. They must not edit source files, commit, or push.
- Follow the mandatory test-first workflow above. Independent subagent verification happens after implementation; it does not replace the red/green cycle.
- Give each subagent the task scope and the actual changed files, including changes in the private server submodule when relevant. Do not give the reviewer a desired conclusion. The main agent waits for both reports, fixes actionable findings itself, and reruns affected checks (and review when the fix is material) before declaring completion.
- If subagents are unavailable, perform the checks directly and say so. For documentation-only or explanatory tasks with no executable-code change, do not spawn these verification subagents unless the user specifically requests them.
