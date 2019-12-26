package com.dhwaniris.locationutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.MutableLiveData


/**
 * Created by ${Sahjad} on 01/29/2019.
 */
class LocationReceiver(locationData: MutableLiveData<Location>) : BroadcastReceiver() {
    private var locationData: MutableLiveData<Location> = locationData

    override fun onReceive(p0: Context?, p1: Intent?) {
        val location = p1?.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)
        if (location?.accuracy != 0.0f)
            locationData.value = location
    }

}