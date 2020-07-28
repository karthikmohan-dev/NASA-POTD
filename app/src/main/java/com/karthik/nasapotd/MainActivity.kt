@file:Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.karthik.nasapotd

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cc.cloudist.acplibrary.ACProgressConstant
import cc.cloudist.acplibrary.ACProgressFlower
import com.github.florent37.tutoshowcase.TutoShowcase
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.karthik.nasapotd.api.DataApi
import com.karthik.nasapotd.api.DataApi.Companion.trans_api_key
import com.karthik.nasapotd.model.DataModel
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.assist.QueueProcessingType
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

    val database = Firebase.database
    private var hd: Boolean = false
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var transFlag: Int = 0
    private var call1: Call<DataApi.TransModel>? = null
    private var descText: String? = null
    private var radius = 20f
    //private var doubleBackToExitPressedOnce = false
    var imageLoader: ImageLoader = ImageLoader.getInstance()
    private lateinit var dialog: ACProgressFlower
    var options: DisplayImageOptions? = null
    private var dateChosen: String? = null
    private var call: Call<DataModel>? = null
    var flag = 0
    private var adCheck = 0
    var imageToDiplay: String? = null
    var sheetBehavior: BottomSheetBehavior<*>? = null
    private var videoId: String? = null
    private var mediaType: String? = null
    private var flagWhichVideoCheck = 0
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
        preinit()
    }

    private fun preinit() {
        if (hasNetwork(this)!!) {
            initfirebase()
            init()
            initRemoteConfig()
            initblur()
            initoptions()
            fetchData()
            initswipe()
        }
        else
        {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Available!")
            builder.setMessage("Sorry! No Internet Available. Switch on the internet and press Refresh or Try Again Later")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("OK"){ _, _ ->
                finish()
            }
            builder.setNeutralButton("Refresh"){ _, _ ->
                preinit()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
            Toast.makeText(this@MainActivity, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initfirebase() {
        firebaseAnalytics = Firebase.analytics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
            )
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d("TAG", "Key: $key Value: $value")
            }
        }
    }

    private fun initRemoteConfig() {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("TAG", "Config params updated: $updated")
                    //Toast.makeText(this, "Fetch and activate succeeded", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("TAG", "Config params updated: Not Updated!")
                    //Toast.makeText(this, "Fetch failed", Toast.LENGTH_SHORT).show()
                }
                checkvar()
            }
    }

    override fun onResume() {
        super.onResume()
        initRemoteConfig()
    }

    private fun checkvar() {
        // [START get_config_values]
        val enableTranslation = remoteConfig["enable_translation"].asString()
        //rewardsOnline = remoteConfig["rewards_enabled"].asString()
        val hdSd = remoteConfig["hd_sd"].asString()
        // [END get_config_values]
        hd = hdSd=="hd"
//      Log.e("TAG", hd.toString())
       if(enableTranslation=="true")
           spinner_language_to.visibility = View.VISIBLE
        else
           spinner_language_to.visibility = View.INVISIBLE
    }

    private fun init() {
        MobileAds.initialize(this) {}
        val mInterstitialAd = InterstitialAd(this)
        //Test AD
        //mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        //Personallized AD
        mInterstitialAd.adUnitId = DataApi.ad_id
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
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        fab_calendar.visibility = View.INVISIBLE
                        counter_fab.visibility = View.INVISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        fab_calendar.visibility = View.VISIBLE
                        counter_fab.visibility = View.VISIBLE
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
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                    baseContext
                )
            val previouslyStarted1: Boolean =
                prefs.getBoolean(
                    getString(R.string.pref_previously_started_1),
                    false
                )
            if (!previouslyStarted1) {
                val edit: SharedPreferences.Editor = prefs.edit()
                edit.putBoolean(
                    getString(R.string.pref_previously_started_1),
                    java.lang.Boolean.TRUE
                )
                edit.apply()
                initchooser()
                initshowcase1()
            }
            else if (mInterstitialAd.isLoaded && adCheck == 0) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
                initchooser()
            }
        }
        counter_fab.setOnLongClickListener {
            Toast.makeText(this@MainActivity, "Like This Picture", Toast.LENGTH_SHORT).show()
            true
        }
        counter_fab.setOnClickListener {
            counter_fab.increase()
            val counter = counter_fab.count
            val myRef = database.getReference(displayDate!!)
            myRef.setValue(counter)
            counter_fab.isEnabled = false
        }
        fab_calendar.setOnLongClickListener {
            Toast.makeText(this@MainActivity, "Pick Date", Toast.LENGTH_SHORT).show()
            true
        }
        fab_calendar.setOnClickListener { datePicker() }
        val spinner = resources.getStringArray(R.array.spinner)
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinner)
        spinner_language_to.adapter = adapter
        spinner_language_to.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {
                        if(transFlag!=0)
                        //getTranslation("en")
                        description.text = descText
                    }
                    1 -> {
                        transFlag = position
                        getTranslation("hi")
                    }
                    2 -> {
                        transFlag = position
                        getTranslation("ta")
                    }
                    3 -> {
                        transFlag = position
                        getTranslation("te")
                    }
//                    4 -> {
//                        transFlag = position
//                        getTranslation("ml")
//                    }
                    5 -> {
                        transFlag = position
                        getTranslation("mr")
                    }
                    6 -> {
                        transFlag = position
                        getTranslation("bn")
                    }
                    7 -> {
                        transFlag = position
                        getTranslation("kn")
                    }
                    8 -> {
                        transFlag = position
                        getTranslation("gu")
                    }
                    9 -> {
                        transFlag = position
                        getTranslation("pa")
                    }
                    10 -> {
                        transFlag = position
                        getTranslation("ja")
                    }
                    11 -> {
                        transFlag = position
                        getTranslation("fr")
                    }
                    12 -> {
                        transFlag = position
                        getTranslation("de")
                    }
                    else -> {
                        Toast.makeText(applicationContext, "This and other languages will be added soon.", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(applicationContext, "No option selected", Toast.LENGTH_SHORT)
                    .show()
            }
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

    private fun initshowcase1() {
        if (mediaType == "image")
        TutoShowcase.from(this)
            .setContentView(R.layout.tutorial_center_4)
            .setListener {}
            .setFitsSystemWindows(true)
            .on(R.id.imageView2)
            .show()
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

            override fun onSwipeTop() {
                if ((sheetBehavior as BottomSheetBehavior<*>).state == BottomSheetBehavior.STATE_COLLAPSED) {
                    (sheetBehavior as BottomSheetBehavior<*>).state =
                        BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
    }

    private fun initchooser()
    {
        if (mediaType == "video") {
            when (flagWhichVideoCheck) {
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
                .cacheInMemory(true) // default
                .cacheOnDisk(true) // default
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2) // default
                .bitmapConfig(Bitmap.Config.ARGB_8888) // default
                .displayer(SimpleBitmapDisplayer()) // default
                .handler(Handler()) // default
                .build()

        val config = ImageLoaderConfiguration.Builder(this)
            .threadPriority(Thread.NORM_PRIORITY - 2)
            .memoryCacheSize(20 * 1024 * 1024) // 20 Mb
            .denyCacheImageMultipleSizesInMemory()
            .diskCache(UnlimitedDiskCache(cacheDir))
            .diskCacheSize(20 * 1024 * 1024) //20 Mb
            .tasksProcessingOrder(QueueProcessingType.LIFO) //.enableLogging() // Not necessary in common
            .defaultDisplayImageOptions(options)
            .threadPoolSize(30)
            .build()

        //imageLoader.init(ImageLoaderConfiguration.createDefault(this))
        imageLoader.init(config)
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
        counter_fab.visibility = View.GONE
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
        counter_fab.visibility = View.VISIBLE
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
        dialog = if (flag == 9999) ACProgressFlower.Builder(this)
            .direction(ACProgressConstant.DIRECT_CLOCKWISE)
            .themeColor(Color.WHITE)
            .isTextExpandWidth(true)
            .text("Please Wait...")
            .fadeColor(Color.DKGRAY).build()
        else ACProgressFlower.Builder(this)
            .direction(ACProgressConstant.DIRECT_CLOCKWISE)
            .themeColor(Color.WHITE)
            .isTextExpandWidth(true)
            .text("Loading...")
            .fadeColor(Color.DKGRAY).build()
        dialog.setCancelable(false)
        dialog.show()
        call!!.enqueue(object : Callback<DataModel?> {
            @SuppressLint("SimpleDateFormat")
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
                        val myRef = database.getReference(displayDate!!)
                        myRef.addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                // This method is called once with the initial value and again
                                // whenever data at this location is updated.
                                val value = dataSnapshot.getValue<Long>()
                                Log.d("TAG", "Value is: $value")
                                if(value!=null)
                                    counter_fab.count = value.toInt()
                                else
                                    counter_fab.count = 1
                            }
                            override fun onCancelled(error: DatabaseError) {
                                // Failed to read value
                                Log.w("TAG", "Failed to read value.", error.toException())
                            }
                        })
                        counter_fab.isEnabled = true
                        when {
                            data.mediaType.equals("image") -> {
                                mediaType = data.mediaType
                                sheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                                imageToDiplay = if (flag == 9999) data.url else {
                                    if(hd)
                                        data.hdurl
                                    else
                                        data.url
                                    //data.hdurl (for high-res images)
                                }
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
                                            descText = data.explanation
                                            title_view.text = data.title
                                            image.isZoomable = false
                                            image.isTranslatable = false
                                            image.autoCenter = false
                                            image.reset(true)
                                            image.scaleType = ImageView.ScaleType.CENTER_CROP
                                            dialog.dismiss()
                                            flag = 1
                                            spinner_language_to.setSelection(0, true)
                                            transFlag = 0
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
                            }
                            data.mediaType.equals("video") -> {
                                mediaType = data.mediaType
                                fab_lens.setImageDrawable(resources.getDrawable(R.drawable.play_1))
                                date_view.text = data.date
                                sheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                                description.text = data.explanation
                                descText = data.explanation
                                title_view.text = data.title
                                spinner_language_to.setSelection(0, true)
                                transFlag = 0
                                try {
                                    Log.e("URL is->", "" + data.url)
                                    val id: String = getVideoId(data.url).toString()
                                    Log.e("URL is->", "" + id)
                                    videoId = id
                                    when {
                                        data.url!!.contains("youtu") -> {
                                            Log.e("YT", "true")
                                            imgUrl = "http://img.youtube.com/vi/$id/0.jpg"
                                            flagWhichVideoCheck = 1
                                            videoThumbnailLoader(imgUrl!!)
                                        }
                                        data.url.contains("vimeo") -> {
                                            Log.e("Vimeo", "false")
                                            //imgUrl = getvimeothumbnail(id)
                                            getvimeothumbnail(id)
                                            flagWhichVideoCheck = 2
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
                                            flagWhichVideoCheck = 3
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
                            else -> {
                                val builder = AlertDialog.Builder(this@MainActivity)
                                builder.setTitle("Data Not Available!")
                                builder.setMessage("Sorry! Data is not available on the selected date. Please try some other date.")
                                builder.setIcon(android.R.drawable.ic_dialog_alert)
                                builder.setPositiveButton("OK"){ dialog1, _ ->
                                    dialog1.cancel()
                                }
                                val alertDialog: AlertDialog = builder.create()
                                alertDialog.setCancelable(true)
                                alertDialog.show()

                                dialog.dismiss()
                            }
                        }
                    }
                }
                else {
                    dialog.dismiss()
                    if(flag == 0)
                    {
                        val c = Calendar.getInstance()
                        currYear = c[Calendar.YEAR]
                        currMonth = c[Calendar.MONTH] + 1
                        currDay = c[Calendar.DAY_OF_MONTH]
                        val sdf = SimpleDateFormat("yyyy-MM-dd")
                        c.time = sdf.parse("$currYear-$currMonth-$currDay")
                        c.add(Calendar.DATE, -1) // number of days to sub
                        maxDate1 = sdf.format(c.time)
                        dateChosen = sdf.format(c.time)
                        flag++
                        fetchData()
                    }
                    else {
                        when (response.code()) {
                            400 -> Toast.makeText(
                                this@MainActivity,
                                "Data Not Found. Try Later",
                                Toast.LENGTH_SHORT
                            ).show()
                            404 -> Toast.makeText(
                                this@MainActivity,
                                "Data Not Found. Try Later",
                                Toast.LENGTH_SHORT
                            ).show()
                            500 -> Toast.makeText(
                                this@MainActivity,
                                "Server Broken. Please Try Again Later",
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.network_issue,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
            @SuppressLint("SimpleDateFormat")
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
        val cacheSize1 = (5 * 1024 * 1024).toLong()
        val myCache1 = Cache(cacheDir, cacheSize1)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(myCache1)
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
        request.getFeedThumbnail()?.enqueue(object : Callback<List<DataApi.VimeoModel>?> {
            override fun onResponse(
                call: Call<List<DataApi.VimeoModel>?>,
                response: Response<List<DataApi.VimeoModel>?>
            ) {
                if (response.isSuccessful) {
                if (response.body() != null) {
                    vimeoImg = response.body()!![0].thumbnailLarge
                    videoThumbnailLoader(vimeoImg!!)
                }
                }
                else
                {
                    dialog.dismiss()
                    when(response.code()) {
                        400 -> Toast.makeText(this@MainActivity, "Data Not Found. Try Later.", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(this@MainActivity, "Data Not Found. Try Later", Toast.LENGTH_SHORT).show()
                        500 -> Toast.makeText(this@MainActivity, "Server Broken. Please Try Again Later", Toast.LENGTH_SHORT).show()
                        else ->{
                            Toast.makeText(this@MainActivity, R.string.network_issue, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onFailure(
                call: Call<List<DataApi.VimeoModel>?>,
                t: Throwable
            ) {
                dialog.dismiss()
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
        } else
//        {
//            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
//                return
//            }
//            doubleBackToExitPressedOnce = true
//            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
//            Handler()
//                .postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
//        }
    }

    private fun hasNetwork(context: Context): Boolean? {
        var isConnected: Boolean? = false // Initial Value
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected)
            isConnected = true
        return isConnected
    }

    private fun getTranslation(lang: String){
        val cacheSize2 = (5 * 1024 * 1024).toLong()
        val myCache2 = Cache(cacheDir, cacheSize2)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(myCache2)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (hasNetwork(this)!!)
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                else
                    request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                chain.proceed(request)
            }
            .build()
        val translateurl = "https://translate.yandex.net/api/v1.5/tr.json/"
        val retrofit = Retrofit.Builder()
            .baseUrl(translateurl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        val service: DataApi = retrofit.create(DataApi::class.java)
        call1 = service.getTranslate(trans_api_key, lang, descText)
        dialog = ACProgressFlower.Builder(this)
            .direction(ACProgressConstant.DIRECT_CLOCKWISE)
            .themeColor(Color.WHITE).
            isTextExpandWidth(true)
            .text("Loading...")
            .fadeColor(Color.DKGRAY).build()
        dialog.setCancelable(false)
        dialog.show()
        call1!!.enqueue(object : Callback<DataApi.TransModel?> {
            override fun onResponse(
                call: Call<DataApi.TransModel?>,
                response: Response<DataApi.TransModel?>
            ) {
                if (response.isSuccessful) {
                    val data: DataApi.TransModel? = response.body()
                    if (response.body() != null) {
                        description.text = data!!.texter!![0]
                        dialog.dismiss()
                    }
                }
                else
                {
                    dialog.dismiss()
                    when(response.code()) {
                        400 -> Toast.makeText(this@MainActivity, "Data Not Found. Try Later.", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(this@MainActivity, "Data Not Found. Try Later.", Toast.LENGTH_SHORT).show()
                        500 -> Toast.makeText(this@MainActivity, "Couldn't Translate. Error!", Toast.LENGTH_SHORT).show()
                        else ->{
                            Toast.makeText(this@MainActivity, R.string.network_issue, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<DataApi.TransModel?>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(
                    this@MainActivity,
                    R.string.network_issue,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}