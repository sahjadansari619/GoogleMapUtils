package com.dhwaniris.locationutils

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import java.util.*


/**
 * Created by ${Sahjad} on 01/29/2019.
 */
class LocationHandler(val context: Context) : GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    var gpsStatus: LocationStateCallback? = null

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    private val requestCodeLocation = 998
    var mService: LocationUpdatesService? = null
    var mBound: Boolean = false
    private var locationHandlerListener: LocationHandlerListener? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    fun setGPSonOffListener(locationHandlerListener: LocationHandlerListener) {
        this.locationHandlerListener = locationHandlerListener

    }


    private val mGpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action!!.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                onStop()
                startGpsService()
            }
        }
    }

    fun startGpsService() {
        val lacManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (lacManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            if (lacManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                context.bindService(Intent(context, LocationUpdatesService::class.java),
                        mServiceConnection,
                        Context.BIND_AUTO_CREATE)
                gpsStatus?.onGPSChanged(true)
            } else {
                enableGPS()
            }
        } else if (lacManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            if (lacManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                context.bindService(Intent(context, LocationUpdatesService::class.java),
                        mServiceConnection,
                        Context.BIND_AUTO_CREATE)
                gpsStatus?.onGPSChanged(true)
            }
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        context.registerReceiver(mGpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

    }

    //Enabling gps from app

    var mGoogleApiClient: GoogleApiClient? = null
    var googleApiclintID = Random().nextInt(10)

    private fun enableGPS() {
        mGoogleApiClient = GoogleApiClient.Builder(context)
                .enableAutoManage(context as androidx.fragment.app.FragmentActivity, ++googleApiclintID, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        mGoogleApiClient?.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (30 * 1000).toLong()
        locationRequest.fastestInterval = (5 * 1000).toLong()
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build())
        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(context as Activity, requestCodeLocation)
                } catch (e: IntentSender.SendIntentException) {
                    // Ignore the error.
                }

            }
        }

    }

    override fun onConnected(p0: Bundle?) {

    }

    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient?.connect()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == requestCodeLocation) {
            if (resultCode == Activity.RESULT_OK) {
                startGpsService()
            } else if (resultCode == Activity.RESULT_CANCELED) {
            /*    AlertDialog.Builder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.gps_is_required_for_this_form)
                        .setPositiveButton(R.string.ok) { dialogInterface, i ->
                            enableGPS()
                            locationHandlerListener?.acceptedGPS()
                            dialogInterface.cancel()
                        }
                        *//*.setNegativeButton(R.string.cancel) { dialogInterface, i ->
                            locationHandlerListener?.deniedGPS()
                            dialogInterface.cancel()
                        }*//*
                        .setCancelable(false)
                        .show()*/
            }
        }

    }

    fun onStop() {
        try {
            context.unregisterReceiver(mGpsSwitchStateReceiver)
        } catch (e: IllegalArgumentException) {

        }
        gpsStatus?.onGPSChanged(false)
        mService?.removeLocationUpdates()
        if (mBound) {
            context.unbindService(mServiceConnection)
            mBound = false
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        onDestroy()
    }

    fun setGPSstatusListener(gpsStatus: LocationStateCallback) {
        this.gpsStatus = gpsStatus
    }

    fun onDestroy() {
        mGoogleApiClient?.disconnect()
        mGoogleApiClient = null
    }

}

interface LocationStateCallback {
    fun onGPSChanged(status: Boolean) {

    }
}