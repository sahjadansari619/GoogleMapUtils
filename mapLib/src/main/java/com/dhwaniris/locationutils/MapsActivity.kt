package com.dhwaniris.locationutils

import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionHandlerListener {

    private lateinit var mMap: GoogleMap

    private lateinit var locationReceiver: LocationReceiver
    private lateinit var locationHandler: LocationHandler
    private lateinit var permissionHandler: PermissionHandler
    private var locationLive: MutableLiveData<Location> = MutableLiveData()
    private var locationTrack: ArrayList<Location> = ArrayList()
    private var locationLatLng: ArrayList<LatLng> = ArrayList()
    private var locationTrackByMark: ArrayList<Location> = ArrayList()
    private var locationLatLngByMark: ArrayList<LatLng> = ArrayList()

    private var isMapReady = false
    private var isStarted = false
    var zoomDone = false
    private var lastLocation: Location? = null
    private lateinit var lastCapcturedLocation: Location
    private val polylineOptions = PolylineOptions()
    private var lastPolyLine: Polyline? = null
    private var lastPolygon: Polygon? = null
    private var resultInMtr: Double = 0.0
    private var computeType = AREA


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        intent.extras?.let { extraData ->
            computeType = extraData.getString(COMPUTE_TYPE, AREA)

        }
        locationHandler = LocationHandler(this)
        permissionHandler = PermissionHandler(this, this)
        locationLive.observe(this, Observer { currentLocation ->
            if (isMapReady && currentLocation != null) {
                lastLocation = currentLocation
                if (!zoomDone) {
                    zoomDone = true
                    zoomTo(currentLocation.latitude, currentLocation.longitude)
                    lastCapcturedLocation = currentLocation
                }
                if (isStarted && validateLocation(lastCapcturedLocation, currentLocation)) {
                    lastCapcturedLocation = currentLocation
                    moveCamera(currentLocation.latitude, currentLocation.longitude)
                    locationTrack.add(currentLocation)
                    locationLatLng.add(LatLng(currentLocation.latitude, currentLocation.longitude))
                    drawLine(currentLocation)
                }
            }
        })
        locationReceiver = LocationReceiver(locationLive)


        btnMark.visibility = View.INVISIBLE
        btnStop.visibility = View.INVISIBLE
        btnStart.setOnClickListener {
            when {
                lastLocation == null -> {
                    Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show()
                }
                lastLocation!!.accuracy > 15 -> {
                    Toast.makeText(
                        this,
                        "Accuracy is ${lastLocation!!.accuracy.toInt()}, Please wait",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    startPath()
                    btnMark.visibility = View.VISIBLE
                    btnStop.visibility = View.VISIBLE
                    btnStart.visibility = View.INVISIBLE

                }
            }
        }
        btnMark.setOnClickListener {
            mark()
        }
        btnStop.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Are you sure?")
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    stopAction()
                }

                .setNegativeButton(R.string.no)
                { dialogInterface, _ -> dialogInterface.dismiss() }
                .create()
                .show()

        }
        btnCompute.setOnClickListener {
            resultInMtr = if (switch_action.isChecked) {
                getResult(locationLatLngByMark)
            } else {
                getResult(locationLatLng)

            }
            if (computeType == AREA) {
                Toast.makeText(this, "Area : $resultInMtr square meters", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Length is : $resultInMtr meters", Toast.LENGTH_SHORT)
                    .show()

            }
            btnDone.visibility = View.VISIBLE

        }

        switch_action.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                drawResult(locationTrackByMark)
            } else {
                drawResult(locationTrack)
            }

        }
        btnDone.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("Are you sure?")
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val intent = Intent()
                    intent.putExtra(RESULT, resultInMtr)
                    intent.putExtra(
                        RESULT_LAT_LONG, getLocationTrackString()
                    )
                    setResult(REQUEST_CODE_FOR_MAP_ACTIVITY, intent)
                    finish()
                }

                .setNegativeButton(R.string.no)
                { dialogInterface, _ -> dialogInterface.dismiss() }
                .create()
                .show()

        }

        switch_layer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }

    fun getLocationTrackString(): ArrayList<String> {
        val arrayList = if (switch_action.isChecked) locationTrackByMark else locationTrack
        val trackList = ArrayList<String>()
        arrayList.forEach {
            trackList.add("${it.latitude},${it.longitude},${it.accuracy}")
        }
        return trackList
    }

    private fun validateLocation(
        lastCapturedLocation: Location,
        currentLocation: Location
    ): Boolean {
        val distance = currentLocation.distanceTo(lastCapturedLocation)
        return when {
            currentLocation.accuracy > 30 -> {
                false
            }
            currentLocation.accuracy > 20 -> {
                distance > 5.0
            }
            currentLocation.accuracy > 10 -> {
                distance > 3.0
            }
            else -> distance > 2.0
        }
    }

    private fun getResult(locationLatLng: ArrayList<LatLng>): Double {
        return if (computeType == AREA) {
            SphericalUtil.computeArea(locationLatLng)
        } else {
            SphericalUtil.computeLength(locationLatLng)
        }


    }

    private fun drawResult(locationTrack: ArrayList<Location>) {
        if (computeType == AREA) {
            drawPolygon(locationTrack)
        } else {
            drawPolyLineResult(locationTrack)
        }

    }

    companion object {
        const val AREA = "AREA"
        const val REQUEST_CODE_FOR_MAP_ACTIVITY = 121
        const val LENGTH = "LENGTH"
        const val COMPUTE_TYPE = "COMPUTE_TYPE"
        const val RESULT = "RESULT"
        const val RESULT_LAT_LONG = "RESULT_LAT_LONG"
    }

    private fun startPath() {
        polylineOptions.add(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
        mark()
        isStarted = true

    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure?")
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialogInterface, _ ->
                super.onBackPressed()
            }

            .setNegativeButton(R.string.no)
            { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
            .show()

    }

    private fun mark() {
        if (lastLocation != null) {
            addMarker(lastLocation!!.latitude, lastLocation!!.longitude)
            locationLatLngByMark.add(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
            locationTrackByMark.add(lastLocation!!)
        }

    }

    private fun stopAction() {
        btnMark.visibility = View.GONE
        btnStop.visibility = View.GONE
        btnStart.visibility = View.GONE

        btnCompute.visibility = View.VISIBLE
        if (locationLatLngByMark.size > 2) {
            switch_action.visibility = View.VISIBLE
        }
        isStarted = false
        lastPolyLine?.remove()

        drawResult(locationTrack)
    }

    private fun drawPolygon(locationTrack: ArrayList<Location>) {
        lastPolygon?.remove()
        val polygonOptions = PolygonOptions()
        locationTrack.forEach { location ->
            polygonOptions.add(LatLng(location.latitude, location.longitude))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            polygonOptions.fillColor(getColor(R.color.transparent))
        } else {
            polygonOptions.fillColor(resources.getColor(R.color.transparent))
        }
        lastPolygon = mMap.addPolygon(polygonOptions)
    }


    private fun drawPolyLineResult(locationTrack: ArrayList<Location>) {
        lastPolyLine?.remove()
        val polygonOptions = PolylineOptions()
        locationTrack.forEach { location ->
            polygonOptions.add(LatLng(location.latitude, location.longitude))
        }
        polygonOptions.width(16f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            polygonOptions.color(getColor(R.color.colorAccent))
        } else {
            polygonOptions.color(resources.getColor(R.color.colorAccent))
        }
        lastPolyLine = mMap.addPolyline(polygonOptions)
    }

    override fun onResume() {
        if (!permissionHandler.checkGpsPermission()) {
            permissionHandler.requestGpsPermission()
        }
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        if (permissionHandler.checkGpsPermission()) {
            locationHandler.startGpsService()
        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                locationReceiver,
                IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
            )

    }

    override fun onStop() {
        super.onStop()
        locationHandler.onStop()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(locationReceiver)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationHandler.onActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        if (permissionHandler.checkGpsPermission()) {
            mMap.isMyLocationEnabled = true
        }

    }


    private fun addMarker(lat: Double, long: Double) {
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(lat, long)
        mMap.addMarker(MarkerOptions().position(sydney).title("Mark"))
        val newLatLng = CameraUpdateFactory.newLatLng(sydney)
        mMap.animateCamera(newLatLng)

    }

    private fun zoomTo(lat: Double, long: Double) {
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(lat, long)
        val zoom = 16f
        val newLatLng = CameraUpdateFactory.newLatLngZoom(sydney, zoom)
        mMap.animateCamera(newLatLng)

    }

    private fun moveCamera(lat: Double, long: Double) {
        val sydney = LatLng(lat, long)
        val newLatLng = CameraUpdateFactory.newLatLng(sydney)
        mMap.animateCamera(newLatLng)

    }

    private fun drawLine(currentLocation: Location) {
        polylineOptions.add(LatLng(currentLocation.latitude, currentLocation.longitude))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            polylineOptions.color(getColor(R.color.colorAccent))
        } else {
            polylineOptions.color(resources.getColor(R.color.colorAccent))
        }
        polylineOptions.width(4f)
        lastPolyLine?.remove()
        lastPolyLine = mMap.addPolyline(polylineOptions)

    }

    override fun acceptedPermission(grantResults: IntArray) {
        locationHandler.startGpsService()
    }

    override fun deniedPermission(isNeverAskAgain: Boolean) {
    }
}
