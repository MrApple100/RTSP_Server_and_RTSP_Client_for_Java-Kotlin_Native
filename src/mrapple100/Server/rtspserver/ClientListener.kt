package com.pedro.rtspserver

import mrapple100.Server.rtspserver.ServerClient

interface ClientListener {
  fun onDisconnected(client: ServerClient)
}