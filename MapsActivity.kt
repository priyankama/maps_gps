package com.example.implementmaps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.Address
import android.location.Geocoder

import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.ZoomControls
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.*
import com.google.android.gms.location.*
import com.google.android.gms.location.places.*
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException


@SuppressLint("ByteOrderMark")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback , GoogleMap.OnMarkerClickListener ,PlaceSelectionListener,
    GoogleApiClient.OnConnectionFailedListener {
    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlaceSelected(p0: Place?) {
        Toast.makeText(applicationContext,""+p0!!.name+p0!!.latLng,Toast.LENGTH_LONG).show();
    }

    override fun onError(p0: Status?) {
        Toast.makeText(applicationContext,""+p0.toString(),Toast.LENGTH_LONG).show();
    }

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    val TAG = "MapActivity"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private lateinit var mGoogleApiClient : GoogleApiClient
    lateinit var mPlace : PlaceInfo

    private lateinit var locationCallback: LocationCallback
    lateinit var mGeoDataClient: GeoDataClient
    private lateinit var lastLocation: Location
    var ACCESS_LOCATION_CODE = 123
    private val REQUEST_CHECK_SETTINGS = 2
    private val PLACE_PICKER_REQUEST = 3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mGeoDataClient = Places.getGeoDataClient(this, null);

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Places.GEO_DATA_API)
            .addApi(Places.PLACE_DETECTION_API)
            .enableAutoManage(this,this)
            .build()
        //val myPlace = LatLng(40.73, -73.99)

        val fab = findViewById<FloatingActionButton>(R.id.fab)

        fab.setOnClickListener(){
            loadPlacePicker()
        }



        @Suppress("DEPRECATION")
        val autocompleteFragment = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment)
                as PlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.getUiSettings().setZoomControlsEnabled(true)
        mMap.setOnMarkerClickListener(this)

        checkPermission()
    }
    fun checkPermission(){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION  )) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_LOCATION_CODE)

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                mMap.isMyLocationEnabled = true
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null ) {
                        lastLocation = location
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        placeMarkerOnMap(currentLatLng)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                    }
                }
            }

    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_LOCATION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty()) {
                    mMap.isMyLocationEnabled = true
                    fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            lastLocation = location
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            placeMarkerOnMap(currentLatLng)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                        }
                    }

                } else {
                        Toast.makeText(applicationContext,"PERMISSION NOT GRANTED",Toast.LENGTH_LONG).show()
                }
                return
            }
            else -> {
                Toast.makeText(applicationContext,"PERMISSION NOT GRANTED",Toast.LENGTH_LONG).show()
            }
        }
    }

        private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),ACCESS_LOCATION_CODE )
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->

            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                val place = PlacePicker.getPlace( this,data)
              //  var addressText = place.name.toString()
               // addressText += "\n" + place.address.toString()

                val placeResult : PendingResult<PlaceBuffer> = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient,place.id)
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback)
                placeMarkerOnMap(place.latLng)
            }
        }

    }

    val mUpdatePlaceDetailsCallback : ResultCallback<PlaceBuffer> = ResultCallback {


        fun onResult(places : PlaceBuffer){
            if(!places.status.isSuccess){
                Log.d("MAPS ACTIVITY","onREsult QUERY not completed"+places.status.toString())
                places.release()
                return
            }
            val place : Place = places.get(0)
            try {
                mPlace = PlaceInfo()
                mPlace.name = place.name.toString()
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.address  =place.address.toString()
                Log.d(TAG, "onResult: address: " + place.getAddress());
//                mPlace.setAttributions(place.getAttributions().toString());
//                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
                mPlace.id = place.id.toString()
                Log.d(TAG, "onResult: id:" + place.getId());
                mPlace.latlng = place.latLng
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.rating = place.rating
                Log.d(TAG, "onResult: rating: " + place.getRating());
                mPlace.phoneNumber = place.phoneNumber.toString()
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                mPlace.websiteUri = place.websiteUri
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());

                Log.d(TAG, "onResult: place: " + mPlace.toString());
            }catch ( e : NullPointerException){
                Log.e(TAG, "onResult: NullPointerException: " + e.message );
            }
           mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place.viewport.center.latitude,place.viewport.center.longitude), 12f))
           placeMarkerOnMap(place.latLng)
            places.release();
        }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }
    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)

        val titleStr = getAddress(location)  // add these two lines
        markerOptions.title(titleStr)
        mMap.addMarker(markerOptions)
    }

    private fun getAddress(latLng: LatLng): String {
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        var addressText = ""

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if ( addresses!= null && !addresses.isEmpty()) {
                val address = addresses[0].getAddressLine(0)
                // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
              //  val city = addresses[0].locality
                //val state = addresses[0].adminArea
               // val country = addresses[0].countryName
                //val postalCode = addresses[0].postalCode
               // val knownName = addresses[0].featureName
                addressText = """$address"""
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }
        return addressText
    }

        private fun loadPlacePicker() {
            val builder = PlacePicker.IntentBuilder()

            try {
                startActivityForResult(builder.build(this@MapsActivity), PLACE_PICKER_REQUEST)
            } catch (e: GooglePlayServicesRepairableException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                e.printStackTrace()
            }


    }
}

