package com.dhwaniris.locationutils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dhwaniris.tata_trust_delta.utils.PermissionHandler
import com.dhwaniris.tata_trust_delta.utils.PermissionHandlerListener
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
                startActivityForResult(intent, MapsActivity.REQUEST_CODE_FOR_MAP_ACTIVITY)
            } else {
                permissionHandler.requestGpsPermission()
            }
        }

        calculate_length.setOnClickListener {
            if (permissionHandler.checkGpsPermission()) {
                intent.putExtra(MapsActivity.COMPUTE_TYPE, MapsActivity.LENGTH)
                startActivityForResult(intent, MapsActivity.REQUEST_CODE_FOR_MAP_ACTIVITY)
            } else {
                permissionHandler.requestGpsPermission()
            }
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
