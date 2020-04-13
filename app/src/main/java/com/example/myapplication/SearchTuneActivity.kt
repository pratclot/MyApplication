package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.myapplication.util.GrpcClient
import com.google.android.gms.security.ProviderInstaller
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder

class SearchTuneActivity : AppCompatActivity(), ProviderInstaller.ProviderInstallListener {

    private lateinit var host: String
    private var port: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProviderInstaller.installIfNeededAsync(this, this)
        setContentView(R.layout.activity_search_tune)
    }

    override fun onProviderInstalled() {
        host = getString(R.string.server_address)
        port = Integer.parseInt(getString(R.string.server_port))

        val tuneName = intent.getStringExtra(EXTRA_MESSAGE)

        readTextInputs()

        var mchannel: ManagedChannel = AndroidChannelBuilder.forAddress(host, port).context(getApplicationContext()).build()

        var showText = GrpcClient.getSomeTunes(mchannel, tuneName)
        findViewById<TextView>(R.id.tuneNameView).apply {
            text = showText
        }
    }

    override fun onProviderInstallFailed(p0: Int, p1: Intent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun readTextInputs() {
        try {
            host = findViewById<TextView>(R.id.serverAddressView).text.toString()
        } catch (e: IllegalStateException) {
            Log.i("tag", "User did not fill server address, using ${host}", e.cause)
        }
        try {
            port = Integer.parseInt(findViewById<TextView>(R.id.serverPortView).text.toString())
        } catch (e: java.lang.IllegalStateException) {
            Log.i("tag", "User did not fill server port, using ${port}", e.cause)
        }
    }

}
