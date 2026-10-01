package eu.andret.plugin.achievementsforintellij.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Service to track unique file extensions that have been opened.
 */
@State(
    name = "fileVoyager",
    storages = [Storage("fileVoyager.xml")]
)
@Service(Service.Level.APP)
class FileVoyagerService : PersistentStateComponent<FileVoyagerService.State> {
    data class State(
        var openedExtensions: MutableSet<String> = LinkedHashSet()
    )

    private var myState: State = State()

    // Returns a snapshot: the platform serializes it on its own thread, outside of our lock
    override fun getState(): State = synchronized(this) {
        State(LinkedHashSet(myState.openedExtensions))
    }

    override fun loadState(state: State) = synchronized(this) {
        myState = state
    }

    fun addExtension(extension: String): Int = synchronized(this) {
        myState.openedExtensions.add(extension)
        myState.openedExtensions.size
    }

    fun clear() = synchronized(this) {
        myState.openedExtensions.clear()
    }

    companion object {
        fun getInstance(): FileVoyagerService =
            ApplicationManager.getApplication().getService(FileVoyagerService::class.java)
    }
}
