package jp.elestyle.androidapp.elepaydemoapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.elestyle.androidapp.elepay.ElepayError
import jp.elestyle.androidapp.elepaydemoapp.R
import jp.elestyle.androidapp.elepaydemoapp.data.SupportedPaymentMethod
import jp.elestyle.androidapp.elepaydemoapp.ui.adapter.PaymentMethodListAdapter
import jp.elestyle.androidapp.elepaydemoapp.ui.adapter.PaymentMethodListAdapterListener
import jp.elestyle.androidapp.elepaydemoapp.ui.dialog.PermissionRequestDialog
import jp.elestyle.androidapp.elepaydemoapp.utils.APIKeys
import jp.elestyle.androidapp.elepaydemoapp.utils.PaymentManager
import jp.elestyle.androidapp.elepaydemoapp.utils.PaymentResultHandler
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class PaymentActivity : AppCompatActivity(), PaymentResultHandler {

    private val tag: String = "PaymentActivity"

    // -----------------------------------------------------------------
    //
    //                   /aaaaaaaaa\
    //                  d'          `b
    //                  8  ,aaa,      "Y888a     ,aaaa,     ,aaa,  ,aa,
    //                  8  8' `8          "88baadP""""YbaaadP"""YbdP""Yb
    //                  8  8   8             """        """      ""    8b
    //                  8  8, ,8         ,aaaaaaaaaaaaaaaaaaaaaaaaaaa88P
    //                  8  `"""'      ,d8""
    //                   \           /
    //                    \aaaaaaaaa/
    //
    private val testModePublicKey = PaymentManager.INVALID_KEY
    private val liveModePublicKey = PaymentManager.INVALID_KEY

    // The following keys are used to generate charge data.
    // You may consider create your charge data from your server for payment management.
    // So these keys may not live here.
    private val testModeSecretKey = PaymentManager.INVALID_KEY
    private val liveModeSecretKey = PaymentManager.INVALID_KEY
    // ↑ Replace your keys above
    // -----------------------------------------------------------------

    private val paymentUrl = PaymentManager.MAKE_CHARGE_DEMO_URL
    // ↑ Change this url to your own server to request charge object if necessary

    private lateinit var paymentMethodIndicator: TextView
//    private lateinit var progressDialog: MaterialDialog
    private lateinit var testModeSwitch: Switch
    private lateinit var paymentMethodListView: RecyclerView

    private lateinit var paymentManager: PaymentManager
    private lateinit var amount: String
    private var paymentMethod: SupportedPaymentMethod = SupportedPaymentMethod.CREDIT_CARD
    private val paymentMethodListAdapterListener = object : PaymentMethodListAdapterListener {
        override fun onSelectPaymentMethod(method: SupportedPaymentMethod) {
            Log.d(tag, "selected ${method.rawValue}")
            selectPaymentMethod(method)
        }
    }

    companion object {
        const val PREFS_NAME = "ElepayDemoAppPrefs"
        const val INTENT_KEY_AMOUNT = "amount"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        amount = intent.extras?.getString(INTENT_KEY_AMOUNT).orEmpty()
        if (amount.isEmpty()) {
            Log.d("PaymentActivity", "no amount.")
            finish()
        }

        // UI

        setContentView(R.layout.activity_payment)

//        progressDialog = MaterialDialog.Builder(this).progress(true, 0).build()
        val amountView = findViewById<TextView>(R.id.amount)
        amountView.text = "¥$amount"
        paymentMethodIndicator = findViewById(R.id.paymentMethodIndicator)
        paymentMethodListView = findViewById(R.id.paymentMethodListView)
        paymentMethodListView.adapter = PaymentMethodListAdapter(paymentMethodListAdapterListener)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        paymentMethodListView.layoutManager = layoutManager
        testModeSwitch = findViewById(R.id.testModeSwitch)
        testModeSwitch.setOnClickListener {
            Log.d("PaymentActivity", "changed: ${testModeSwitch.isChecked}")
            saveTestModeSetting(testModeSwitch.isChecked)
            showTestModeChangingRestartDialog(getString(R.string.test_mode_switch_restart_prompt))
        }
        testModeSwitch.isChecked = loadTestModeSetting()

        findViewById<Button>(R.id.payButton).setOnClickListener {
//            progressDialog.setContent(R.string.content_processing)
//            progressDialog.show()
            performPaying(amount = amount)
        }

        selectPaymentMethod(SupportedPaymentMethod.CREDIT_CARD)

        // Data
        setupPaymentManager()
    }

    override fun onPaySucceeded() {
        runOnUiThread {
//            progressDialog.dismiss()
            showResultMessage(message = "Succeeded paying ${paymentMethod.rawValue} $amount")
        }
    }

    override fun onPayFailed(error: ElepayError) {
        runOnUiThread {
//            progressDialog.dismiss()
        }
        val message = when (error) {
            is ElepayError.InvalidPayload -> "${error.errorCode} ${error.message}"
            is ElepayError.SystemError -> "${error.errorCode} ${error.message}"
            is ElepayError.AlreadyMakingPayment -> "Already paying: ${error.paymentId}"
            is ElepayError.PaymentFailure -> "${error.errorCode} ${error.message}"
            is ElepayError.UnsupportedPaymentMethod -> error.paymentMethod
            is ElepayError.UninitializedPaymentMethod -> "${error.errorCode} ${error.paymentMethod}"
            is ElepayError.PermissionRequired -> null
            is ElepayError.SDKNotSetup -> TODO()
        }
        if (message != null) {
            runOnUiThread { showResultMessage(message) }
        }
        if (error is ElepayError.PermissionRequired) {
            val permissions = error.permissions.joinToString()
            runOnUiThread {
                PermissionRequestDialog.show(
                    "Permissions",
                    "Permissions are required: $permissions",
                    context = this
                )
            }
        }
    }

    override fun onPayCanceled() {
        runOnUiThread {
//            progressDialog.dismiss()
            showResultMessage(message = "Canceled paying ${paymentMethod.rawValue} $amount")
        }
    }

    private fun setupPaymentManager() {
        val json = try {
            JSONObject(String(assets.open("config.json").readBytes()))
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        // You may uses your own server to process the whole payment procedure. Please contact our support for details.
        // Leave this value to empty to use elepay's server.
        val baseUrl: String
        val paymentUrl: String
        val testPubKey: String
        val livePubKey: String
        val testSecKey: String
        val liveSecKey: String
        if (json != null) {
            baseUrl = json.optString("baseUrl", "")
            paymentUrl = json.optString("paymentUrl", this.paymentUrl)
            testPubKey = json.optString("pk_test", this.testModePublicKey)
            livePubKey = json.optString("pk_live", this.liveModePublicKey)
            testSecKey = json.optString("sk_test", this.testModeSecretKey)
            liveSecKey = json.optString("sk_live", this.liveModeSecretKey)
        } else {
            baseUrl = ""
            paymentUrl = this.paymentUrl
            testPubKey = this.testModePublicKey
            livePubKey = this.liveModePublicKey
            testSecKey = this.testModeSecretKey
            liveSecKey = this.liveModeSecretKey
        }

        if (testPubKey == PaymentManager.INVALID_KEY
            || livePubKey == PaymentManager.INVALID_KEY
            || testSecKey == PaymentManager.INVALID_KEY
            || liveSecKey == PaymentManager.INVALID_KEY
        ) {
            finishWithoutValidKeys()
        }

        val keys = APIKeys(
            publicLiveKey = livePubKey,
            secretLiveKey = liveSecKey,
            publicTestKey = testPubKey,
            secretTestKey = testSecKey
        )
        paymentManager = PaymentManager(
            isTestMode = testModeSwitch.isChecked,
            apiKeys = keys,
            baseUrl = baseUrl,
            paymentUrl = paymentUrl
        )
        paymentManager.resultHandler = this
    }

    private fun performPaying(amount: String) {
        Log.d(tag, "performPaying: method=${paymentMethod.rawValue}")

        paymentManager.makePayment(amount = amount, method = paymentMethod, activity = this)
    }

    private fun loadTestModeSetting(): Boolean =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).run {
            getBoolean("test_mode", false)
        }

    @SuppressLint("ApplySharedPref")
    private fun saveTestModeSetting(testMode: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("test_mode", testMode)
            .commit()
    }

    private fun finishWithoutValidKeys() {
        AlertDialog.Builder(this)
            .setMessage("No valid keys provided. The keys are generated from elepay's admin page.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .create()
            .show()
    }

    private fun showResultMessage(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun showTestModeChangingRestartDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_button_title_ok) { _, _ -> recreate() }
            .setNegativeButton(R.string.dialog_button_title_cancel) { _, _ ->
                // Restore the checked state, since the dialog is showed after the changing.
                testModeSwitch.isChecked = !testModeSwitch.isChecked
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun selectPaymentMethod(paymentMethod: SupportedPaymentMethod) {
        this.paymentMethod = paymentMethod

        paymentMethod.associatedLocalisedResrouceId().also { resourceId ->
            if (resourceId > 0) {
                paymentMethodIndicator.setText(resourceId)
            }
        }
    }
}
