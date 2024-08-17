package org.gary.env

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import org.gary.persistence.userPersistence
import org.gary.service.UserService
import org.gary.service.JwtService
import org.gary.service.userService

class Dependencies(val userService: UserService)

fun dependencies(env: Env): Resource<Dependencies> = resource {
  val hikari = hikari(env.dataSource).bind()
  val sqlDelight = sqlDelight(hikari).bind()
  val userPersistence = userPersistence(sqlDelight.usersQueries)
  Dependencies(userService(userPersistence, JwtService(env.auth)))
}
