package com.screenai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelInspector {

    data class TensorInfo(
        val index: Int,
        val shape: IntArray,
        val type: String,
        val elements: Int,
        val bytes: Int
    )

    data class ModelInfo(
        val inputs: List<TensorInfo>,
        val outputs: List<TensorInfo>
    )

    fun inspect(context: Context): ModelInfo {
        val model = loadModel(context)

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }

        val interpreter = Interpreter(model, options)

        try {
            interpreter.allocateTensors()

            val inputs = (0 until interpreter.inputTensorCount).map { index ->
                val tensor = interpreter.getInputTensor(index)

                TensorInfo(
                    index = index,
                    shape = tensor.shape(),
                    type = tensor.dataType().name,
                    elements = tensor.numElements(),
                    bytes = tensor.numBytes()
                )
            }

            val outputs = (0 until interpreter.outputTensorCount).map { index ->
                val tensor = interpreter.getOutputTensor(index)

                TensorInfo(
                    index = index,
                    shape = tensor.shape(),
                    type = tensor.dataType().name,
                    elements = tensor.numElements(),
                    bytes = tensor.numBytes()
                )
            }

            return ModelInfo(inputs, outputs)
        } finally {
            interpreter.close()
        }
    }

    private fun loadModel(context: Context): MappedByteBuffer {
        val descriptor = context.assets.openFd("model.tflite")

        FileInputStream(descriptor.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    fun format(info: ModelInfo): String {
        val text = StringBuilder()

        text.appendLine("===== TFLITE MODEL =====")
        text.appendLine()
        text.appendLine("INPUTS: ${info.inputs.size}")

        for (t in info.inputs) {
            text.appendLine(
                "Input ${t.index}: " +
                "shape=${t.shape.contentToString()} " +
                "type=${t.type} " +
                "elements=${t.elements} " +
                "bytes=${t.bytes}"
            )
        }

        text.appendLine()
        text.appendLine("OUTPUTS: ${info.outputs.size}")

        for (t in info.outputs) {
            text.appendLine(
                "Output ${t.index}: " +
                "shape=${t.shape.contentToString()} " +
                "type=${t.type} " +
                "elements=${t.elements} " +
                "bytes=${t.bytes}"
            )
        }

        text.appendLine()
        text.appendLine("========================")

        return text.toString()
    }
}
