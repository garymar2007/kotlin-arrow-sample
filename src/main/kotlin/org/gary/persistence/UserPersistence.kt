package org.gary.persistence

import arrow.core.Either
import arrow.core.continuations.either
import at.favre.lib.crypto.bcrypt.BCrypt
import org.gary.Unexpected
import org.gary.UsernameAlreadyExists
import org.gary.UserError
import org.gary.sqldelight.UsersQueries
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

@JvmInline
value class UserId(val serial: Long)

interface UserPersistence {
  /** Creates a new user in the database, and returns the [UserId] of the newly created user */
  suspend fun insert(username: String, email: String, password: String): Either<UserError, UserId>
}

/** UserPersistence implementation based on SqlDelight and JavaX Crypto */
fun userPersistence(usersQueries: UsersQueries, rounds: Int = 10) = object : UserPersistence {
  override suspend fun insert(
    username: String,
    email: String,
    password: String
  ): Either<UserError, UserId> =
    Either.catch {
      val hash = BCrypt.withDefaults().hash(rounds, password.toByteArray())
      usersQueries.insertAndGetId(
        username = username,
        email = email,
        hashed_password = hash,
      ).executeAsOne()
    }.mapLeft { error ->
      if (error is PSQLException && error.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
        UsernameAlreadyExists(username)
      } else {
        Unexpected("Failed to persist user: $username:$email", error)
      }
    }
}
