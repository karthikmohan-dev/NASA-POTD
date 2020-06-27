@file:Suppress("DEPRECATION")

package com.example.nasapotd

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nasapotd.api.DataApi
import com.example.nasapotd.model.DataModel
import com.example.nasapotd.model.VimeoModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var radius = 20f
    private var doubleBackToExitPressedOnce = false
    var imageLoader: ImageLoader = ImageLoader.getInstance()
    private lateinit var dialog: ProgressDialog
    var options: DisplayImageOptions? = null
    private var dateChosen: String? = null
    private var call: Call<DataModel>? = null
    var flag = 0
    var imageToDiplay: String? = null
    var sheetBehavior: BottomSheetBehavior<*>? = null
    private var videoId: String? = null
    private var mediaType: String? = null
    private var maxDate: String? = null
    private var flagCheck = 0
    private var i: Intent? = null
    var vimeoImg: String? = null
    private var imgUrl: String? = null
    private var vidUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initCustom()
        imageLoader.init(ImageLoaderConfiguration.createDefault(this))
        initoptions()
        desc_title.setOnClickListener {
            mScrollView.smoothScrollTo(
                0,
                description.top
            )
        }
        sheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        (sheetBehavior as BottomSheetBehavior<*>).isHideable = false
        (sheetBehavior as BottomSheetBehavior<*>).setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING, BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> fab_calendar.visibility = View.INVISIBLE
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        fab_calendar.visibility = View.VISIBLE
                        mScrollView.smoothScrollTo(0, description.top)
                    }
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float
            ) {
            }
        })
        fab_lens.setOnLongClickListener {
            if (mediaType == "image") Toast.makeText(
                this@MainActivity,
                "Zoom in/out ",
                Toast.LENGTH_SHORT
            ).show() else if (mediaType == "video") Toast.makeText(
                this@MainActivity,
                "Play Video ",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
        fab_lens.setOnClickListener {
            if (mediaType == "video") {
                when (flagCheck) {
                    1 -> {
                        i = Intent(this@MainActivity, VideoActivity1::class.java)
                        i!!.putExtra("video_id", videoId)
                        startActivityForResult(i, 400)
                    }
                    2 -> {
                        i = Intent(this@MainActivity, VideoActivity2::class.java)
                        i!!.putExtra("video_id", videoId)
                        startActivityForResult(i, 400)
                    }
                    else -> {
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(vidUrl))
                        startActivity(browserIntent)
                    }
                }
            } else {
                if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_HIDDEN) backToNormalFunc() else if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_COLLAPSED || (sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_EXPANDED) fullScreenFunc()
            }
        }
        fab_calendar.setOnLongClickListener {
            Toast.makeText(this@MainActivity, "Pick Date", Toast.LENGTH_SHORT).show()
            true
        }
        fab_calendar.setOnClickListener { datepicker() }
        content_layout.setOnClickListener {
            if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_EXPANDED) {
                (sheetBehavior as BottomSheetBehavior<*>).state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        initblur()
        fetchData()
    }

    private fun initCustom() {
        val window = this.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    private fun initoptions() {
        options =
            DisplayImageOptions.Builder()
                //                .showImageOnLoading(R.drawable.loading) // resource or drawable
                //                .showImageForEmptyUri(R.drawable.empty) // resource or drawable
                //                .showImageOnFail(R.drawable.error) // resource or drawable
                .resetViewBeforeLoading(false) // default
                //.delayBeforeLoading(1000)
                .cacheInMemory(true) // default
                .cacheOnDisk(false) // default
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2) // default
                .bitmapConfig(Bitmap.Config.ARGB_8888) // default
                .displayer(SimpleBitmapDisplayer()) // default
                .handler(Handler()) // default
                .build()
    }

    private fun fullScreenFunc() {
        sheetBehavior!!.isHideable = true
        sheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.isZoomable = true
        image.isTranslatable = true
        image.autoCenter = true
        fab_lens.setImageDrawable(resources.getDrawable(R.drawable.zoom_off))
        blurView.visibility = View.GONE
        fab_calendar.visibility = View.GONE
    }

    private fun backToNormalFunc() {
        sheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        sheetBehavior!!.isHideable = false
        image.isZoomable = false
        image.isTranslatable = false
        image.autoCenter = false
        image.reset(true)
        blurView.visibility = View.VISIBLE
        fab_calendar.visibility = View.VISIBLE
        if (mediaType == "video") {
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            fab_lens.setImageDrawable(resources.getDrawable(R.drawable.play_1))
        } else {
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            fab_lens.setImageDrawable(resources.getDrawable(R.drawable.zoom_on))
        }
    }

    private fun initblur() {
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView.background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
            blurView.clipToOutline = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurAlgorithm(RenderScriptBlur(this))
                .setBlurRadius(radius)
                .setHasFixedTransformationMatrix(true)
        }
    }

    private fun datepicker() {
        val c = Calendar.getInstance()
        //Current Date
        val curryear = c[Calendar.YEAR]
        val currmonth = c[Calendar.MONTH]
        val currday = c[Calendar.DAY_OF_MONTH]
        val dialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _: DatePicker?, year1: Int, month1: Int, day_of_month: Int ->
                dateChosen = "" + year1 + "-" + (month1 + 1) + "-" + day_of_month
                Log.e(dateChosen, "Check")
                fetchData()
            },
            curryear,
            currmonth,
            currday
        )
        //Min Date
        c[1995, 5] = 16
        dialog.datePicker.minDate = c.timeInMillis
        //Max Date
        if (maxDate != null) {
            val items1 = maxDate!!.split("-".toRegex()).toTypedArray()
            val maxyear = items1[0].toInt()
            val maxmonth = items1[1].toInt()
            val maxday = items1[2].toInt()
            c[maxyear, maxmonth - 1] = maxday
            dialog.datePicker.maxDate = c.timeInMillis
        } else {
            val today = System.currentTimeMillis() - 1000
            dialog.datePicker.maxDate = today
        }
        dialog.show()
    }

    fun fetchData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/planetary/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: DataApi = retrofit.create(DataApi::class.java)
        //first run
        call = if (flag == 0) {
            service.get_feed(DataApi.api_key)
        } else {
            service.get_feed_with_date(DataApi.api_key, dateChosen)
        }
        dialog = if (flag == 9999) ProgressDialog.show(
            this@MainActivity,
            "",
            "Taking more time than usual. Please wait...",
            true
        ) else ProgressDialog.show(this@MainActivity, "", "Loading. Please wait...", true)
        call!!.enqueue(object : Callback<DataModel?> {
            override fun onResponse(
                call: Call<DataModel?>,
                response: Response<DataModel?>
            ) {
                val data: DataModel? = response.body()
                if (flag == 0) {
                    if (data != null) {
                        maxDate = data.date
                        flag++
                    }
                }
                if (data != null) {
                    if (data.media_type.equals("image")) {
                        mediaType = data.media_type
                        sheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                        imageToDiplay = if (flag == 9999) data.url else data.hdurl
                        imageLoader.displayImage(imageToDiplay,
                            image,
                            options,
                            object : ImageLoadingListener {
                                override fun onLoadingStarted(
                                    imageUri: String,
                                    view: View
                                ) {
                                }

                                override fun onLoadingFailed(
                                    imageUri: String,
                                    view: View,
                                    failReason: FailReason
                                ) {
                                    if (flag == 9999) {
                                        Log.e("TAg", "failed")
                                        dialog.dismiss()
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.server_issue,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        dialog.dismiss()
                                        flag = 9999
                                        fetchData()
                                    }
                                }

                                override fun onLoadingComplete(
                                    imageUri: String,
                                    view: View,
                                    loadedImage: Bitmap
                                ) {
                                    fab_lens.setImageDrawable(resources.getDrawable(R.drawable.zoom_on))
                                    date_view.text = data.date
                                    description.text = data.explanation
                                    title_view.text = data.title
                                    image.isZoomable = false
                                    image.isTranslatable = false
                                    image.autoCenter = false
                                    image.reset(true)
                                    image.scaleType = ImageView.ScaleType.CENTER_CROP
                                    dialog.dismiss()
                                    flag = 1
                                }

                                override fun onLoadingCancelled(
                                    imageUri: String,
                                    view: View
                                ) {
                                    dialog.dismiss()
                                }
                            }
                        ) { _: String?, _: View?, _: Int, _: Int -> }
                    } else if (data.media_type.equals("video")) {
                        mediaType = data.media_type
                        fab_lens.setImageDrawable(resources.getDrawable(R.drawable.play_1))
                        date_view.text = data.date
                        sheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                        description.text = data.explanation
                        title_view!!.text = data.title
                        try {
                            Log.e("URL is->", "" + data.url)
                            val id: String = getVideoId(data.url).toString()
                            Log.e("URL is->", "" + id)
                            videoId = id
                            when {
                                data.url!!.contains("youtu") -> {
                                    Log.e("YT", "true")
                                    imgUrl = "http://img.youtube.com/vi/$id/0.jpg"
                                    flagCheck = 1
                                    videoThumbnailLoader(imgUrl!!)
                                }
                                data.url.contains("vimeo") -> {
                                    Log.e("Vimeo", "false")
                                    imgUrl = getvimeothumbnail(id)
                                    flagCheck = 2
                                }
                                else -> {
                                    image.setImageDrawable(resources.getDrawable(R.drawable.loading))
                                    image.isZoomable = false
                                    image.isTranslatable = false
                                    image.autoCenter = false
                                    image.reset(true)
                                    image.scaleType = ImageView.ScaleType.FIT_CENTER
                                    vidUrl = data.url
                                    dialog.dismiss()
                                    flagCheck = 3
                                }
                            }
                        } catch (throwable: Throwable) {
                            dialog.dismiss()
                            throwable.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<DataModel?>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@MainActivity, R.string.network_issue, Toast.LENGTH_SHORT).show()
            }
        })
    }

//    fun isYoutubeUrl(youTubeURl: String): Boolean {
//        val success: Boolean
//        val pattern = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+"
//        // Not Valid youtube URL
//        success = !youTubeURl.isEmpty() && youTubeURl.matches(pattern)
//        return success
//    }

    private fun videoThumbnailLoader(img_url: String) {
        imageLoader.displayImage(img_url, image, options, object : ImageLoadingListener {
            override fun onLoadingStarted(
                imageUri: String,
                view: View
            ) {
            }

            override fun onLoadingFailed(
                imageUri: String,
                view: View,
                failReason: FailReason
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Error. Please try again later.",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            }

            override fun onLoadingComplete(
                imageUri: String,
                view: View,
                loadedImage: Bitmap
            ) {
                image.isZoomable = false
                image.isTranslatable = false
                image.autoCenter = false
                image.reset(true)
                image.scaleType = ImageView.ScaleType.FIT_CENTER
                dialog.dismiss()
            }

            override fun onLoadingCancelled(
                imageUri: String,
                view: View
            ) {
                dialog.dismiss()
            }
        }
        ) { _: String?, _: View?, _: Int, _: Int -> }
    }

    private fun getVideoId(url: String?): String? {
        val idgroup = 6
        var videoId: String? = ""
        if (url != null && url.trim { it <= ' ' }.isNotEmpty()) {
            val expression =
                "(http:|https:|)\\/\\/(player.|www.)?(vimeo\\.com|youtu(be\\" +
                        ".com|\\.be|be\\.googleapis\\.com))\\/(video\\/|embed\\/|watch\\?v=|v\\/)?" +
                        "([A-Za-z0-9._%-]*)(\\&\\S+)?"
            val pattern = Pattern.compile(
                expression,
                Pattern.CASE_INSENSITIVE
            )
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                val groupIndex = matcher.group(idgroup)
                if (groupIndex != null) {
                    videoId = groupIndex
                }
            }
        }
        return videoId
    }

    private fun getvimeothumbnail(videoID: String): String? {
        val vimeophotourl = "https://vimeo.com/api/v2/video/$videoID.json/"
        val retrofit = Retrofit.Builder()
            .baseUrl(vimeophotourl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val request = retrofit.create(DataApi::class.java)
        request.get_feed_thumbnail()?.enqueue(object : Callback<List<VimeoModel>?> {
            override fun onResponse(
                call: Call<List<VimeoModel>?>,
                response: Response<List<VimeoModel>?>
            ) {
                if (response.body() != null) {
                    vimeoImg = response.body()!![0].thumbnail_large
                    videoThumbnailLoader(vimeoImg!!)
                }
            }

            override fun onFailure(
                call: Call<List<VimeoModel>?>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't load thumbnail. Error!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        return vimeoImg
    }

    override fun onBackPressed() {
        if (sheetBehavior!!.state == BottomSheetBehavior.STATE_HIDDEN || sheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            backToNormalFunc()
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
            Handler()
                .postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
    }

}