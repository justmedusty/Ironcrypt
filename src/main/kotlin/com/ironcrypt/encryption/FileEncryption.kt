package com.ironcrypt.encryption

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.SymmetricKeyAlgorithm
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.EncryptionStream
import org.pgpainless.encryption_signing.ProducerOptions
import java.io.InputStream
import java.io.OutputStream

fun encryptFileStream(publicKey: String, inputStream: InputStream, outputStream: OutputStream) {
    // Parse the publicKey String to a PGPPublicKeyRing
    val publicKeyObj: PGPPublicKeyRing = PGPainless.readKeyRing().publicKeyRing(publicKey)!!

    // Create an EncryptionStream
    val encryptionStream: EncryptionStream = PGPainless.encryptAndOrSign().onOutputStream(outputStream).withOptions(
            ProducerOptions.encrypt(
                EncryptionOptions().addRecipient(publicKeyObj)
                    .overrideEncryptionAlgorithm(SymmetricKeyAlgorithm.AES_192)
            ).setAsciiArmor(true)
        ) // Ascii armor or not

    // Pipe the input stream to the encryption stream
    inputStream.use { input ->
        encryptionStream.use { encryption ->
            input.copyTo(encryption)
        }
    }
}