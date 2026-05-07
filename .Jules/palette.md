# Palette's Journal

## 2025-05-22 - Initial UX Audit of ExamPrepAI
**Learning:** The initial implementation uses basic `clickable` modifiers without explicit roles or click labels, which can be confusing for screen reader users. Specifically, the "OptionCard" acts as a radio button but isn't semantically declared as one. The "BookmarkRibbonButton" is also missing a role and clear state description for accessibility.
**Action:** Use `Modifier.selectable` with `Role.RadioButton` for option cards and `Modifier.semantics { role = Role.Button }` for custom icon-only or specialized buttons.
