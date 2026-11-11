# Compare the original scan with grading highlights

After a successful new camera or gallery scan, the result screen displays the
graded sheet followed by **ORIGINAL SCAN** (**ẢNH BAN ĐẦU** in Vietnamese).
The second image shows the same aligned sheet before the scanner draws its
colored circles. It preserves the marks actually present in the scan; it is
not an answer key generated from the model's predictions.

Original images are stored losslessly as PNGs under the app's private
`history_original_images` directory. Results retain the file path rather than
a second full-size bitmap, so a gallery batch does not retain all originals
in memory. Coil loads the selected result's original image on demand.

New history records retain this local path, including through the background
sync worker. The original can be reopened from History on the phone where it
was scanned. Original image files are not uploaded. Deleting a history record
also deletes its original file, limited to the app's original-image directory.

Earlier history records have no saved original and continue to show the graded
image. Scan a sheet again to create a comparison. A storage failure leaves
grading available; the original image is omitted if saving fails.

Validation covers all five processors retaining the unchanged input bitmap,
grading overlays being drawn on a separate image, PNG preservation of faint
marks, and cleanup refusing paths outside the original-image directory.

Verified on 2026-11-11: the debug app and Android test APKs build successfully,
all 17 JVM unit tests pass, and all five targeted Android tests pass on the
Xiaomi Redmi Note 13 Pro (Android 16, arm64-v8a). The update is installed on
the phone.
