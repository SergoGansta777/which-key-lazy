package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.WhichKeyPopup
import com.intellij.openapi.editor.Editor
import javax.swing.Timer

object WhichKeyPopupManager {

    private var popup: WhichKeyPopup? = null
    private var pendingPopup: Timer? = null

    val isActive: Boolean get() = popup?.isShowing == true || pendingPopup != null

    fun hidePopup() {
        pendingPopup?.stop()
        pendingPopup = null
        popup?.close()
        popup = null
    }

    fun showPopup(editor: Editor, path: List<String>, entries: Map<String, KeyNode>) {
        if (entries.isEmpty()) return

        pendingPopup?.stop()
        pendingPopup = null

        popup?.let { current ->
            if (current.isShowing && current.isFor(editor)) {
                current.updateLevel(path, entries)
                return
            }
            current.close()
            popup = null
        }

        val delay = WhichKeyConfigService.getInstance().settings.delay.coerceAtLeast(0)
        pendingPopup = Timer(delay) {
            pendingPopup = null
            if (!editor.isDisposed) {
                val newPopup = WhichKeyPopup(editor)
                popup = newPopup
                newPopup.show(path, entries)
            }
        }.apply {
            isRepeats = false
            start()
        }
    }
}
