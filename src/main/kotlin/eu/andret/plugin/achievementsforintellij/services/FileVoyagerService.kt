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

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun addExtension(extension: String): Int {
        myState.openedExtensions.add(extension)
        return myState.openedExtensions.size
    }

    fun getUniqueExtensionCount(): Int = myState.openedExtensions.size

    companion object {
        fun getInstance(): FileVoyagerService =
            ApplicationManager.getApplication().getService(FileVoyagerService::class.java)
    }
}
