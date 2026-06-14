package com.example.splurge.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.welcome.Welcome
import com.example.splurge.notifications.BillReminderPreferences
import com.example.splurge.notifications.NotificationPermissionChecker
import com.google.android.material.button.MaterialButton
import androidx.appcompat.widget.SwitchCompat

/**
 * Placeholder profile screen that currently just hosts the shared bottom navigation.
 */
class Profile : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var reminderPreferences: BillReminderPreferences
    private var updatingReminderSwitches = false

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindProfileSummary()
        setupNotificationSettings()
        setupSessionAction()
        setupBottomNavigation(R.id.navigation_profile)
    }

    override fun onResume() {
        super.onResume()
        bindProfileSummary()
        syncNotificationSettings()
    }

    private fun bindProfileSummary() {
        val sessionUserId = sessionManager.getSignedInUserId()
        val user = sessionUserId?.let(repository::getUserById)
        findViewById<TextView>(R.id.profile_name).text = user?.fullName ?: getString(R.string.profile_guest_name)
        findViewById<TextView>(R.id.profile_email).text = user?.email ?: getString(R.string.profile_guest_email)

        findViewById<MaterialButton>(R.id.buttonProfileSessionAction).text =
            if (user == null) getString(R.string.profile_login_button) else getString(R.string.profile_logout_button)
    }

    private fun setupSessionAction() {
        findViewById<MaterialButton>(R.id.buttonProfileSessionAction).setOnClickListener {
            sessionManager.clearUserSession()
            startActivity(
                Intent(this, Welcome::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }

    /** Wires the notification switches to the persisted reminder preferences. */
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

    /** Applies the stored reminder preferences to the visible switches. */
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
}
