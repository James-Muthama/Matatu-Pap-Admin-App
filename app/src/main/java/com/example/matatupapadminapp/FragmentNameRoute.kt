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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_name_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.textView) // Ensure this ID exists in your layout
        nameRouteButton = view.findViewById(R.id.name_route_btn) // Assuming this ID
        backToAddRouteButton = view.findViewById(R.id.back_to_add_route_btn) // Ensure this ID matches in your layout

        backToAddRouteButton.setOnClickListener {
            requireActivity().finish()
        }

        // Now set the click listener
        nameRouteButton.setOnClickListener {
            val intent = Intent(requireContext(), NameRouteActivity::class.java)
            val nearbyStops = arguments?.getParcelableArrayList<LatLng>("nearbyStops")
                ?: (activity as? AddRoutePageActivity)?.nearbyStops

            intent.putParcelableArrayListExtra("nearbyStops", ArrayList(nearbyStops ?: emptyList()))
            startActivity(intent)
        }
    }
}