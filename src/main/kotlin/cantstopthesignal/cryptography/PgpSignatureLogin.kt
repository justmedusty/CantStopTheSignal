package com.freedom.cantstopthesignal.cryptography

import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserId
import com.freedom.cantstopthesignal.applicationScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.bouncycastle.openpgp.api.OpenPGPKey
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.DecryptionStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Duration.Companion.milliseconds


/*
    This will be for PGP challenge based login or password recovery. I will make PGP login a configurable option
 */

data class PgpSignatureLoginChallenge(
    val userPublicKey: OpenPGPKey,
    val challengeString: String,
)

val pgpChallengeHashSet: MutableMap<String, PgpSignatureLoginChallenge> = HashMap()

fun verifySignature(publicKey: String, message: String, expectedMessage: String): Boolean {
    val api = PGPainless.getInstance()
    val inputStream: InputStream = ByteArrayInputStream(message.toByteArray())
    val outputStream = ByteArrayOutputStream()
    val publicKey = PGPainless.getInstance().readKey().parseKey(publicKey)
    val decryptionStream: DecryptionStream =
        PGPainless.getInstance().processMessage().onInputStream(inputStream).withOptions(
            ConsumerOptions.get(api)
                .addVerificationCert(publicKey)
        )

    Streams.pipeAll(decryptionStream, outputStream)
    decryptionStream.close()

    val metadata = decryptionStream.metadata

    if (!metadata.isVerifiedSigned()) {
        return false
    }

    if (metadata.verifiedSignatures.none { it.signingKey == publicKey.fingerprint }) return false
    val recovered = outputStream.toString(Charsets.UTF_8.name())
    return recovered == expectedMessage
}

/*
    Register a challenge, generates a random UUID
 */
fun registerNewChallenge(user: String): String? /* Successful creation or not */ {
    val challengeString = (UUID.randomUUID().toString() + UUID.randomUUID().toString() + java.time.LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
    val userId = getUserId(user) ?: return null
    val publicKey = getPublicKey(userId) ?: return null

    val signatureLoginChallenge = PgpSignatureLoginChallenge(
        PGPainless.getInstance().readKey().parseKey(publicKey),
        challengeString,
    )

    pgpChallengeHashSet[user] = signatureLoginChallenge

    // Schedule expiry
    applicationScope.launch {
        delay(java.time.Duration.ofMinutes(5).toMillis().milliseconds)
        // Only remove if it's still the same challenge (not replaced/consumed)
        pgpChallengeHashSet.remove(user, signatureLoginChallenge)
    }

    return challengeString
}