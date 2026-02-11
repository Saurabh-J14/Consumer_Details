import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.PendingConsumerRepository
import com.example.feeder.ui.viewModel.PendingConsumerViewModel

class PendingConsumerViewModelFactory(
    private val repository: PendingConsumerRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PendingConsumerViewModel(repository) as T
    }
}
