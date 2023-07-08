package com.terraconnect.exchangerates.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.terraconnect.exchangerates.databinding.ItemViewListCurrenciesBinding
import com.terraconnect.exchangerates.models.Rate

class CurrencyListAdapter :
    ListAdapter<Rate, CurrencyListAdapter.CurrencyViewHolder>(CurrencyDiffCallBack()) {


    class CurrencyViewHolder private constructor(
        private val viewDataBinding: ItemViewListCurrenciesBinding,
    ) :
        RecyclerView.ViewHolder(viewDataBinding.root) {
        fun bind(rates: Rate) {
            viewDataBinding.rate = rates
            viewDataBinding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): CurrencyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val itemBinding = ItemViewListCurrenciesBinding.inflate(
                    layoutInflater, parent, false
                )
                return CurrencyViewHolder(itemBinding)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        return CurrencyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class CurrencyDiffCallBack : DiffUtil.ItemCallback<Rate>() {
    override fun areItemsTheSame(oldItem: Rate, newItem: Rate): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: Rate, newItem: Rate): Boolean {
        return oldItem == newItem
    }


}
