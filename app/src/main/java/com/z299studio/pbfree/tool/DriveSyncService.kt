package com.z299studio.pbfree.tool

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream


enum class SyncStatus {
    Ready,
    Preparing,
    Loading,
    Sending,
    Resolving,
    Failed,
    Done,
    Canceled
}

class DriveSyncService private constructor() {

    interface SyncListener {
        fun onConnected() { }
        fun onReceived(data: ByteArray)
        fun onDataSent() { }
        fun onError(error: Throwable) {}
        fun onCancel()
    }
    private lateinit var listener: SyncListener
    private lateinit var appName: String
    private var googleDrive: Drive? = null

    fun handleActivityResult(activity: Activity, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { initializeDrive(activity, appName, it) }
                .addOnFailureListener { error ->
                    Log.e("DriveSyncService", "Unable to sign in.", error)
                    listener.onError(error)
                }
        } else {
            listener.onCancel()
        }
    }

    fun create(activity: Activity, appName: String, listener: SyncListener): Intent? {
        this.appName = appName
        this.listener = listener
        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(activity.applicationContext),
                Scope(Scopes.DRIVE_APPFOLDER), Scope(Scopes.EMAIL)
            )) {
            return GoogleSignIn.getClient(activity, GoogleSignInOptions.Builder().requestEmail()
                .requestScopes(Scope(Scopes.DRIVE_APPFOLDER)).build()).signInIntent
        }
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity)
        initializeDrive(activity, appName, googleSignInAccount)
        return null
    }

    fun send(path: java.io.File) {
        try {
            // Upload the new content
            val metadata = File()
            metadata.setName(DRIVE_DATA_FILE).parents = listOf(APP_FOLDER)
            val fileContent = FileContent("application/json", path)
            val fileId = googleDrive?.files()
                ?.create(metadata, fileContent)
                ?.setFields("id")
                ?.execute()?.id
            listener.onDataSent()
            Log.d("DriveSyncService", "Successfully updated to Drive app folder, file id: $fileId")

            // Now delete other files of the same name under the folder
            googleDrive?.files()?.list()
                ?.setSpaces(APP_FOLDER)
                ?.setFields("files(id, name)")
                ?.setOrderBy("modifiedTime desc")
                ?.execute()
                ?.files
                ?.filter { it.name == DRIVE_DATA_FILE && it.id != fileId }
                ?.forEach {
                    googleDrive?.files()?.delete(it.id)?.execute()
                }
        } catch (error: Exception) {
            Log.e("DriveSyncService", "Error in sending data", error)
            listener.onError(error)
        }
    }

    fun read() {
        try {
            val fileId = googleDrive?.files()?.list()
                ?.setSpaces(APP_FOLDER)
                ?.setFields("files(id, name)")
                ?.execute()
                ?.files?.find { it.name == DRIVE_DATA_FILE || it.name == DEPRECATED_GAMES_FILE }?.id
            if (fileId != null) {
                val outputStream = ByteArrayOutputStream()
                googleDrive?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
                listener.onReceived(outputStream.toByteArray())
                outputStream.close()
            } else {
                listener.onReceived(ByteArray(0))
            }
        } catch (error: Exception) {
            Log.e("DriveSyncService", "Error in retrieving data", error)
            listener.onError(error)
        }
    }

    private fun initializeDrive(activity: Activity, appName: String, googleSignInAccount: GoogleSignInAccount?) {
        val credential = GoogleAccountCredential.usingOAuth2(
            activity.applicationContext, setOf(Scopes.DRIVE_APPFOLDER, Scopes.EMAIL))
        credential.selectedAccount = googleSignInAccount?.account
        googleDrive = Drive.Builder(NetHttpTransport() , GsonFactory(), credential)
            .setApplicationName(appName)
            .build()
        listener.onConnected()
    }

    companion object {
        const val DRIVE_DATA_FILE = "pb-drive-data"
        const val DEPRECATED_GAMES_FILE = "Passbook-Saved-Data"
        const val APP_FOLDER = "appDataFolder"
        fun get(): DriveSyncService = syncService
        private val syncService = DriveSyncService()
    }
}