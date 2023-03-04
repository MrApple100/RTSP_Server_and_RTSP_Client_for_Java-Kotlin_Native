package mrapple100.Server.rtspserver

import mrapple100.Server.rtspserver.ServerClient

interface ClientListener {
  fun onDisconnected(client: ServerClient)
}