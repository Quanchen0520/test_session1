package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    // 1
    private lateinit var networkMonitor: NetworkMonitor // -> 4
    private lateinit var networkCallback: NetworkCallback
    private lateinit var adapter: Adapter
    private lateinit var networkstate: TextView
    private val musicList = mutableListOf<String>()
    private val imageList = mutableListOf<String>()
    private val urlList = mutableListOf<String>()
    private val musicItemList = mutableListOf<MusicItem>()

    @SuppressLint("NotifyDataSetChanged")
    // Auto
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 2
        val viewpager = findViewById<ViewPager2>(R.id.viewpager)
        networkstate = findViewById(R.id.networkState)
        adapter = Adapter(
            musicList = musicItemList,
            onDownloadClick = { position ->
                musicItemList[position].SongName?.let { MusicName ->
                    musicItemList[position].SongURL?.let { MusicURL ->
                        downloadMusic(MusicURL, MusicName, position) // -> 3
                    }
                }
                Toast.makeText(
                    this,
                    "下載音樂:${musicItemList[position].SongName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        viewpager.adapter = adapter
        MusicRepository.fetchMusicList { MusicList ->
            runOnUiThread {
                if (MusicList.isNotEmpty()) {
                    musicList.clear()
                    imageList.clear()
                    urlList.clear()
                    musicItemList.clear()
                    musicList.addAll(MusicList.mapNotNull { it.SongName })
                    urlList.addAll(MusicList.mapNotNull { it.SongURL })
                    imageList.addAll(MusicList.mapNotNull { it.imageURL })
                    musicItemList.addAll(MusicList)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "無音樂資料", Toast.LENGTH_SHORT).show()
                }
            }
        }
        networkMonitor = NetworkMonitor(this) // -> 4
        networkCallback = object : NetworkCallback() {
            // System (onAvailable & onLost)
            override fun onAvailable(network: Network) {
                runOnUiThread { networkstate.text = "網路狀態: 已連線" }
            }

            override fun onLost(network: Network) {
                runOnUiThread { networkstate.text = "網路狀態: 已斷線" }
            }
        }
        networkMonitor.registerNetworkCallbake(networkCallback)
    }

    // 3
    private fun downloadMusic(url: String, fileName: String, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(getExternalFilesDir(null), fileName)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            val outputStream: OutputStream = file.outputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytes = 0
            val fileSize = connection.contentLength
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
                val process = (totalBytes * 100) / fileSize
                withContext(Dispatchers.Main) {
                    adapter.updateDownloadProgress(position, process)
                }
            }
            inputStream.close()
            outputStream.close()
            connection.disconnect()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Download Success", Toast.LENGTH_SHORT).show()
                Log.d("Download", "下載完成: ${file.absolutePath}")
                adapter.updateDownloadProgress(position, 100)
            }
        }
    }

    // 4
    class NetworkMonitor(context: Context) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun registerNetworkCallbake(callback: NetworkCallback) {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, callback)
        }

        fun unregisterNatworkCallback(callback: NetworkCallback) {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    // System (ctrl + O)
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregisterNatworkCallback(networkCallback)
        adapter.releaseResources()
    }
}