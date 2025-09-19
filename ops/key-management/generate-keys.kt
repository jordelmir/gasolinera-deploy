#!/usr/bin/env kotlin

@file:DependsOn("org.bouncycastle:bcprov-jdk15on:1.70")

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.StringWriter
import java.security.KeyPairGenerator
import java.security.Security

/**
 * Key Generation Utility for Gasolinera JSM QR Code Signing
 *
 * This utility generates RSA key pairs for signing QR codes in the gas station system.
 * The keys are compatible with both the TypeScript signing utility and the Kotlin backend.
 *
 * Usage:
 *   kotlin generate-keys.kt
 *
 * This will generate:
 *   - private-key.pem (for signing QR codes)
 *   - public-key.pem (for verifying QR codes)
 */

fun main() {
    // Add BouncyCastle provider for PEM support
    Security.addProvider(BouncyCastleProvider())

    println("🔐 Generating RSA Key Pair for QR Code Signing...")

    // Generate 2048-bit RSA key pair
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()

    // Convert private key to PEM format
    val privateKeyPem = StringWriter().use { writer ->
        JcaPEMWriter(writer).use { pemWriter ->
            pemWriter.writeObject(keyPair.private)
        }
        writer.toString()
    }

    // Convert public key to PEM format
    val publicKeyPem = StringWriter().use { writer ->
        JcaPEMWriter(writer).use { pemWriter ->
            pemWriter.writeObject(keyPair.public)
        }
        writer.toString()
    }

    // Write keys to files
    File("private-key.pem").writeText(privateKeyPem)
    File("public-key.pem").writeText(publicKeyPem)

    println("✅ Keys generated successfully!")
    println("📁 Files created:")
    println("   - private-key.pem (Keep this secure! Used for signing)")
    println("   - public-key.pem (Used for verification)")
    println()
    println("🔒 Security Notes:")
    println("   - Store private-key.pem securely (use Vault in production)")
    println("   - Never commit private keys to version control")
    println("   - Rotate keys regularly in production")
    println("   - Use HSM (Hardware Security Module) for production keys")
    println()
    println("🚀 Next steps:")
    println("   1. Move private-key.pem to a secure location")
    println("   2. Update application configuration to use the keys")
    println("   3. Test QR code generation with: npm run sign-qr")

    // Display key fingerprints for verification
    val privateKeyFingerprint = keyPair.private.encoded.contentHashCode()
    val publicKeyFingerprint = keyPair.public.encoded.contentHashCode()

    println()
    println("🔍 Key Fingerprints (for verification):")
    println("   Private Key: ${privateKeyFingerprint}")
    println("   Public Key:  ${publicKeyFingerprint}")
}