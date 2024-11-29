package com.dicoding.story_app.component

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import com.dicoding.story_app.R
import com.google.android.material.textfield.TextInputLayout

class EmailEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var parentLayout: TextInputLayout? = null

    init {
        addTextChangedListener { s ->
            if (parentLayout != null && !isValidInput(s)) {
                parentLayout?.error = context.getString(R.string.error_email)
            } else {
                parentLayout?.error = null
            }
        }

    }

    private fun isValidInput(input: CharSequence?): Boolean {
        return !input.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(input)
            .matches()
    }

    fun setParentLayout(layout: TextInputLayout) {
        parentLayout = layout
    }
}