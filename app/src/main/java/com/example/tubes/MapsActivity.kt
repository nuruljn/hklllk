package com.example.tubes

import android.content.pm.PackageManager
import android.graphics.Camera
import android.location.Location
//import android.location.LocationRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.telecom.Call
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tubes.Common.Common
import com.example.tubes.Model.MyPlaces
import com.example.tubes.Remote.IGoogleAPIService

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tubes.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
//import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Response
import java.lang.StringBuilder
import java.util.jar.Manifest
import javax.security.auth.callback.Callback

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var latitude:Double=0.toDouble()
    private var longitude:Double=0.toDouble()

    private lateinit var mLastLocation:Location
    private var mMarker: Marker?=null

    //Location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object{
        private const val MY_PERMISSION_CODE: Int = 1000;
    }

    lateinit var mService:IGoogleAPIService
    internal lateinit var currentPlaces: MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Init Service
        mService = Common.googleApiService

        //Request runtime permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest();
                buildLocationCallBack();

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                );
            } else {
                buildLocationRequest();
                buildLocationCallBack();
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                );
            }


            findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
                .setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.action_hotel -> nearByPlace("hotel")
                        R.id.action_tour -> nearByPlace("tour")
                        R.id.action_market -> nearByPlace("market")
                        R.id.action_mall -> nearByPlace("mall")
                        R.id.action_restaurant -> nearByPlace("restaurant")
                    }
                }

        }

    }

    private fun nearByPlace(typeplace: String): Boolean {

        //clear all marker on map
        mMap.clear()
        //build URL request base on location
        val url = getUrl(latitude,longitude,typeplace)

        mService.getNearbyPlaces(url).enqueue(object : Callback<MyPlaces>{
                override fun onFailure(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {
                    currentPlaces = response!!.body()!!

                    if(response!!.isSuccessful)
                    {

                        for(i in 0 until response!!.body()!!.results!!.size)
                        {
                            val markerOptions = MarkerOptions()
                            val googlePlaces = response.body()!!.results!![i]
                            val lat = googlePlaces.geometry!!.location!!.lat
                            val lng = googlePlaces.geometry!!.location!!.lng
                            val placeName = googlePlaces.name
                            val latLng = LatLng(lng, lat)

                            markerOptions.position(latLng)
                            markerOptions.title(placeName)
                            if(typeplace.equals("restaurant"))
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_restaurant_24))
                            else if(typeplace.equals("market"))
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_shopping_cart_24))
                            else if(typeplace.equals("mall"))
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_local_mall_24))
                            else if(typeplace.equals("tour"))
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_tour_24))
                            else if(typeplace.equals("hotel"))
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_hotel_24))
                            else
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                            markerOptions.snippet(i.toString())

                            mMap!!.addMarker(markerOptions)


                        }
                        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng))
                        mMap!!.animateCamera((CameraUpdateFactory.zoomTo(11f)))
                    }
                }

                override fun onFailure(call: Call<MyPlaces>?, t: Throwable?) {
                    Toast.makeText(baseContext,""+t!!.message,Toast.LENGTH_SHORT).show()
                }
        })
    }

    private fun getUrl(latitude: Double, longitude: Double, typeplace: String): String {

        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?&location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000")
        googlePlaceUrl.append("&type=$typeplace")
        googlePlaceUrl.append("&key=AIzaSyCqB59NPX3LPKFMllqbRiaAPe-3ea39sfc")

        Log.d("URL_DEBUG",googlePlaceUrl.toString())
        return  googlePlaceUrl.toString()

    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation =  p0!!.locations.get(p0!!.locations.size-1) //Get Last Location

                if(mMarker != null){
                    mMarker!!.remove()
                }

                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                val latLng = LatLng(latitude,longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("Your Position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                mMarker = mMap!!.addMarker(markerOptions)

                //Move Camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
            }


        }

    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f

    }

    private fun checkLocationPermission() : Boolean{

        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_CODE)
            else
                ActivityCompat.requestPermissions(this, arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_CODE)
            return false
        }
        else
            return true
    }

    //Override OnRequestPermissionResult
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            MY_PERMISSION_CODE-> {
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if(checkLocationPermission()){
                            mMap!!.isMyLocationEnabled = true
                        }
                }
                else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Init Google Play Services
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        }
        else
            mMap!!.isMyLocationEnabled = true

        //Enable Zoom control
        mMap.uiSettings.isZoomGesturesEnabled=true
    }
}