
package mrapple100.Server.rtsp.rtsp.commands

/**
 * Created by pedro on 7/04/21.
 */
data class Command(val method: Method, val cSeq: Int, val status: Int, val text: String)
