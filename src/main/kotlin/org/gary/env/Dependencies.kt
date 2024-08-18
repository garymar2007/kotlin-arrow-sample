package org.gary.env

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import com.zaxxer.hikari.HikariDataSource
import org.gary.persistence.UserPersistence
import org.gary.persistence.userPersistence
import org.gary.service.JwtService
import org.gary.sqldelight.SqlDelight

class Dependencies(val userPersistence: UserPersistence, val jwtService: JwtService)

fun dependencies(env: Env): Resource<Dependencies> = resource {
  val hikari: HikariDataSource = hikari(env.dataSource).bind()
  val sqlDelight: SqlDelight = sqlDelight(hikari).bind()
  val userPersistence = userPersistence(sqlDelight.usersQueries)
  Dependencies(userPersistence, JwtService(env.auth))
}
