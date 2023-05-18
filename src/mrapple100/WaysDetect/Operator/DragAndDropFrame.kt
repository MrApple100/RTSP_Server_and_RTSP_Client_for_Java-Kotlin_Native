package mrapple100.WaysDetect.Operator

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.*


class DragAndDropFrame(
        messagePlace:JLabel
) : JPanel() {
    lateinit var drawablePlace: DrawingBoard
    private var myDropTarget:DropTarget? = null

        protected fun gDT():DropTarget {
            if (myDropTarget == null) {
                myDropTarget = DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, null)
            }
            return myDropTarget!!
        }

        private var dropTargetHandler: DropTargetHandler? = null

        protected fun getDropTargetHandler():DropTargetHandler {
            if (dropTargetHandler == null) {
                dropTargetHandler = DropTargetHandler();
            }
            return dropTargetHandler!!
        }

        private var dragPoint: Point? = null
        private var dragAccepted = false
        private var dragOver = false
        private val message: JLabel

        init {
            layout = GridBagLayout()
            message = messagePlace
            add(message)
        }

        override fun getPreferredSize(): Dimension {
            return Dimension(400, 400)
        }



        override fun addNotify() {
            super.addNotify()
            try {
                gDT().addDropTargetListener(getDropTargetHandler())
            } catch (ex: TooManyListenersException) {
                ex.printStackTrace()
            }
        }

        override fun removeNotify() {
            super.removeNotify()
            gDT().removeDropTargetListener(getDropTargetHandler())
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (dragOver) {
                val g2d = g.create() as Graphics2D
                if (dragAccepted) {
                    g2d.color = Color(0, 255, 0, 64)
                } else {
                    g2d.color = Color(255, 0, 0, 64)
                }
                g2d.fill(Rectangle(width, height))
                if (dragPoint != null) {
                    val x = dragPoint!!.x - 25
                    val y = dragPoint!!.y - 25
                    g2d.color = Color.BLACK
                    g2d.drawRect(x, y, 50, 50)
                }
                g2d.dispose()
            }
        }

        protected fun importedImage(image: Image?) {//neef change
            val run = Runnable {
                if (image == null) {
                    message.icon = null
                } else {
                    message.icon = ImageIcon(image.getScaledInstance(400, 400, Image.SCALE_DEFAULT))
                    val g2d =  drawablePlace.getImage().getGraphics() as Graphics2D

                    g2d.drawImage(image.getScaledInstance(400, 400, Image.SCALE_DEFAULT), 400, 0, null)
                    g2d.dispose()
                }
            }
            SwingUtilities.invokeLater(run)
        }

        protected fun importFailed(message: String?) {
            SwingUtilities.invokeLater { JOptionPane.showMessageDialog(this@DragAndDropFrame, message, "Error", JOptionPane.ERROR_MESSAGE) }
        }

        protected inner class DropTargetHandler : DropTargetListener {
            private val validExtenions = arrayOf(
                    ".png", ".jpeg", ".jpg", "bmp", "gif"
            )

            protected fun canAcceptFile(file: File): Boolean {
                val name = file.name.toLowerCase()
                for (ext in validExtenions) {
                    if (name.endsWith(ext)) {
                        return true
                    }
                }
                return false
            }

            @Throws(UnsupportedFlavorException::class, IOException::class)
            protected fun acceptableFileFrom(transferable: Transferable): File? {
                val transferData = transferable.getTransferData(DataFlavor.javaFileListFlavor) ?: return null
                val fileList = transferData as List<*>
                if (fileList.size > 1) {
                    return null
                }
                val file = fileList[0] as File

                return if (canAcceptFile(file)) {

                    file
                } else null
            }

            protected fun canAcceptDrag(transferable: Transferable?): Boolean {
                if (transferable == null) {
                    return true
                }
                try {
                    val file = acceptableFileFrom(transferable)
                if(file!=null)
                    return true
                } catch (ex: UnsupportedFlavorException) {
                    Logger.getLogger(DragAndDropFrame::class.java.name).log(Level.SEVERE, null, ex)
                    return false
                } catch (ex: IOException) {
                    Logger.getLogger(DragAndDropFrame::class.java.name).log(Level.SEVERE, null, ex)
                    return false
                }
                return false
            }

            protected fun acceptsDrag(dtde: DropTargetDragEvent): Boolean {
                return if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val transferable = dtde.transferable
                    if (canAcceptDrag(transferable)) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY)
                        true
                    } else {
                        dtde.rejectDrag()
                        false
                    }
                } else {
                    dtde.rejectDrag()
                    false
                }
            }

            override fun dragEnter(dtde: DropTargetDragEvent) {
                val accepted = acceptsDrag(dtde)
                SwingUtilities.invokeLater(DragUpdate(true, dtde.location, accepted))
                repaint()
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                val accepted = acceptsDrag(dtde)
                SwingUtilities.invokeLater(DragUpdate(true, dtde.location, accepted))
                repaint()
            }

            override fun dropActionChanged(dtde: DropTargetDragEvent) {}
            override fun dragExit(dte: DropTargetEvent) {
                SwingUtilities.invokeLater(DragUpdate(false, null, false))
                repaint()
            }

            override fun drop(dtde: DropTargetDropEvent) {
                SwingUtilities.invokeLater(DragUpdate(false, null, false))
                importedImage(null)
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(dtde.dropAction)
                    try {
                        val transferable = dtde.transferable
                        val file = acceptableFileFrom(transferable)

                        if (file == null) {
                            println("FILE "+file!!.name)
                            importFailed("Not a supported image file")
                            dtde.dropComplete(false)
                        } else {
                            val image = ImageIO.read(file)
                            dtde.dropComplete(true)
                            importedImage(image)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        dtde.dropComplete(false)
                        importFailed("Not a supported image format")
                    }
                } else {
                    dtde.rejectDrop()
                }
            }
        }

        inner class DragUpdate(private val dragOver: Boolean, private val dragPoint: Point?, private val accepted: Boolean) : Runnable {
            override fun run() {
                this@DragAndDropFrame.dragOver = dragOver
                this@DragAndDropFrame.dragPoint = dragPoint
                dragAccepted = accepted
                this@DragAndDropFrame.repaint()
            }
        }
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DragAndDropFrame(JLabel())
        }
    }
    }


