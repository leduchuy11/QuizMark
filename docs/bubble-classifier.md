# Offline bubble classifier (practice v1)

Debug builds include `app/src/main/assets/models/bubble_classifier_v1.onnx` and
its original `model_card.json`, exported from `quizmark-ml` commit `fae7b0b`.
The model was trained on generated sheet images, not real paper scans.
Release builds keep the original threshold scanner until real-data validation.

## Phone comparison

1. Install `app/build/outputs/apk/debug/app-debug.apk`.
2. Open Account and find **Data tools (debug build only)**.
3. **Use offline AI for scanning** is enabled by default. Turn it off to use the
   original 50% rule, then repeat a scan of the same photo. The choice is saved.
4. Match each photo album to its exam template: `QM_20cau`, `QM_40cau`,
   `QM_50cau`, `QM_120cau`, or `QM_THPT`. Use exam code `101` for basic
   templates, `0101` for THPT. Compare detected answers as well as the score.

The older `QM_*` photos were generated separately from the training dataset.
They still share a synthetic source, so testing them does not establish accuracy
on real paper. Scans from the gallery follow the existing app behavior and save
grading history; use practice exams and an anonymous roster for this comparison.

## Inference contract

OpenCV still detects corners and warps the page to 800 x 1200. All twelve
reading sites across templates 1-5 pass a clean grayscale cell to the classifier.
The classifier resizes it to 32 x 32 with `INTER_AREA`, scales by 1/255, and runs
OpenCV 4.5.3 DNN on the CPU. Output logits are ordered `empty, filled, erased`.
Only `filled` counts as marked. The complete `setInput` / `forward` operation is
synchronized because camera and gallery share a network.

If the winning softmax probability is below 0.6, that cell keeps the original
masked 50% decision. Loading or inference failures also fall back to that rule.
An inference failure disables the model until the app restarts. The Account
switch is disabled when the model is unavailable. Log tag: `BubbleClassifier`.

Data collection continues to save the original threshold's temporary labels,
even when AI scanning is enabled. Review labels before training; predictions
must not silently become ground truth.

Unsupported template IDs are rejected by `ExamProcessorFactory`. Camera and
gallery display an unsupported message instead of treating template 6 as a
20-question sheet. This classifier does not add arbitrary-layout grading.
Corner detection and mismatched supported sheet templates still need separate
validation; the model cannot correct a wrongly positioned crop.

## Validation

The source model card reports 4,574 held-out synthetic cells: 99.803% accuracy,
9 filled/not-filled errors for the model and 150 for the original rule. This
is synthetic performance, not a production accuracy claim. The sheet-level
random split contains no 50-question sheet in its final test set; that template
is represented in validation and the Android regression fixtures.

Local unit tests cover stable softmax, the confidence fallback, all three
classes, invalid outputs, and processor selection. Android instrumentation tests
exercise the actual native model on 15 validation/test crops (each class and template),
the native error fallback, and all twelve processor reading sites. Run them on
a connected test phone:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

The training/compatibility report and model SHA-256 are also recorded in
`quizmark-ml/models/verification_v1.json`.

Local integration checks on 2026-11-11:

- Debug app and Android test APKs compile; all 17 JVM unit tests pass.
- OpenCV 4.5.3 reproduces the model card's full test confusion matrix. The app's
  confidence fallback also gives 9 filled/not-filled errors (10 fallback cells).
- Sixty-four inputs match an independent ONNX reference evaluator within
  0.000023 logits; preprocessing differs by less than 0.00000006.
- The debug APK was installed over Wi-Fi on a Xiaomi 23117RA68G running Android
  16 (SDK 36, arm64-v8a). A cold launch logged `Practice model v1 ready`, which
  confirms that model loading and the startup forward pass succeeded on-device.
- All five targeted Android tests passed on that phone: three classifier tests
  (native inference, every processor, and failure fallback), plus two original
  scan storage tests (lossless preservation and safe cleanup). Installing with
  `adb install --no-streaming -r -t` succeeded. The test helper was removed after
  validation; QuizMark remains installed.
- Project-wide lint is not clean: existing Compose locale/resource lookups,
  an Activity cast, and a ChromeOS manifest check remain.
  Lint 32.3.3 also stalled in its `BidiSpoofing` Kotlin comment walker. A one-run
  exclusion allowed other checks to finish; a manual source scan found no
  bidirectional control characters. No lint rules or baselines were changed in
  the project.
- A focused follow-up of the resource lookup check reports 78 findings, all on
  lines unchanged from Git HEAD. The two new unsupported-template messages use
  `stringResource` and introduce no findings in that check.
