package com.carlosvicente.uberkotlin.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.adapters.HistoriesAdapter
//import com.carlosvicente.uberkotlin.databinding.ActivityHistoriesBinding
import com.carlosvicente.uberkotlin.databinding.ActivityHistoryBinding
import com.carlosvicente.uberkotlin.models.History
import com.carlosvicente.uberkotlin.providers.HistoryProvider
import com.tommasoberlose.progressdialog.ProgressDialogFragment

class HistoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private var historyProvider = HistoryProvider()
    private var histories = ArrayList<History>()
    private lateinit var adapter: HistoriesAdapter

    private var progressDialog = ProgressDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog.showProgressBar(this)
        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistories.layoutManager = linearLayoutManager

        //CORREGIR EL ACTION BAR
        val actionBar = (this as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            // El tema actual utiliza ActionBar
            Log.d("TEMA", "ENTRO A TEMA CON ACTION VAR")
        } else {
            Log.d("TEMA", "ENTRO A TEMA SIN ACTION VAR")
            setSupportActionBar(binding.toolbar)
        }

        supportActionBar?.title = "Historial de viajes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        getHistories()
    }

    private fun getHistories() {
        histories.clear()

        historyProvider.getHistories().get().addOnSuccessListener { query ->

            if (query != null) {
                if (query.documents.size > 0) {
                    val documents = query.documents

                    for (d in documents) {
                        var history = d.toObject(History::class.java)
                        history?.id = d.id
                        histories.add(history!!)
                    }

                    adapter = HistoriesAdapter(this@HistoriesActivity, histories)
                    binding.recyclerViewHistories.adapter = adapter
                }
            }
        progressDialog.hideProgressBar(this)
        }
    }
}