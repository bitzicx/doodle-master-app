package com.bitzicx.doodlemaster.profile_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitzicx.doodlemaster.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel()
{
    val username = userRepository.username
    val playerId = userRepository.playerId

    fun changeUserName(newName: String){
        viewModelScope.launch {
            userRepository.setUsername(newName)
        }
    }

}