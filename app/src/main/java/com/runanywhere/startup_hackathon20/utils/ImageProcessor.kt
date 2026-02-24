package com.runanywhere.startup_hackathon20.utils

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Image preprocessing utilities using OpenCV
 * Optimized for medicine label/text recognition
 */
object ImageProcessor {
    
    private const val TAG = "ImageProcessor"
    
    init {
        try {
            System.loadLibrary("opencv_java4")
            Log.i(TAG, "OpenCV loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load OpenCV: ${e.message}")
        }
    }
    
    /**
     * Preprocess image for OCR - applies multiple enhancements
     */
    fun preprocessForOCR(bitmap: Bitmap): Bitmap {
        return try {
            val mat = bitmapToMat(bitmap)
            
            // 1. Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // 2. Apply Gaussian blur to reduce noise
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            
            // 3. Apply adaptive thresholding for better text extraction
            val threshold = Mat()
            Imgproc.adaptiveThreshold(
                blurred,
                threshold,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                11,
                2.0
            )
            
            // 4. Apply morphological operations to clean up
            val kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(2.0, 2.0)
            )
            val morphed = Mat()
            Imgproc.morphologyEx(threshold, morphed, Imgproc.MORPH_CLOSE, kernel)
            
            // Convert back to bitmap
            val result = matToBitmap(morphed)
            
            // Clean up
            mat.release()
            gray.release()
            blurred.release()
            threshold.release()
            morphed.release()
            kernel.release()
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error preprocessing image: ${e.message}")
            bitmap // Return original if processing fails
        }
    }
    
    /**
     * Enhance contrast for better text visibility
     */
    fun enhanceContrast(bitmap: Bitmap): Bitmap {
        return try {
            val mat = bitmapToMat(bitmap)
            val enhanced = Mat()
            
            // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
            val clahe = Imgproc.createCLAHE()
            clahe.clipLimit = 2.0
            clahe.tilesGridSize = Size(8.0, 8.0)
            
            // Convert to grayscale if not already
            val gray = if (mat.channels() > 1) {
                val grayMat = Mat()
                Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
                grayMat
            } else {
                mat
            }
            
            clahe.apply(gray, enhanced)
            
            val result = matToBitmap(enhanced)
            
            mat.release()
            if (gray != mat) gray.release()
            enhanced.release()
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error enhancing contrast: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Detect and correct skew/rotation
     */
    fun deskew(bitmap: Bitmap): Bitmap {
        return try {
            val mat = bitmapToMat(bitmap)
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // Apply threshold
            val thresh = Mat()
            Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
            
            // Find coordinates of all non-zero pixels
            val points = MatOfPoint()
            Core.findNonZero(thresh, points)
            
            if (points.rows() > 0) {
                // Get rotation angle
                val rotatedRect = Imgproc.minAreaRect(MatOfPoint2f(*points.toArray()))
                var angle = rotatedRect.angle
                
                // Correct angle
                if (angle < -45) {
                    angle += 90
                }
                
                // Rotate image if needed
                if (kotlin.math.abs(angle) > 0.5) {
                    val center = Point(mat.cols() / 2.0, mat.rows() / 2.0)
                    val rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0)
                    val rotated = Mat()
                    Imgproc.warpAffine(
                        mat,
                        rotated,
                        rotationMatrix,
                        mat.size(),
                        Imgproc.INTER_CUBIC
                    )
                    
                    val result = matToBitmap(rotated)
                    rotated.release()
                    rotationMatrix.release()
                    
                    mat.release()
                    gray.release()
                    thresh.release()
                    points.release()
                    
                    return result
                }
            }
            
            mat.release()
            gray.release()
            thresh.release()
            points.release()
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error deskewing image: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Remove shadows and uneven lighting
     */
    fun removeShadows(bitmap: Bitmap): Bitmap {
        return try {
            val mat = bitmapToMat(bitmap)
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // Apply morphological operations to estimate background
            val dilated = Mat()
            val kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(7.0, 7.0)
            )
            Imgproc.dilate(gray, dilated, kernel)
            
            val background = Mat()
            Imgproc.medianBlur(dilated, background, 21)
            
            // Subtract background
            val result = Mat()
            Core.absdiff(gray, background, result)
            
            // Normalize
            Core.normalize(result, result, 0.0, 255.0, Core.NORM_MINMAX)
            
            val finalBitmap = matToBitmap(result)
            
            mat.release()
            gray.release()
            dilated.release()
            kernel.release()
            background.release()
            result.release()
            
            finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error removing shadows: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Sharpen image for better text clarity
     */
    fun sharpen(bitmap: Bitmap): Bitmap {
        return try {
            val mat = bitmapToMat(bitmap)
            val sharpened = Mat()
            
            // Create sharpening kernel
            val kernel = Mat(3, 3, CvType.CV_32F)
            kernel.put(0, 0, 
                0.0, -1.0, 0.0,
                -1.0, 5.0, -1.0,
                0.0, -1.0, 0.0
            )
            
            Imgproc.filter2D(mat, sharpened, -1, kernel)
            
            val result = matToBitmap(sharpened)
            
            mat.release()
            sharpened.release()
            kernel.release()
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error sharpening image: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Complete preprocessing pipeline optimized for medicine labels
     */
    fun preprocessMedicineLabel(bitmap: Bitmap): Bitmap {
        return try {
            Log.i(TAG, "Starting medicine label preprocessing")
            
            // Step 1: Deskew
            var processed = deskew(bitmap)
            Log.i(TAG, "Deskew complete")
            
            // Step 2: Remove shadows
            processed = removeShadows(processed)
            Log.i(TAG, "Shadow removal complete")
            
            // Step 3: Enhance contrast
            processed = enhanceContrast(processed)
            Log.i(TAG, "Contrast enhancement complete")
            
            // Step 4: Sharpen
            processed = sharpen(processed)
            Log.i(TAG, "Sharpening complete")
            
            // Step 5: Final OCR preprocessing
            processed = preprocessForOCR(processed)
            Log.i(TAG, "OCR preprocessing complete")
            
            processed
        } catch (e: Exception) {
            Log.e(TAG, "Error in preprocessing pipeline: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Resize image while maintaining aspect ratio
     */
    fun resizeImage(bitmap: Bitmap, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            
            if (width <= maxWidth && height <= maxHeight) {
                return bitmap
            }
            
            val ratio = minOf(
                maxWidth.toFloat() / width,
                maxHeight.toFloat() / height
            )
            
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing image: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Crop image to region of interest
     */
    fun cropToROI(bitmap: Bitmap, rect: android.graphics.Rect): Bitmap {
        return try {
            Bitmap.createBitmap(
                bitmap,
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.width().coerceAtMost(bitmap.width - rect.left),
                rect.height().coerceAtMost(bitmap.height - rect.top)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping image: ${e.message}")
            bitmap
        }
    }
    
    // Helper functions
    
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
    
    private fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    
    /**
     * Get image quality score (0-100)
     */
    fun calculateImageQuality(bitmap: Bitmap): Int {
        return try {
            val mat = bitmapToMat(bitmap)
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // Calculate Laplacian variance (measure of blur)
            val laplacian = Mat()
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            
            val mean = MatOfDouble()
            val stddev = MatOfDouble()
            Core.meanStdDev(laplacian, mean, stddev)
            
            val variance = stddev[0, 0][0] * stddev[0, 0][0]
            
            mat.release()
            gray.release()
            laplacian.release()
            mean.release()
            stddev.release()
            
            // Convert variance to quality score (0-100)
            // Higher variance = sharper image
            val quality = (variance / 10.0).coerceIn(0.0, 100.0)
            quality.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating image quality: ${e.message}")
            50 // Return medium quality on error
        }
    }
}
