import com.example.feeder.data.remote.ApiInterface

class SubstationRepository(
    private val apiInterface: ApiInterface
) {
    suspend fun getSubstation(token: String) =
        apiInterface.getSubstation(token)
}
