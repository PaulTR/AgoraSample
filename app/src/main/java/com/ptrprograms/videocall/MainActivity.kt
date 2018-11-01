package com.ptrprograms.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item_user.*
import java.lang.Exception

class MainActivity : AppCompatActivity(), UserSelectedListener {

    private val PERMISSION_REQ_ID = 22
    private val CHANNEL_NAME = "1000"
    private val userIdList = ArrayList<Int>()
    private lateinit var usersAdapter : UsersAdapter

    private lateinit var rtcEngine : RtcEngine
    private val rtcEventHandler = object: IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            runOnUiThread { addUserToList(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            runOnUiThread { removeUserFromList(uid) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQ_ID)
        } else {
            initAgora()
        }
    }

    private fun initAgora() {
        initAgoraEngine()
        initVideoProfile()
        //initLocalVideo()
        initChannel()
        initViews()
    }

    private fun initViews() {
        usersAdapter = UsersAdapter(this, rtcEngine, this)
        userIdList.add(0)
        usersAdapter.submitList(userIdList)
        list_participants.adapter = usersAdapter
    }

    private fun initAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.agora_id), rtcEventHandler)
        } catch( e: Exception ) {
            Log.e("Test", "initagoraengine fail")
        }
    }

    private fun initVideoProfile() {
        rtcEngine.enableVideo()

        rtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT)
        )
    }

    private fun addUserToList(uid: Int) {
        userIdList.add(uid)
        usersAdapter.submitList(userIdList)
        list_participants.adapter = usersAdapter
    }

    private fun removeUserFromList(uid: Int) {
        userIdList.remove(uid)
        usersAdapter.submitList(userIdList)
        list_participants.adapter = usersAdapter
    }

    private fun initChannel() {
        rtcEngine.joinChannel(null, CHANNEL_NAME, "", 0)
    }

    private fun showUserOnMainVideo(uid: Int) {
        if( video_container_main.childCount >= 1 ) {
            video_container_main.removeAllViews()
        }

        val surfaceView = RtcEngine.CreateRendererView(this)
        video_container_main.addView(surfaceView)
        if( uid == 0 ) {
            rtcEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        } else {
            rtcEngine.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        }
    }

    override fun userSelected(uid: Int) {
        showUserOnMainVideo(uid)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQ_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initAgora()
                } else {
                    //Show a message explaining that they need to grant permissions to use the app
                    finish()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
