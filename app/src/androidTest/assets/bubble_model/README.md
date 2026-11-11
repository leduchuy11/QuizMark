# Synthetic model fixtures

These fifteen grayscale PNG crops come from the validation and test splits of
`bubble_dataset_synth_v1.zip` (74 sheets, generator seed 2026). The split uses
`quizmark-ml/src/data.py` with seed 42, identical to the Colab run.

Each filename identifies the template (1-5) and ground-truth class
(`empty`, `filled`, `erased`). Ground truth comes from `labels_override.csv`,
not the model. Examples were selected where the verified v1 model predicts the
correct class with over 95% confidence, so either opposite fallback value
must still yield the expected result. There is one crop per class per template.
The final test split has no template-4 sheet, so its three native regression
fixtures come from validation sheets. No fixture comes from the training split.

The Android test embeds each crop at a nonzero page offset before classification,
covering cell extraction, resize, input scale, class order, and native DNN loading.
These tiny generated fixtures contain no real student data.
