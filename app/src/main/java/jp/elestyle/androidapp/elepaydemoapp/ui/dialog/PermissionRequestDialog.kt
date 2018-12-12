package jp.elestyle.androidapp.elepaydemoapp.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import jp.elestyle.androidapp.elepay.R

/**
 * Show a normal dialog with text indicating permission request.
 * Action buttons are fixed to [Cancel] and [Settings], clicking [Settings] button will open
 * app's settings page.
 */
object PermissionRequestDialog {
    fun show(title: String, message: String, context: Context) {
        AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Settings", DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                })
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
    }
}