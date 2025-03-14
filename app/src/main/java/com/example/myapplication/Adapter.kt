package com.example.myapplication

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import coil.load

class Adapter(
    // 1
    private val musicList: List<MusicItem>,
    private val onDownloadClick: (Int) -> Unit
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {
    // 2
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private val seekBarUpdateRunnable = HashMap<Int, Runnable>()
    private val downloadsInProcess = mutableMapOf<Int, Boolean>()
    private val downloadProgressMap = mutableMapOf<Int, Int>()
    private val downloadCompleted = mutableMapOf<Int, Boolean>()

    // 3
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val musicImage: ImageView = itemView.findViewById(R.id.musicImage)
        val playBtn: ImageButton = itemView.findViewById(R.id.playBtn)
        private val downloadBtn: ImageButton = itemView.findViewById(R.id.downloadBtn)
        val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        // 4
        fun bind(position: Int, musicItem: MusicItem) {
            itemView.findViewById<TextView>(R.id.MusicName).text = musicItem.SongName
            musicImage.load(musicItem.imageURL)
            setupUI(position)
            setupListeners(position, musicItem.SongURL)
        }

        // 5
        private fun setupUI(position: Int) {
            playBtn.setImageResource(
                if (currentPlayingPosition == position)
                    R.drawable.baseline_stop_circle_24
                else
                    R.drawable.baseline_play_circle_24
            )

            seekBar.visibility =
                if (currentPlayingPosition == position)
                    View.VISIBLE
                else
                    View.GONE

            progressBar.visibility =
                if (downloadsInProcess[position] == true)
                    View.VISIBLE
                else
                    View.GONE

            progressBar.progress = downloadProgressMap[position] ?: 0

            downloadBtn.setImageResource(
                if (downloadCompleted[position] == true)
                    R.drawable.baseline_cloud_done_24
                else
                    R.drawable.baseline_cloud_download_24
            )
            downloadBtn.isEnabled = downloadCompleted[position] != true
        }

        // 6
        private fun setupListeners(position: Int, url: String?) {
            playBtn.setOnClickListener {
                if (currentPlayingPosition == position) stopMusic()
                else url?.let { playMusic(it, position, this) }
            }
            downloadBtn.setOnClickListener {
                if (downloadCompleted[position] != true) startDownload(position)
            }
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) mediaPlayer?.seekTo(progress * mediaPlayer!!.duration / 100)
                }

                // System (ctrl + O)
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        // 10
        private fun startDownload(position: Int) {
            downloadsInProcess[position] = true
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
            onDownloadClick(position)
        }
    }

    // 7
    private fun playMusic(url: String, position: Int, holder: UserViewHolder) {
        stopMusic()
        currentPlayingPosition = position
        holder.playBtn.setImageResource(R.drawable.baseline_stop_circle_24)
        holder.seekBar.visibility = View.VISIBLE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                updateSeekBar(position, holder)
            }
            setOnCompletionListener { stopMusic() }
            prepareAsync()
        }
    }

    // 8
    private fun stopMusic() {
        currentPlayingPosition?.let {
            seekBarUpdateRunnable.remove(it)?.let { handler.removeCallbacks(it) }
            notifyItemChanged(it)
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingPosition = null
    }

    // 9
    private fun updateSeekBar(position: Int, holder: UserViewHolder) {
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        holder.seekBar.progress = (player.currentPosition * 100) / player.duration
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        seekBarUpdateRunnable[position] = runnable
        handler.post(runnable)
    }

    // 11 System(ctrl + O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.musiclist, parent, false)
    )

    override fun getItemCount() = musicList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        musicList.getOrNull(position)?.let { holder.bind(position, it) }
    }

    // 12
    fun updateDownloadProgress(position: Int, progress: Int) {
        downloadProgressMap[position] = progress
        if (progress >= 100) {
            downloadsInProcess[position] = false
            downloadCompleted[position] = true
        }
        notifyItemChanged(position)
    }

    // 13
    fun releaseResources() {
        stopMusic()
        handler.removeCallbacksAndMessages(null)
    }
}