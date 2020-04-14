package com.example.myapplication.util

import android.util.Log
import com.example.demo.gTune
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver

private val TAG = "foo"

class Factories {
    companion object {
        class responseObservergTune: StreamObserver<gTune> {
            override fun onNext(value: gTune?) {
                if (value != null) {
                    Log.i(TAG, "${value.name} matches the recording!")
                }
            }
            override fun onError(t: Throwable?) {
                Log.i(TAG, "ERROR!")
                throw t!!
            }
            override fun onCompleted() {
                Log.i(TAG, "call finished")
            }
        }
        class responseObserverEmpty: StreamObserver<Empty> {
            override fun onNext(value: Empty?) {
                if (value != null) {
                    Log.i(TAG, "${value} matches the recording!")
                }
            }
            override fun onError(t: Throwable?) {
                Log.i(TAG, "ERROR!")
                throw t!!
            }
            override fun onCompleted() {
                Log.i(TAG, "call finished")
            }
        }
    }
}
