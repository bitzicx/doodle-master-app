package com.bitzicx.doodlemaster

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import javax.inject.Inject


@HiltAndroidApp
class DoodleMasterApp: Application(){
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.ensurePlayerIdExists()
        }
    }
}