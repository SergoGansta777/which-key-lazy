package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Purely visual which-key popup. Does not handle key events -- IdeaVim processes
 * all keys, and WhichKeyPopupManager updates this popup externally.
 */
class WhichKeyPopup(private val editor: Editor) {

    private val breadcrumb = BreadcrumbBar()
    private val gridPanel = WhichKeyPanel(WhichKeyConfigService.getInstance().settings)

    private var popup: JBPopup? = null
    private var rootPanel: JPanel? = null

    fun show(bindings: Map<String, KeyNode>) {
        updateAvailableSize()

        val panel = JPanel(BorderLayout()).apply {
            background = WhichKeyColors.PANEL_BG
            border = BorderFactory.createLineBorder(WhichKeyColors.BORDER, 1)
            add(breadcrumb, BorderLayout.NORTH)
            add(gridPanel, BorderLayout.CENTER)
        }
        rootPanel = panel

        gridPanel.updateEntries(bindings)

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setRequestFocus(false)
            .setCancelKeyEnabled(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setFocusable(false)
            .setMovable(false)
            .setResizable(false)
            .createPopup()

        showAtConfiguredPosition()
    }

    fun updateLevel(path: List<String>, bindings: Map<String, KeyNode>) {
        updateAvailableSize()
        breadcrumb.updatePath(path)
        gridPanel.updateEntries(bindings)

        rootPanel?.let { panel ->
            panel.revalidate()
            panel.repaint()
            val visibleArea = editor.scrollingModel.visibleArea
            val prefSize = panel.preferredSize
            popup?.size = Dimension(
                prefSize.width.coerceAtLeast(300).coerceAtMost(visibleArea.width),
                prefSize.height.coerceAtLeast(80)
            )
        }
    }

    fun close() {
        popup?.cancel()
        popup = null
    }

    private fun updateAvailableSize() {
        val visibleArea = editor.scrollingModel.visibleArea
        gridPanel.setAvailableSize(visibleArea.width, (visibleArea.height * 0.6).toInt())
    }

    private fun showAtConfiguredPosition() {
        val panel = rootPanel ?: return
        val contentComponent = editor.contentComponent
        val visibleRect = contentComponent.visibleRect

        panel.doLayout()
        val prefSize = panel.preferredSize
        val popupWidth = prefSize.width.coerceAtLeast(300).coerceAtMost(visibleRect.width)
        val popupHeight = prefSize.height.coerceAtLeast(80)

        val popupSize = Dimension(popupWidth, popupHeight)
        val origin = PopupGeometry.origin(
            WhichKeyConfigService.getInstance().settings.position,
            visibleRect,
            popupSize,
            editor.visualPositionToXY(editor.caretModel.visualPosition)
        )

        popup?.size = popupSize
        popup?.show(RelativePoint(contentComponent, origin))
    }
}
