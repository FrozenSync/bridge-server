package com.github.frozensync

import com.github.frozensync.persistence.firestore.firestoreModule
import com.github.frozensync.raspberrypi.RaspberryPiService
import com.github.frozensync.raspberrypi.raspberryPiModule
import com.github.frozensync.tournament.tournamentModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.Koin
import org.koin.core.context.startKoin
import java.util.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun main(): Unit = runBlocking {
    val koinApplication = startKoin {
        modules(firestoreModule, raspberryPiModule, tournamentModule)
        environmentProperties()
    }
    val koin = koinApplication.koin.assertProperties()

    val id = UUID.fromString(koin.getProperty<String>(RASPBERRY_PI_ID))
    logger.info { "Started with id $id." }

    val raspberryPiService = koin.get<RaspberryPiService>()
    raspberryPiService.register(id)

    embeddedServer(Netty, 8080, module = Application::module).start()
    return@runBlocking
}

/**
 * Returns a valid [Koin] instance after asserting all required properties. Terminates the program if any required property is missing.
 */
private fun Koin.assertProperties(): Koin {
    val errorMessage = StringBuilder()
    var isIllegalState = false

    if (getProperty<String>(RASPBERRY_PI_ID) == null) {
        errorMessage.appendln("Missing environment variable: RASPBERRY_PI_ID")
        isIllegalState = true
    }
    if (getProperty<String>(GOOGLE_APPLICATION_CREDENTIALS) == null) {
        errorMessage.appendln("Missing environment variable: GOOGLE_APPLICATION_CREDENTIALS")
        isIllegalState = true
    }

    if (isIllegalState) {
        System.err.println(errorMessage)
        exitProcess(1)
    } else {
        return this
    }
}
