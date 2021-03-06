package com.lvvi.hotsearch.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lvvi.hotsearch.R
import com.lvvi.hotsearch.ui.douyin.DouyinFragment
import com.lvvi.hotsearch.model.DouyinModel
import com.lvvi.hotsearch.utils.Utils
import kotlinx.android.synthetic.main.item_douyin.view.*
import java.util.*
import kotlin.collections.ArrayList

class DouyinAdapter() :
    RecyclerView.Adapter<DouyinAdapter.ViewHolder>() {

    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var handler: DouyinFragment.MyHandler

    private lateinit var updateTime: String
    private var beans = ArrayList<DouyinModel.AwemeListBean>()

    private var currPlayingPosition = 0

    private var timer = Timer()

    private var webView: WebView? = null
    private var myWebViewClient: MyWebViewClient? = null

    fun setData(beans: ArrayList<DouyinModel.AwemeListBean>){
        this.beans = beans
        notifyDataSetChanged()
    }

    fun getData(): ArrayList<DouyinModel.AwemeListBean> {
        return beans
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun setUpdateTime(updateTime: String) {
        this.updateTime = updateTime
    }

    fun setHandler(handler: DouyinFragment.MyHandler) {
        this.handler = handler
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_douyin, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return beans.size
    }

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.update.visibility = View.VISIBLE
            holder.update.text = updateTime
        } else {
            holder.update.visibility = View.GONE
        }
        when (position) {
            0 -> holder.index.setTextColor(ContextCompat.getColor(context, R.color.others))
            1 -> holder.index.setTextColor(ContextCompat.getColor(context, R.color.others))
            2 -> holder.index.setTextColor(ContextCompat.getColor(context, R.color.others))
            else -> holder.index.setTextColor(ContextCompat.getColor(context, R.color.colorButtonBg))
        }
        holder.index.text = String.format(context.resources.getString(R.string.dot), position + 1)

        val coverUrl = if (beans[position].aweme_info.video.cover.url_list.size > 0) {
            beans[position].aweme_info.video.cover.url_list[0].replace("300x400", "226x400")
        } else {
            beans[position].aweme_info.video.dynamic_cover.url_list[0]
        }

        Glide.with(holder.cover)
            .load(coverUrl)
            .placeholder(R.mipmap.default_pic)
            .into(holder.cover)

        holder.title.text = beans[position].aweme_info.desc
        holder.nickName.text = beans[position].aweme_info.author.nickname
        holder.hotValue.text = beans[position].hot_value.toString()

        holder.title.visibility = View.VISIBLE
        holder.nickName.visibility = View.VISIBLE
        holder.fire.visibility = View.VISIBLE
        holder.hotValue.visibility = View.VISIBLE

        val layoutParams = holder.video.layoutParams
        layoutParams.width = Utils.convertDpToPixel(context, 113.toFloat())
        layoutParams.height = Utils.convertDpToPixel(context, 200.toFloat())
        holder.video.layoutParams = layoutParams

        if (beans[position].aweme_info.video.isSelected) {
            prepareVideo(holder, position)
        } else {
            hideVideo(holder)
        }

        holder.video.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_UP -> {
                    Log.e("douyin adapter", "videoView onTouch action up")
                    val message = Message()
                    message.what = DouyinFragment.HANDLER_ITEM_CLICK
                    message.arg1 = position
                    handler.handleMessage(message)
                }
            }
            true
        }

        holder.itemView.setOnClickListener {
            Log.e("douyin adapter", "itemView onClick isPlaying: ${holder.video.isPlaying} currentPosition: ${holder.video.currentPosition}")
            if (holder.video.isPlaying) {
                holder.video.pause()
                holder.cover.visibility = View.VISIBLE
                holder.play.visibility = View.VISIBLE
            } else {
                if (holder.video.visibility == View.VISIBLE) {
                    holder.video.start()
                    holder.play.visibility = View.GONE
                } else {
                    holder.cover.visibility = View.VISIBLE
                    holder.play.visibility = View.VISIBLE
                    playNext(position)
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            val message = Message()
            message.what = DouyinFragment.HANDLER_ITEM_LONG_CLICK
            message.obj = beans[position].aweme_info.video.play_addr.url_list[0]
            handler.handleMessage(message)
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun prepareVideo(holder: ViewHolder, position: Int) {
        timer.cancel()
        if (holder.cover.visibility == View.GONE) {
            holder.cover.visibility = View.VISIBLE
        }

        if (beans[position].aweme_info.video.url.isNullOrEmpty()) {
            if (webView == null) {
                webView = WebView(context)
                webView?.settings?.javaScriptEnabled = true
                myWebViewClient = MyWebViewClient()
                webView?.webViewClient = myWebViewClient
            }
            myWebViewClient?.setData(holder, position)
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.loadUrl(
                beans[position].aweme_info.video.play_addr.url_list[0].replace("playwm", "play"))
        } else {
            if (beans[position].aweme_info.video.isPlaying) {
                currPlayingPosition = position

                playVideo(holder, position)
            } else {
                if (holder.video.isPlaying) {
                    holder.video.pause()
                    holder.video.seekTo(0)
                }
                holder.cover.visibility = View.VISIBLE
                holder.video.visibility = View.GONE
                holder.duration.visibility = View.GONE
                holder.play.visibility = View.VISIBLE
                holder.progressBar.visibility = View.GONE
            }
        }
    }

    private fun hideVideo(holder: ViewHolder) {
        timer.cancel()
        if (holder.video.isPlaying) {
            holder.video.pause()
            holder.video.seekTo(0)
        }

        holder.cover.visibility = View.VISIBLE
        holder.video.visibility = View.GONE
        holder.duration.visibility = View.GONE
        holder.play.visibility = View.VISIBLE
        holder.progressBar.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun playVideo(holder: ViewHolder, position: Int) {
        if (holder.video.currentPosition > 0) {
            if (holder.progressBar.visibility == View.VISIBLE) {
                holder.progressBar.visibility = View.GONE
            }
            if (holder.cover.visibility == View.VISIBLE) {
                holder.cover.visibility = View.GONE
            }

            holder.video.start()
            return
        }

        holder.video.setOnPreparedListener {
            Log.e("douyin adapter", "OnPrepared")
            it.setVolume(0f, 0f)
            it.isLooping = true

            if (holder.video.currentPosition == 0) {
                startDurationTimer(holder, beans[position].aweme_info.video.duration)
            }

            if (it.videoWidth > it.videoHeight) {
                setHorizontalLayoutParams(holder)
            }
        }

        holder.video.setOnErrorListener { _, what, _ ->
            Log.e("douyin adapter", "OnError what: $what")
            if (holder.video.isPlaying) {
                holder.video.pause()
                holder.video.seekTo(0)
            }
            if (webView == null) {
                webView = WebView(context)
                webView?.settings?.javaScriptEnabled = true
                myWebViewClient = MyWebViewClient()
                webView?.webViewClient = myWebViewClient
            }
            myWebViewClient?.setData(holder, position)
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.loadUrl(
                beans[position].aweme_info.video.play_addr.url_list[0].replace("playwm", "play"))
            true
        }

        holder.video.setOnCompletionListener {
//            scrollToNext(position)
//            playNext(position + 1)
        }

        holder.video.setVideoURI(Uri.parse(beans[position].aweme_info.video.url))
        holder.video.requestFocus()
        holder.video.start()

        holder.duration.visibility = View.GONE
        holder.play.visibility = View.GONE
        holder.progressBar.visibility = View.VISIBLE
        holder.video.visibility = View.VISIBLE
    }

    private fun setHorizontalLayoutParams(holder: ViewHolder) {
        val layoutParams = holder.video.layoutParams
        layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
        layoutParams.height = Utils.convertDpToPixel(context, 200.toFloat())
        holder.video.layoutParams = layoutParams

        holder.title.visibility = View.GONE
        holder.nickName.visibility = View.GONE
        holder.fire.visibility = View.GONE
        holder.hotValue.visibility = View.GONE

        holder.progressBar.visibility = View.GONE
        holder.cover.visibility = View.GONE
    }

    //unused
    private fun scrollToNext(position: Int) {
        val message = Message()
        message.what = DouyinFragment.HANDLER_ITEM_SCROLL_TO_NEXT
        message.arg1 = position
        handler.sendMessage(message)
    }

    private fun playNext(position: Int) {
        for ((index, bean) in beans.withIndex()) {
            bean.aweme_info.video.isSelected = false
            bean.aweme_info.video.isPlaying = false
            if (index == position) {
                bean.aweme_info.video.isSelected = true
                bean.aweme_info.video.isPlaying = true
            }
        }
        notifyDataSetChanged()
    }

    private fun startDurationTimer(holder: ViewHolder, duration: Int) {
        var mDuration = duration

        timer.cancel()
        timer = Timer()

        var timerTaskExecuteTimes = 0

        timer.schedule(object : TimerTask() {
            override fun run() {
                timerTaskExecuteTimes ++

                if (timerTaskExecuteTimes >= 10) {
                    timer.cancel()

                    activity.runOnUiThread {
                        holder.duration.visibility = View.GONE
                    }
                } else {
                    if (duration >= 1000 * 60) {
                        mDuration -= 500
                        activity.runOnUiThread {
                            if (holder.duration.visibility == View.GONE) {
                                holder.duration.visibility = View.VISIBLE
                            }
                            holder.duration.text = Utils.getTime(mDuration.toLong())
                        }
                    }
                }

                if (holder.video.currentPosition > 0) {
                    if (holder.progressBar.visibility == View.VISIBLE) {
                        activity.runOnUiThread {
                            holder.progressBar.visibility = View.GONE
                        }
                    }
                    if (holder.cover.visibility == View.VISIBLE) {
                        activity.runOnUiThread {
                            holder.cover.visibility = View.GONE
                        }
                    }
                }
            }

        }, 500, 500)
    }

    inner class MyWebViewClient: WebViewClient() {
        private var mHolder: ViewHolder? = null
        private var mPosition = -1

        fun setData(holder: ViewHolder, position: Int) {
            mHolder = holder
            mPosition = position
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view?.loadUrl("javascript:document.getElementsByTagName(\"video\")[0].pause();")

            beans[mPosition].aweme_info.video.url = url
            if (beans[mPosition].aweme_info.video.isPlaying) {
                playVideo(mHolder!!, mPosition)
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val update: TextView = itemView.update_time_tv
        val index: TextView = itemView.index_tv
        val cover: ImageView = itemView.cover_iv
        val title: TextView = itemView.title_tv
        val nickName: TextView = itemView.nick_name_tv
        val fire: ImageView = itemView.fire_iv
        val hotValue: TextView = itemView.hot_value_tv
        val video: VideoView = itemView.video_view
        val duration: TextView = itemView.duration_tv
        val play: Button = itemView.play_btn
        val progressBar: ProgressBar = itemView.progressBar
    }

}