package com.dicoding.story_app.component

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import com.dicoding.story_app.R
import com.google.android.material.textfield.TextInputLayout


class PasswordEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var parentLayout: TextInputLayout? = null

    init {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        addTextChangedListener { s ->
            if (parentLayout != null && (s == null || s.length < 8)) {
                parentLayout?.error =
                    context.getString(R.string.error_password)
            } else {
                parentLayout?.error = null
            }
        }
    }

    fun setParentLayout(layout: TextInputLayout) {
        parentLayout = layout
    }
}
