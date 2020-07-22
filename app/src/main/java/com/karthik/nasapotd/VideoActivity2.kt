@file:Suppress("DEPRECATION")

package com.karthik.nasapotd

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_video2.*

class VideoActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video2)
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
                Toast.makeText(this@VideoActivity2, "Exit Video", Toast.LENGTH_SHORT).show()
                true
            }
            if (videoId != null) {
                vimeoPlayer!!.initialize(videoId.toInt())
                vimeoPlayer!!.loadVideo(videoId.toInt())
            }
            vimeoPlayer!!.play()
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
        vimeoPlayer!!.onDestroy()
        finish()
    }
    override fun onResume() {
        super.onResume()
        vimeoPlayer!!.play()
    }
    override fun onPause() {
        super.onPause()
        vimeoPlayer!!.pause()
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