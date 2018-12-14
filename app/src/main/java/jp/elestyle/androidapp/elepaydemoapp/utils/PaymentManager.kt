package jp.elestyle.androidapp.elepaydemoapp.utils

import androidx.appcompat.app.AppCompatActivity
import jp.elestyle.androidapp.elepay.ElePay
import jp.elestyle.androidapp.elepay.ElePayError
import jp.elestyle.androidapp.elepay.ElePayResult
import jp.elestyle.androidapp.elepaydemoapp.data.SupportedPaymentMethod
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

interface PaymentResultHandler {
    fun onPaySucceeded()
    fun onPayFailed(error: ElePayError)
    fun onPayCanceled()
}

class PaymentManager(
        private val isTestMode: Boolean,
        appScheme: String,
        testModeKey: String,
        liveModeKey: String,
        baseUrl: String = "",
        private val paymentUrl: String = PaymentManager.DEFAULT_PAYMENT_URL) {

    var resultHandler: PaymentResultHandler? = null

    companion object {
        const val DEFAULT_PAYMENT_URL = "https://demo.icart.jp/api/orders"
        const val INVALID_TEST_KEY = "Your test key here. Please use the key generated from elepay admin page."
        const val INVALID_LIVE_KEY = "Your live key here. Please use the key generated from elepay admin page."
    }

    init {
        ElePay.setup(appScheme = appScheme, appKey = if (isTestMode) testModeKey else liveModeKey, remoteHostBaseUrl = baseUrl)
    }

    fun makePayment(amount: String, method: SupportedPaymentMethod, activity: AppCompatActivity) {
        // NOTE: The charge object should be created from your own server.
        // Here just a demo for requesting charge object.

        // You can change this map value to specify the mode forcibly.
        val headerFields = mapOf("live-mode" to if (isTestMode) "false" else "true")
        val params = JSONObject().apply {
            put("paymentMethod", method.rawValue)
            put("amount", amount)
        }
        Thread(POSTJsonRequester(
                url = paymentUrl, headerFields = headerFields, params = params, resultHandler = { result ->
            when (result) {
                is RequestResult.Error ->
                    resultHandler?.onPayFailed(ElePayError.SystemError(errorCode = 1111, message = "Failed creating charge object."))
                is RequestResult.JSONResult ->
                    ElePay.processPayment(chargeData = result.jsonObject, fromActivity = activity) { payResult ->
                        when (payResult) {
                            is ElePayResult.Succeeded -> resultHandler?.onPaySucceeded()
                            is ElePayResult.Failed -> resultHandler?.onPayFailed(error = payResult.error)
                            is ElePayResult.Canceled -> resultHandler?.onPayCanceled()
                        }
                    }
            }

        })).start()
    }
}

private sealed class RequestResult {
    class JSONResult(val jsonObject: JSONObject) : RequestResult()
    class Error : RequestResult()
}

private class POSTJsonRequester(
        val url: String,
        val headerFields: Map<String, String>,
        val params: JSONObject,
        val resultHandler: (RequestResult) -> Unit
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

            val json = JSONObject(String(conn.inputStream.readBytes()))
            resultHandler(RequestResult.JSONResult(jsonObject = json))
        } catch (e: MalformedURLException) {
            resultHandler(RequestResult.Error())
        } catch (e: IOException) {
            e.printStackTrace()
            resultHandler(RequestResult.Error())
        } catch (e: JSONException) {
            resultHandler(RequestResult.Error())
        }
    }
}
