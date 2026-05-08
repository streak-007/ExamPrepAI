## 2025-05-15 - Improving Accessibility and Feedback in Jetpack Compose

**Learning:** Using `Modifier.selectable` with `Role.RadioButton` provides better screen reader semantics for multiple-choice lists than `Modifier.clickable`. Subtle animations like scale changes using `animateFloatAsState` enhance visual feedback for user selections.

**Action:** Always prefer `selectable` for single-choice components and add micro-animations for interactive state transitions. Use `semantics` for detailed `stateDescription` and `onClickLabel` for custom buttons.
