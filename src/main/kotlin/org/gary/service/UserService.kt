package org.gary.service

import arrow.core.Either
import arrow.core.continuations.EffectScope
import arrow.core.continuations.either
import org.gary.DomainError
import org.gary.persistence.UserPersistence
import org.gary.validate

data class RegisterUser(val username: String, val email: String, val password: String)

typealias DomainErrors = EffectScope<DomainError>

object UserService {

  context(UserPersistence, JwtService, DomainErrors)
  suspend fun register(input: RegisterUser): JwtToken {
    val (username, email, password) = input.validate().bind()
    val userId = insert(username, email, password).bind()
    return generateJwtToken(userId).bind()
  }
}
