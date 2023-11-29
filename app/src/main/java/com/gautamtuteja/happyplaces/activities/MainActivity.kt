package com.gautamtuteja.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gautamtuteja.happyplaces.R
import com.gautamtuteja.happyplaces.adapters.HappyPlaceAdapter
import com.gautamtuteja.happyplaces.database.DatabaseHandler
import com.gautamtuteja.happyplaces.models.HappyPlaceModel
import com.gautamtuteja.happyplaces.utils.SwipeToDeleteCallback
import com.gautamtuteja.happyplaces.utils.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabAddHappyPlace.setOnClickListener{
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }

        getHappyPlacesListFromLocalDB()
    }

    private fun setUpHappyPlacesRecyclerView(
        happyPlaceList:ArrayList<HappyPlaceModel>){
        rv_happy_place.layoutManager = LinearLayoutManager(this)
        rv_happy_place.setHasFixedSize(true)

        val placesAdapter = HappyPlaceAdapter(this,happyPlaceList)
        rv_happy_place.adapter = placesAdapter

        placesAdapter.setOnClickListener(object :HappyPlaceAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAIL, model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object :SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_place.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(rv_happy_place)


        val DeleteSwipeHandler = object :SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_place.adapter as HappyPlaceAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                getHappyPlacesListFromLocalDB()
            }
        }
        val DeleteItemTouchHelper = ItemTouchHelper(DeleteSwipeHandler)
        DeleteItemTouchHelper.attachToRecyclerView(rv_happy_place)

    }

    private fun getHappyPlacesListFromLocalDB() {

        val dbHandler = DatabaseHandler(this)

        val getHappyPlacesList = dbHandler.getHappyPlacesList()

        if (getHappyPlacesList.size > 0) {
            rv_happy_place.visibility = View.VISIBLE
            tv_no_record.visibility = View.GONE
            setUpHappyPlacesRecyclerView(getHappyPlacesList)
        } else {
            rv_happy_place.visibility = View.GONE
            tv_no_record.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                getHappyPlacesListFromLocalDB()    //for dynamic list show
            }else{

            }
        }
    }

    companion object{
        var ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        var EXTRA_PLACE_DETAIL = "extra place details"
    }
}