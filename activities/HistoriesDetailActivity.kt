package com.carlosvicente.uberkotlin.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.bumptech.glide.Glide
//import com.carlosvicente.uberkotlin.databinding.ActivityHistoriesDetailBinding
import com.carlosvicente.uberkotlin.databinding.ActivityHistoryDetailBinding

import com.carlosvicente.uberkotlin.models.Client
import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.models.History
import com.carlosvicente.uberkotlin.providers.ClientProvider
import com.carlosvicente.uberkotlin.providers.DriverProvider
import com.carlosvicente.uberkotlin.providers.HistoryProvider
import com.carlosvicente.uberkotlin.utils.RelativeTime
import com.tommasoberlose.progressdialog.ProgressDialogFragment

class HistoriesDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryDetailBinding
    private var historyProvider = HistoryProvider()
    private var driverProvider = DriverProvider()
    private var extraId = ""
    private var progressDialog = ProgressDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        progressDialog.showProgressBar(this)
        extraId = intent.getStringExtra("id")!!
        getHistory()

        binding.imageViewBack.setOnClickListener { finish() }
    }

    private fun getHistory() {
        historyProvider.getHistoryById(extraId).addOnSuccessListener { document ->

            if (document.exists()) {
                val history = document.toObject(History::class.java)
                binding.textViewOrigin.text = history?.origin
                binding.textViewDestination.text = history?.destination
                binding.textViewDate.text = RelativeTime.getTimeAgo(history?.timestamp!!, this@HistoriesDetailActivity)
                binding.textViewPrice.text = "${String.format("%.1f", history?.price)}$"
                binding.textViewMyCalification.text = "${history?.calificationToDriver}"
                binding.textViewClientCalification.text = "${history?.calificationToClient}"
                binding.textViewTimeAndDistance.text = "${history?.time} Min - ${String.format("%.1f", history?.km)} Km"
                getDriverInfo(history?.idDriver!!)
            }

        }
    }

    private fun getDriverInfo(id: String) {
        driverProvider.getDriver(id).addOnSuccessListener { document ->
            if (document.exists()) {
                val driver = document.toObject(Driver::class.java)
                binding.textViewEmail.text = driver?.email
                binding.textViewName.text = "${driver?.name} ${driver?.lastname}"
                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(this).load(driver?.image).into(binding.circleImageProfile)
                    }
                }
            }
        }
        progressDialog.hideProgressBar(this)
    }
}