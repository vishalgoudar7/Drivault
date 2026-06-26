package com.owncloud.android.ui.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.util.zip.ZipFile

class ZipViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zipPath = intent.getStringExtra("ZIP_PATH") ?: return

        val zipFile = ZipFile(zipPath)

        val entries = zipFile.entries().toList().map {
            it.name
        }

        val listView = ListView(this)
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            entries
        )

        setContentView(listView)
    }
}