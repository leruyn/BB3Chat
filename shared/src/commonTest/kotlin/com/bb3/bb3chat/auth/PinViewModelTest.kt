package com.bb3.bb3chat.auth

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.disguise.DisguiseConfig
import com.bb3.bb3chat.core.platform.IntruderCapture
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.vip.VipEntitlements
import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository
import com.bb3.bb3chat.feature.auth.domain.usecase.SetupPinUseCase
import com.bb3.bb3chat.feature.auth.domain.usecase.ValidatePinUseCase
import com.bb3.bb3chat.feature.auth.presentation.PinUiEffect
import com.bb3.bb3chat.feature.auth.presentation.PinUiEvent
import com.bb3.bb3chat.feature.auth.presentation.PinViewModel
import com.bb3.bb3chat.feature.auth.presentation.SetupStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PinViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePinAuthRepository
    private lateinit var disguiseConfig: DisguiseConfig
    private lateinit var viewModel: PinViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePinAuthRepository()
        disguiseConfig = DisguiseConfig(FakeKeyValueStorage())
        viewModel = createViewModel()
    }

    private fun createViewModel() = PinViewModel(
        validatePin = ValidatePinUseCase(repository),
        setupPin = SetupPinUseCase(repository),
        repository = repository,
        disguiseConfig = disguiseConfig,
        intruderCapture = FakeIntruderCapture(),
        vipEntitlements = VipEntitlements(FakeKeyValueStorage())
    )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun `setup rejects confirm PIN that does not match`() = runTest {
        enterPin("1111")
        assertEquals(SetupStep.CONFIRM_REAL, viewModel.state.value.setupStep)

        enterPin("2222")
        advanceTimeBy(700)

        assertEquals(1, viewModel.state.value.wrongAttempts)
        assertEquals(SetupStep.CONFIRM_REAL, viewModel.state.value.setupStep)
        assertEquals(null, repository.realPin)
    }

    @Test
    fun `setup accepts matching confirm and moves to decoy step`() = runTest {
        enterPin("1111")
        enterPin("1111")

        assertEquals(SetupStep.ENTER_DECOY, viewModel.state.value.setupStep)
        assertEquals("1111", repository.realPin)
        assertFalse(viewModel.state.value.shakeError)
    }

    @Test
    fun `confirm still works after view model recreation`() = runTest {
        enterPin("1111")
        assertTrue(repository.hasPendingRealPin())

        viewModel.onCleared()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SetupStep.CONFIRM_REAL, viewModel.state.value.setupStep)
        enterPin("1111")

        assertEquals(SetupStep.ENTER_DECOY, viewModel.state.value.setupStep)
        assertEquals("1111", repository.realPin)
    }

    @Test
    fun `setup rejects decoy PIN same as real PIN`() = runTest {
        enterPin("1111")
        enterPin("1111")
        enterPin("1111")
        advanceTimeBy(700)

        assertEquals(1, viewModel.state.value.wrongAttempts)
        assertEquals(null, repository.decoyPin)
    }

    @Test
    fun `setup completes with session and navigates to inbox`() = runTest {
        enterPin("1111")
        enterPin("1111")
        enterPin("0000")

        assertFalse(viewModel.state.value.isSetupMode)
        assertEquals("1111", repository.realPin)
        assertEquals("0000", repository.decoyPin)
        assertTrue(repository.sessionOpened)
        assertIs<PinUiEffect.NavigateToRealInbox>(viewModel.effect.first())
    }

    private fun TestScope.enterPin(pin: String) {
        pin.forEach { digit ->
            viewModel.handleEvent(PinUiEvent.DigitPressed(digit.toString()))
        }
        advanceUntilIdle()
    }
}

private class FakePinAuthRepository : PinAuthRepository {
    var realPin: String? = null
    var decoyPin: String? = null
    var pendingHash: String? = null
    var sessionOpened = false

    override suspend fun validatePin(inputPin: String): PinValidationResult {
        return when (inputPin) {
            realPin -> {
                sessionOpened = true
                PinValidationResult.RealAccess
            }
            decoyPin -> PinValidationResult.DecoyAccess
            else -> PinValidationResult.InvalidPin
        }
    }

    override suspend fun setRealPin(newPin: String) {
        realPin = newPin
    }

    override suspend fun setDecoyPin(newPin: String) {
        decoyPin = newPin
    }

    override suspend fun savePendingRealPin(pin: String) {
        pendingHash = CryptoManager.deriveKeyFromPin(pin).toHex()
    }

    override suspend fun confirmPendingRealPin(pin: String): Boolean {
        val inputHash = CryptoManager.deriveKeyFromPin(pin).toHex()
        if (pendingHash == null || inputHash != pendingHash) return false
        setRealPin(pin)
        pendingHash = null
        return true
    }

    override fun hasPendingRealPin(): Boolean = pendingHash != null

    override fun isSameAsRealPin(pin: String): Boolean {
        val real = realPin ?: return false
        return pin == real
    }

    override fun isPinConfigured(): Boolean = realPin != null

    override fun isDecoyPinConfigured(): Boolean = decoyPin != null
}

private class FakeIntruderCapture : IntruderCapture {
    override suspend fun capture(attemptCount: Int) {}
}

private class FakeKeyValueStorage : KeyValueStorage {
    private val data = mutableMapOf<String, Any>()

    override fun putString(key: String, value: String) { data[key] = value }
    override fun getString(key: String): String? = data[key] as? String
    override fun putLong(key: String, value: Long) { data[key] = value }
    override fun getLong(key: String, default: Long) = data[key] as? Long ?: default
    override fun putInt(key: String, value: Int) { data[key] = value }
    override fun getInt(key: String, default: Int) = data[key] as? Int ?: default
    override fun putBoolean(key: String, value: Boolean) { data[key] = value }
    override fun getBoolean(key: String, default: Boolean) = data[key] as? Boolean ?: default
    override fun remove(key: String) { data.remove(key) }
    override fun clearAll() { data.clear() }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
