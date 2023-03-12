package mrapple100.Server.encoder


import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class FrameQueueSend(frameQueueSize: Int) {


    private val queue: BlockingQueue<Frame> = ArrayBlockingQueue(frameQueueSize)

    @Throws(InterruptedException::class)
    fun push(frame: Frame): Boolean {
        if (queue.offer(frame, 5, TimeUnit.MILLISECONDS)) {
            return true
        }
        // Log.w(TAG, "Cannot add frame, queue is full")
        return false
    }

    @Throws(InterruptedException::class)
    fun pop(): Frame? {
        try {
            val frame: Frame? = queue.poll(1000, TimeUnit.MILLISECONDS)
            if (frame == null) {
                //  Log.w(TAG, "Cannot get frame, queue is empty")
            }
            return frame
        } catch (e: InterruptedException) {
            // Log.w(TAG, "Cannot add frame, queue is full", e)
            Thread.currentThread().interrupt()
        }
        return null
    }

    fun clear() {
        queue.clear()
    }

    companion object {
        private val TAG: String = FrameQueueSend::class.java.simpleName
    }

}
