package com.example.matatupapadminapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class RouteStartFragment : Fragment() {

    private lateinit var textViewRouteStart: TextView
    private lateinit var buttonAddRouteStart: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.route_start, container, false)
        buttonAddRouteStart = view.findViewById(R.id.button_add_route_start)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up click listener for the button
        buttonAddRouteStart.setOnClickListener {
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