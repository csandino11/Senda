package com.senda.lecturabiblica.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.senda.lecturabiblica.model.ReadingPlan
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

const val SENDA_BACKUP_MIME = "application/vnd.senda.plan"
const val SENDA_BACKUP_EXTENSION = ".senda"

data class PlanBackupData(
    val plan: ReadingPlan,
    val completed: Set<String>,
    val bibleVersion: String,
)

data class SavedPlanBackup(val uri: Uri, val fileName: String)

object PlanBackupCodec {
    private val magic = byteArrayOf(0x53, 0x45, 0x4E, 0x44, 0x41, 0x01) // SENDA + format 1
    private const val maxCompressedBytes = 1_000_000
    private const val maxJsonBytes = 3_000_000

    fun encode(data: PlanBackupData): ByteArray {
        val json = JSONObject().apply {
            put("format", 1)
            put("plan", PlanJson.encode(data.plan))
            put("completed", JSONArray(data.completed.sorted()))
            put("bibleVersion", data.bibleVersion)
        }.toString().toByteArray(Charsets.UTF_8)

        return ByteArrayOutputStream().use { output ->
            output.write(magic)
            GZIPOutputStream(output).use { it.write(json) }
            output.toByteArray()
        }
    }

    fun decode(input: InputStream): PlanBackupData {
        val encoded = input.readLimited(maxCompressedBytes)
        require(encoded.size > magic.size && encoded.take(magic.size).toByteArray().contentEquals(magic)) {
            "El archivo no es un respaldo válido de Senda."
        }
        val jsonBytes = GZIPInputStream(ByteArrayInputStream(encoded, magic.size, encoded.size - magic.size)).use {
            it.readLimited(maxJsonBytes)
        }
        val json = JSONObject(jsonBytes.toString(Charsets.UTF_8))
        require(json.getInt("format") == 1) { "Esta versión del respaldo todavía no es compatible." }
        val completedJson = json.getJSONArray("completed")
        return PlanBackupData(
            plan = PlanJson.decode(json.getJSONObject("plan")),
            completed = (0 until completedJson.length()).map(completedJson::getString).toSet(),
            bibleVersion = json.optString("bibleVersion", "RVC"),
        )
    }

    private fun InputStream.readLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "El archivo de respaldo es demasiado grande." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}

object PlanBackupFiles {
    private val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun saveToDownloads(context: Context, data: ByteArray, year: Int): SavedPlanBackup {
        val fileName = "Senda-plan-$year-${LocalDateTime.now().format(timestamp)}$SENDA_BACKUP_EXTENSION"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, fileName, data)
        } else {
            saveLegacy(context, fileName, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(context: Context, fileName: String, data: ByteArray): SavedPlanBackup {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, SENDA_BACKUP_MIME)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Android no permitió crear el respaldo en Descargas."
        }
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")) {
                "No se pudo abrir el archivo de respaldo."
            }.use { it.write(data) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return SavedPlanBackup(uri, fileName)
        } catch (cause: Throwable) {
            resolver.delete(uri, null, null)
            throw cause
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, fileName: String, data: ByteArray): SavedPlanBackup {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        check(downloads.exists() || downloads.mkdirs()) { "No se pudo acceder a la carpeta Descargas." }
        val file = uniqueFile(downloads, fileName)
        FileOutputStream(file).use { it.write(data) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return SavedPlanBackup(uri, file.name)
    }

    private fun uniqueFile(directory: File, desiredName: String): File {
        val desired = File(directory, desiredName)
        if (!desired.exists()) return desired
        val base = desiredName.removeSuffix(SENDA_BACKUP_EXTENSION)
        return generateSequence(2) { it + 1 }
            .map { File(directory, "$base-$it$SENDA_BACKUP_EXTENSION") }
            .first { !it.exists() }
    }
}
