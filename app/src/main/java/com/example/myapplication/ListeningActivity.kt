package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.demo.AudioSampleGrpc
import com.example.demo.gTune
import com.example.myapplication.util.Factories
import com.example.myapplication.util.Factories.Companion.responseObserverEmpty
import com.example.myapplication.util.Factories.Companion.responseObservergTune
import com.example.myapplication.util.GrpcClient
import com.example.myapplication.util.ViewTools
import com.example.myapplication.util.WavTools
import com.google.android.gms.security.ProviderInstaller
import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.coroutines.CoroutineContext


private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val SAMPLE_RATE = 44100
private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
    SAMPLE_RATE,
    AudioFormat.CHANNEL_IN_STEREO,
    AudioFormat.ENCODING_PCM_16BIT
)

class ListeningActivity : AppCompatActivity(), ProviderInstaller.ProviderInstallListener,
    CoroutineScope {
    private lateinit var host: String
    private var port: Int = 0

    private var permissionToRecordAccepted = false
    private val permissions: Array<String> =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var recorder: AudioRecord? = null
    private val TAG: String = "MainActivity"
    private lateinit var asyncStub: AudioSampleGrpc.AudioSampleStub
    private lateinit var outputStream: FileOutputStream
    private lateinit var file: File
    private lateinit var appDir: File
    private lateinit var fileList: Array<String>
    private lateinit var currentPlay: Unit

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var resultIntent: Intent

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProviderInstaller.installIfNeededAsync(this, this)
        setContentView(R.layout.activity_listening)

        for (i in permissions) {
            Log.e(TAG, "Checking permission ${ContextCompat.checkSelfPermission(this, i)}")
        }
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        viewManager = LinearLayoutManager(this)
        makeAppDir()
        updateFileList()
        viewAdapter = ViewTools.Companion.MyAdapter(fileList)
        recyclerView = findViewById<RecyclerView>(R.id.filesView).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
            addOnItemTouchListener(TouchListener())
        }
    }

    inner class TouchListener : RecyclerView.OnItemTouchListener {
        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            Log.i(TAG, "onTouchEvent was triggered!")
            resultIntent = packageManager.getLaunchIntentForPackage("com.google.android.music")!!
            val touchedTextView: TextView = rv.findChildViewUnder(e.x, e.y) as TextView
            val touchedFileName: String = touchedTextView.getText().toString()
            val requestFile = File(appDir, touchedFileName)
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    applicationContext,
                    packageName + ".fileprovider",
                    requestFile
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "File Selector",
                    "The selected file can't be shared: ${requestFile}"
                )
                null
            }
            Log.i(TAG, "The file is ${fileUri}")
            if (fileUri != null) {
                resultIntent.setAction(Intent.ACTION_VIEW)
                resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                resultIntent.setDataAndType(fileUri, contentResolver.getType(fileUri))
                setResult(Activity.RESULT_OK, resultIntent)
                startActivity(resultIntent)
            } else {
                resultIntent.setDataAndType(null, "")
                setResult(RESULT_CANCELED, resultIntent)
                Log.i(TAG, "The file was null!")
            }
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            Log.i(TAG, "something was touched ${e.action}")
            if (e.action == MotionEvent.ACTION_UP) {
                Log.i(TAG, "Indeed!")
                onTouchEvent(rv, e)
                return true
            }
            return false
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            Log.i(TAG, "boo!")
        }

    }

    override fun onProviderInstallFailed(p0: Int, p1: Intent?) {
        TODO("Not yet implemented")
    }

    override fun onProviderInstalled() {
        host = getString(R.string.server_address)
        port = Integer.parseInt(getString(R.string.python_port))

        var mchannel: ManagedChannel =
            AndroidChannelBuilder.forAddress(host, port).context(applicationContext).build()
        asyncStub = AudioSampleGrpc.newStub(mchannel)

    }

    fun startRecording(view: View) {
        job = Job()

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        )
        val fileName = "track1.wav"
        file = File(appDir, fileName)
        outputStream = file.outputStream()

        launch {
            readAudioAndSendToServer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun stopRecording(view: View) {
        launch {
            Log.i(TAG, "Stopped recording")
            recorder?.stop()
            job.cancel()
            outputStream.close()
            WavTools.updateWavHeader(file)

//            saveFileToMediaStorage()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveFileToMediaStorage() {
        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "track1.pcm")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/*")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/samples")
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openInputStream(uri).use {
//                need too figure out how to write to this stream
            }
        }
    }

    fun updateFileList() {
        fileList = listFiles()
    }

    fun showFiles(view: View) {
        viewAdapter.notifyDataSetChanged()
    }

    fun playRemotely(view: View) {
        currentPlay = asyncStub.playRemotely(Empty.newBuilder().build(), responseObserverEmpty())
        Log.i(TAG, "Playing...")
    }

    fun cleanRemote(view: View) {
        asyncStub.cleanRemote(Empty.newBuilder().build(), responseObserverEmpty())
    }

    fun recognize(view: View) {
        asyncStub.recognize(Empty.newBuilder().build(), responseObservergTune())
    }

    private fun listFiles(): Array<String> {
        Log.i(TAG, "Files: ${applicationContext.filesDir.resolve(appDir).list()}")
        return applicationContext.filesDir.resolve(appDir).list(
            { file: File, s: String -> file.canonicalPath.endsWith("") }
        )
    }

    private fun makeAppDir() {
        val appDirName = "samples"
        appDir = File(applicationContext.filesDir, appDirName)
        appDir.mkdir()
    }

    private suspend fun readAudioAndSendToServer() {
        Log.i(TAG, "Reading audio")
        var showText = ""
        val data = ByteArray(BUFFER_SIZE * 2)
        Log.i(TAG, BUFFER_SIZE.toString())

        val requestObserver = asyncStub.searchSample(responseObservergTune())

        recorder!!.startRecording()
        WavTools.writeWavHeader(
            outputStream,
            AudioFormat.CHANNEL_IN_STEREO,
            SAMPLE_RATE,
            AudioFormat.ENCODING_PCM_16BIT
        )

        launch(Dispatchers.IO) {
            while (isActive) {
//                read mic input
                val opStatus = recorder?.read(data, 0, BUFFER_SIZE * 2)
//                send to grpc server
                GrpcClient.searchSample(data, requestObserver)
//                additionally write to file
                try {
                    outputStream.write(data)
                } catch (e: IOException) {
                    if (e.message == "Stream Closed") {
                        Log.i(TAG, "Write stream was closed, handling clumsily")
                    } else {
                        throw e
                    }
                }
            }
        }

//        return showText
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }
}
