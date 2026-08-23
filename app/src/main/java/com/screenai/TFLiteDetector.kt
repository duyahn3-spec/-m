package com.screenai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.Image
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class TFLiteDetector(
    private val context: Context
) {

    private val interpreter:
        Interpreter

    private val inputWidth: Int
    private val inputHeight: Int

    private val inputType:
        DataType

    init {

        val model =
            loadModel()

        interpreter =
            Interpreter(
                model,
                Interpreter.Options().apply {

                    /*
                     * T606 yếu:
                     * bắt đầu bằng 2 threads.
                     */

                    setNumThreads(2)
                }
            )

        val inputTensor =
            interpreter.getInputTensor(0)

        val shape =
            inputTensor.shape()

        inputHeight =
            shape[1]

        inputWidth =
            shape[2]

        inputType =
            inputTensor.dataType()
    }

    private fun loadModel():
        ByteBuffer {

        val fd =
            context.assets.openFd(
                "model.tflite"
            )

        val input =
            FileInputStream(
                fd.fileDescriptor
            )

        return input.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        ).order(
            ByteOrder.nativeOrder()
        )
    }

    fun detect(
        image: Image
    ): Detection? {

        /*
         * Chuyển Image RGBA -> Bitmap.
         */

        val bitmap =
            imageToBitmap(
                image
            ) ?: return null

        val resized =
            Bitmap.createScaledBitmap(
                bitmap,
                inputWidth,
                inputHeight,
                true
            )

        val input =
            makeInputBuffer(
                resized
            )

        /*
         * Output buffers phổ biến
         * của SSD / EfficientDet TFLite.
         */

        val boxes =
            Array(
                1
            ) {
                Array(
                    100
                ) {
                    FloatArray(4)
                }
            }

        val classes =
            Array(
                1
            ) {
                FloatArray(100)
            }

        val scores =
            Array(
                1
            ) {
                FloatArray(100)
            }

        val count =
            FloatArray(1)

        val outputs =
            HashMap<Int, Any>()

        outputs[0] = boxes
        outputs[1] = classes
        outputs[2] = scores
        outputs[3] = count

        try {

            interpreter.runForMultipleInputsOutputs(
                arrayOf(input),
                outputs
            )

        } catch (
            _: Throwable
        ) {

            /*
             * Model không có output
             * dạng detection chuẩn.
             */

            return null
        }

        val n =
            count[0]
                .toInt()
                .coerceIn(
                    0,
                    100
                )

        var best:
            Detection? = null

        for (i in 0 until n) {

            val score =
                scores[0][i]

            if (score < 0.40f) {
                continue
            }

            /*
             * boxes:
             * top, left, bottom, right
             *
             * tọa độ normalized 0..1
             */

            val top =
                boxes[0][i][0]

            val left =
                boxes[0][i][1]

            val bottom =
                boxes[0][i][2]

            val right =
                boxes[0][i][3]

            val x =
                (left + right) / 2f

            val y =
                (top + bottom) / 2f

            val width =
                right - left

            val height =
                bottom - top

            val candidate =
                Detection(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    confidence = score
                )

            if (
                best == null ||
                score >
                best!!.confidence
            ) {
                best = candidate
            }
        }

        bitmap.recycle()

        if (resized !== bitmap) {
            resized.recycle()
        }

        return best
    }

    private fun makeInputBuffer(
        bitmap: Bitmap
    ): ByteBuffer {

        val pixels =
            inputWidth *
                    inputHeight

        return if (
            inputType ==
            DataType.UINT8
        ) {

            val buffer =
                ByteBuffer.allocateDirect(
                    pixels * 3
                )

            buffer.order(
                ByteOrder.nativeOrder()
            )

            val values =
                IntArray(pixels)

            bitmap.getPixels(
                values,
                0,
                inputWidth,
                0,
                0,
                inputWidth,
                inputHeight
            )

            for (pixel in values) {

                buffer.put(
                    ((pixel shr 16) and 0xFF)
                        .toByte()
                )

                buffer.put(
                    ((pixel shr 8) and 0xFF)
                        .toByte()
                )

                buffer.put(
                    (pixel and 0xFF)
                        .toByte()
                )
            }

            buffer.rewind()

            buffer

        } else {

            val buffer =
                ByteBuffer.allocateDirect(
                    pixels * 3 * 4
                )

            buffer.order(
                ByteOrder.nativeOrder()
            )

            val values =
                IntArray(pixels)

            bitmap.getPixels(
                values,
                0,
                inputWidth,
                0,
                0,
                inputWidth,
                inputHeight
            )

            for (pixel in values) {

                buffer.putFloat(
                    ((pixel shr 16) and 0xFF) /
                            255f
                )

                buffer.putFloat(
                    ((pixel shr 8) and 0xFF) /
                            255f
                )

                buffer.putFloat(
                    (pixel and 0xFF) /
                            255f
                )
            }

            buffer.rewind()

            buffer
        }
    }

    private fun imageToBitmap(
        image: Image
    ): Bitmap? {

        val plane =
            image.planes.firstOrNull()
                ?: return null

        val buffer =
            plane.buffer

        val pixelStride =
            plane.pixelStride

        val rowStride =
            plane.rowStride

        val rowPadding =
            rowStride -
                    pixelStride *
                    image.width

        val bitmapWidth =
            image.width +
                    rowPadding /
                    pixelStride

        val bitmap =
            Bitmap.createBitmap(
                bitmapWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )

        bitmap.copyPixelsFromBuffer(
            buffer
        )

        if (
            bitmapWidth ==
            image.width
        ) {
            return bitmap
        }

        val cropped =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                image.width,
                image.height
            )

        if (!bitmap.isRecycled) {
    bitmap.recycle()
}

if (resized !== bitmap && !resized.isRecycled) {
    resized.recycle()
}

return best
