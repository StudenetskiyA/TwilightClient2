package com.shi.dayre.twilightclient2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import xdroid.toaster.Toaster
import android.content.Context.LOCATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog


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
            Toaster.toast("Включите разрешения для доступа к геоданным.")
            ActivityCompat.requestPermissions(mView,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            //I know what I must check, what user say about permission. But without it application do nothing.
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
            user.location = location
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            user.locationNet = location
          //  Toaster.toast("Location tick")
        }
        mView.refresh()
    }

    private fun checkGPSEnabled():Boolean{
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    private fun checkNetEnabled():Boolean{
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isAnySensorEnable():Boolean {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        return true
        else return false
    }

    val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            Log.i("TLC.location","onLocation change.")
            updateLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            if (!isAnySensorEnable()) {
                val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            mView.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            mView.logOut()
                        }
                    }
                }
                val builder = AlertDialog.Builder(context)
                builder.setMessage(context.getString(R.string.sensorOpen)).setPositiveButton("Да", dialogClickListener)
                        .setNegativeButton("Отмена", dialogClickListener).show()
            }
            updateLocation(null)
        }

        override fun onProviderEnabled(provider: String) {
            Log.i("TLC.location","onProviderEnable.")
           // Toaster.toast("Location tick")
            Log.i("TLC.location","Net provider is "+checkNetEnabled()+".")
            Log.i("TLC.location","GPS provider is "+checkGPSEnabled()+".")
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toaster.toast("Включите разрешения для доступа к геоданным.")
                ActivityCompat.requestPermissions(mView,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
            updateLocation(getLastKnownLocation())
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toaster.toast("Включите разрешения для доступа к геоданным.")
                ActivityCompat.requestPermissions(mView,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
            updateLocation(getLastKnownLocation())
        }

        private fun getLastKnownLocation(): Location? {
            var mLocationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = mLocationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                if (Build.VERSION.SDK_INT >= 23 &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val l = mLocationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.getAccuracy() < bestLocation.accuracy) {
                    // Found best last known location: %s", l);
                    bestLocation = l
                }
                }
            }
            return bestLocation
        }
    }



}