package cantstopthesignal.cryptography

import cantstopthesignal.log.logger
import org.bouncycastle.openpgp.api.MessageEncryptionMechanism
import org.bouncycastle.openpgp.api.OpenPGPKey
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.SymmetricKeyAlgorithm
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


/**
 * Encrypt message
 *
 * @param publicKey
 * @param message
 * @return encrypted message
 */
fun encryptMessage(publicKey: String, message: String): ByteArray? {
    return try {
        val outputStream = ByteArrayOutputStream()

        val publicKey: OpenPGPKey = PGPainless.getInstance().readKey().parseKey(publicKey) ?: return null
        val plaintextInputStream = ByteArrayInputStream(message.toByteArray())
        val api: PGPainless = PGPainless.getInstance()

        val encryptionStream = api.generateMessage().onOutputStream(outputStream).withOptions(
            ProducerOptions.encrypt(
                EncryptionOptions.get(api).addRecipient(publicKey).overrideEncryptionMechanism(
                    MessageEncryptionMechanism.librePgp(SymmetricKeyAlgorithm.AES_256.ordinal)
                )
            ).setAsciiArmor(true)
        )


        Streams.pipeAll(plaintextInputStream, encryptionStream)
        encryptionStream.close()

        val encryptedMessage = Base64.getEncoder().encodeToString(outputStream.toByteArray())

        encryptedMessage.toByteArray()
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}


fun encryptMessageForMany(publicKeys: List<String>, message: String): ByteArray? {
    return try {
        val outputStream = ByteArrayOutputStream()
        val plaintextInputStream = ByteArrayInputStream(message.toByteArray())
        val api: PGPainless = PGPainless.getInstance()

        val encryptionOptions = EncryptionOptions.get(api)
        for (publicKey in publicKeys) {
            val publicKey: OpenPGPKey = PGPainless.getInstance().readKey().parseKey(publicKey) ?: continue
            encryptionOptions.addRecipient(publicKey)
        }
        encryptionOptions.overrideEncryptionMechanism(
            MessageEncryptionMechanism.librePgp(SymmetricKeyAlgorithm.AES_256.algorithmId)
        )

        val encryptionStream = api.generateMessage().onOutputStream(outputStream).withOptions(
            ProducerOptions.encrypt(encryptionOptions).setAsciiArmor(true)
        )

        Streams.pipeAll(plaintextInputStream, encryptionStream)
        encryptionStream.close()

        val encryptedMessage = Base64.getEncoder().encodeToString(outputStream.toByteArray())
        encryptedMessage.toByteArray()
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

