import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.matatupapadminapp.R

class FragmentConfirmDeleteRoute : DialogFragment() {

    private lateinit var backBtn: Button
    private lateinit var continueDeleteBtn: Button
    private var routeName: String? = null

    private var onDeleteConfirmed: (() -> Unit)? = null
    private var onDeleteCancelled: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            routeName = it.getString("routeName")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirm_delete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backBtn = view.findViewById(R.id.back_btn)
        continueDeleteBtn = view.findViewById(R.id.continue_delete_btn)

        backBtn.setOnClickListener {
            onDeleteCancelled?.invoke()
            dismiss()
        }

        continueDeleteBtn.setOnClickListener {
            onDeleteConfirmed?.invoke()
            dismiss()
        }

        // Optionally, adjust dialog size if needed
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setOnDeleteConfirmedListener(listener: () -> Unit) {
        this.onDeleteConfirmed = listener
    }

    fun setOnDeleteCancelledListener(listener: () -> Unit) {
        this.onDeleteCancelled = listener
    }
}