package com.example.myapplication.util

import android.widget.TextView

class Validators {
    companion object{
        fun validatePort(view: TextView, port: Int) {
            if (port !in 1..65535) {
                showInputValidationError(view, "Must be 1-65535")
            }
        }

        fun showInputValidationError(view: TextView, error: String) {
            view.setError(error)
        }
    }
}