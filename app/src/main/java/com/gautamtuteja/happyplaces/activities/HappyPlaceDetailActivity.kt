package com.gautamtuteja.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gautamtuteja.happyplaces.R
import com.gautamtuteja.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.activity_happy_place_detail.*

class HappyPlaceDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_happy_place_detail)

        var happyPlaceDetailModel : HappyPlaceModel ? =null

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAIL)){

            happyPlaceDetailModel = intent.getSerializableExtra(
                MainActivity.EXTRA_PLACE_DETAIL) as HappyPlaceModel

        }
        if(happyPlaceDetailModel != null){
            setSupportActionBar(toolbar_happy_place_detail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlaceDetailModel.title

            toolbar_happy_place_detail.setNavigationOnClickListener{
                onBackPressed()
            }

            iv_place_image.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            tv_description.text = happyPlaceDetailModel.description
            tv_location.text = happyPlaceDetailModel.location
        }

        btn_view_map.setOnClickListener {
            val intent = Intent(this,MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACE_DETAIL,happyPlaceDetailModel)
            startActivity(intent)
        }
    }
}