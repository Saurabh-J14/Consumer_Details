import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.feeder.data.model.ConsumerUpdateResponse
import com.example.feeder.data.remote.ApiInterface
import com.example.feeder.utils.MultipartUtils
import com.example.feeder.utils.MultipartUtils.toPart
import retrofit2.Response

class ConsumerUpdateRepository(
    private val api: ApiInterface
) {

    suspend fun updateConsumer(
        context: Context,
        token: String,
        body: ConsumerUpdateBody,
        bitmap: Bitmap?
    ): Response<ConsumerUpdateResponse> {

        return api.updateConsumer(
            token = token,
           consumerNumber = body.ConsumerNumber,
            MeterNumber = body.MeterNumber.toPart(),
            FeederId = body.FeederId.toPart(),
            Feeder_Name = body.Feeder_Name.toPart(),
            Substation_Name = body.Substation_Name.toPart(),
            PhaseDesignation = body.PhaseDesignation.toPart(),
            Voltage = body.Voltage.toPart(),
            DTCName = body.DTCName.toPart(),
            DTCCode = body.DTCCode.toPart(),
            Latitude = body.Latitude.toPart(),
            Longitude = body.Longitude.toPart(),
            Location = body.Location.toPart(),
            UserID = body.UserID.toPart(),
            SanctionedLoad = body.SanctionedLoad.toPart(),
            MobileNo = body.MobileNo.toPart(),
            CreatedOn = body.CreatedOn.toPart(),
            Image = MultipartUtils.createImagePart(context, bitmap)
            )

        Log.d("updateConsumer: ", body.toString())

    }
}
