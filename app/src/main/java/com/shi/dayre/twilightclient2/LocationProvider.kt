package com.shi.dayre.twilightclient2

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log

/**
 * Created by StudenetskiyA on 06.01.2018.
 */


class LocationProvider(val context:Context, val mView:MainActivity) {
    var locationManager: LocationManager

    init{
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager!=null)
        Log.i("TLC.location","Location service inited ok.")
    }

    fun start(){
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //For CONNECT_EVERY_SECOND second, minimum 5 meter
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                (1000 * CONNECT_EVERY_SECOND).toLong(), 5f, locationListener)
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, (1000 * CONNECT_EVERY_SECOND).toLong(), 5f,
                locationListener)
        Log.i("TLC.location","Location service started.")
    }

    private fun updateLocation(location: Location?) {
        if (location == null)
            return

        Log.i("TLC.location","Location updated")
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            user.location = location;
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            user.locationNet = location;
        }
        mView.refresh()
    }

    private fun checkGPSEnabled():Boolean{
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    private fun checkNetEnabled():Boolean{
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            Log.i("TLC.location","onLocation change.")
            updateLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            updateLocation(null)
        }

        override fun onProviderEnabled(provider: String) {
            Log.i("TLC.location","onProviderEnable.")

            Log.i("TLC.location","Net provider is "+checkNetEnabled()+".")
            Log.i("TLC.location","GPS provider is "+checkGPSEnabled()+".")
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
        }
    }



}