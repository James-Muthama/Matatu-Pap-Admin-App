package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng

class FragmentNameRoute : Fragment() {

    private lateinit var textView: TextView
    private lateinit var nameRouteButton: Button
    private lateinit var backToAddRouteButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_name_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.textView)
        nameRouteButton = view.findViewById(R.id.name_route_btn)
        backToAddRouteButton = view.findViewById(R.id.back_to_add_route_btn)

        // Set click listener for back_to_add_route_btn to go back to FragmentAddRoute
        backToAddRouteButton.setOnClickListener {
            goBackToPreviousFragment()
        }

        // Set click listener for name_route_btn to start NameRouteActivity with route details
        nameRouteButton.setOnClickListener {
            val intent = Intent(requireContext(), NameRouteActivity::class.java)

            // Retrieve routeStart and routeEnd from arguments if passed
            val routeStart = arguments?.getParcelable<LatLng>("routeStart")
            val routeEnd = arguments?.getParcelable<LatLng>("routeEnd")

            // Get nearby stops
            val nearbyStops = arguments?.getParcelableArrayList<LatLng>("nearbyStops")
                ?: (activity as? AddRoutePageActivity)?.nearbyStops

            // Add routeStart, routeEnd, and nearbyStops to the intent
            if (routeStart != null) {
                intent.putExtra("routeStart", routeStart)
            }
            if (routeEnd != null) {
                intent.putExtra("routeEnd", routeEnd)
            }
            intent.putParcelableArrayListExtra("nearbyStops", ArrayList(nearbyStops ?: emptyList()))

            startActivity(intent)
        }
    }

    private fun goBackToPreviousFragment() {
        parentFragmentManager.popBackStack()
    }
}