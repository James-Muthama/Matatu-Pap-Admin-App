package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class RouteEndFragment : Fragment() {

    private lateinit var buttonAddRouteEnd: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.route_end_fragment, container, false)
        buttonAddRouteEnd = view.findViewById<Button>(R.id.button_add_route_end)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up click listener for the button
        buttonAddRouteEnd.setOnClickListener {
            // Add functionality here, like calling a method in the activity to set the route start
            (activity as? AddRoutePageActivity)?.let { activity ->
                activity.setRouteStart() // You'd need to implement this method in AddRoutePageActivity
            }
        }
    }

    // Placeholder for a method to set the route start, which would be implemented in the activity
    private fun AddRoutePageActivity.setRouteStart() {
        // Implement the logic to set the route start here
    }
}