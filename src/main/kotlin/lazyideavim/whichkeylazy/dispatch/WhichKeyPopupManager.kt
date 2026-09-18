package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.WhichKeyPopup
import com.intellij.openapi.editor.Editor
import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import javax.swing.Timer

/** Coordinates delayed display and closes the current popup on the next keystroke. */
object WhichKeyPopupManager {

    private var popup: WhichKeyPopup? = null
    private var pendingPopup: Timer? = null

    val isActive: Boolean get() = popup != null || pendingPopup != null

    fun hidePopup() {
        pendingPopup?.stop()
        pendingPopup = null
        popup?.close()
        popup = null
    }

    fun showPopup(editor: Editor, path: List<String>, entries: Map<String, KeyNode>) {
        if (entries.isEmpty()) return

        pendingPopup?.stop()
        val delay = WhichKeyConfigService.getInstance().settings.delay.coerceAtLeast(0)
        pendingPopup = Timer(delay) {
            pendingPopup = null
            if (!editor.isDisposed) {
                val newPopup = WhichKeyPopup(editor)
                popup = newPopup
                newPopup.show(entries)
                if (path.isNotEmpty()) newPopup.updateLevel(path, entries)
            }
        }.apply {
            isRepeats = false
            start()
        }
    }
}
