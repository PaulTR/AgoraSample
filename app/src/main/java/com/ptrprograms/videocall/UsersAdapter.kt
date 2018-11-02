package com.ptrprograms.videocall

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ptrprograms.videocall.databinding.ListItemUserBinding
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas


/**
 *  Known issue: video pauses when clicked on
 */
class UsersAdapter(val context: Context, val rtcEngine: RtcEngine, val userSelectedListener: UserSelectedListener) : ListAdapter<Int, UsersAdapter.ViewHolder>(UserDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val id = getItem(position)
        holder.apply {
            bind(id, context, rtcEngine, createOnClickListener(id))
            itemView.tag = id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    private fun createOnClickListener(uid: Int) : View.OnClickListener {
        return View.OnClickListener {
            userSelectedListener.userSelected(uid)
        }
    }

    class ViewHolder(private val binding: ListItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uid: Int, context: Context, rtcEngine: RtcEngine, listener: View.OnClickListener) {
            binding.apply {
                id = uid
                clickListener = listener
                val surfaceView : SurfaceView
                if( userContainer.childCount == 0 ) {
                    surfaceView = RtcEngine.CreateRendererView(context)
                    userContainer.addView(surfaceView)
                } else {
                    surfaceView = userContainer.getChildAt(0) as SurfaceView
                }

                if( uid != 0 ) {
                    rtcEngine.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
                } else {
                    rtcEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
                }

                executePendingBindings()
            }
        }
    }
}