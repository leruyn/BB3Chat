import CoreImage
import UIKit
import shared

final class QrCodeGeneratorBridge {

    static let shared = QrCodeGeneratorBridge()
    private init() {}

    func registerWithKotlin() {
        QrCodeGeneratorBridgeHolder.shared.bindGenerator { content, sizePx in
            QrCodeGeneratorBridge.shared.generatePng(content: content, sizePx: Int(sizePx))
        }
    }

    private func generatePng(content: String, sizePx: Int) -> KotlinByteArray? {
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(Data(content.utf8), forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else { return nil }

        let scale = CGFloat(sizePx) / output.extent.size.width
        let scaled = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let context = CIContext()
        guard let cgImage = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        guard let png = UIImage(cgImage: cgImage).pngData() else { return nil }
        return png.toKotlinByteArray()
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
