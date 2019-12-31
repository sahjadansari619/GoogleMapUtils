package com.dhwaniris.maputils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dhwaniris.locationutils.MapsActivity
import com.dhwaniris.locationutils.MapsActivityTest
import com.dhwaniris.locationutils.PermissionHandler
import com.dhwaniris.locationutils.PermissionHandlerListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PermissionHandlerListener {
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionHandler = PermissionHandler(this, this)

        val intent = Intent(this, MapsActivity::class.java)
        calculate_area.setOnClickListener {

            if (permissionHandler.checkGpsPermission()) {
                intent.putExtra(MapsActivity.COMPUTE_TYPE, MapsActivity.AREA)
                intent.putExtra(MapsActivity.MIN_ACCURACY_TO_START,18.0)
                startActivityForResult(intent, MapsActivity.REQUEST_CODE_FOR_MAP_ACTIVITY)
            } else {
                permissionHandler.requestGpsPermission()
            }
        }

        calculate_length.setOnClickListener {
            startActivity(Intent(this, MapsActivityTest::class.java))

           /* if (permissionHandler.checkGpsPermission()) {
                intent.putExtra(MapsActivity.COMPUTE_TYPE, MapsActivity.LENGTH)
                startActivityForResult(intent, MapsActivity.REQUEST_CODE_FOR_MAP_ACTIVITY)
            } else {
                permissionHandler.requestGpsPermission()
            }*/
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MapsActivity.REQUEST_CODE_FOR_MAP_ACTIVITY && data != null) {
            val stringExtra = data.getDoubleExtra(MapsActivity.RESULT, 0.0).toString()
            tv_result.text = stringExtra
        }
    }

    override fun acceptedPermission(grantResults: IntArray) {
    }

    override fun deniedPermission(isNeverAskAgain: Boolean) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
