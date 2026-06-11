import CryptoKit
import Foundation
import shared

final class CryptoBridge {

    static let shared = CryptoBridge()
    private init() {}

    func registerWithKotlin() {
        CryptoBridgeHolder.shared.register(
            encrypt: { plain, key, iv in
                guard let result = CryptoBridge.shared.aesGcmEncrypt(
                    plain: plain.toData(),
                    key: key.toData(),
                    iv: iv.toData()
                ) else {
                    return plain
                }
                return result.toKotlinByteArray()
            },
            decrypt: { cipherWithTag, key, iv in
                guard let result = CryptoBridge.shared.aesGcmDecrypt(
                    cipherWithTag: cipherWithTag.toData(),
                    key: key.toData(),
                    iv: iv.toData()
                ) else {
                    return cipherWithTag
                }
                return result.toKotlinByteArray()
            }
        )
    }

    private func aesGcmEncrypt(plain: Data, key: Data, iv: Data) -> Data? {
        guard key.count == 32, iv.count == 12 else { return nil }
        do {
            let symKey = SymmetricKey(data: key)
            let nonce = try AES.GCM.Nonce(data: iv)
            let sealed = try AES.GCM.seal(plain, using: symKey, nonce: nonce)
            guard let combined = sealed.combined else { return nil }
            // Match Android: ciphertext || 16-byte tag (no nonce prefix).
            let tagStart = combined.count - 16
            let cipherStart = tagStart - plain.count
            return combined.subdata(in: cipherStart..<combined.count)
        } catch {
            return nil
        }
    }

    private func aesGcmDecrypt(cipherWithTag: Data, key: Data, iv: Data) -> Data? {
        guard key.count == 32, iv.count == 12, cipherWithTag.count > 16 else { return nil }
        do {
            let symKey = SymmetricKey(data: key)
            let nonce = try AES.GCM.Nonce(data: iv)
            let tagLen = 16
            let cipherLen = cipherWithTag.count - tagLen
            let ciphertext = cipherWithTag.subdata(in: 0..<cipherLen)
            let tag = cipherWithTag.subdata(in: cipherLen..<cipherWithTag.count)
            let sealed = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
            return try AES.GCM.open(sealed, using: symKey)
        } catch {
            return nil
        }
    }
}

private extension KotlinByteArray {
    func toData() -> Data {
        var bytes = [UInt8](repeating: 0, count: Int(size))
        for i in 0..<Int(size) {
            bytes[i] = UInt8(bitPattern: get(index: Int32(i)))
        }
        return Data(bytes)
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(count))
        for (i, byte) in enumerated() {
            array.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return array
    }
}
