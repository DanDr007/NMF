package com.example.nmf

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var isOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnToggle = findViewById<Button>(R.id.btnToggle)

        btnToggle.setOnClickListener {
            isOn = !isOn
            if (isOn) {
                btnToggle.setText(R.string.status_on)
                btnToggle.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.button_green),
                )
            } else {
                btnToggle.setText(R.string.status_off)
                btnToggle.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.button_red),
                )
            }
        }
    }
}