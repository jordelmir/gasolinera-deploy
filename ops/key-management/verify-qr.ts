import * as crypto from 'crypto';
import * as fs from 'fs';

interface QrPayload {
  s: string;  // Station ID
  d: string;  // Dispenser ID
  n: string;  // Nonce
  t: number;  // Timestamp
  exp: number; // Expiration
}

function verify(signedToken: string, publicKey: string): { valid: boolean; payload?: QrPayload; error?: string } {
  try {
    const parts = signedToken.split('.');
    if (parts.length !== 2) {
      return { valid: false, error: 'Invalid token format' };
    }

    const [payloadBase64, signatureBase64] = parts;

    // Verify signature
    const verifier = crypto.createVerify('sha256');
    verifier.update(payloadBase64);
    verifier.end();

    const isValid = verifier.verify(publicKey, signatureBase64, 'base64url');

    if (!isValid) {
      return { valid: false, error: 'Invalid signature' };
    }

    // Decode payload
    const payloadJson = Buffer.from(payloadBase64, 'base64url').toString();
    const payload: QrPayload = JSON.parse(payloadJson);

    // Check expiration
    const now = Math.floor(Date.now() / 1000);
    if (payload.exp < now) {
      return { valid: false, error: 'Token expired', payload };
    }

    return { valid: true, payload };

  } catch (error) {
    return { valid: false, error: `Verification failed: ${error.message}` };
  }
}

// Example usage
if (require.main === module) {
  try {
    // Check if we have keys
    if (!fs.existsSync('private-key.pem') || !fs.existsSync('public-key.pem')) {
      console.log('🔑 Keys not found. Generating keys first...');
      console.log('Run: npm run generate-keys');
      process.exit(1);
    }

    const publicKey = fs.readFileSync('public-key.pem', 'utf-8');

    // Example signed token (you would get this from sign-qr.ts or the API)
    const exampleToken = process.argv[2];

    if (!exampleToken) {
      console.log('📝 Usage: npm run verify-qr <signed-token>');
      console.log('📝 Or: ts-node verify-qr.ts <signed-token>');
      console.log('');
      console.log('💡 To get a signed token, run: npm run sign-qr');
      process.exit(1);
    }

    console.log('🔍 Verifying QR Token...');
    console.log('Token:', exampleToken);
    console.log('');

    const result = verify(exampleToken, publicKey);

    if (result.valid) {
      console.log('✅ Token is VALID!');
      console.log('');
      console.log('📋 Payload Details:');
      console.log('   Station ID:', result.payload!.s);
      console.log('   Dispenser ID:', result.payload!.d);
      console.log('   Nonce:', result.payload!.n);
      console.log('   Timestamp:', new Date(result.payload!.t * 1000).toISOString());
      console.log('   Expires:', new Date(result.payload!.exp * 1000).toISOString());

      const timeLeft = result.payload!.exp - Math.floor(Date.now() / 1000);
      console.log('   Time Left:', Math.max(0, Math.floor(timeLeft / 60)), 'minutes');

    } else {
      console.log('❌ Token is INVALID!');
      console.log('Error:', result.error);

      if (result.payload) {
        console.log('');
        console.log('📋 Payload (for debugging):');
        console.log('   Station ID:', result.payload.s);
        console.log('   Dispenser ID:', result.payload.d);
        console.log('   Expires:', new Date(result.payload.exp * 1000).toISOString());
      }
    }

  } catch (error) {
    console.error('💥 Error:', error.message);
    process.exit(1);
  }
}

export { verify, QrPayload };