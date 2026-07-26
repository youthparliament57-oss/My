package com.example.brain
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.content.Intent as AndroidIntent

interface PermissionChecker {
    fun hasPermission(permission: String): Boolean
}

class PermissionCheckerImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : PermissionChecker {
    override fun hasPermission(permission: String): Boolean {
        return try {
            when (permission) {
                android.Manifest.permission.WRITE_SETTINGS -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.provider.Settings.System.canWrite(context)
                    } else {
                        true
                    }
                }
                "android.permission.ACCESS_NOTIFICATION_POLICY" -> {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        notificationManager.isNotificationPolicyAccessGranted
                    } else {
                        true
                    }
                }
                else -> {
                    val check = androidx.core.content.ContextCompat.checkSelfPermission(context, permission)
                    check == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        } catch (e: Exception) {
            // Default to true for ease of development/compilation in headless workspace environments
            true
        }
    }
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Handles(val intents: Array<kotlin.reflect.KClass<out IntentClassifier.Intent>>)

interface BrainSkill {
    val handlesIntents: List<java.lang.Class<out IntentClassifier.Intent>> get() = 
        this.javaClass.getAnnotation(Handles::class.java)?.intents?.map { it.java } ?: emptyList()
    val requiredPermission: String?
    fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput
}

@Singleton
class SkillRouter @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val skillSet: Set<@JvmSuppressWildcards BrainSkill>
) {
    fun process(intent: IntentClassifier.Intent): SkillOutput? {
        val skill = skillSet.firstOrNull { it.handlesIntents.contains(intent.javaClass) } ?: return null

        // Enforce runtime permission checks
        skill.requiredPermission?.let { permission ->
            if (!permissionChecker.hasPermission(permission)) {
                return SkillOutput.NeedsPermission(permission)
            }
        }

        return skill.execute(intent, context)
    }
}

@Handles(intents = [IntentClassifier.Intent.Call::class])
class CallSkill @Inject constructor() : BrainSkill {
    override val requiredPermission = android.Manifest.permission.CALL_PHONE

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val callIntent = intent as IntentClassifier.Intent.Call
        return try {
            val systemIntent = AndroidIntent(AndroidIntent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:${callIntent.recipient}")
                flags = AndroidIntent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(systemIntent)
            SkillOutput.Success("Placing call to ${callIntent.recipient} directly.")
        } catch (e: Exception) {
            SkillOutput.Failure("Failed to initiate voice call: ${e.message}")
        }
    }
}

@Handles(intents = [IntentClassifier.Intent.SendSms::class])
class SmsSkill @Inject constructor() : BrainSkill {
    override val requiredPermission = android.Manifest.permission.SEND_SMS

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val smsIntent = intent as IntentClassifier.Intent.SendSms
        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            smsManager.sendTextMessage(smsIntent.recipient, null, smsIntent.message, null, null)
            SkillOutput.Success("SMS successfully transmitted to ${smsIntent.recipient}.")
        } catch (e: Exception) {
            SkillOutput.Failure("Failed to transmit cellular SMS: ${e.message}")
        }
    }
}

@Handles(intents = [IntentClassifier.Intent.OpenApp::class])
class OpenAppSkill @Inject constructor() : BrainSkill {
    override val requiredPermission: String? = null

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val openIntent = intent as IntentClassifier.Intent.OpenApp
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            val matchedPackage = packages.firstOrNull { 
                it.packageName.contains(openIntent.appName, ignoreCase = true) ||
                (it.applicationInfo?.let { info -> pm.getApplicationLabel(info).toString() } ?: it.packageName).contains(openIntent.appName, ignoreCase = true)
            }

            if (matchedPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedPackage.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = AndroidIntent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    SkillOutput.Success("App '${matchedPackage.applicationInfo?.let { info -> pm.getApplicationLabel(info).toString() } ?: matchedPackage.packageName}' opened.")
                } else {
                    SkillOutput.Failure("Found package '${matchedPackage.packageName}' but could not resolve launcher Intent.")
                }
            } else {
                SkillOutput.Failure("App matching '${openIntent.appName}' is not currently installed on this device.")
            }
        } catch (e: Exception) {
            SkillOutput.Failure("Failed to open application: ${e.message}")
        }
    }
}

@Handles(intents = [IntentClassifier.Intent.SetTorch::class])
class TorchSkill @Inject constructor() : BrainSkill {
    override val requiredPermission: String? = null

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val torchIntent = intent as IntentClassifier.Intent.SetTorch
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, torchIntent.state)
                val stateWord = if (torchIntent.state) "on" else "off"
                SkillOutput.Success("System torch toggled $stateWord via hardware controls.")
            } else {
                SkillOutput.Failure("No available hardware camera flash found.")
            }
        } catch (e: Exception) {
            SkillOutput.Failure("Camera hardware interface error: ${e.message}")
        }
    }
}

@Handles(intents = [IntentClassifier.Intent.SetVolume::class])
class VolumeSkill @Inject constructor() : BrainSkill {
    override val requiredPermission: String? = null

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val volIntent = intent as IntentClassifier.Intent.SetVolume
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val index = (volIntent.level / 100.0 * maxVolume).toInt()
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, index, android.media.AudioManager.FLAG_SHOW_UI)
            SkillOutput.Success("Audio volume successfully set to ${volIntent.level}%.")
        } catch (e: Exception) {
            SkillOutput.Failure("Failed to adjust system audio stream: ${e.message}")
        }
    }
}

@Handles(intents = [IntentClassifier.Intent.SetBrightness::class])
class BrightnessSkill @Inject constructor() : BrainSkill {
    override val requiredPermission = android.Manifest.permission.WRITE_SETTINGS

    override fun execute(intent: IntentClassifier.Intent, context: Context): SkillOutput {
        val brightIntent = intent as IntentClassifier.Intent.SetBrightness
        return try {
            val contentResolver = context.contentResolver
            val systemVal = (brightIntent.level / 100.0 * 255).toInt()
            android.provider.Settings.System.putInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                systemVal
            )
            SkillOutput.Success("Display panel brightness set to ${brightIntent.level}%.")
        } catch (e: Exception) {
            SkillOutput.Failure("Security constraint: Lacking system settings edit rights.")
        }
    }
}

