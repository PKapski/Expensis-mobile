package pl.polsl.expensis_mobile.rest.requests

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import pl.polsl.expensis_mobile.utils.SharedPreferencesUtils
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


class CustomJsonArrayRequest(
        method: Int,
        url: String?,
        requestBody: JSONObject?,
        listener: Response.Listener<JSONArray?>,
        errorListener: Response.ErrorListener
) : JsonRequest<JSONArray>(
        method, url, requestBody.toString(), listener, errorListener
) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONArray> {
        return try {
            val jsonString = String(
                    response!!.data,
                    Charset.forName(HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET))
            )
            Response.success<JSONArray>(
                    JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: UnsupportedEncodingException) {
            Response.error<JSONArray>(ParseError(e))
        } catch (je: JSONException) {
            Response.error<JSONArray>(ParseError(je))
        }
    }

    override fun getHeaders(): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        if (SharedPreferencesUtils.isTokenPresent()) {
            headers["Authorization"] = "Bearer " + SharedPreferencesUtils.getAccessToken()
        }
        return headers
    }
}