package org.gary.routes

import arrow.core.Either
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import org.gary.DomainError
import org.gary.UserError
import org.gary.JwtError
import org.gary.ValidationError
import org.gary.UserNotFound
import org.gary.UsernameAlreadyExists
import org.gary.EmailAlreadyExists
import org.gary.JwtGeneration
import org.gary.JwtInvalid
import org.gary.IncorrectInput
import org.gary.Unexpected
import org.gary.KtorCtx
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

// Conduit OpenAPI error types
@Serializable
data class GenericErrorModel(val errors: GenericErrorModelErrors)

@Serializable
data class GenericErrorModelErrors(val body: List<String>)

fun GenericErrorModel(vararg msg: String): GenericErrorModel =
  GenericErrorModel(GenericErrorModelErrors(msg.toList()))

context(KtorCtx)
  suspend inline fun <reified A : Any> Either<DomainError, A>.respond(status: HttpStatusCode): Unit =
  when (this) {
    is Either.Left -> respond(value)
    is Either.Right -> call.respond(status, value)
  }

context(KtorCtx)
  suspend inline fun <reified A : Any> conduit(
  status: HttpStatusCode,
  crossinline block: suspend context(EffectScope<DomainError>) () -> A
): Unit = effect<DomainError, A> {
  block(this)
}.fold({ respond(it) }, { call.respond(status, it) })

suspend fun KtorCtx.respond(error: Unexpected): Unit =
  internal(
    """
        Unexpected failure occurred:
          - description: ${error.description}
          - cause: ${error.error}
        """.trimIndent()
  )

suspend fun KtorCtx.respond(error: UserError): Unit =
  when (error) {
    is UserNotFound -> unprocessable("User with ${error.property} not found")
    is EmailAlreadyExists -> unprocessable("${error.email} is already registered")
    is UsernameAlreadyExists -> unprocessable("Username ${error.username} already exists")
    is Unexpected -> respond(error)
  }

suspend fun KtorCtx.respond(error: JwtError): Unit =
  when (error) {
    is JwtGeneration -> unprocessable(error.description)
    is JwtInvalid -> unprocessable(error.description)
  }

suspend fun KtorCtx.respond(error: ValidationError): Unit =
  when (error) {
    is IncorrectInput ->
      unprocessable(error.errors.joinToString { field -> "${field.field}: ${field.errors.joinToString()}" })
  }

suspend fun KtorCtx.respond(error: DomainError): Unit =
  when (error) {
    is Unexpected -> respond(error)
    is JwtError -> respond(error)
    is UserError -> respond(error)
    is ValidationError -> respond(error)
  }

private suspend inline fun KtorCtx.unprocessable(
  error: String
): Unit = call.respond(HttpStatusCode.UnprocessableEntity, GenericErrorModel(error))

private suspend inline fun KtorCtx.internal(error: String): Unit =
  call.respond(HttpStatusCode.InternalServerError, GenericErrorModel(error))
