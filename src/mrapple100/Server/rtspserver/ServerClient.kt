package mrapple100.Server.rtspserver

import mrapple100.Server.rtsp.rtsp.Protocol
import mrapple100.Server.rtsp.rtsp.RtspSender
import mrapple100.Server.rtsp.rtsp.commands.Method
import mrapple100.Server.rtsp.utils.ConnectCheckerRtsp
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer

open class ServerClient(private val socket: Socket, serverIp: String, serverPort: Int,
                        private val connectCheckerRtsp: ConnectCheckerRtsp, clientAddress: String, sps: ByteBuffer?,
                        pps: ByteBuffer?, vps: ByteBuffer?, sampleRate: Int, isStereo: Boolean,
                        videoDisabled: Boolean, audioDisabled: Boolean, user: String?, password: String?,
                        private val listener: ClientListener) : Thread() {

  private val TAG = "Client"
  private val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
  private val input = BufferedReader(InputStreamReader(socket.getInputStream()))
  val rtspSender = RtspSender(connectCheckerRtsp)
  val commandsManager = ServerCommandManager(serverIp, serverPort, clientAddress)
  var canSend = false

  init {
    commandsManager.videoDisabled = videoDisabled
    commandsManager.audioDisabled = audioDisabled
    commandsManager.isStereo = isStereo
    commandsManager.sampleRate = sampleRate
    if (!commandsManager.videoDisabled) {
      commandsManager.setVideoInfo(sps!!, pps!!, vps)
    }
    commandsManager.setAuth(user, password)
  }

  override fun run() {
    super.run()
    println("New client ${commandsManager.clientIp}")

    // Log.i(TAG, "New client ${commandsManager.clientIp}")
    while (!interrupted()) {
      try {
        val request = commandsManager.getRequest(input)
        val cSeq = request.cSeq //update cSeq
        if (cSeq == -1) { //If cSeq parsed fail send error to client
          output.write(commandsManager.createError(500, cSeq))
          output.flush()
          continue
        }
        val response = commandsManager.createResponse(request.method, request.text, cSeq)
        println(response)
        output.write(response)
        output.flush()

        if (request.method == Method.PLAY) {
          println("Protocol ${commandsManager.protocol}")

          //   Log.i(TAG, "Protocol ${commandsManager.protocol}")
          rtspSender.setSocketsInfo(commandsManager.protocol, commandsManager.videoServerPorts,
              commandsManager.audioServerPorts)
          if (!commandsManager.videoDisabled) {
            rtspSender.setVideoInfo(commandsManager.sps!!, commandsManager.pps!!, commandsManager.vps)
          }

          rtspSender.setDataStream(socket.getOutputStream(), commandsManager.clientIp)
          if (commandsManager.protocol == Protocol.UDP) {
            if (!commandsManager.videoDisabled) {
              rtspSender.setVideoPorts(commandsManager.videoPorts[0], commandsManager.videoPorts[1])
            }

          }
          rtspSender.start()
          connectCheckerRtsp.onConnectionSuccessRtsp()
          canSend = true
        } else if (request.method == Method.TEARDOWN) {
          println("Client disconnected")
        //  Log.i(TAG, "Client disconnected")
          listener.onDisconnected(this)
        }
      } catch (e: SocketException) { // Client has left
        println(e)
      //  Log.e(TAG, "Client disconnected", e)
        listener.onDisconnected(this)
        break
      } catch (e: Exception) {
        println(e)

        //   Log.e(TAG, "Unexpected error", e)
      }
    }
  }

  fun stopClient() {
    canSend = false
    rtspSender.stop()
    interrupt()
    try {
      join(100)
    } catch (e: InterruptedException) {
      interrupt()
    } finally {
      socket.close()
    }
  }

  fun hasCongestion(): Boolean = rtspSender.hasCongestion()
}