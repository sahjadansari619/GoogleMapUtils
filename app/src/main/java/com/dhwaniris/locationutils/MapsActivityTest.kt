package com.dhwaniris.locationutils

import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.dhwaniris.tata_trust_delta.utils.PermissionHandler
import com.dhwaniris.tata_trust_delta.utils.PermissionHandlerListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivityTest : AppCompatActivity(), OnMapReadyCallback, PermissionHandlerListener {

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
    private val polylineOptions = PolylineOptions()
    private var lastPolyLine: Polyline? = null
    private var lastPolygone: Polygon? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationHandler = LocationHandler(this)
        permissionHandler = PermissionHandler(this, this)
        /*locationLive.observe(this, Observer {
            if (isMapReady && it != null) {
                lastLocation = it
                if (!zoomDone) {
                    zoomDone = true
                    zoomTo(it.latitude, it.longitude)
                }
                if (isStarted) {
                    moveCamera(it.latitude, it.longitude)
                    locationTrack.add(it)
                    locationLatLng.add(LatLng(it.latitude, it.longitude))
                    drawLine()
                }
            }
        })*/
        locationReceiver = LocationReceiver(locationLive)


        btnStop.visibility = View.GONE
        btnStart.visibility = View.GONE
        btnMark.visibility = View.GONE
        btnCompute.visibility = View.VISIBLE
        switch_action.visibility = View.VISIBLE

        btnCompute.setOnClickListener {
            val concaveBoundries = ConcaveHull().calculateConcaveHull(getlist(), 10)
            val convexBoundries = ConvexHull.makeHull(getList2())
            drawPolygon2(convexBoundries)
        }

        switch_action.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val concaveBoundries = ConcaveHull().calculateConcaveHull(getlist(), 10)

                drawPolygon(concaveBoundries)

            } else {
                val convexBoundries = ConvexHull.makeHull(getList2())
                drawPolygon2(convexBoundries)

            }

        }


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
        btnCompute.visibility = View.VISIBLE
        if (locationLatLngByMark.size > 2) {
            switch_action.visibility = View.VISIBLE
        }
        isStarted = false
        lastPolyLine?.remove()
        //   drawPolygon(locationTrack)
    }

    private fun drawPolygon(locationTrack: java.util.ArrayList<ConcaveHull.Point>) {
        lastPolygone?.remove()
        val polygonOptions = PolygonOptions()
        locationTrack.forEach { location ->
            polygonOptions.add(LatLng(location.x, location.y))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            polygonOptions.fillColor(getColor(R.color.transparent))
        } else {
            polygonOptions.fillColor(resources.getColor(R.color.transparent))
        }
        lastPolygone = mMap.addPolygon(polygonOptions)
    }

    private fun drawPolygon2(locationTrack: MutableList<Point>) {
        lastPolygone?.remove()
        val polygonOptions = PolygonOptions()
        locationTrack.forEach { location ->
            polygonOptions.add(LatLng(location.x, location.y))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            polygonOptions.fillColor(getColor(R.color.transparent))
        } else {
            polygonOptions.fillColor(resources.getColor(R.color.transparent))
        }
        lastPolygone = mMap.addPolygon(polygonOptions)
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
        mMap.isMyLocationEnabled = true
        zoomTo(22.7432007, 86.196432)

    }


    private fun addMarker(lat: Double, long: Double) {
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(lat, long)
        mMap.addMarker(MarkerOptions().position(sydney).title("Mark"))
        val zoom = 16f
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

    private fun drawLine() {
        polylineOptions.add(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
        lastPolyLine?.remove()
        lastPolyLine = mMap.addPolyline(polylineOptions)

    }

    override fun acceptedPermission(grantResults: IntArray) {
    }

    override fun deniedPermission(isNeverAskAgain: Boolean) {
    }


    fun getlist(): ArrayList<ConcaveHull.Point> {
        val pointsList = ArrayList<ConcaveHull.Point>()
        getPoints().forEach {
            pointsList.add(ConcaveHull.Point(it.first, it.second))
        }
        return pointsList
    }


    fun getList2(): ArrayList<Point> {
        val pointsList = ArrayList<Point>()
        getPoints().forEach {
            pointsList.add(Point(it.first, it.second))
        }
        return pointsList
    }

    private fun getPoints(): ArrayList<Pair<Double, Double>> {
        val pointsList = ArrayList<Pair<Double, Double>>()
        pointsList.add(Pair(22.7432007, 86.196432))
        pointsList.add(Pair(22.7424855, 86.1973367))
        pointsList.add(Pair(22.7424699, 86.1973604))
        pointsList.add(Pair(22.7438209, 86.1962817))
        pointsList.add(Pair(22.7429821, 86.197252))
        pointsList.add(Pair(22.7431946, 86.1971515))
        pointsList.add(Pair(22.7430714, 86.1975925))
        pointsList.add(Pair(22.7432463, 86.1974909))
        pointsList.add(Pair(22.7438637, 86.1969282))
        pointsList.add(Pair(22.7442391, 86.1966199))
        pointsList.add(Pair(22.7433391, 86.1976447))
        pointsList.add(Pair(22.7446858, 86.1963379))
        pointsList.add(Pair(22.7449994, 86.1963058))
        pointsList.add(Pair(22.7436078, 86.1977444))
        pointsList.add(Pair(22.7447337, 86.1967511))
        pointsList.add(Pair(22.7484528, 86.1939298))
        pointsList.add(Pair(22.7444458, 86.1981073))
        pointsList.add(Pair(22.7450613, 86.1975421))
        pointsList.add(Pair(22.7446172, 86.1981643))
        pointsList.add(Pair(22.7446015, 86.1981995))
        pointsList.add(Pair(22.7432691, 86.199816))
        pointsList.add(Pair(22.7456425, 86.1978717))
        pointsList.add(Pair(22.7457944, 86.1978524))
        pointsList.add(Pair(22.7485371, 86.1952835))
        pointsList.add(Pair(22.743403, 86.2004392))
        pointsList.add(Pair(22.7436184, 86.2005253))
        pointsList.add(Pair(22.7433862, 86.2007776))
        pointsList.add(Pair(22.7436965, 86.2005377))
        pointsList.add(Pair(22.7437001, 86.2005605))
        pointsList.add(Pair(22.7435056, 86.2007608))
        pointsList.add(Pair(22.7438121, 86.2006623))
        pointsList.add(Pair(22.7452222, 86.199367))
        pointsList.add(Pair(22.7442389, 86.2003607))
        pointsList.add(Pair(22.7444403, 86.2001868))
        pointsList.add(Pair(22.7444337, 86.200218))
        pointsList.add(Pair(22.7442612, 86.2004016))
        pointsList.add(Pair(22.7439122, 86.2007582))
        pointsList.add(Pair(22.7439587, 86.2007514))
        pointsList.add(Pair(22.7451004, 86.1996622))
        pointsList.add(Pair(22.7446009, 86.2002155))
        pointsList.add(Pair(22.748925, 86.1959862))
        pointsList.add(Pair(22.7437986, 86.2011556))
        pointsList.add(Pair(22.744195, 86.2007871))
        pointsList.add(Pair(22.7503783, 86.1946092))
        pointsList.add(Pair(22.7446586, 86.2004271))
        pointsList.add(Pair(22.7446549, 86.2004944))
        pointsList.add(Pair(22.7504305, 86.1947315))
        pointsList.add(Pair(22.7440055, 86.2011986))
        pointsList.add(Pair(22.7443743, 86.2009786))
        pointsList.add(Pair(22.7499786, 86.195435))
        pointsList.add(Pair(22.7480861, 86.1975194))
        pointsList.add(Pair(22.7463999, 86.1992061))
        pointsList.add(Pair(22.7502385, 86.1953699))
        pointsList.add(Pair(22.7479009, 86.1977159))
        pointsList.add(Pair(22.7479465, 86.1977026))
        pointsList.add(Pair(22.7501373, 86.1955919))
        pointsList.add(Pair(22.7498911, 86.1959097))
        pointsList.add(Pair(22.747554, 86.1982578))
        pointsList.add(Pair(22.7458568, 86.199979))
        pointsList.add(Pair(22.747714, 86.198124))
        pointsList.add(Pair(22.7481247, 86.1977173))
        pointsList.add(Pair(22.7482842, 86.1976437))
        pointsList.add(Pair(22.7495253, 86.196547))
        pointsList.add(Pair(22.7476651, 86.1984184))
        pointsList.add(Pair(22.7497405, 86.196447))
        pointsList.add(Pair(22.7482876, 86.1979004))
        pointsList.add(Pair(22.7482254, 86.1980154))
        pointsList.add(Pair(22.7482432, 86.1980208))
        pointsList.add(Pair(22.7499424, 86.1963756))
        pointsList.add(Pair(22.7509465, 86.1953966))
        pointsList.add(Pair(22.7501539, 86.1961923))
        pointsList.add(Pair(22.747805, 86.1986461))
        pointsList.add(Pair(22.7484796, 86.1980229))
        pointsList.add(Pair(22.7469692, 86.199614))
        pointsList.add(Pair(22.7476645, 86.1990987))
        pointsList.add(Pair(22.7475797, 86.1992347))
        pointsList.add(Pair(22.7481861, 86.1986548))
        pointsList.add(Pair(22.7476114, 86.1992747))
        pointsList.add(Pair(22.7498435, 86.1970605))
        pointsList.add(Pair(22.7499364, 86.19709))
        pointsList.add(Pair(22.7499597, 86.1970952))
        pointsList.add(Pair(22.748293, 86.19881))
        pointsList.add(Pair(22.7483176, 86.1987916))
        pointsList.add(Pair(22.7485085, 86.1986285))
        pointsList.add(Pair(22.7483894, 86.1987631))
        pointsList.add(Pair(22.7486755, 86.1984892))
        pointsList.add(Pair(22.7481853, 86.198991))
        pointsList.add(Pair(22.7504864, 86.1966997))
        pointsList.add(Pair(22.7499246, 86.1973097))
        pointsList.add(Pair(22.7478302, 86.1994238))
        pointsList.add(Pair(22.7485659, 86.1986975))
        pointsList.add(Pair(22.7486287, 86.1986471))
        pointsList.add(Pair(22.7481462, 86.1993129))
        pointsList.add(Pair(22.7475997, 86.1999133))
        pointsList.add(Pair(22.7480313, 86.1996384))
        pointsList.add(Pair(22.7485954, 86.1990949))
        pointsList.add(Pair(22.7480487, 86.199686))
        pointsList.add(Pair(22.7477289, 86.200076))
        pointsList.add(Pair(22.7481166, 86.1997317))
        pointsList.add(Pair(22.7478731, 86.2000071))
        pointsList.add(Pair(22.7481708, 86.1997179))
        pointsList.add(Pair(22.7484316, 86.1994757))
        pointsList.add(Pair(22.7478886, 86.2000754))
        pointsList.add(Pair(22.7487797, 86.1993266))
        pointsList.add(Pair(22.7486613, 86.2000479))
        pointsList.add(Pair(22.7490369, 86.199909))
        pointsList.add(Pair(22.748533, 86.2004259))
        pointsList.add(Pair(22.7495273, 86.1995225))
        pointsList.add(Pair(22.7491294, 86.20018))
        pointsList.add(Pair(22.7490551, 86.2003618))
        pointsList.add(Pair(22.7494936, 86.2001351))
        pointsList.add(Pair(22.7492167, 86.2005699))
        pointsList.add(Pair(22.7497934, 86.2011563))
        pointsList.add(Pair(22.7499043, 86.2012922))
        pointsList.add(Pair(22.749907, 86.20136))
        pointsList.add(Pair(22.7500146, 86.2013481))
        pointsList.add(Pair(22.7501793, 86.2015266))
        pointsList.add(Pair(22.7508279, 86.2021533))
        pointsList.add(Pair(22.7513237, 86.2026802))
        pointsList.add(Pair(22.7530313, 86.2042871))
        pointsList.add(Pair(22.7532026, 86.2042705))
        pointsList.add(Pair(22.7533258, 86.2041715))
        pointsList.add(Pair(22.7531652, 86.2044371))
        pointsList.add(Pair(22.753361, 86.20435))
        pointsList.add(Pair(22.7535801, 86.2043034))
        pointsList.add(Pair(22.7534472, 86.2045323))
        pointsList.add(Pair(22.7534927, 86.2045872))
        pointsList.add(Pair(22.7535362, 86.2045461))
        pointsList.add(Pair(22.7536132, 86.2045871))
        pointsList.add(Pair(22.7536214, 86.2047655))
        pointsList.add(Pair(22.7538177, 86.2046733))
        pointsList.add(Pair(22.7537494, 86.2048516))
        pointsList.add(Pair(22.7538341, 86.2047753))
        pointsList.add(Pair(22.7538893, 86.2047848))
        pointsList.add(Pair(22.7540164, 86.204725))
        pointsList.add(Pair(22.754172, 86.2049844))
        pointsList.add(Pair(22.7541699, 86.2049972))
        pointsList.add(Pair(22.7540919, 86.2052157))
        pointsList.add(Pair(22.7544209, 86.2053307))
        pointsList.add(Pair(22.7544695, 86.2053204))
        pointsList.add(Pair(22.7544514, 86.2053481))
        pointsList.add(Pair(22.75436, 86.2054791))
        pointsList.add(Pair(22.7545553, 86.205318))
        pointsList.add(Pair(22.7547082, 86.2054933))
        pointsList.add(Pair(22.7550423, 86.205369))
        pointsList.add(Pair(22.7552123, 86.2059258))
        pointsList.add(Pair(22.7552863, 86.206015))
        pointsList.add(Pair(22.755479, 86.20611))
        pointsList.add(Pair(22.7552502, 86.2064303))
        pointsList.add(Pair(22.7556065, 86.2064587))
        return pointsList
    }
}
