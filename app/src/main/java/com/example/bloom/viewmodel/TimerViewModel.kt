package com.example.bloom.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bloom.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _elapsedTime = MutableLiveData<Long>()
    val elapsedTime: LiveData<Long> = _elapsedTime

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private var timerService: TimerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true

            // Mettre à jour l'état
            _isServiceRunning.value = true

            // Écouter les updates du timer
            timerService?.setUpdateCallback { time ->
                _elapsedTime.postValue(time)
            }

            // Récupérer le temps actuel
            _elapsedTime.postValue(timerService?.getElapsedTime() ?: 0)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
            _isServiceRunning.value = false
        }
    }

    fun startTimer() {
        if (!isBound) {
            // Démarrer et binder le service
            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = "START"
            }
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            getApplication<Application>().startService(intent)
        } else {
            timerService?.startTimer()
        }
    }

    fun stopTimer() {
        if (isBound) {
            timerService?.stopTimer()
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
            _isServiceRunning.value = false
            _elapsedTime.postValue(0)
        } else {
            // Arrêter directement
            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = "STOP"
            }
            getApplication<Application>().startService(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }
    }
}