@file:Suppress("DEPRECATION")

package com.karthik.nasapotd

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import kotlinx.android.synthetic.main.activity_video1.*

class VideoActivity1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video1)
        if (hasNetwork(this)!!) {
            this.window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            val extras = intent.extras ?: return
            val videoId = extras.getString("video_id")
            if (videoId == null) {
                finish()
            }

            exit_button.setOnClickListener { finish() }
            exit_button.setOnLongClickListener {
                Toast.makeText(this@VideoActivity1, "Exit Video", Toast.LENGTH_SHORT).show()
                true
            }
            if (videoId != null) {
                Log.e("TAG", videoId)
            }
            youtube_player_view.let { lifecycle.addObserver(it) }
            youtube_player_view.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    // do stuff with it
                    if (videoId != null) {
                        Log.e("TAG", videoId)
                    }
                    if (videoId != null) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }
            })
            youtube_player_view.enterFullScreen()
        }
        else
        {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Available!")
            builder.setMessage("Sorry! You can't play videos without internet. Try Again Later")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("OK"){ _, _ ->
                finish()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
        youtube_player_view.release()
        finish()
    }
    override fun finish() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)
        super.finish()
    }
    private fun hasNetwork(context: Context): Boolean? {
        var isConnected: Boolean? = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected)
            isConnected = true
        return isConnected
    }
}
