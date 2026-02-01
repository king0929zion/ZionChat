package com.zionchat.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.zionDataStore: DataStore<Preferences> by preferencesDataStore(name = "zionchat")

