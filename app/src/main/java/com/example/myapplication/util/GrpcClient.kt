package com.example.myapplication.util

import android.util.Log
import com.example.demo.TuneProtoGrpc
import com.example.demo.gTune
import com.example.demo.gTuneName
import com.example.demo.gAudioSample
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver

private val TAG = "GRPC"

class GrpcClient {
    companion object {
        fun getSomeTunes(mchannel: ManagedChannel, tuneName: String): String {
            val blockingStub = TuneProtoGrpc.newBlockingStub(mchannel)
            val response: Iterator<gTune> =
                blockingStub.getSomeTunes(gTuneName.newBuilder().setName(tuneName).build())

            var showText: String = ""
            while (response.hasNext()) {
                showText += response.next().name + "\n"
            }
            return showText
        }

        //        fun searchSample(
//            asyncStub: TuneProtoGrpc.TuneProtoStub,
//            audioRec: ByteArray,
//            responseObserver: StreamObserver<gTune>
//        ): String {
        fun searchSample(
            audioRec: ByteArray,
            requestObserver: StreamObserver<gAudioSample>
        ) {
//            val blockingStub = TuneProtoGrpc.newBlockingStub(mchannel)
//            val asyncStub = TuneProtoGrpc.newStub(mchannel)
            Log.i(TAG,"sending sample")

            requestObserver.onNext(
                gAudioSample.newBuilder().setAudioSample(ByteString.copyFrom(audioRec)).build()
            )

//            return response
        }
    }
}