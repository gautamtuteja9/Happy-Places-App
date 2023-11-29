package com.gautamtuteja.happyplaces.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.gautamtuteja.happyplaces.R
import com.gautamtuteja.happyplaces.database.DatabaseHandler
import com.gautamtuteja.happyplaces.models.HappyPlaceModel
import com.gautamtuteja.happyplaces.utils.GetAddressFromLatLng
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest


class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {


    private val cal = Calendar.getInstance()
    private lateinit var dateSetListener:DatePickerDialog.OnDateSetListener

    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double =0.0

    private var mHappyPlaceDetails : HappyPlaceModel?= null

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)
        if(supportActionBar!=null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title="ADD NEW PLACE"
        }
        toolbar_add_place.setNavigationOnClickListener{
            onBackPressed()
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if(!Places.isInitialized()){
            Places.initialize(this,resources.getString(R.string.google_maps_api_key))

        }



        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAIL)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAIL) as HappyPlaceModel
        }

        if (mHappyPlaceDetails !=null){
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            iv_place_image.setImageURI(saveImageToInternalStorage)

            btn_save.text="UPDATE"
        }


        dateSetListener = DatePickerDialog.OnDateSetListener {
                _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }
        updateDateInView()   //put the current date



        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
        tv_select_current_loc.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when(p0!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }


            R.id.tv_add_image ->{
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Gallery","Camera")
                pictureDialog.setItems(pictureDialogItems){
                    _,which->
                    when(which){
                        0-> choosePhotoFromGallery()
                        1-> takePhotoFromCam()
                    }
                }
                pictureDialog.show()
            }

            R.id.btn_save ->{
                when{
                    et_title.text.isNullOrEmpty() ->{
                        Toast.makeText(this, "Enter Title", Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty() ->{
                        Toast.makeText(this, "Enter Description", Toast.LENGTH_SHORT).show()
                    }
                    et_location.text.isNullOrEmpty() ->{
                        Toast.makeText(this, "Enter Location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null ->{
                        Toast.makeText(this, "Select an Image", Toast.LENGTH_SHORT).show()
                    }
                    else ->{
                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null)0 else mHappyPlaceDetails!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)

                        if(mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace>0){
                                setResult(RESULT_OK)
                                finish()
                            }
                        }else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace>0){
                                setResult(RESULT_OK)
                                finish()
                            }
                        }

                    }
                }


            }


            R.id.et_location ->{
                try {

                    val fields = listOf(Place.Field.ID,Place.Field.NAME,
                        Place.Field.LAT_LNG,Place.Field.ADDRESS)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN , fields).build(this)

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_loc ->{
                if (!isLocationEnable()){
                    Toast.makeText(this, "location provider is off", Toast.LENGTH_SHORT).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withActivity(this).withPermissions(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener{
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?){
                            if (report!!.areAllPermissionsGranted()){
                                requestNewLocationData()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            p0: MutableList<PermissionRequest>?,
                            p1: PermissionToken?,
                        ) {
                            showRationaleDialogForPermissions()
                        }
                    }).onSameThread().check()
                }
            }

        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                if(data!=null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)

                        saveImageToInternalStorage = saveImgToInternalStorage(selectedImageBitmap)
                        Log.e("Saved image","Path :: $saveImageToInternalStorage")

                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    }catch (e:IOException){
                        e.printStackTrace()
                        Toast.makeText(this, "fail to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else if(requestCode == CAMERA){
                val thumbnail:Bitmap = data!!.extras!!.get("data")as Bitmap

                saveImageToInternalStorage = saveImgToInternalStorage(thumbnail)
                Log.e("Saved image","Path :: $saveImageToInternalStorage")

                iv_place_image.setImageBitmap(thumbnail)
            }

            else if(requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                et_location.setText(place.address)
                mLatitude= place.latLng!!.latitude
                mLongitude= place.latLng!!.longitude

                Log.d("AddHappyPlace", "onResult[PLACE]: resultCode= ${resultCode}")
                if (resultCode == Activity.RESULT_OK) {
                    val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                    Log.d("AddHappyPlace", "onActivityResult: place=${place}")
                    et_location.setText(place.address)
                    if (et_title.text.isNullOrEmpty()) {
                        et_title.setText(place.name)
                    }
                    mLatitude = place.latLng!!.latitude
                    mLongitude = place.latLng!!.longitude
                } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    var status: Status? = Autocomplete.getStatusFromIntent(data!!)
                    Log.d("AddHappyPlace", "onResult[PLACE] error: ${status?.statusMessage}")
                }
            }
        }

    }


    private fun takePhotoFromCam(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()){

                    val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(galleryIntent, CAMERA)

                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?, token: PermissionToken?)
            {
                showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
    }



    private fun choosePhotoFromGallery() {

        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()){

                    val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALLERY)

                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?, token: PermissionToken?)
            {
                    showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationaleDialogForPermissions(){
        AlertDialog.Builder(this).setMessage(
            "Enable Required Permissions in Settings")
            .setPositiveButton("GO TO SETTINGS"){
                _,_->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                }catch (e:ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){ dialog,_->
                dialog.dismiss()
            }.show()

    }

    private fun updateDateInView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }


    private fun saveImgToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }



   private fun isLocationEnable():Boolean{
       val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
       return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
               || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
   }


    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval=1000
        mLocationRequest.numUpdates = 1

        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest , mLocationCallBack , Looper.myLooper())

    }



    private var mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult?) {

            val mLastLocation: Location = p0!!.lastLocation
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity,mLatitude,mLongitude)

            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address : String?){
                    et_location.setText(address)
                }
                override fun onError(){
                    Log.e("Get Address:: ","failed")
                }
            })
            addressTask.getAddress()

        }
    }

    companion object {
        private const val GALLERY = 1
        private const val CAMERA =2
        private const val IMAGE_DIRECTORY = "HappyPlaceImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

}