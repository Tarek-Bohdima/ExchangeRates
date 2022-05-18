package com.terraconnect.exchangerates.util

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.terraconnect.exchangerates.R

@BindingAdapter("currencyRate")
fun bindDoubleToString(textView: TextView, number: Double) {
    val context = textView.context
    textView.text = String.format(context.getString(R.string.currency_rate_double), number)
}