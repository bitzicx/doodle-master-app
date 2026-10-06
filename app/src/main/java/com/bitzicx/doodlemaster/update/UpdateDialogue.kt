package com.bitzicx.doodlemaster.update

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.net.toUri

@Composable
fun UpdateDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val info = viewModel.updateInfo ?: return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { viewModel.dismiss() },
        title = { Text("Update available: v${info.versionName}") },
        text = { Text(info.notes.ifBlank { "A new version is ready to download." }) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, info.downloadUrl.toUri())
                )
                viewModel.dismiss()
            }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismiss() }) { Text("Later") }
        }
    )
}