package com.screenai

import android.content.Context
import android.media.Image
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteDetector(
    private val context: Context
) {

    private var interpreter: Interpreter? = null

    init {

        val model =
            loadModel()

        interpreter =
            Interpreter(
                model,
                Interpreter.Options().apply {
                    setNumThreads(2)
                }
            )
    }

    private fun loadModel(): MappedByteBuffer {

        val fd =
            context.assets.openFd(
                "model.tflite"
            )

        FileInputStream(
            fd.fileDescriptor
        ).use { input ->

            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }
    }

    fun detect(
        image: Image
    ): Detection? {

        /*
         * Model hiện tại cần xác định
         * input/output tensor trước khi
         * viết decoder chính xác.
         *
         * Không tự bịa X/Y ở đây.
         */

        return null
    }

    fun close() {

        interpreter?.close()
        interpreter = null
    }
}
