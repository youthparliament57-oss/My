package com.example.brain
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.os.Build

interface SystemOperationExecutor {
    fun setTorchState(on: Boolean): Boolean
    fun setVolumeLevel(percent: Int): Boolean
    fun setBrightnessLevel(percent: Int): Boolean
    fun setMuteState(muted: Boolean): Boolean
    fun setDndState(enabled: Boolean): Boolean
    fun setBatterySaverState(enabled: Boolean): Boolean
}

// Real system executor that manages actual hardware and platform services via Android framework APIs,
// ensuring zero simulation/mock state variables.
class SystemOperationExecutorImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : SystemOperationExecutor {

    override fun setTorchState(on: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun setVolumeLevel(percent: Int): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val index = (percent / 100.0 * maxVolume).toInt()
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, index, android.media.AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setBrightnessLevel(percent: Int): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val systemVal = (percent / 100.0 * 255).toInt()
            android.provider.Settings.System.putInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                systemVal
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setMuteState(muted: Boolean): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (muted) {
                audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
            } else {
                audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
            }
            true
        } catch (e: Exception) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val direction = if (muted) android.media.AudioManager.ADJUST_MUTE else android.media.AudioManager.ADJUST_UNMUTE
                    audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.setStreamMute(android.media.AudioManager.STREAM_MUSIC, muted)
                }
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    override fun setDndState(enabled: Boolean): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    val filter = if (enabled) android.app.NotificationManager.INTERRUPTION_FILTER_NONE else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                    notificationManager.setInterruptionFilter(filter)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun setBatterySaverState(enabled: Boolean): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val value = if (enabled) "1" else "0"
            android.provider.Settings.Global.putString(
                contentResolver,
                "low_power",
                value
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class SkillOutput {
    data class Success(val message: String, val extraMetadata: Map<String, Any> = emptyMap()) : SkillOutput()
    data class Failure(val reason: String) : SkillOutput()
    data class NeedsPermission(val permission: String) : SkillOutput()
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BrainRule(val priority: Int = 0)

interface Rule {
    val priority: Int get() = this.javaClass.getAnnotation(BrainRule::class.java)?.priority ?: 0
    fun match(intent: IntentClassifier.Intent): Boolean
    fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput
}

@Singleton
class RuleEngine @Inject constructor(
    private val executor: SystemOperationExecutor,
    private val ruleSet: Set<@JvmSuppressWildcards Rule>
) {
    private val sortedRules = ruleSet.sortedByDescending { it.priority }

    fun process(intent: IntentClassifier.Intent): SkillOutput? {
        for (rule in sortedRules) {
            if (rule.match(intent)) {
                return rule.execute(intent, executor)
            }
        }
        return null // No rule matched, cascade to Layer 2
    }
}

@BrainRule(priority = 100)
class TorchToggleRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.SetTorch
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val torchIntent = intent as IntentClassifier.Intent.SetTorch
        val success = executor.setTorchState(torchIntent.state)
        return if (success) {
            val stateWord = if (torchIntent.state) "on" else "off"
            SkillOutput.Success("Torch turned $stateWord successfully.")
        } else {
            SkillOutput.Failure("Failed to modify torch state.")
        }
    }
}

@BrainRule(priority = 90)
class VolumeAdjustRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.SetVolume
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val volIntent = intent as IntentClassifier.Intent.SetVolume
        val success = executor.setVolumeLevel(volIntent.level)
        return if (success) {
            SkillOutput.Success("Volume adjusted to ${volIntent.level}%.")
        } else {
            SkillOutput.Failure("Failed to modify audio system volume.")
        }
    }
}

@BrainRule(priority = 80)
class BrightnessAdjustRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.SetBrightness
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val brightIntent = intent as IntentClassifier.Intent.SetBrightness
        val success = executor.setBrightnessLevel(brightIntent.level)
        return if (success) {
            SkillOutput.Success("Screen brightness adjusted to ${brightIntent.level}%.")
        } else {
            SkillOutput.Failure("Failed to modify system brightness.")
        }
    }
}

@BrainRule(priority = 70)
class MuteRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.Mute
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val success = executor.setMuteState(true)
        return if (success) {
            SkillOutput.Success("Device silenced successfully.")
        } else {
            SkillOutput.Failure("Failed to silence audio hardware.")
        }
    }
}

@BrainRule(priority = 70)
class UnmuteRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.Unmute
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val success = executor.setMuteState(false)
        return if (success) {
            SkillOutput.Success("Device sound restored successfully.")
        } else {
            SkillOutput.Failure("Failed to restore audio hardware output.")
        }
    }
}

@BrainRule(priority = 60)
class DndToggleRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.ToggleDnd
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val dndIntent = intent as IntentClassifier.Intent.ToggleDnd
        val success = executor.setDndState(dndIntent.enabled)
        return if (success) {
            val stateWord = if (dndIntent.enabled) "enabled" else "disabled"
            SkillOutput.Success("Do Not Disturb (DND) $stateWord.")
        } else {
            SkillOutput.Failure("Failed to toggle system DND state.")
        }
    }
}

@BrainRule(priority = 50)
class BatterySaverToggleRule @Inject constructor() : Rule {
    override fun match(intent: IntentClassifier.Intent) = intent is IntentClassifier.Intent.ToggleBatterySaver
    override fun execute(intent: IntentClassifier.Intent, executor: SystemOperationExecutor): SkillOutput {
        val batIntent = intent as IntentClassifier.Intent.ToggleBatterySaver
        val success = executor.setBatterySaverState(batIntent.enabled)
        return if (success) {
            val stateWord = if (batIntent.enabled) "enabled" else "disabled"
            SkillOutput.Success("Battery Saver $stateWord successfully.")
        } else {
            SkillOutput.Failure("Failed to adjust power state.")
        }
    }
}

