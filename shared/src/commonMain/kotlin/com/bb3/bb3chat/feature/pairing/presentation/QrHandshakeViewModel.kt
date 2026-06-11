package com.bb3.bb3chat.feature.pairing.presentation

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.RoomIdDeriver
import com.bb3.bb3chat.core.platform.QrCodeGenerator
import com.bb3.bb3chat.core.profile.UserProfileRepository
import com.bb3.bb3chat.core.util.toHexUpper
import com.bb3.bb3chat.feature.pairing.domain.RoomCodePhraseNormalizer
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.RoomCodeLobbyRepository
import com.bb3.bb3chat.feature.pairing.domain.usecase.ConnectRoomUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class QrHandshakeViewModel(
    private val connectRoom: ConnectRoomUseCase,
    private val pairingSessionRepository: PairingSessionRepository,
    private val roomCodeLobby: RoomCodeLobbyRepository,
    private val userProfile: UserProfileRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : BaseViewModel<QrHandshakeUiState, QrHandshakeUiEvent, QrHandshakeUiEffect>(
    QrHandshakeUiState(userAlias = "")
) {

    private var expiryJob: Job? = null
    private var hostObserveJob: Job? = null
    private var hostPollJob: Job? = null
    private var roomMatchObserveJob: Job? = null
    private var roomMatchTimerJob: Job? = null
    private var activeHostCode: String? = null
    private var activeRoomPhrase: String? = null
    private var isHostCompleting = false
    private var isRoomCompleting = false

    init {
        updateState { copy(userAlias = userProfile.getAlias()) }
        generateCode()
    }

    override suspend fun onEvent(event: QrHandshakeUiEvent) {
        when (event) {
            is QrHandshakeUiEvent.SelectTab           -> selectTab(event.tab)
            is QrHandshakeUiEvent.RoomPhraseChanged -> updateState { copy(roomPhrase = event.value, error = null) }
            is QrHandshakeUiEvent.GenerateRoomPhrase -> generateRandomPhrase()
            is QrHandshakeUiEvent.StartRoomCodeMatch -> startRoomCodeMatch()
            is QrHandshakeUiEvent.CancelRoomCodeMatch -> stopRoomCodeMatch()
            is QrHandshakeUiEvent.GenerateCode        -> generateCode()
            is QrHandshakeUiEvent.ToggleScanMode      -> toggleScanMode()
            is QrHandshakeUiEvent.OnCodeScanned       -> handleScanned(event.code)
            is QrHandshakeUiEvent.ManualCodeEntered   -> updateState { copy(scannedCode = event.code, error = null) }
            is QrHandshakeUiEvent.ConfirmConnect      -> doConnect()
            is QrHandshakeUiEvent.Dismiss             -> {
                stopHostSession()
                stopRoomCodeMatch()
                emitEffect(QrHandshakeUiEffect.Dismissed)
            }
        }
    }

    private suspend fun selectTab(tab: HubTab) {
        if (tab == currentState.hubTab) return
        when (tab) {
            HubTab.ROOM_CODE -> stopHostSession()
            HubTab.QR        -> {
                stopRoomCodeMatch()
                if (currentState.myRoomCode.isEmpty()) generateCode()
            }
            HubTab.MY_ID     -> {
                stopHostSession()
                stopRoomCodeMatch()
            }
        }
        updateState { copy(hubTab = tab, error = null) }
    }

    private fun generateRandomPhrase() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        updateState { copy(roomPhrase = "BB3$suffix", error = null) }
    }

    private suspend fun startRoomCodeMatch() {
        val phrase = currentState.roomPhrase.trim()
        if (phrase.length < 4) {
            updateState { copy(error = "Mã phòng cần ít nhất 4 ký tự") }
            return
        }
        runCatching { RoomCodePhraseNormalizer.normalize(phrase) }.onFailure {
            updateState { copy(error = "Mã phòng chỉ gồm chữ và số") }
            return
        }
        stopHostSession()
        stopRoomCodeMatch()
        ensureMyCode()
        val myCode = currentState.myRoomCode
        activeRoomPhrase = phrase
        isRoomCompleting = false
        updateState {
            copy(
                isRoomMatching = true,
                roomMatchSecondsLeft = MATCH_TTL_SEC,
                isConnecting = false,
                error = null
            )
        }
        startRoomMatchTimer(phrase)
        roomMatchObserveJob = scope.launch {
            roomCodeLobby.observeMatch(phrase, myCode)
                .catch { err ->
                    if (!err.isCancellation()) {
                        updateState { copy(error = "Lỗi ghép mã phòng: ${err.message}") }
                    }
                }
                .collect { match ->
                    dispatchRoomCodeConnection(phrase, match.roomId, match.myCode, match.peerCode)
                }
        }
        scope.launch {
            val immediate = runCatching { roomCodeLobby.joinOrWait(phrase, myCode) }.getOrNull()
            if (immediate != null) {
                dispatchRoomCodeConnection(phrase, immediate.roomId, immediate.myCode, immediate.peerCode)
            }
        }
    }

    private fun startRoomMatchTimer(phrase: String) {
        roomMatchTimerJob?.cancel()
        roomMatchTimerJob = scope.launch {
            var left = MATCH_TTL_SEC
            while (isActive && left > 0 && !isRoomCompleting) {
                delay(1_000)
                left -= 1
                updateState { copy(roomMatchSecondsLeft = left) }
            }
            if (!isRoomCompleting && left <= 0) {
                stopRoomCodeMatch()
                updateState { copy(error = "Hết thời gian chờ ghép mã phòng") }
            }
        }
    }

    private fun dispatchRoomCodeConnection(phrase: String, roomId: String, myCode: String, peerCode: String) {
        if (isRoomCompleting) return
        isRoomCompleting = true
        scope.launch {
            completeRoomCodeConnection(phrase, roomId, myCode, peerCode)
        }
    }

    private suspend fun completeRoomCodeConnection(
        phrase: String,
        roomId: String,
        myCode: String,
        peerCode: String
    ) {
        roomMatchObserveJob?.cancel()
        roomMatchTimerJob?.cancel()
        updateState { copy(isConnecting = true, isRoomMatching = false) }
        try {
            connectRoom(roomId = roomId, myCode = myCode, peerCode = peerCode)
            roomCodeLobby.clearLobby(phrase)
            activeRoomPhrase = null
            updateState { copy(isConnecting = false) }
            emitEffect(QrHandshakeUiEffect.RoomCreated(roomId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            isRoomCompleting = false
            updateState { copy(isConnecting = false, error = e.message ?: "Lỗi kết nối") }
        }
    }

    private fun stopRoomCodeMatch() {
        roomMatchObserveJob?.cancel()
        roomMatchObserveJob = null
        roomMatchTimerJob?.cancel()
        roomMatchTimerJob = null
        val phrase = activeRoomPhrase
        activeRoomPhrase = null
        isRoomCompleting = false
        if (phrase != null) {
            scope.launch { roomCodeLobby.clearLobby(phrase) }
        }
        updateState { copy(isRoomMatching = false, roomMatchSecondsLeft = 0) }
    }

    private suspend fun toggleScanMode() {
        if (currentState.scanMode) {
            generateCode()
        } else {
            stopHostSession()
            updateState { copy(scanMode = true, error = null, isWaitingForPeer = false) }
        }
    }

    private fun generateCode() {
        scope.launch {
            stopHostSession()
            expiryJob?.cancel()
            val bytes = CryptoManager.randomBytes(6)
            val code  = bytes.toHexUpper().take(8)
            val payload = "$QR_PREFIX$code"
            val png = qrCodeGenerator.generate(payload)
            updateState {
                copy(
                    myRoomCode       = code,
                    qrImageBytes     = png,
                    isExpired        = false,
                    isWaitingForPeer = false,
                    error            = null,
                    scanMode         = false
                )
            }
            if (currentState.hubTab == HubTab.QR && !currentState.scanMode) {
                startExpiryTimer()
                startHostSession(code)
            }
        }
    }

    private fun ensureMyCode() {
        if (currentState.myRoomCode.isNotEmpty()) return
        val bytes = CryptoManager.randomBytes(6)
        val code  = bytes.toHexUpper().take(8)
        updateState { copy(myRoomCode = code) }
    }

    private fun startHostSession(hostCode: String) {
        isHostCompleting = false
        hostObserveJob = scope.launch {
            val registered = runCatching { pairingSessionRepository.registerHost(hostCode) }
            if (registered.isFailure) {
                val err = registered.exceptionOrNull()
                if (err.isCancellation()) return@launch
                updateState {
                    copy(error = formatHostRegisterError(err), isWaitingForPeer = false)
                }
                return@launch
            }
            activeHostCode = hostCode
            updateState { copy(isWaitingForPeer = true, error = null) }
            pairingSessionRepository.observePeerJoin(hostCode)
                .catch { err ->
                    if (err.isCancellation()) throw err
                    updateState { copy(error = "Lỗi lắng nghe ghép đôi: ${err.message}") }
                }
                .collect { joined ->
                    dispatchHostConnection(hostCode, joined.peerCode, joined.roomId)
                }
        }
        startHostPolling(hostCode)
    }

    private fun startHostPolling(hostCode: String) {
        hostPollJob?.cancel()
        hostPollJob = scope.launch {
            while (isActive && !isHostCompleting) {
                delay(HOST_POLL_MS)
                if (isHostCompleting) break
                val joined = runCatching {
                    pairingSessionRepository.getPeerJoinIfConnected(hostCode)
                }.getOrNull() ?: continue
                dispatchHostConnection(hostCode, joined.peerCode, joined.roomId)
                break
            }
        }
    }

    private fun formatHostRegisterError(err: Throwable?): String {
        val msg = err?.message.orEmpty()
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Không ghi được lên Firestore. Hãy deploy rules: firebase deploy --only firestore:rules"
            msg.isNotBlank() -> "Không thể đăng ký mã chờ: $msg"
            else             -> "Không thể đăng ký mã chờ"
        }
    }

    private fun dispatchHostConnection(hostCode: String, peerCode: String, roomId: String) {
        if (isHostCompleting) return
        isHostCompleting = true
        scope.launch { completeHostConnection(hostCode, peerCode, roomId) }
    }

    private suspend fun completeHostConnection(hostCode: String, peerCode: String, roomId: String) {
        hostObserveJob?.cancel()
        hostObserveJob = null
        hostPollJob?.cancel()
        hostPollJob = null
        activeHostCode = null
        updateState { copy(isWaitingForPeer = false, isConnecting = true, error = null) }
        try {
            connectRoom(roomId, hostCode, peerCode)
            pairingSessionRepository.clearSession(hostCode)
            updateState { copy(isConnecting = false) }
            emitEffect(QrHandshakeUiEffect.RoomCreated(roomId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateState { copy(isConnecting = false, error = e.message ?: "Lỗi kết nối") }
            emitEffect(QrHandshakeUiEffect.ShowError(e.message ?: "Lỗi kết nối"))
        }
    }

    private fun stopHostSession() {
        hostObserveJob?.cancel()
        hostObserveJob = null
        hostPollJob?.cancel()
        hostPollJob = null
        val code = activeHostCode
        activeHostCode = null
        if (code != null) {
            scope.launch { pairingSessionRepository.clearSession(code) }
        }
    }

    private fun startExpiryTimer() {
        expiryJob = scope.launch {
            delay(CODE_TTL_MS)
            stopHostSession()
            updateState {
                copy(isExpired = true, isWaitingForPeer = false, error = "Mã đã hết hạn. Tạo mã mới.")
            }
        }
    }

    private fun handleScanned(raw: String) {
        val code = if (raw.startsWith(QR_PREFIX)) raw.removePrefix(QR_PREFIX) else raw
        if (!ROOM_CODE_REGEX.matches(code)) {
            updateState { copy(error = "Mã QR không hợp lệ") }
            return
        }
        if (code == currentState.myRoomCode) {
            updateState { copy(error = "Không thể kết nối với mã của chính bạn") }
            return
        }
        updateState { copy(scannedCode = code, error = null) }
    }

    private suspend fun doConnect() {
        val code = currentState.scannedCode.trim().uppercase()
        if (code.isEmpty()) {
            updateState { copy(error = "Chưa nhập mã phòng") }
            return
        }
        if (!ROOM_CODE_REGEX.matches(code)) {
            updateState { copy(error = "Mã phòng không hợp lệ") }
            return
        }
        updateState { copy(isConnecting = true, error = null) }
        runCatching {
            val myCode = currentState.myRoomCode
            val roomId = RoomIdDeriver.derive(myCode, code)
            connectRoom(roomId = roomId, myCode = myCode, peerCode = code, hostCode = code)
        }.onSuccess { roomId ->
            updateState { copy(isConnecting = false) }
            emitEffect(QrHandshakeUiEffect.RoomCreated(roomId))
        }.onFailure { err ->
            if (err.isCancellation()) return@onFailure
            updateState { copy(isConnecting = false, error = err.message ?: "Lỗi kết nối") }
        }
    }

    private fun Throwable?.isCancellation(): Boolean =
        this is CancellationException ||
            this?.cause is CancellationException ||
            this?.message?.contains("cancel", ignoreCase = true) == true

    override fun onCleared() {
        expiryJob?.cancel()
        stopHostSession()
        stopRoomCodeMatch()
        super.onCleared()
    }

    companion object {
        private const val QR_PREFIX = "BB3:"
        private const val CODE_TTL_MS = 5 * 60 * 1000L
        private const val MATCH_TTL_SEC = 5 * 60
        private const val HOST_POLL_MS = 2_000L
        private val ROOM_CODE_REGEX = Regex("^[A-F0-9]{8}$")
    }
}
