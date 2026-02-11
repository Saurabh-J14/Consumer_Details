package com.example.feeder.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.ListRepository

class ListViewModeFactory(
    private val repository: ListRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ListActivityViewModel(repository) as T
    }
}