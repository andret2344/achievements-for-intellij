package eu.andret.plugin.achievementsforintellij.watcher

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.util.messages.MessageBusConnection

@Service(Service.Level.PROJECT)
class UserCreatedFileWatcher(project: Project) : Disposable {

    private val bus: MessageBusConnection = project.messageBus.connect(this)

    @Volatile
    private var creatingFromUi: Boolean = false

    init {
        // 1) Słuchaj akcji z UI i ustawiaj/zeruj flagę
        bus.subscribe(AnActionListener.TOPIC, object : AnActionListener {
            override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
                val id = ActionManager.getInstance().getId(action)
                creatingFromUi = id in UI_CREATE_ACTION_IDS
            }

            override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                creatingFromUi = false
            }

            override fun beforeEditorTyping(c: Char, dataContext: DataContext) {
                creatingFromUi = false
            }
        })

        bus.subscribe(BulkFileListener.TOPIC, object : BulkFileListener {
            override fun after(events: MutableList<out com.intellij.openapi.vfs.newvfs.events.VFileEvent>) {
                events.asSequence()
                    .filterIsInstance<VFileCreateEvent>()
                    .filter { !it.isFromRefresh }
                    .filter { creatingFromUi }
                    .forEach { e -> onUserCreatedFile(e) }
            }
        })
    }

    private fun onUserCreatedFile(e: VFileCreateEvent) {
        println("User-created file: ${e.path}")
    }

    override fun dispose() {}

    companion object {
        private val UI_CREATE_ACTION_IDS = setOf(
            IdeActions.ACTION_NEW_ELEMENT,
            "NewFile",
            "NewScratchFile",
            "NewFromTemplate",
            "NewKotlinFile",
            "NewJavaClass"
        )
    }
}
