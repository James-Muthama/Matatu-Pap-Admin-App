package com.example.matatupapadminapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class RouteStartFragment : Fragment() {

    private lateinit var buttonAddRouteStart: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.route_start_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the button here since onViewCreated is called after onCreateView
        buttonAddRouteStart = view.findViewById(R.id.button_add_route_start)

        // Set up click listener for the button
        buttonAddRouteStart.setOnClickListener {
            // Add functionality here, like calling a method in the activity to set the route start
            (activity as? AddRoutePageActivity)?.let { activity ->
                activity.setRouteStart() // You'd need to implement this method in AddRoutePageActivity
            }
        }
    }

    // This method is correctly placed as it's meant to be called from the activity or fragment
    // Note: It's not necessary to keep this within the fragment as a private method unless it's used here
    // If it's meant for use outside this fragment, it should be public or moved to the activity.
    fun AddRoutePageActivity.setRouteStart() {
        // Implement the logic to set the route start here
    }
}