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

import kotlinx.coroutines.delay

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
                // When a network is lost, we check if there's any other fallback network
                // before immediately reporting as Lost.
                updateStatus(isLosing = true)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                updateStatus()
            }

            private fun updateStatus(isLosing: Boolean = false) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                // We check for INTERNET capability AND VALIDATED status.
                // VALIDATED means the system has confirmed there is actual internet access.
                val hasInternet = capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                launch {
                    if (!hasInternet && isLosing) {
                        // Small delay before confirming loss to handle network handovers (WiFi -> Data)
                        delay(2000)
                        val retryCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                        val stillNoInternet = retryCapabilities == null ||
                                !retryCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        
                        if (stillNoInternet) {
                            send(Status.Lost)
                        }
                    } else {
                        send(if (hasInternet) Status.Available else Status.Lost)
                    }
                }
            }
        }

        // registerDefaultNetworkCallback is available from API 24+
        connectivityManager.registerDefaultNetworkCallback(callback)
        
        // Initial state check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isInitiallyConnected = capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        launch { send(if (isInitiallyConnected) Status.Available else Status.Lost) }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}
