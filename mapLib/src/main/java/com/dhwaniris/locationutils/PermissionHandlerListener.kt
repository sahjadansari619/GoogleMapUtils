package com.dhwaniris.locationutils
/**
 * Created by ${Sahjad} on 01/29/2019.
 */
interface PermissionHandlerListener {
    fun acceptedPermission(grantResults: IntArray)

    fun deniedPermission(isNeverAskAgain: Boolean)
}
interface LocationHandlerListener{
    fun acceptedGPS()

    fun deniedGPS()
}