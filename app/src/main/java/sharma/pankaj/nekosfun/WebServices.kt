package sharma.pankaj.nekosfun

import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface WebServices {

    @GET
    fun getImage(@Url key: String): Observable<NekosResponse>
}