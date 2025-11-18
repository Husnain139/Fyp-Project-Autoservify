package com.hstan.autoservify.ui.reviews

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.hstan.autoservify.R
import com.hstan.autoservify.model.Review
import com.hstan.autoservify.model.repositories.ReviewRepository
import kotlinx.coroutines.launch

class ReviewDialog : DialogFragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var ratingText: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    private val reviewRepository = ReviewRepository()

    private var itemId: String = ""
    private var itemType: String = ""
    private var shopId: String = ""
    private var userId: String = ""
    private var userName: String = ""
    private var onReviewSubmitted: (() -> Unit)? = null

    companion object {
        fun newInstance(
            itemId: String,
            itemType: String,
            shopId: String,
            userId: String,
            userName: String,
            onReviewSubmitted: () -> Unit
        ): ReviewDialog {
            val dialog = ReviewDialog()
            dialog.itemId = itemId
            dialog.itemType = itemType
            dialog.shopId = shopId
            dialog.userId = userId
            dialog.userName = userName
            dialog.onReviewSubmitted = onReviewSubmitted
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ratingBar = view.findViewById(R.id.ratingBar)
        ratingText = view.findViewById(R.id.ratingText)
        commentInput = view.findViewById(R.id.commentInput)
        submitButton = view.findViewById(R.id.submitButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        setupRatingBar()
        setupButtons()
    }

    private fun setupRatingBar() {
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            when {
                rating == 0f -> {
                    ratingText.text = "Tap stars to rate"
                    submitButton.isEnabled = false
                }
                rating <= 1.5f -> {
                    ratingText.text = "Poor"
                    submitButton.isEnabled = true
                }
                rating <= 2.5f -> {
                    ratingText.text = "Fair"
                    submitButton.isEnabled = true
                }
                rating <= 3.5f -> {
                    ratingText.text = "Good"
                    submitButton.isEnabled = true
                }
                rating <= 4.5f -> {
                    ratingText.text = "Very Good"
                    submitButton.isEnabled = true
                }
                else -> {
                    ratingText.text = "Excellent"
                    submitButton.isEnabled = true
                }
            }
        }
    }

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        submitButton.setOnClickListener {
            submitReview()
        }
    }

    private fun submitReview() {
        val rating = ratingBar.rating
        if (rating == 0f) {
            Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = commentInput.text?.toString()?.trim() ?: ""

        val review = Review(
            userId = userId,
            userName = userName,
            shopId = shopId,
            itemId = itemId,
            itemType = itemType,
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        submitButton.isEnabled = false
        submitButton.text = "Submitting..."

        lifecycleScope.launch {
            try {
                val result = reviewRepository.saveReview(review)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                    onReviewSubmitted?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to submit review", Toast.LENGTH_SHORT).show()
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Review"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Review"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

