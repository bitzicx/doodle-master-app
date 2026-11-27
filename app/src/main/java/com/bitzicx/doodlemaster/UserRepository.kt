package com.bitzicx.doodlemaster

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context
){

    val username : Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[UserPreferenceKeys.USERNAME]
    }

    val playerId: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[UserPreferenceKeys.PLAYER_ID]
    }

    suspend fun ensurePlayerIdExists(): String{
        var id = ""
        context.userDataStore.edit { preferences ->
            id = preferences[UserPreferenceKeys.PLAYER_ID]?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()
            preferences[UserPreferenceKeys.PLAYER_ID] = id
        }
        return id
    }

    suspend fun setUsername(newUsername: String){
        val trimmed = newUsername.trim()
        if (trimmed.isBlank()) return
        context.userDataStore.edit{preferences->
            preferences[UserPreferenceKeys.USERNAME] = trimmed
        }
    }

    suspend fun getUsernameOnce(): String? = username.first()

    suspend fun  getPlayerIdOnce(): String? = playerId.first()


}