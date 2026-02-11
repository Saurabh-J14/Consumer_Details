package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.TotalConsumerRepository
import com.example.feeder.ui.viewModel.TotalConsumerViewModel

class TotalConsumerViewModelFactory(
    private val repository: TotalConsumerRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TotalConsumerViewModel(repository) as T
    }
}
