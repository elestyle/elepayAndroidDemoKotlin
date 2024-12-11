package jp.elestyle.androidapp.elepaydemoapp.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

/**
 * Show a normal dialog with text indicating permission request.
 * Action buttons are fixed to [Cancel] and [Settings], clicking [Settings] button will open
 * app's settings page.
 */
object PermissionRequestDialog {
    fun show(title: String, message: String, context: Context) {
        AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }
}