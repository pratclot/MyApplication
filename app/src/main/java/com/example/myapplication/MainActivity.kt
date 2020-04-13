package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.myapplication.util.Validators
import java.lang.NumberFormatException

const val EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE"

class MainActivity : AppCompatActivity() {

    private lateinit var serverAddressView: TextView
    private lateinit var serverPortView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverAddressView = findViewById(R.id.serverAddressView)
        serverPortView = findViewById(R.id.serverPortView)

        serverPortView.addTextChangedListener(object: watchUserInput(serverPortView){
            override fun validate(view: TextView, s: CharSequence?) {
                var port: Int
                try {
                    port = Integer.parseInt(s.toString())
                } catch (e: NumberFormatException) {
                    port = 1
                }
                Validators.validatePort(view, port)
            }
        })
    }

    abstract class watchUserInput(view: TextView): TextWatcher {
        private val view: TextView = view
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            validate(view, s)
        }

        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        abstract fun validate(view: TextView, s: CharSequence?)
    }

    fun searchTune(view: View) {
        val editText = findViewById<EditText>(R.id.tuneNameView)
        val tuneName = editText.text.toString()
        val intent = Intent(this, SearchTuneActivity::class.java).apply{
            putExtra(EXTRA_MESSAGE, tuneName)
        }
        startActivity(intent)
    }

    fun streamMic(view: View){
        val intent = Intent(this, ListeningActivity::class.java).apply {}
        startActivity(intent)
    }

}
