package com.ion606.installer

import android.app.AlertDialog
import android.content.Context

class Alert {
    companion object {
        fun showPopup(
            context: Context,
            title: String,
            message: String,
            onAccept: (Int) -> Unit,
            onCancel: (Int) -> Unit
        ) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)

            builder.setPositiveButton("OK") { dialogue, it ->
                onAccept(it)
                dialogue.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->
                onCancel(which)
                dialog.dismiss()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
}