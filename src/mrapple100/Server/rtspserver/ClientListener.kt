package mrapple100.Server.rtspserver

interface ClientListener {
  fun onDisconnected(client: ServerClient)
}