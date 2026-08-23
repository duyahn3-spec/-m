package com.screenai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class TFLiteEngine(
    context: Context
) {

    private val interpreter: Interpreter

    val inputWidth: Int
    val inputHeight: Int
    val inputType: DataType

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }

        interpreter = Interpreter(
            loadModel(context),
            options
        )

        interpreter.allocateTensors()

        val tensor = interpreter.getInputTensor(0)

        val shape = tensor.shape()

        inputHeight = shape[1]
        inputWidth = shape[2]
        inputType = tensor.dataType()
    }

    private fun loadModel(
        context: Context
    ): MappedByteBuffer {

        val fd = context.assets.openFd("model.tflite")

        FileInputStream(fd.fileDescriptor).use { input ->

            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }
    }

    fun run(bitmap: Bitmap): Long {

        val start = System.nanoTime()

        val resized = Bitmap.createScaledBitmap(
            bitmap,
            inputWidth,
            inputHeight,
            true
        )

        val input = createInput(resized)

        val outputs = HashMap<Int, Any>()

        for (i in 0 until interpreter.outputTensorCount) {

            val tensor = interpreter.getOutputTensor(i)

            outputs[i] = createOutputBuffer(
                tensor.shape(),
                tensor.dataType()
            )
        }

        interpreter.runForMultipleInputsOutputs(
            arrayOf(input),
            outputs
        )

        resized.recycle()

        return (System.nanoTime() - start) / 1_000_000
    }

    private fun createInput(
        bitmap: Bitmap
    ): ByteBuffer {

        val tensor = interpreter.getInputTensor(0)

        val bytes = tensor.numBytes()

        val buffer = ByteBuffer
            .allocateDirect(bytes)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(
            bitmap.width * bitmap.height
        )

        bitmap.getPixels(
            pixels,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )

        val quant = tensor.quantizationParams()

        val scale = quant.scale
        val zero = quant.zeroPoint

        for (pixel in pixels) {

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            when (inputType) {

                DataType.UINT8 -> {
                    buffer.put(r.toByte())
                    buffer.put(g.toByte())
                    buffer.put(b.toByte())
                }

                DataType.INT8 -> {

                    val qr = quantize(r / 255f, scale, zero)
                    val qg = quantize(g / 255f, scale, zero)
                    val qb = quantize(b / 255f, scale, zero)

                    buffer.put(qr.toByte())
                    buffer.put(qg.toByte())
                    buffer.put(qb.toByte())
                }

                DataType.FLOAT32 -> {
                    buffer.putFloat(r / 255f)
                    buffer.putFloat(g / 255f)
                    buffer.putFloat(b / 255f)
                }

                else -> {
                    throw IllegalStateException(
                        "Unsupported input type: $inputType"
                    )
                }
            }
        }

        buffer.rewind()

        return buffer
    }

    private fun quantize(
        value: Float,
        scale: Float,
        zero: Int
    ): Int {

        if (scale == 0f) {
            return 0
        }

        return max(
            -128,
            min(
                127,
                (value / scale + zero).toInt()
            )
        )
    }

    private fun createOutputBuffer(
        shape: IntArray,
        type: DataType
    ): ByteBuffer {

        var elements = 1

        for (s in shape) {
            elements *= s
        }

        val bytesPerElement = when (type) {
            DataType.FLOAT32 -> 4
            DataType.INT32 -> 4
            DataType.INT64 -> 8
            DataType.INT8 -> 1
            DataType.UINT8 -> 1
            else -> 4
        }

        return ByteBuffer
            .allocateDirect(
                elements * bytesPerElement
            )
            .order(ByteOrder.nativeOrder())
    }

    fun describe(): String {

        val out = StringBuilder()

        val input = interpreter.getInputTensor(0)

        out.appendLine(
            "Input: " +
                input.shape().contentToString()
        )

        out.appendLine(
            "Type: " +
                input.dataType()
        )

        out.appendLine(
            "Outputs: " +
                interpreter.outputTensorCount
        )

        for (i in 0 until interpreter.outputTensorCount) {

            val t = interpreter.getOutputTensor(i)

            out.appendLine(
                "Output $i: " +
                    t.shape().contentToString() +
                    " " +
                    t.dataType()
            )
        }

        return out.toString()
    }

    fun close() {
        interpreter.close()
    }
}
