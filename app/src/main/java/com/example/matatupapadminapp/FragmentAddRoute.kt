package com.example.matatupapadminapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class FragmentAddRoute : Fragment() {

    private lateinit var textView: TextView
    private lateinit var confirmRouteButton: Button
    private lateinit var removeStageButton: Button

    // Declare references to listen to our custom interfaces
    private var routeActionsListener: RouteActions? = null
    private var removeStageListener: RemoveStageListener? = null

    // Interface for removing stage markers
    interface RemoveStageListener {
        fun removeStageMarkers()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RouteActions) {
            routeActionsListener = context
        } else {
            throw RuntimeException("$context must implement RouteActions")
        }
        if (context is RemoveStageListener) {
            removeStageListener = context
        } else {
            throw RuntimeException("$context must implement RemoveStageListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        routeActionsListener = null
        removeStageListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.textView10)
        confirmRouteButton = view.findViewById(R.id.add_route_btn)
        removeStageButton = view.findViewById(R.id.remove_stages_btn)

        // Set click listener for remove stage button
        removeStageButton.setOnClickListener {
            removeStageListener?.removeStageMarkers()
        }

        // Set click listener for confirm route button
        confirmRouteButton.setOnClickListener {
            val result = routeActionsListener?.planRoute() ?: false
            if (result) {
                // Launch a new coroutine to check for the result
                activity?.runOnUiThread {
                    checkRoutePlanningSuccess()
                }
            }
        }
    }

    private fun checkRoutePlanningSuccess() {
        activity?.let { activity ->
            (activity as? AddRoutePageActivity)?.let { addRouteActivity ->
                addRouteActivity.globalScopeJob?.invokeOnCompletion { throwable ->
                    if (throwable != null) {
                        // An error occurred during route planning
                        Toast.makeText(activity, "An error occurred during route planning: ${throwable.message}", Toast.LENGTH_SHORT).show()
                    } else {
                        // Check if route planning was successful
                        if (addRouteActivity.isRoutePlanningSuccessful) {
                            try {
                                parentFragmentManager.beginTransaction()
                                    .hide(this)
                                    .add(R.id.fragment_route, FragmentNameRoute())
                                    .addToBackStack(null)
                                    .commit()
                            } catch (e: Exception) {
                                // Handle the exception, perhaps log it or show a user-friendly message
                                Toast.makeText(activity, "Failed to show name route fragment: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(activity, "Route planning was not successful", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}