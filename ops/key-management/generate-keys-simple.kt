import java.io.File
import java.security.KeyPairGenerator
import java.util.Base64

fun main() {
    println("Generando claves RSA para firma de QR...")

    // Generar par de claves RSA
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()

    // Codificar claves en Base64
    val privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
    val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

    // Crear archivos PEM simplificados
    val privateKeyPem = """
        -----BEGIN PRIVATE KEY-----
        $privateKeyBase64
        -----END PRIVATE KEY-----
    """.trimIndent()

    val publicKeyPem = """
        -----BEGIN PUBLIC KEY-----
        $publicKeyBase64
        -----END PUBLIC KEY-----
    """.trimIndent()

    // Guardar claves
    File("private-key.pem").writeText(privateKeyPem)
    File("public-key.pem").writeText(publicKeyPem)

    println("✅ Claves generadas exitosamente:")
    println("   - private-key.pem")
    println("   - public-key.pem")

    // Generar configuración para application.yml
    val config = """
        # Configuración de claves QR para application.yml
        qr:
          signing:
            private-key: |
              $privateKeyPem
            public-key: |
              $publicKeyPem
    """.trimIndent()

    File("qr-keys-config.yml").writeText(config)
    println("   - qr-keys-config.yml (para configuración)")
}