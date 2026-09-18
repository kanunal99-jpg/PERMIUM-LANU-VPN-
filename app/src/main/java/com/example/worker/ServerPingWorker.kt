package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.util.ServerHealthChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServerPingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
      ServerHealthChecker.checkAllServers(applicationContext)
      Result.success()
    } catch (e: Exception) {
      Result.retry()
    }
  }
}
