package com.example

import android.app.Application
import com.example.data.local.StudioDatabase
import com.example.data.repository.StudioRepository

class KamarTajApp : Application() {
  val database: StudioDatabase by lazy { StudioDatabase.getInstance(this) }
  val repository: StudioRepository by lazy { StudioRepository(this, database) }
}
