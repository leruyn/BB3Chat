import AVFoundation
import shared

final class VoiceBridge {

    static let shared = VoiceBridge()

    private var recorder: AVAudioRecorder?
    private var player: AVAudioPlayer?
    private var outputURL: URL?
    private var startTime: TimeInterval = 0

    private init() {}

    func registerWithKotlin() {
        VoiceBridgeHolder.shared.registerRecorder(
            start: { [weak self] in KotlinBoolean(value: self?.startRecording() ?? false) },
            stop: { [weak self] in self?.stopRecording() },
            cancel: { [weak self] in self?.cancelRecording() },
            elapsed: { [weak self] in KotlinLong(value: self?.elapsedMs() ?? 0) },
            isRecording: { [weak self] in KotlinBoolean(value: self?.recorder?.isRecording ?? false) }
        )
        VoiceBridgeHolder.shared.registerPlayer(
            play: { [weak self] bytes in self?.play(bytes: bytes) },
            stop: { [weak self] in self?.stopPlayback() }
        )
    }

    private func startRecording() -> Bool {
        cancelRecording()
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("bb3_voice_\(Int(Date().timeIntervalSince1970)).m4a")
            outputURL = url
            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 44_100,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
            ]
            recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder?.prepareToRecord()
            startTime = Date().timeIntervalSince1970
            return recorder?.record(forDuration: 30) ?? false
        } catch {
            return false
        }
    }

    private func stopRecording() -> VoiceClip? {
        guard let recorder = recorder, let url = outputURL else { return nil }
        recorder.stop()
        self.recorder = nil
        let duration = min(30_000, Int64((Date().timeIntervalSince1970 - startTime) * 1000))
        guard let data = try? Data(contentsOf: url) else {
            try? FileManager.default.removeItem(at: url)
            outputURL = nil
            return nil
        }
        try? FileManager.default.removeItem(at: url)
        outputURL = nil
        return VoiceClip(
            bytes: data.toKotlinByteArray(),
            durationMs: max(500, duration),
            mimeType: "audio/mp4"
        )
    }

    private func cancelRecording() {
        recorder?.stop()
        recorder = nil
        if let url = outputURL {
            try? FileManager.default.removeItem(at: url)
        }
        outputURL = nil
        startTime = 0
    }

    private func elapsedMs() -> Int64 {
        guard startTime > 0 else { return 0 }
        return min(30_000, Int64((Date().timeIntervalSince1970 - startTime) * 1000))
    }

    private func play(bytes: KotlinByteArray) {
        stopPlayback()
        var data = Data()
        for i in 0..<bytes.size {
            data.append(UInt8(bitPattern: bytes.get(index: i)))
        }
        do {
            player = try AVAudioPlayer(data: data)
            player?.prepareToPlay()
            player?.play()
        } catch {
            NSLog("BB3 VoiceBridge play error: \(error)")
        }
    }

    private func stopPlayback() {
        player?.stop()
        player = nil
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(count))
        for i in 0..<count {
            array.set(index: Int32(i), value: Int8(bitPattern: self[i]))
        }
        return array
    }
}
