import PhotosUI
import UIKit
import shared

final class ImagePickerBridge: NSObject, PHPickerViewControllerDelegate {

    static let shared = ImagePickerBridge()
    private override init() { super.init() }

    func registerWithKotlin() {
        ImagePickerBridgeHolder.shared.bindPresenter {
            ImagePickerBridge.shared.presentPicker()
        }
    }

    private func presentPicker() {
        guard let presenter = topViewController() else { return }
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.filter = .images
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let provider = results.first?.itemProvider,
              provider.canLoadObject(ofClass: UIImage.self) else { return }

        provider.loadObject(ofClass: UIImage.self) { [weak self] object, _ in
            guard let image = object as? UIImage else { return }
            let data = image.jpegData(compressionQuality: 0.92)
                ?? image.pngData()
            guard let data else { return }
            DispatchQueue.main.async {
                self?.deliverToKotlin(data)
            }
        }
    }

    private func deliverToKotlin(_ data: Data) {
        ImagePickerBridgeHolder.shared.deliverPicked(bytes: data.toKotlinByteArray())
    }

    private func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let window = scenes
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
        guard var top = window?.rootViewController else { return nil }
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
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
