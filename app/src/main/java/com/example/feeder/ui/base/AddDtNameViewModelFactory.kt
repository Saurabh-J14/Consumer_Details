package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.AddDtNameRepository
import com.example.feeder.ui.viewModel.AddDtNameActivityViewModel

class AddDtNameViewModelFactory(
    private val repo: AddDtNameRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddDtNameActivityViewModel(repo) as T
    }
}
