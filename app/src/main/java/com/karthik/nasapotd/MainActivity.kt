@file:Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.karthik.nasapotd

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.florent37.tutoshowcase.TutoShowcase
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.karthik.nasapotd.api.DataApi
import com.karthik.nasapotd.model.DataModel
import com.karthik.nasapotd.model.VimeoModel
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
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
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
    private var adCheck = 0
    var imageToDiplay: String? = null
    var sheetBehavior: BottomSheetBehavior<*>? = null
    private var videoId: String? = null
    private var mediaType: String? = null
    private var flagCheck = 0
    private var i: Intent? = null
    var vimeoImg: String? = null
    private var imgUrl: String? = null
    private var vidUrl: String? = null
    private var userchosenyear = 0
    private  var userchosenmonth:Int = 0
    private  var userchosenday:Int = 0
    private var currDay = 0
    private  var currYear:Int = 0
    private  var currMonth:Int = 0
    private var maxDate1: String? = null
    private var displayDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (hasNetwork(this)!!) {
            init()
            initblur()
            initoptions()
            fetchData()
            initswipe()
        }
        else
        {
            Toast.makeText(this@MainActivity, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
        }
    }


    private fun initshowcase() {
        TutoShowcase.from(this)
            .setContentView(R.layout.tutorial_left)
            .setListener {
                TutoShowcase.from(this)
                .setContentView(R.layout.tutorial_right)
                    .setListener {
                        TutoShowcase.from(this)
                            .setContentView(R.layout.tutorial_center_1)
                            .setListener {
                                TutoShowcase.from(this)
                                    .setContentView(R.layout.tutorial_center_2)
                                    .setListener {
                                        TutoShowcase.from(this)
                                            .setContentView(R.layout.tutorial_center_3)
                                            .show()
                                    }
                                    .on(R.id.fab_lens)
                                    .addCircle()
                                    .withBorder()
                                    .show()
                            }
                            .on(R.id.fab_calendar)
                            .addCircle()
                            .withBorder()
                            .show()
                    }
                    .setFitsSystemWindows(true)
                    .on(R.id.swiper)
                    .displaySwipableRight()
                    .delayed(399)
                    .animated(true)
                .show()
            }
            .setFitsSystemWindows(true)
            .on(R.id.swiper)
            .displaySwipableLeft()
            .delayed(399)
            .animated(true)
            .show()
    }

    private fun init() {
        MobileAds.initialize(this) {}
        val mInterstitialAd = InterstitialAd(this)

        //Test AD
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"

        //Personallized AD
        //mInterstitialAd.adUnitId = "ca-app-pub-2747296886141297/7705354849"

        mInterstitialAd.loadAd(AdRequest.Builder().build())

        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && intent.action != null
            && intent.action.equals(Intent.ACTION_MAIN)
        ) {
            finish()
            return
        }

        val window = this.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        desc_title.setOnClickListener {
            mScrollView.smoothScrollTo(
                0,
                description.top
            )
        }
        sheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        (sheetBehavior as BottomSheetBehavior<*>).isHideable = false
        (sheetBehavior as BottomSheetBehavior<*>).setBottomSheetCallback(object :
            BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING, BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> fab_calendar.visibility =
                        View.INVISIBLE
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

        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                //mInterstitialAd.show()
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                Log.e("TAG", "AD Not Working")
                initchooser()
            }

            override fun onAdClicked() {
//                onAdClosed()
//                initchooser()
            }

            override fun onAdClosed() {
                initchooser()
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

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
            if (mInterstitialAd.isLoaded && adCheck == 0) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
                initchooser()
            }

        }
        fab_calendar.setOnLongClickListener {
            Toast.makeText(this@MainActivity, "Pick Date", Toast.LENGTH_SHORT).show()
            true
        }
        fab_calendar.setOnClickListener { datePicker() }
    }

    private fun initswipe() {
        content_layout.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            @SuppressLint("SimpleDateFormat", "ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_EXPANDED) {
                    (sheetBehavior as BottomSheetBehavior<*>).state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
                return super.onTouch(v, event)
            }

            @SuppressLint("SimpleDateFormat")
            override fun onSwipeLeft() {
                if (maxDate1 != null) {
                    val items2: Array<String> =
                        displayDate!!.split("-".toRegex()).toTypedArray()
                    val dispYear = items2[0].toInt()
                    val dispMonth = items2[1].toInt()
                    val dispDay = items2[2].toInt()

                    val c = Calendar.getInstance()
                    if (displayDate == maxDate1)
                        Log.d("TAG", "Not Applied")
                    else {
                        val sdf = SimpleDateFormat("yyyy-MM-dd")
                        c.time = sdf.parse("$dispYear-$dispMonth-$dispDay")
                        c.add(Calendar.DATE, 1) // number of days to add
                        dateChosen = sdf.format(c.time)
                        Log.d("TAG", dateChosen)
                        fetchData()

                        userchosenyear = c.get(Calendar.YEAR)
                        userchosenmonth = c.get(Calendar.MONTH) + 1
                        userchosenday = c.get(Calendar.DAY_OF_MONTH)
                    }
                }
            }

            @SuppressLint("SimpleDateFormat")
            override fun onSwipeRight() {
                val c = Calendar.getInstance()

                val items2: Array<String> = displayDate!!.split("-".toRegex()).toTypedArray()
                val dispYear = items2[0].toInt()
                val dispMonth = items2[1].toInt()
                val dispDay = items2[2].toInt()

                val minYear = 1995
                val minMonth = 6
                val minDay = 16
                //Log.d("TAG", dispDay.toString()+" "+minDay+" "+(dispMonth).toString()+" "+minMonth)
                if (dispDay == minDay && dispMonth == minMonth && dispYear == minYear)
                    Log.d("TAG", "Not Applied")
                else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd")
                    c.time = sdf.parse("$dispYear-$dispMonth-$dispDay")
                    c.add(Calendar.DATE, -1) // number of days to sub
                    dateChosen = sdf.format(c.time)
                    Log.d("TAG", dateChosen)
                    fetchData()

                    userchosenyear = c.get(Calendar.YEAR)
                    userchosenmonth = c.get(Calendar.MONTH) + 1
                    userchosenday = c.get(Calendar.DAY_OF_MONTH)
                }
            }
        })
    }

    private fun initchooser()
    {
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
            if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_HIDDEN) { backToNormalFunc(); adCheck = 0} else if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_COLLAPSED || (sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_EXPANDED) {fullScreenFunc(); adCheck = 1}
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
                .cacheOnDisk(true) // default
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2) // default
                .bitmapConfig(Bitmap.Config.ARGB_8888) // default
                .displayer(SimpleBitmapDisplayer()) // default
                .handler(Handler()) // default
                .build()

        imageLoader.init(ImageLoaderConfiguration.createDefault(this))
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

    private fun datePicker() {
        val c = Calendar.getInstance()
        if (userchosenyear != 0 && userchosenmonth != 0 && userchosenday != 0) {
            currYear = userchosenyear
            currMonth = userchosenmonth - 1
            currDay = userchosenday
        } else {
            //Current Date
            currYear = c[Calendar.YEAR]
            currMonth = c[Calendar.MONTH]
            currDay = c[Calendar.DAY_OF_MONTH]
        }
        val dialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _: DatePicker?, year1: Int, month1: Int, day_of_month: Int ->
                dateChosen = "" + year1 + "-" + (month1 + 1) + "-" + day_of_month
                Log.e(dateChosen, "Check")
                Log.e(displayDate, "Check")
                userchosenyear = year1
                userchosenmonth = month1 + 1
                userchosenday = day_of_month
                val format: DateFormat =
                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                try {
                    val date = format.parse(dateChosen)
                    var finalPickedDate: String? = null
                    if (date != null) {
                        finalPickedDate = format.format(date)
                    }
                    if (displayDate != finalPickedDate) fetchData()
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            },
            currYear,
            currMonth,
            currDay
        )
        if (userchosenyear != 0 && userchosenmonth != 0 && userchosenday != 0) dialog.datePicker
            .init(
                currYear, currMonth, currDay
            ) { _, _, _, _ -> }

        //Min Date
        c[1995, 5] = 16
        dialog.datePicker.minDate = c.timeInMillis
        //Max Date
        if (maxDate1 != null) {
            val items1: Array<String> = maxDate1!!.split("-".toRegex()).toTypedArray()
            val maxYear = items1[0].toInt()
            val maxMonth = items1[1].toInt()
            val maxDay = items1[2].toInt()
            c[maxYear, maxMonth - 1] = maxDay
            dialog.datePicker.maxDate = c.timeInMillis
        } else {
            val today = System.currentTimeMillis() - 1000
            dialog.datePicker.maxDate = today
        }
        dialog.show()
    }

    fun fetchData() {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val myCache = Cache(cacheDir, cacheSize)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(myCache)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (hasNetwork(this)!!)
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                else
                    request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                chain.proceed(request)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/planetary/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        val service: DataApi = retrofit.create(DataApi::class.java)
        //first run
        call = if (flag == 0) {
            service.getFeed(DataApi.api_key)
        } else {
            service.getFeedWithDate(DataApi.api_key, dateChosen)
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
                if (response.isSuccessful) {
                    val data: DataModel? = response.body()
                    if (flag == 0) {
                        if (data != null) {
                            maxDate1 = data.date
                            flag++
                        }
                    }
                    if (data != null) {
                        displayDate = data.date
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

                                        val prefs: SharedPreferences =
                                            PreferenceManager.getDefaultSharedPreferences(
                                                baseContext
                                            )
                                        val previouslyStarted: Boolean =
                                            prefs.getBoolean(
                                                getString(R.string.pref_previously_started),
                                                false
                                            )
                                        if (!previouslyStarted) {
                                            val edit: SharedPreferences.Editor = prefs.edit()
                                            edit.putBoolean(
                                                getString(R.string.pref_previously_started),
                                                java.lang.Boolean.TRUE
                                            )
                                            edit.apply()
                                            initshowcase()
                                        }
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
                                        val prefs: SharedPreferences =
                                            PreferenceManager.getDefaultSharedPreferences(
                                                baseContext
                                            )
                                        val previouslyStarted: Boolean =
                                            prefs.getBoolean(
                                                getString(R.string.pref_previously_started),
                                                false
                                            )
                                        if (!previouslyStarted) {
                                            val edit: SharedPreferences.Editor = prefs.edit()
                                            edit.putBoolean(
                                                getString(R.string.pref_previously_started),
                                                java.lang.Boolean.TRUE
                                            )
                                            edit.apply()
                                            initshowcase()
                                        }
                                    }
                                }
                            } catch (throwable: Throwable) {
                                dialog.dismiss()
                                throwable.printStackTrace()
                            }
                        }
                    }
                }
                else {
                    dialog.dismiss()
                    when(response.code()) {
                        400 -> Toast.makeText(this@MainActivity, "Data Not Found. Try something else.", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(this@MainActivity, "Data Not Found. Try something else.", Toast.LENGTH_SHORT).show()
                        500 -> Toast.makeText(this@MainActivity, "Server Broken. Please Try Again Later", Toast.LENGTH_SHORT).show()
                        else ->{
                            Toast.makeText(this@MainActivity, R.string.network_issue, Toast.LENGTH_SHORT).show()
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

                val prefs: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(baseContext)
                val previouslyStarted: Boolean =
                    prefs.getBoolean(getString(R.string.pref_previously_started), false)
                if (!previouslyStarted) {
                    val edit: SharedPreferences.Editor = prefs.edit()
                    edit.putBoolean(
                        getString(R.string.pref_previously_started),
                        java.lang.Boolean.TRUE
                    )
                    edit.apply()
                    initshowcase()
                }
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
        val cacheSize = (5 * 1024 * 1024).toLong()
        val myCache = Cache(cacheDir, cacheSize)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(myCache)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (hasNetwork(this)!!)
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                else
                    request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                chain.proceed(request)
            }
            .build()
        val vimeophotourl = "https://vimeo.com/api/v2/video/$videoID.json/"
        val retrofit = Retrofit.Builder()
            .baseUrl(vimeophotourl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        val request = retrofit.create(DataApi::class.java)
        request.getFeedThumbnail()?.enqueue(object : Callback<List<VimeoModel>?> {
            override fun onResponse(
                call: Call<List<VimeoModel>?>,
                response: Response<List<VimeoModel>?>
            ) {
                if (response.isSuccessful) {
                if (response.body() != null) {
                    vimeoImg = response.body()!![0].thumbnail_large
                    videoThumbnailLoader(vimeoImg!!)
                }
                }
                else
                {
                    dialog.dismiss()
                    when(response.code()) {
                        400 -> Toast.makeText(this@MainActivity, "Data Not Found. Try something else.", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(this@MainActivity, "Data Not Found. Try something else.", Toast.LENGTH_SHORT).show()
                        500 -> Toast.makeText(this@MainActivity, "Server Broken. Please Try Again Later", Toast.LENGTH_SHORT).show()
                        else ->{
                            Toast.makeText(this@MainActivity, R.string.network_issue, Toast.LENGTH_SHORT).show()
                        }
                    }
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
            adCheck = 0
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

    private fun hasNetwork(context: Context): Boolean? {
        var isConnected: Boolean? = false // Initial Value
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected)
            isConnected = true
        return isConnected
    }
}

