package com.tejashaqua.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Robust network observer that tracks the actual internet connectivity status.
 * It uses the default network callback to ensure we only respond to changes
 * in the network that the system has selected for the application.
 */
class NetworkObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val observe: Flow<Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                updateStatus()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                updateStatus()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                updateStatus()
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                updateStatus()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                updateStatus()
            }

            private fun updateStatus() {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                // We check for INTERNET capability.
                // To avoid flickering, we consider it available if it has INTERNET capability.
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false

                launch {
                    send(if (hasInternet) Status.Available else Status.Lost)
                }
            }
        }

        // registerDefaultNetworkCallback is available from API 24+
        connectivityManager.registerDefaultNetworkCallback(callback)
        
        // Initial state check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isInitiallyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        launch { send(if (isInitiallyConnected) Status.Available else Status.Lost) }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}
