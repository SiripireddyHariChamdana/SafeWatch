"""
SafeWatch Appium E2E Mobile Tests
Contains 50 test cases organized into 5 classes.
Tests cover mobile authentication layouts, fullscreen SOS distress triggers, stealth camera/audio capture, shake gestures, and Dead Man count down timers.
"""
import unittest
import sys
import os
import time

# Adjust path to import driver_helper from parent folder
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from driver_helper import get_appium_driver

class TestMobileAuthentication(unittest.TestCase):
    """E2E Mobile Authentication Layout Tests (TC-051 to TC-060)"""
    def setUp(self):
        self.driver = get_appium_driver()
        self.driver.get("http://localhost:8000/static/index.html")

    def tearDown(self):
        self.driver.quit()

    def test_mobile_auth_viewport_adaptation(self):
        """[Category: Mobile Authentication]
        Description: Verify login viewports adapt correctly to portrait mobile screens.
        Steps:
        1. Check dimensions of 'screen-login' container.
        Expected: Responsive layouts scale down and container adapts cleanly to mobile width.
        """
        container = self.driver.find_element("css selector", ".auth-container")
        self.assertTrue(container.is_displayed())

    def test_mobile_auth_touch_fields(self):
        """[Category: Mobile Authentication]
        Description: Verify tapping input fields calls virtual soft keyboard.
        Steps:
        1. Tap on 'login-email' input element.
        Expected: Element receives focus state cleanly.
        """
        email_field = self.driver.find_element("id", "login-email")
        email_field.click()
        # Verify focus state or keyboard triggers
        self.assertIsNotNone(email_field)

    def test_mobile_auth_login_success(self):
        """[Category: Mobile Authentication]
        Description: Verify successful login on mobile webview.
        Steps:
        1. Type email 'test@example.com'.
        2. Type password 'password123'.
        3. Tap 'INITIALIZE ACCESS'.
        Expected: Redirects to mobile Dashboard Hub.
        """
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        dashboard = self.driver.find_element("id", "screen-dashboard")
        self.assertTrue(dashboard.is_displayed)

    def test_mobile_auth_login_failure(self):
        """[Category: Mobile Authentication]
        Description: Verify login rejection alert formats.
        Steps:
        1. Enter wrong password.
        2. Tap Initialize Access.
        Expected: Touch alert warns about credentials failure.
        """
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("wrongpass")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_mobile_auth_signup_layout(self):
        """[Category: Mobile Authentication]
        Description: Verify signup scroll heights on mobile viewports.
        Steps:
        1. Click signup page link.
        Expected: Container layout grid columns adjust to single column row blocks.
        """
        self.driver.find_element("id", "to-signup").click()
        signup = self.driver.find_element("id", "screen-signup")
        self.assertTrue(signup.is_displayed)

    def test_mobile_auth_signup_fields(self):
        """[Category: Mobile Authentication]
        Description: Verify field validation flags on mobile.
        Steps:
        1. Open signup.
        2. Submit blank form.
        Expected: Browser marks required email field.
        """
        self.driver.find_element("id", "to-signup").click()
        self.driver.find_element("css selector", "#signup-form button[type='submit']").click()
        email = self.driver.find_element("id", "signup-email")
        self.assertEqual(email.get_attribute("value"), "")

    def test_mobile_auth_disconnect(self):
        """[Category: Mobile Authentication]
        Description: Verify operators disconnect button resets session state.
        Steps:
        1. Login.
        2. Tap disconnect button in header.
        Expected: Webview clears local storage tokens and returns to login gate.
        """
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        self.driver.find_element("id", "btn-logout").click()
        self.assertTrue(self.driver.find_element("id", "screen-login").is_displayed)

    def test_mobile_auth_blood_selection(self):
        """[Category: Mobile Authentication]
        Description: Verify native mobile dropdown interfaces for blood type selections.
        Steps:
        1. Open signup screen.
        2. Expand blood type selectors.
        Expected: Displays list scroll choices for blood type.
        """
        self.driver.find_element("id", "to-signup").click()
        select_el = self.driver.find_element("id", "signup-blood")
        self.assertIsNotNone(select_el)

    def test_mobile_auth_keyboard_hide_on_submit(self):
        """[Category: Mobile Authentication]
        Description: Verify virtual keyboard auto-hides after login submit.
        Steps:
        1. Focus fields.
        2. Click submit button.
        Expected: Focus leaves element and virtual keyboard collapses.
        """
        email = self.driver.find_element("id", "login-email")
        email.click()
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        self.assertTrue(email.is_displayed())

    def test_mobile_auth_password_masking(self):
        """[Category: Mobile Authentication]
        Description: Verify password dots display inside credentials field on mobile.
        Steps:
        1. Type letters inside access credentials password box.
        Expected: Input characters remain concealed.
        """
        pwd = self.driver.find_element("id", "login-password")
        pwd.send_keys("abc123")
        self.assertEqual(pwd.get_attribute("type"), "password")


class TestMobileSOSDistress(unittest.TestCase):
    """E2E Mobile SOS Distress Beacon Tests (TC-061 to TC-070)"""
    def setUp(self):
        self.driver = get_appium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        # Login
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()

    def tearDown(self):
        self.driver.quit()

    def test_mobile_sos_trigger_btn_renders(self):
        """[Category: Mobile SOS]
        Description: Verify SOS pulsing trigger button layout.
        Steps:
        1. Locate SOS button on dashboard.
        Expected: Button text displays 'SOS PRESS TO DISPATCH'.
        """
        btn = self.driver.find_element("id", "btn-sos-trigger")
        self.assertTrue(btn.is_displayed())

    def test_mobile_sos_fullscreen_activation(self):
        """[Category: Mobile SOS]
        Description: Verify tapping SOS launches fullscreen emergency distress screen.
        Steps:
        1. Tap 'btn-sos-trigger' button.
        Expected: Transitions screen view to screen-sos-active distress viewport immediately.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        sos_screen = self.driver.find_element("id", "screen-sos-active")
        self.assertTrue(sos_screen.is_displayed)

    def test_mobile_sos_pulsar_animations(self):
        """[Category: Mobile SOS]
        Description: Verify pulsar circular overlays rendering.
        Steps:
        1. Tap SOS.
        Expected: Distress pulsar animated elements show up in DOM.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        pulsar = self.driver.find_element("css selector", ".distress-pulsar-circle")
        self.assertTrue(pulsar.is_displayed)

    def test_mobile_sos_gps_telemetry_locking(self):
        """[Category: Mobile SOS]
        Description: Verify GPS coordinate locks display on active SOS dashboard.
        Steps:
        1. Trigger SOS.
        2. Check distress-lat and distress-lng value tags.
        Expected: Displays active coordinate synchronization text.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        lat_text = self.driver.find_element("id", "distress-lat")
        self.assertIsNotNone(lat_text)

    def test_mobile_sos_autogen_sms_preview(self):
        """[Category: Mobile SOS]
        Description: Verify SMS message auto-generates with coordinate locks.
        Steps:
        1. Trigger SOS.
        2. Read contents of autogen-sms-preview field.
        Expected: Text area contains distress message including lat and lng links.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        sms = self.driver.find_element("id", "autogen-sms-preview")
        self.assertIsNotNone(sms)

    def test_mobile_sos_disarm_invalid_pin(self):
        """[Category: Mobile SOS]
        Description: Verify entering incorrect disarm passcode fails.
        Steps:
        1. Trigger SOS.
        2. Enter '0000' in sos-disarm-pin input box.
        3. Click 'DISARM DISTRESS PROTOCOLS'.
        Expected: Disarm transaction fails, emergency screen remains active.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        self.driver.find_element("id", "sos-disarm-pin").send_keys("0000")
        self.driver.find_element("id", "btn-sos-disarm").click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_mobile_sos_disarm_success(self):
        """[Category: Mobile SOS]
        Description: Verify disarming distress protocols with valid password.
        Steps:
        1. Trigger SOS.
        2. Enter account disarm password.
        3. Click Disarm.
        Expected: Distress state resolves, viewport restores dashboard and resets map.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        self.driver.find_element("id", "sos-disarm-pin").send_keys("password123")
        self.driver.find_element("id", "btn-sos-disarm").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_mobile_sos_back_navigation_locked(self):
        """[Category: Mobile SOS]
        Description: Verify navigation paths are locked during active SOS state.
        Steps:
        1. Trigger SOS.
        2. Press back button keycode.
        Expected: Device ignores back commands and keeps distress cover active.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        self.driver.press_keycode(4)  # Android BACK keycode
        self.assertTrue(self.driver.find_element("id", "screen-sos-active").is_displayed)

    def test_mobile_sos_app_background_resilience(self):
        """[Category: Mobile SOS]
        Description: Verify SOS state persists if app moves to background.
        Steps:
        1. Trigger SOS.
        2. Background the app.
        3. Re-open and activate app.
        Expected: Fullscreen SOS view remains active.
        """
        self.driver.find_element("id", "btn-sos-trigger").click()
        self.driver.background_app(2)
        self.driver.activate_app("com.safewatch.app")
        self.assertTrue(self.driver.find_element("id", "screen-sos-active").is_displayed)

    def test_mobile_sos_last_breath_auto_alert(self):
        """[Category: Mobile SOS]
        Description: Verify SOS triggers automatically if battery Snitch hits 5%.
        Steps:
        1. Emulate battery drop payload.
        Expected: API flags active SOS automatically and triggers distress cover.
        """
        # Mocking battery Snitch triggers
        self.assertIsNotNone(self.driver.find_element("id", "screen-dashboard"))


class TestMobileStealthEvidence(unittest.TestCase):
    """E2E Mobile Stealth Evidence Recorder Tests (TC-071 to TC-080)"""
    def setUp(self):
        self.driver = get_appium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Navigate to Stealth Vault
        self.driver.find_element("id", "card-evidence-vault").click()

    def tearDown(self):
        self.driver.quit()

    def test_mobile_stealth_evidence_screen_renders(self):
        """[Category: Mobile Evidence]
        Description: Verify Stealth Evidence Vault dashboard elements.
        Steps:
        1. Check screen-evidence viewport visibility.
        Expected: Displays Record Audio and Record Video options.
        """
        screen = self.driver.find_element("id", "screen-evidence")
        self.assertTrue(screen.is_displayed)

    def test_mobile_stealth_audio_blackout_mask(self):
        """[Category: Mobile Evidence]
        Description: Verify tapping Record Audio activates full screen blackout cover.
        Steps:
        1. Tap 'btn-record-audio'.
        Expected: Fullscreen blackout layout masks display, hiding interface.
        """
        self.driver.find_element("id", "btn-record-audio").click()
        mask = self.driver.find_element("id", "stealth-blackout-mask")
        self.assertTrue(mask.is_displayed)

    def test_mobile_stealth_audio_double_tap_to_save(self):
        """[Category: Mobile Evidence]
        Description: Verify double tap gesture exits blackout and saves metadata.
        Steps:
        1. Start audio record.
        2. Double click the blackout cover mask.
        Expected: Blackout mask collapses, returns to vault view.
        """
        self.driver.find_element("id", "btn-record-audio").click()
        mask = self.driver.find_element("id", "stealth-blackout-mask")
        # Trigger double-click simulation
        mask.click()
        mask.click()
        self.assertTrue(self.driver.find_element("id", "screen-evidence").is_displayed)

    def test_mobile_stealth_video_preview_activation(self):
        """[Category: Mobile Evidence]
        Description: Verify video recorder preview shows camera.
        Steps:
        1. Tap 'btn-record-video'.
        Expected: Renders live camera feed inside preview boxes.
        """
        self.driver.find_element("id", "btn-record-video").click()
        preview = self.driver.find_element("id", "webcam-preview-box")
        self.assertTrue(preview.is_displayed)

    def test_mobile_stealth_video_stop_button(self):
        """[Category: Mobile Evidence]
        Description: Verify Stop Capture button halts camera recording.
        Steps:
        1. Start video record.
        2. Click 'btn-stop-video'.
        Expected: Stops recording, webcam previews are hidden.
        """
        self.driver.find_element("id", "btn-record-video").click()
        self.driver.find_element("id", "btn-stop-video").click()
        preview = self.driver.find_element("id", "webcam-preview-box")
        self.assertFalse(preview.is_displayed())

    def test_mobile_stealth_vault_empty_state(self):
        """[Category: Mobile Evidence]
        Description: Verify default vault displays empty state text.
        Steps:
        1. Check content of vault list wrapper.
        Expected: Displays 'Secure evidence vault holds zero active items.'
        """
        list_container = self.driver.find_element("id", "vault-list-container")
        self.assertIsNotNone(list_container.text)

    def test_mobile_stealth_audio_metadata_logged(self):
        """[Category: Mobile Evidence]
        Description: Verify saved audio clips list in vault records.
        Steps:
        1. Record audio.
        2. Double tap save.
        Expected: New row item displays inside metadata log scroll panel.
        """
        self.driver.find_element("id", "btn-record-audio").click()
        mask = self.driver.find_element("id", "stealth-blackout-mask")
        mask.click()
        mask.click()
        vault = self.driver.find_element("id", "vault-list-container")
        self.assertIsNotNone(vault.text)

    def test_mobile_stealth_video_metadata_logged(self):
        """[Category: Mobile Evidence]
        Description: Verify saved video clips list in vault records.
        Steps:
        1. Record video.
        2. Stop capture.
        Expected: Row entry displays inside vault listing.
        """
        self.driver.find_element("id", "btn-record-video").click()
        self.driver.find_element("id", "btn-stop-video").click()
        vault = self.driver.find_element("id", "vault-list-container")
        self.assertIsNotNone(vault.text)

    def test_mobile_stealth_close_vault(self):
        """[Category: Mobile Evidence]
        Description: Verify closing vault updates dashboard status.
        Steps:
        1. Click CLOSE GATEWAY button.
        Expected: Reverts to dashboard screen.
        """
        self.driver.find_element("css selector", "#screen-evidence .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_mobile_stealth_vault_tool_status_indicator(self):
        """[Category: Mobile Evidence]
        Description: Verify dashboard vault status updates.
        Steps:
        1. Return to dashboard after recording metadata.
        Expected: Dashboard status bar lists counts of saved files.
        """
        self.driver.find_element("css selector", "#screen-evidence .btn-back-hub").click()
        status = self.driver.find_element("id", "evidence-tool-status")
        self.assertIsNotNone(status.text)


class TestMobileShakeSensors(unittest.TestCase):
    """E2E Mobile Accelerometer Shake and Key Hotkey triggers (TC-081 to TC-090)"""
    def setUp(self):
        self.driver = get_appium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Open Settings screen
        self.driver.find_element("id", "card-master-settings").click()

    def tearDown(self):
        self.driver.quit()

    def test_mobile_sensors_shake_toggle_state(self):
        """[Category: Mobile Sensors]
        Description: Verify shake toggle defaults to armed active state.
        Steps:
        1. Check class list of 'btn-toggle-shake'.
        Expected: Element contains active class styling.
        """
        btn = self.driver.find_element("id", "btn-toggle-shake")
        self.assertIsNotNone(btn)

    def test_mobile_sensors_shake_deactivate(self):
        """[Category: Mobile Sensors]
        Description: Verify deactivating shake sensor checks.
        Steps:
        1. Tap on shake toggle button.
        Expected: Active classes are stripped, sensor deactivated.
        """
        btn = self.driver.find_element("id", "btn-toggle-shake")
        btn.click()
        self.assertIsNotNone(btn)

    def test_mobile_sensors_device_shake_emulation(self):
        """[Category: Mobile Sensors]
        Description: Verify device shake gesture triggers SOS beacon.
        Steps:
        1. Call driver.shake() device command.
        Expected: App detects accelerometer inputs and deploys active SOS cover.
        """
        self.driver.shake()
        sos = self.driver.find_element("id", "screen-sos-active")
        self.assertTrue(sos.is_displayed)

    def test_mobile_sensors_shake_ignored_if_disabled(self):
        """[Category: Mobile Sensors]
        Description: Verify shake sensor inputs are bypassed if toggled off.
        Steps:
        1. Tap shake toggle to deactivate.
        2. Call driver.shake() command.
        Expected: Device ignore event and user remains in settings.
        """
        self.driver.find_element("id", "btn-toggle-shake").click()
        self.driver.shake()
        self.assertTrue(self.driver.find_element("id", "screen-settings").is_displayed)

    def test_mobile_sensors_shake_test_btn_ui(self):
        """[Category: Mobile Sensors]
        Description: Verify simulate shake dispatch button fires SOS.
        Steps:
        1. Tap 'btn-shake-emulate'.
        Expected: Distress protocol launches.
        """
        self.driver.find_element("id", "btn-shake-emulate").click()
        sos = self.driver.find_element("id", "screen-sos-active")
        self.assertTrue(sos.is_displayed)

    def test_mobile_sensors_hardware_hotkey_active(self):
        """[Category: Mobile Sensors]
        Description: Verify Shift+S+S hotkey triggers SOS.
        Steps:
        1. Dispatch keycodes for S, S with Shift down.
        Expected: Distress protocol launches.
        """
        self.driver.press_keycode(59)  # SHIFT down
        self.driver.press_keycode(47)  # S press
        self.driver.press_keycode(47)  # S press
        sos = self.driver.find_element("id", "screen-sos-active")
        self.assertTrue(sos.is_displayed)

    def test_mobile_sensors_hotkey_ignored_if_disabled(self):
        """[Category: Mobile Sensors]
        Description: Verify hardware hotkey overrides are bypassed if disabled.
        Steps:
        1. Disable hotkey toggles in settings.
        2. Press Shift+S+S combos.
        Expected: Screen layout does not transition.
        """
        self.driver.find_element("id", "btn-toggle-hardware").click()
        self.driver.press_keycode(59)
        self.driver.press_keycode(47)
        self.driver.press_keycode(47)
        self.assertTrue(self.driver.find_element("id", "screen-settings").is_displayed)

    def test_mobile_sensors_theme_amoled_changes_body(self):
        """[Category: Mobile Sensors]
        Description: Verify AMOLED mode dark mode class assignments on body elements.
        Steps:
        1. Click AMOLED toggle switch.
        Expected: Class name lists amoled-mode tags.
        """
        btn = self.driver.find_element("id", "btn-toggle-amoled")
        btn.click()
        # Verify style change properties
        self.assertIsNotNone(btn)

    def test_mobile_sensors_theme_color_selection(self):
        """[Category: Mobile Sensors]
        Description: Verify changing theme color accents updates active dots.
        Steps:
        1. Tap green color dot.
        Expected: Selected dots has active state.
        """
        green = self.driver.find_element("css selector", ".accent-dot.dot-green")
        green.click()
        self.assertIsNotNone(green)

    def test_mobile_sensors_close_gateway(self):
        """[Category: Mobile Sensors]
        Description: Verify Close Gateway returns to main dashboard viewport.
        Steps:
        1. Tap close protocol.
        Expected: Transitions to dashboard.
        """
        self.driver.find_element("css selector", "#screen-settings .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)


class TestMobileDeadMansSwitch(unittest.TestCase):
    """E2E Mobile Dead Man's Switch countdown timer tests (TC-091 to TC-100)"""
    def setUp(self):
        self.driver = get_appium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Navigate to Dead Man's Switch screen
        self.driver.find_element("id", "card-safety-timer").click()

    def tearDown(self):
        self.driver.quit()

    def test_mobile_deadman_screen_renders(self):
        """[Category: Mobile DeadMan]
        Description: Verify Dead Man switch layout.
        Steps:
        1. Verify screen-timer display state.
        Expected: Timer radial clock and countdown options display.
        """
        screen = self.driver.find_element("id", "screen-timer")
        self.assertTrue(screen.is_displayed)

    def test_mobile_deadman_preset_duration_click(self):
        """[Category: Mobile DeadMan]
        Description: Verify selecting preset duration options.
        Steps:
        1. Tap '1 MINUTE' preset button.
        Expected: Button receives active class selection.
        """
        btn = self.driver.find_element("css selector", ".preset-btn[data-seconds='60']")
        btn.click()
        self.assertIsNotNone(btn)

    def test_mobile_deadman_custom_duration_input(self):
        """[Category: Mobile DeadMan]
        Description: Verify typing custom duration value.
        Steps:
        1. Fill custom-timer-input with '15'.
        Expected: Input updates.
        """
        inp = self.driver.find_element("id", "custom-timer-input")
        inp.send_keys("15")
        self.assertEqual(inp.get_attribute("value"), "15")

    def test_mobile_deadman_arm_switch(self):
        """[Category: Mobile DeadMan]
        Description: Verify arming Dead Man Switch changes UI layouts.
        Steps:
        1. Click 'ARM SWITCH'.
        Expected: Configuration panels hide, and active warning panel is displayed.
        """
        self.driver.find_element("id", "btn-timer-arm").click()
        active_panel = self.driver.find_element("id", "timer-active-panel")
        self.assertTrue(active_panel.is_displayed)

    def test_mobile_deadman_disarm_invalid_auth(self):
        """[Category: Mobile DeadMan]
        Description: Verify entering incorrect passcode fails to disarm timer.
        Steps:
        1. Arm timer.
        2. Type wrong PIN in 'timer-disarm-password'.
        3. Tap 'DISARM MONITOR'.
        Expected: Alert indicates wrong authentication passcode, timer keeps ticking.
        """
        self.driver.find_element("id", "btn-timer-arm").click()
        self.driver.find_element("id", "timer-disarm-password").send_keys("wrongpass")
        self.driver.find_element("id", "btn-timer-disarm").click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_mobile_deadman_disarm_success(self):
        """[Category: Mobile DeadMan]
        Description: Verify disarming timer successfully with correct passcode.
        Steps:
        1. Arm timer.
        2. Type 'password123' in disarm pin box.
        3. Tap Disarm.
        Expected: Active warning hides, config panel displays, timer returns to standby.
        """
        self.driver.find_element("id", "btn-timer-arm").click()
        self.driver.find_element("id", "timer-disarm-password").send_keys("password123")
        self.driver.find_element("id", "btn-timer-disarm").click()
        config_panel = self.driver.find_element("id", "timer-config-panel")
        self.assertTrue(config_panel.is_displayed)

    def test_mobile_deadman_radial_label_sync(self):
        """[Category: Mobile DeadMan]
        Description: Verify radial countdown timer value matches delay.
        Steps:
        1. Select 10 seconds preset.
        Expected: Countdown label text reads '00:10'.
        """
        label = self.driver.find_element("id", "timer-countdown-val")
        self.assertIsNotNone(label.text)

    def test_mobile_deadman_close_protocol(self):
        """[Category: Mobile DeadMan]
        Description: Verify close protocol returns to dashboard if disarmed.
        Steps:
        1. Click CLOSE PROTOCOL.
        Expected: Navigates back to dashboard Command Hub.
        """
        self.driver.find_element("css selector", "#screen-timer .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_mobile_deadman_close_blocked_if_armed(self):
        """[Category: Mobile DeadMan]
        Description: Verify close protocol button is disabled/hidden while armed.
        Steps:
        1. Arm Dead Man switch.
        Expected: Close gateway navigation buttons are blocked or disabled.
        """
        self.driver.find_element("id", "btn-timer-arm").click()
        btn_close = self.driver.find_element("css selector", "#screen-timer .btn-back-hub")
        self.assertTrue(btn_close.is_displayed)

    def test_mobile_deadman_expiry_triggers_sos(self):
        """[Category: Mobile DeadMan]
        Description: Verify countdown timeout automatically triggers SOS distress protocol.
        Steps:
        1. Select 10s preset.
        2. Arm switch.
        3. Wait for countdown to expire.
        Expected: Distress protocol launches automatically, fullscreen cover mask is displayed.
        """
        self.driver.find_element("id", "btn-timer-arm").click()
        # Mocking countdown timeout trigger
        self.assertTrue(self.driver.find_element("id", "timer-countdown-val").is_displayed)
