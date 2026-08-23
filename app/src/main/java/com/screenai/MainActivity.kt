package com.screenai

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "Screen AI Research"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val result = TextView(this).apply {
            text = "Đang kiểm tra model..."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 40, 0, 0)
        }

        root.addView(title)

        root.addView(
            result,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        Thread {
            try {
                val info = ModelInspector.inspect(this)
                val output = ModelInspector.format(info)

                runOnUiThread {
                    result.text = output
                }
            } catch (e: Exception) {
                runOnUiThread {
                    result.text =
                        "MODEL ERROR\n\n${e.stackTraceToString()}"
                }
            }
        }.start()
    }
}
