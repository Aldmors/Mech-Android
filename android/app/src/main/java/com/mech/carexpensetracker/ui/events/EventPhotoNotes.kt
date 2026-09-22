package com.mech.carexpensetracker.ui.events

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.EventPhotoFiles
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

class EventPhotoSession(
    private val viewModel: EventsViewModel,
) {
    var pending by mutableStateOf(listOf<EventPhotoEntity>())
        private set
    var removedIds by mutableStateOf(setOf<String>())
        private set
    private var committed = false

    suspend fun add(uris: List<Uri>, currentCount: Int) {
        val room = (EventPhotoFiles.MAX_PER_EVENT - currentCount).coerceAtLeast(0)
        uris.take(room).forEach { uri ->
            viewModel.copyPickedPhoto(uri)?.let { pending = pending + it }
        }
    }

    fun removeSaved(photo: EventPhotoEntity) {
        removedIds = removedIds + photo.externalId
    }

    fun removePending(photo: EventPhotoEntity) {
        viewModel.discardPhotoFile(photo)
        pending = pending.filter { it.externalId != photo.externalId }
    }

    suspend fun commit(eventExternalId: String, savedPhotos: List<EventPhotoEntity>) {
        val keepIds = savedPhotos.map { it.externalId }.filter { it !in removedIds }.toSet()
        viewModel.commitPhotos(eventExternalId, keepIds, pending)
        committed = true
    }

    fun discardIfNeeded() {
        if (!committed) pending.forEach { viewModel.discardPhotoFile(it) }
    }
}

@Composable
fun rememberEventPhotoSession(viewModel: EventsViewModel): EventPhotoSession {
    val session = remember(viewModel) { EventPhotoSession(viewModel) }
    DisposableEffect(session) {
        onDispose { session.discardIfNeeded() }
    }
    return session
}

@Composable
fun EventPhotoNotesSection(
    savedPhotos: List<EventPhotoEntity>,
    session: EventPhotoSession,
    photoModel: (EventPhotoEntity) -> Any,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val visibleSaved = savedPhotos.filter { it.externalId !in session.removedIds }
    val photos = visibleSaved + session.pending
    val canAdd = photos.size < EventPhotoFiles.MAX_PER_EVENT
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Any?>(null) }
    var showSource by remember { mutableStateOf(false) }
    var capturePath by rememberSaveable { mutableStateOf<String?>(null) }
    val canCapture = remember(context) { canCapturePhoto(context) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch { session.add(uris, photos.size) }
    }
    val camera = rememberLauncherForActivityResult(capturePictureContract) { success ->
        val file = capturePath?.let(::File)
        capturePath = null
        if (file == null) return@rememberLauncherForActivityResult
        // ponytail: some cameras write EXTRA_OUTPUT then return CANCELED; file size is the check
        val captured = success || file.length() > 0
        if (!captured) {
            file.delete()
            return@rememberLauncherForActivityResult
        }
        val uri = captureOutputUri(context, file)
        scope.launch {
            try {
                session.add(listOf(uri), photos.size)
            } finally {
                file.delete()
            }
        }
    }
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val launchCamera = {
        val file = EventPhotoFiles.captureFile(context.cacheDir, UUID.randomUUID().toString())
        try {
            capturePath = file.absolutePath
            camera.launch(captureOutputUri(context, file))
        } catch (_: Exception) {
            capturePath = null
            file.delete()
            launchPicker()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
    ) {
        SectionHeader(title = stringResource(R.string.event_photos))
        if (photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                items(photos, key = { it.externalId }) { photo ->
                    val model = photoModel(photo)
                    EventPhotoThumb(
                        model = model,
                        onOpen = { preview = model },
                        onRemove = {
                            if (session.pending.any { it.externalId == photo.externalId }) {
                                session.removePending(photo)
                            } else {
                                session.removeSaved(photo)
                            }
                        },
                    )
                }
            }
        }
        SecondaryButton(
            text = stringResource(R.string.add_event_photo),
            onClick = {
                if (canCapture) showSource = true else launchPicker()
            },
            icon = Icons.Default.AddAPhoto,
            enabled = canAdd,
        )
    }

    if (showSource) {
        AlertDialog(
            onDismissRequest = { showSource = false },
            title = { Text(stringResource(R.string.add_event_photo)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSource = false
                        launchCamera()
                    },
                ) {
                    Text(stringResource(R.string.take_photo))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSource = false
                        launchPicker()
                    },
                ) {
                    Text(stringResource(R.string.choose_photo))
                }
            },
        )
    }

    preview?.let { model ->
        Dialog(onDismissRequest = { preview = null }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = stringResource(R.string.event_photo),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun EventPhotoThumb(
    model: Any,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val thumbSize = DesignTokens.Spacing.xl * 2
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.event_photo),
            modifier = Modifier
                .size(thumbSize)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onOpen),
            contentScale = ContentScale.Crop,
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete_event_photo),
            )
        }
    }
}

private val capturePictureContract = object : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent {
        val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        val intent = super.createIntent(context, input).apply {
            addFlags(flags)
            clipData = ClipData.newRawUri("", input)
        }
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach { info ->
                context.grantUriPermission(info.activityInfo.packageName, input, flags)
            }
        return intent
    }
}

private fun canCapturePhoto(context: Context): Boolean =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null

private fun captureOutputUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
