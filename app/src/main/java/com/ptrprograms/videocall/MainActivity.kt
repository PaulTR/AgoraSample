package com.ptrprograms.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_main.*

/*
    Notes:
    Goal is to demonstrate usage, not make a feature complete app.
    Would use viewmodel and Android architecture more, navigation components, toolbar and rx

    Bonus: add screen sharing
    Status: Not going to happen because of time. Documentation: https://github.com/AgoraIO/Advanced-Video/tree/master/Custom-Media-Device/Agora-Custom-Media-Device-Android/app/src/main/java/io/agora/rtc/mediaio/app/shareScreen
 */
class MainActivity : AppCompatActivity(), UserSelectedListener {

    private val PERMISSION_REQ_ID = 22
    private val CHANNEL_NAME = "1000"
    private val userIdList = ArrayList<Int>()
    private lateinit var usersAdapter : UsersAdapter

    private var isMuted = false
    private var isPaused = false

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.video_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when( item!!.itemId ) {
            R.id.mute -> {
                isMuted = !isMuted
                rtcEngine.muteLocalAudioStream(isMuted)
            }

            R.id.hide_video -> {
                isPaused = !isPaused
                rtcEngine.muteLocalVideoStream(isPaused)
            }
        }

        return true
    }

    private fun initAgora() {
        initAgoraEngine()
        initVideoProfile()
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

    override fun onPause() {
        super.onPause()
        if( isFinishing ) {
            rtcEngine.leaveChannel()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQ_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initAgora()
                } else {
                    finish()
                }
                return
            }
            else -> {}
        }
    }
}
