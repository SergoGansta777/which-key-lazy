package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.MaskProvider
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Purely visual which-key popup. Does not handle key events -- IdeaVim processes
 * all keys, and WhichKeyPopupManager updates this popup externally.
 */
class WhichKeyPopup(private val editor: Editor) {

    private val breadcrumb = BreadcrumbBar()
    private val gridPanel = WhichKeyPanel(WhichKeyConfigService.getInstance().settings)

    private var popup: JBPopup? = null
    private var rootPanel: JPanel? = null

    val isShowing: Boolean get() = popup?.isVisible == true

    fun isFor(candidate: Editor): Boolean = editor === candidate

    fun show(path: List<String>, bindings: Map<String, KeyNode>) {
        updateAvailableSize()
        breadcrumb.updatePath(path)

        val panel = PopupSurface().apply {
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
            .setShowBorder(false)
            .setShowShadow(true)
            .setMaskProvider(MaskProvider(PopupSurface::shape))
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
            val popupSize = preferredPopupSize(panel)
            popup?.size = popupSize
            val location = configuredOrigin(popupSize)
            SwingUtilities.convertPointToScreen(location, editor.contentComponent)
            popup?.setLocation(location)
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

        panel.doLayout()
        val popupSize = preferredPopupSize(panel)
        val origin = configuredOrigin(popupSize)

        popup?.size = popupSize
        popup?.show(RelativePoint(contentComponent, origin))
    }

    private fun preferredPopupSize(panel: JPanel): Dimension {
        val visibleArea = editor.scrollingModel.visibleArea
        return Dimension(
            panel.preferredSize.width.coerceAtLeast(JBUI.scale(300)).coerceAtMost(visibleArea.width),
            panel.preferredSize.height.coerceAtLeast(JBUI.scale(80))
        )
    }

    private fun configuredOrigin(popupSize: Dimension) = PopupGeometry.origin(
        WhichKeyConfigService.getInstance().settings.position,
        editor.contentComponent.visibleRect,
        popupSize,
        editor.visualPositionToXY(editor.caretModel.visualPosition)
    )
}

private class PopupSurface : JPanel(BorderLayout()) {
    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val borderWidth = JBUI.CurrentTheme.Popup.borderWidth().coerceAtLeast(1f)
        val inset = borderWidth / 2
        val shape = shape(Dimension(width, height), inset)
        g2.color = WhichKeyColors.PANEL_BG
        g2.fill(shape)
        g2.color = WhichKeyColors.BORDER
        g2.stroke = BasicStroke(borderWidth)
        g2.draw(shape)
        g2.dispose()
    }

    companion object {
        fun shape(size: Dimension): RoundRectangle2D = shape(size, 0f)

        private fun shape(size: Dimension, inset: Float): RoundRectangle2D {
            val arc = JBUI.scale(14).toDouble()
            return RoundRectangle2D.Double(
                inset.toDouble(),
                inset.toDouble(),
                (size.width - inset * 2).toDouble(),
                (size.height - inset * 2).toDouble(),
                arc,
                arc
            )
        }
    }
}
