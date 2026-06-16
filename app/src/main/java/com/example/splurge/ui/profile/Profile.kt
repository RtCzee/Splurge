package com.example.splurge.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.FinanceRepository
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.login.Login
import com.example.splurge.ui.welcome.Welcome
import com.example.splurge.notifications.BillReminderPreferences
import com.example.splurge.notifications.NotificationPermissionChecker
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

/**
 * Settings screen that works for both guests and signed-in users.
 */
class Profile : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var reminderPreferences: BillReminderPreferences
    private lateinit var privacyPreferences: PrivacyPreferences
    private var updatingReminderSwitches = false

    private val profilePhotoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                saveProfilePhoto(uri)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            reminderPreferences.setPushNotificationsEnabled(granted)
            if (!granted) {
                Toast.makeText(this, R.string.bill_notifications_disabled_message, Toast.LENGTH_SHORT).show()
            }
            syncNotificationSettings()
            repository.rescheduleAllBillReminders()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FinanceRepository.getInstance(this)
        sessionManager = AuthSessionManager(this)
        reminderPreferences = BillReminderPreferences(this)
        privacyPreferences = PrivacyPreferences(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupAccountSection()
        setupPrivacySettings()
        setupNotificationSettings()
        setupBottomNavigation(R.id.navigation_profile)
    }

    override fun onResume() {
        super.onResume()
        setupAccountSection()
        syncPrivacySettings()
        syncNotificationSettings()
    }

    private fun setupAccountSection() {
        val user = getSignedInUser()
        val avatar = findViewById<ImageView>(R.id.profile_avatar)
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)
        val profileStatus = findViewById<TextView>(R.id.profile_status_text)
        val photoHint = findViewById<TextView>(R.id.profile_photo_hint)
        val photoAction = findViewById<MaterialButton>(R.id.buttonProfilePhotoAction)
        val photoRemove = findViewById<MaterialButton>(R.id.buttonProfilePhotoRemove)
        val sessionAction = findViewById<MaterialButton>(R.id.buttonProfileSessionAction)

        if (user == null) {
            profileName.text = getString(R.string.profile_guest_name)
            profileEmail.text = getString(R.string.profile_guest_email)
            profileStatus.text = getString(R.string.settings_guest_message)
            photoHint.text = getString(R.string.settings_photo_guest_hint)
            avatar.setImageResource(R.drawable.ic_launcher_foreground)
            photoAction.visibility = android.view.View.GONE
            photoRemove.visibility = android.view.View.GONE
            sessionAction.text = getString(R.string.settings_sign_in_button)
            sessionAction.setOnClickListener {
                startActivity(Intent(this, Login::class.java))
            }
            return
        }

        profileName.text = user.fullName
        profileEmail.text = user.email
        profileStatus.text = getString(R.string.settings_logged_in_message)
        photoHint.text = getString(R.string.settings_photo_ready_hint)
        sessionAction.text = getString(R.string.settings_sign_out_button)
        sessionAction.setOnClickListener {
            sessionManager.clearUserSession()
            startActivity(
                Intent(this, Welcome::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }

        val hasPhoto = loadProfilePhoto(avatar, user.id, user.profilePicturePath)
        photoAction.visibility = android.view.View.VISIBLE
        photoAction.text = if (hasPhoto) {
            getString(R.string.settings_change_photo_button)
        } else {
            getString(R.string.settings_add_photo_button)
        }
        photoAction.setOnClickListener {
            profilePhotoPicker.launch("image/*")
        }

        photoRemove.visibility = if (hasPhoto) android.view.View.VISIBLE else android.view.View.GONE
        photoRemove.setOnClickListener {
            clearProfilePhoto(avatar, user.id)
        }
    }

    private fun setupPrivacySettings() {
        val hideBalancesSwitch = findViewById<SwitchCompat>(R.id.hide_balances_switch)
        hideBalancesSwitch.setOnCheckedChangeListener { _, isChecked ->
            privacyPreferences.setHideBalancesEnabled(isChecked)
            Toast.makeText(
                this,
                if (isChecked) R.string.settings_privacy_enabled_message else R.string.settings_privacy_disabled_message,
                Toast.LENGTH_SHORT
            ).show()
        }
        syncPrivacySettings()
    }

    private fun syncPrivacySettings() {
        findViewById<SwitchCompat>(R.id.hide_balances_switch).isChecked =
            privacyPreferences.isHideBalancesEnabled()
    }

    private fun setupNotificationSettings() {
        val pushSwitch = findViewById<SwitchCompat>(R.id.push_notifications_switch)
        val billRemindersSwitch = findViewById<SwitchCompat>(R.id.bill_reminders_switch)
        val sevenDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_seven_switch)
        val threeDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_three_switch)
        val oneDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_one_switch)

        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (updatingReminderSwitches) {
                return@setOnCheckedChangeListener
            }

            if (isChecked && !NotificationPermissionChecker.hasPostNotificationsPermission(this)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return@setOnCheckedChangeListener
            }

            reminderPreferences.setPushNotificationsEnabled(isChecked)
            if (!isChecked) {
                reminderPreferences.setBillRemindersEnabled(false)
            }
            repository.rescheduleAllBillReminders()
            syncNotificationSettings()
        }

        billRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (updatingReminderSwitches) {
                return@setOnCheckedChangeListener
            }

            if (isChecked && !pushSwitch.isChecked) {
                Toast.makeText(this, R.string.bill_notifications_disabled_message, Toast.LENGTH_SHORT).show()
                updatingReminderSwitches = true
                billRemindersSwitch.isChecked = false
                updatingReminderSwitches = false
                return@setOnCheckedChangeListener
            }

            reminderPreferences.setBillRemindersEnabled(isChecked)
            repository.rescheduleAllBillReminders()
            syncNotificationSettings()
        }

        val intervalListener = {
            if (!updatingReminderSwitches) {
                reminderPreferences.setReminderIntervals(
                    sevenDaySwitch.isChecked,
                    threeDaySwitch.isChecked,
                    oneDaySwitch.isChecked
                )
                repository.rescheduleAllBillReminders()
                syncNotificationSettings()
            }
        }

        sevenDaySwitch.setOnCheckedChangeListener { _, _ -> intervalListener() }
        threeDaySwitch.setOnCheckedChangeListener { _, _ -> intervalListener() }
        oneDaySwitch.setOnCheckedChangeListener { _, _ -> intervalListener() }

        syncNotificationSettings()
    }

    private fun syncNotificationSettings() {
        updatingReminderSwitches = true

        val pushSwitch = findViewById<SwitchCompat>(R.id.push_notifications_switch)
        val billRemindersSwitch = findViewById<SwitchCompat>(R.id.bill_reminders_switch)
        val sevenDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_seven_switch)
        val threeDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_three_switch)
        val oneDaySwitch = findViewById<SwitchCompat>(R.id.bill_reminder_one_switch)

        val pushEnabled = reminderPreferences.isPushNotificationsEnabled() &&
            NotificationPermissionChecker.hasPostNotificationsPermission(this)
        val remindersEnabled = reminderPreferences.areBillRemindersEnabled() && pushEnabled
        val reminderDays = reminderPreferences.getReminderIntervals()

        pushSwitch.isChecked = pushEnabled
        billRemindersSwitch.isChecked = remindersEnabled
        sevenDaySwitch.isChecked = reminderDays.contains(7)
        threeDaySwitch.isChecked = reminderDays.contains(3)
        oneDaySwitch.isChecked = reminderDays.contains(1)

        billRemindersSwitch.isEnabled = pushEnabled
        sevenDaySwitch.isEnabled = remindersEnabled
        threeDaySwitch.isEnabled = remindersEnabled
        oneDaySwitch.isEnabled = remindersEnabled

        updatingReminderSwitches = false
    }

    private fun getSignedInUser() = sessionManager.getSignedInUserId()?.let(repository::getUserById)

    private fun loadProfilePhoto(avatar: ImageView, userId: Long, storedPath: String?): Boolean {
        val photoFile = storedPath?.let(::File)
        if (photoFile == null || !photoFile.exists()) {
            avatar.setImageResource(R.drawable.ic_launcher_foreground)
            if (storedPath != null) {
                repository.updateUserProfilePicturePath(userId, null)
            }
            return false
        }

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap == null) {
            avatar.setImageResource(R.drawable.ic_launcher_foreground)
            return false
        }

        avatar.setImageBitmap(bitmap)
        return true
    }

    private fun saveProfilePhoto(uri: Uri) {
        val user = getSignedInUser()
        if (user == null) {
            Toast.makeText(this, R.string.settings_photo_guest_hint, Toast.LENGTH_SHORT).show()
            return
        }

        val photoDir = File(filesDir, "profile_photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        val photoFile = File(photoDir, "profile_${user.id}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(photoFile).use { output ->
                input.copyTo(output)
            }
        } ?: run {
            Toast.makeText(this, R.string.settings_photo_guest_hint, Toast.LENGTH_SHORT).show()
            return
        }

        repository.updateUserProfilePicturePath(user.id, photoFile.absolutePath)
        setupAccountSection()
        Toast.makeText(this, R.string.settings_photo_saved_message, Toast.LENGTH_SHORT).show()
    }

    private fun clearProfilePhoto(avatar: ImageView, userId: Long) {
        val user = repository.getUserById(userId) ?: return
        user.profilePicturePath?.let { path ->
            runCatching { File(path).delete() }
        }
        repository.updateUserProfilePicturePath(userId, null)
        avatar.setImageResource(R.drawable.ic_launcher_foreground)
        setupAccountSection()
        Toast.makeText(this, R.string.settings_photo_removed_message, Toast.LENGTH_SHORT).show()
    }
}
