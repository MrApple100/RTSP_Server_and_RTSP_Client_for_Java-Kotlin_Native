package mrapple100.WaysDetect.Operator

import java.awt.Dimension
import java.awt.Frame
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel


class DADFandDB :JPanel() {
    lateinit var drawingBoard:DrawingBoard;
    lateinit var dragAndDropFrame:DragAndDropFrame;
    var instance = this
    init{
        val desktopPane = JDesktopPane()

        val jLabel = JLabel()
        drawingBoard = DrawingBoard(800,500, jLabel)
        drawingBoard.setBounds(0,0,800,800)
        drawingBoard.preferredSize = Dimension(800,500)
        dragAndDropFrame = DragAndDropFrame(jLabel)
        dragAndDropFrame.setBounds(0,0,400,400)


        desktopPane.add(dragAndDropFrame)
        desktopPane.add(drawingBoard)
        desktopPane.preferredSize = Dimension(400, 400)
        val frame:JFrame = JFrame()
        frame.add(desktopPane)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        this.add(frame)

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame("Drawing Panel")
            val panel = DADFandDB()
            frame.contentPane.add(panel)
            frame.pack()
            frame.isVisible = true
        }
    }
}