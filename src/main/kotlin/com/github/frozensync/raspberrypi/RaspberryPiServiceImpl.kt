package com.github.frozensync.raspberrypi

import com.github.frozensync.Configuration
import com.github.frozensync.persistence.firestore.retry
import com.google.api.gax.rpc.AlreadyExistsException
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.concurrent.ExecutionException

private const val RASPBERRY_PI_COLLECTION = "raspberryPis"

class RaspberryPiServiceImpl(private val configuration: Configuration, private val db: Firestore) : RaspberryPiService {

    private val logger = KotlinLogging.logger { }

    override suspend fun register() {
        logger.entry()

        val deviceId = configuration.deviceId
        val raspberryPi = RaspberryPi(deviceId)
        val raspberryPiRef = db.collection(RASPBERRY_PI_COLLECTION).document(raspberryPi.id.toString())
        val data = mapOf("owned" to raspberryPi.owned)

        logger.info { "Registering device..." }
        retry {
            withContext(Dispatchers.IO) {
                try {
                    raspberryPiRef.create(data).get()
                } catch (e: ExecutionException) {
                    val cause = e.cause
                    if (cause is AlreadyExistsException) null else throw cause!!
                }
            }
        }.also { result ->
            val message = if (result == null) "Device is already registered." else "Device registration completed."
            logger.info { message }
        }

        logger.exit()
    }
}
