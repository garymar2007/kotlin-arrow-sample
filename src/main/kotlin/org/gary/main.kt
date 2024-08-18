package org.gary

import org.gary.env.Env
import org.gary.env.Dependencies
import org.gary.env.dependencies
import org.gary.routes.userRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import kotlinx.coroutines.Dispatchers

fun main(): Unit = cancelOnShutdown(Dispatchers.Default) {
  val env = Env()
  // .use function to read the resource and close it after the block is executed
  dependencies(env).use { module ->
    embeddedServer(Netty, host = env.http.host, port = env.http.port) {
      app(module)
    }.start(wait = true)
  }
}

fun Application.app(module: Dependencies) {
  install(DefaultHeaders)
  install(ContentNegotiation) { json() }
  with(module.userPersistence, module.jwtService) {
    userRoutes()
  }
}
