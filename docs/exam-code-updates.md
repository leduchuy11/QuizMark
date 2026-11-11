# Editing exam codes and grading mismatches

The edit-exam screen has a **Save changes** action. It validates the fields and
shows the old and new codes in a confirmation dialog. Cancel keeps the draft;
confirm updates the existing exam. Each row keeps its answer key when renamed,
including when two rows exchange codes. The original exam ID, owner, creation
time, and THPT configuration are preserved.

Added codes start without answers. Direct saving rejects incomplete keys and
asks the user to complete them through **Edit answers**. Answer-edit screens also
require confirmation before saving. Renaming a code preserves its existing
answers when the question structure is unchanged.

Camera and gallery grading require an exact saved code. For example, a sheet
marked `101` cannot use a key saved as `001`, even if it is the only key. Missing
or unmatched codes produce a message with the available codes, rather than a
successful zero score. Gallery errors are shown per image and are not saved to
grading history. Valid zero scores still display as scores.

For the sample 20-question photos marked `101`, change the saved exam code to
`101`, confirm the change, and scan again. Existing history is not recalculated.

Validation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Regression tests cover renaming `001` to `101` with all twenty answers, swapping
two codes, adding/removing rows, keeping all three THPT parts, rejecting unknown
or unreadable codes and empty key lists, preserving leading zeroes, and choosing
the exact key among multiple codes.

On-device check (2026-11-11): the debug APK installed successfully on the
connected Xiaomi phone. Changing `001` to `101` showed the expected confirmation
preview. Cancelling kept the draft. Opening **Edit answers** with code `101`
retained all twenty answers and passed completeness validation before showing
its own confirmation dialog. Returning from that screen also preserved draft
code `101` after adding saveable state to the edit form. Both test dialogs were cancelled; the phone was
left on the edit-exam screen with `101` as an unsaved draft. All 17 JVM tests
passed and the debug APK build succeeded.
