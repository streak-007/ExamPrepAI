## 2025-01-24 - Accessibility and Visual Feedback in Quiz Options
**Learning:** Standard clickable items for quiz options lack necessary semantic context for screen readers and transition abruptly. Using `Modifier.selectable` with `Role.RadioButton` and `stateDescription` provides much better assistive feedback. Adding `animateColorAsState` provides a smooth micro-interaction that delights the user without being distracting.
**Action:** Always prefer `selectable` with appropriate roles for single/multiple choice UI and use `animate*AsState` for state-driven color changes.

## 2025-01-24 - Dynamic onClickLabels for Toggle Buttons
**Learning:** For buttons that toggle state (like bookmarks), a static `contentDescription` or no label makes it unclear what clicking will do. A dynamic `onClickLabel` improves predictability.
**Action:** Implement dynamic `onClickLabel` based on the current state of toggleable buttons.
