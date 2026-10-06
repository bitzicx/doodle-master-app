package com.bitzicx.doodlemaster.update

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repo: UpdateRepository,
    @ApplicationContext private val context: Context
): ViewModel(){


    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    private set

    init{
        viewModelScope.launch {
            val code = context.packageManager
                .getPackageInfo(context.packageName,0)
                .versionCode.toInt()
            updateInfo = repo.checkForUpdate(code)
        }
    }
    fun dismiss(){updateInfo = null}

}