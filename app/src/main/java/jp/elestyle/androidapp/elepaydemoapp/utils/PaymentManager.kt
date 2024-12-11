package jp.elestyle.androidapp.elepaydemoapp.utils

import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import jp.elestyle.androidapp.elepay.Elepay
import jp.elestyle.androidapp.elepay.ElepayConfiguration
import jp.elestyle.androidapp.elepay.ElepayError
import jp.elestyle.androidapp.elepay.ElepayResult
import jp.elestyle.androidapp.elepaydemoapp.data.SupportedPaymentMethod
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID

interface PaymentResultHandler {
    fun onPaySucceeded()
    fun onPayFailed(error: ElepayError)
    fun onPayCanceled()
}

data class APIKeys(
    val publicTestKey: String,
    val secretTestKey: String,
    val publicLiveKey: String,
    val secretLiveKey: String,
)

class PaymentManager(
    private val isTestMode: Boolean,
    private val apiKeys: APIKeys,
    baseUrl: String = "",
    private val paymentUrl: String = PaymentManager.MAKE_CHARGE_DEMO_URL,
) {

    var resultHandler: PaymentResultHandler? = null

    companion object {
        const val MAKE_CHARGE_DEMO_URL = "https://api.elepay.io/charges"
        const val INVALID_KEY = "Please use the key generated from elepay admin page."
    }

    init {
        val configuration = ElepayConfiguration(
            apiKey = if (isTestMode) apiKeys.publicTestKey else apiKeys.publicLiveKey,
            remoteHostBaseUrl = baseUrl
        )
        Elepay.setup(configuration)
    }

    fun makePayment(amount: String, method: SupportedPaymentMethod, activity: AppCompatActivity) {
        // NOTE: The charge object should be created from your own server.
        // Here just a demo for requesting charge object.

        val authString: String = if (isTestMode) {
            "Basic ${
                Base64.encode(
                    (apiKeys.secretTestKey + ":").toByteArray(), Base64.NO_WRAP
                ).toString(Charsets.UTF_8)
            }"
        } else {
            "Basic ${
                Base64.encode(
                    (apiKeys.secretLiveKey + ":").toByteArray(), Base64.NO_WRAP
                ).toString(Charsets.UTF_8)
            }"
        }
        val headerFields = mapOf("Authorization" to authString)
        val params = JSONObject().apply {
            put("paymentMethod", method.rawValue)
            put("amount", amount)
            put("orderNo", UUID.randomUUID().toString())
            put("description", "iCart Store Android app charge")
        }
        Thread(
            POSTJsonRequester(
                url = paymentUrl,
                headerFields = headerFields,
                params = params,
                resultHandler = { result ->
                    when (result) {
                        is RequestResult.Error ->
                            resultHandler?.onPayFailed(
                                ElepayError.SystemError(
                                    errorCode = "1111",
                                    message = result.message
                                )
                            )

                        is RequestResult.JSONResult ->
                            Elepay.processPayment(
                                chargeData = result.jsonObject,
                                fromActivity = activity
                            ) { payResult ->
                                when (payResult) {
                                    is ElepayResult.Succeeded -> resultHandler?.onPaySucceeded()
                                    is ElepayResult.Failed -> resultHandler?.onPayFailed(error = payResult.error)
                                    is ElepayResult.Canceled -> resultHandler?.onPayCanceled()
                                }
                            }
                    }

                })
        ).start()
    }
}

private sealed class RequestResult {
    class JSONResult(val jsonObject: JSONObject) : RequestResult()
    class Error(val message: String) : RequestResult()
}

private class POSTJsonRequester(
    val url: String,
    val headerFields: Map<String, String>,
    val params: JSONObject,
    val resultHandler: (RequestResult) -> Unit,
) : Runnable {

    override fun run() {
        val conn: HttpURLConnection?
        try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            for (entry in headerFields) {
                conn.setRequestProperty(entry.key, entry.value)
            }
            conn.connectTimeout = 20000
            conn.readTimeout = 10000

            val outputStream = conn.outputStream
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            writer.write(params.toString())
            writer.flush()
            writer.close()
            outputStream.close()

            if (conn.errorStream != null) {
                handleError(conn)
            } else {
                val json = JSONObject(String(conn.inputStream.readBytes()))
                resultHandler(RequestResult.JSONResult(jsonObject = json))
            }
        } catch (e: MalformedURLException) {
            resultHandler(RequestResult.Error(e.localizedMessage))
        } catch (e: IOException) {
            e.printStackTrace()
            resultHandler(RequestResult.Error(e.localizedMessage))
        } catch (e: JSONException) {
            resultHandler(RequestResult.Error(e.localizedMessage))
        }
    }

    private fun handleError(conn: HttpURLConnection) {
        var rawErrorStr = ""
        try {
            BufferedReader(InputStreamReader(conn.errorStream)).also {
                var line: String?
                do {
                    line = it.readLine()
                    rawErrorStr += line
                } while (line != null)
            }
        } catch (e: Exception) {
            rawErrorStr = e.toString()
        }
        Log.d("PaymentManager", rawErrorStr)
        val jsonError = try {
            JSONObject(rawErrorStr)
        } catch (e: Exception) {
            JSONObject()
        }
        resultHandler(RequestResult.Error(jsonError.optString("message", "")))
    }
}
