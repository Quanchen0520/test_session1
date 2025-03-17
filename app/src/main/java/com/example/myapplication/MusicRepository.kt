package com.example.myapplication

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

object MusicRepository {
    private val  client = OkHttpClient()
    private const val JSON_URL = "https://drive.google.com/uc?id=1Civ_XqRNn49IFHIvlIP1sJng-xE0UG2h&export=download"
    fun fetchMusicList(callback: (List<MusicItem>) -> Unit) {
        val requst = Request.Builder().url(JSON_URL).build()
        client.newCall(requst).enqueue(object : Callback {

            // System ( ctrl + O )
            override fun onFailure(call: Call, e: IOException) {
                Log.e("download", "下載失敗: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string().let { json ->
                    Log.e("download", "下載成功: $json")
                    val musicResponse = Gson().fromJson(json, MusicResponse::class.java)
                    callback(musicResponse.resultList)
                }
            }
        })
    }
}