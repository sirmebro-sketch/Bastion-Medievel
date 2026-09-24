package de.bastion.medieval

import android.content.Context
import android.util.Log
import androidx.core.util.AtomicFile
import de.bastion.medieval.engine.SaveGame
import de.bastion.medieval.engine.World
import java.io.File
import java.io.IOException

/** Keeps the single save slot in the app's private storage. Writes are atomic. */
class SaveStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "savegame.json"))

    fun load(world: World): SaveGame? = try {
        SaveGame.decode(file.readFully().decodeToString(), world)
    } catch (_: IOException) {
        null
    }

    fun write(save: SaveGame) {
        val stream = try {
            file.startWrite()
        } catch (e: IOException) {
            Log.w(TAG, "Cannot open save file", e)
            return
        }
        try {
            stream.write(SaveGame.encode(save).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: IOException) {
            Log.w(TAG, "Cannot write save file", e)
            file.failWrite(stream)
        }
    }

    private companion object {
        const val TAG = "SaveStore"
    }
}
