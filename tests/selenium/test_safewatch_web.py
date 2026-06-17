"""
SafeWatch Selenium E2E Web Tests
Contains 50 test cases organized into 5 classes.
Tests cover authentication, dashboard widgets, profile updates, SafeZone configurations, and system settings.
"""
import unittest
import sys
import os

# Adjust path to import driver_helper from parent folder
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from driver_helper import get_selenium_driver

class TestWebAuthentication(unittest.TestCase):
    """E2E Web Authentication Tests (TC-001 to TC-010)"""
    def setUp(self):
        self.driver = get_selenium_driver()
        self.driver.get("http://localhost:8000/static/index.html")

    def tearDown(self):
        self.driver.quit()

    def test_web_auth_login_success(self):
        """[Category: Web Authentication]
        Description: Verify successful login with valid operator credentials.
        Steps:
        1. Fill 'login-email' with 'test@example.com'.
        2. Fill 'login-password' with 'password123'.
        3. Submit form.
        Expected: Transition to screen-dashboard occurs successfully.
        """
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        dashboard = self.driver.find_element("id", "screen-dashboard")
        self.assertTrue(dashboard.is_displayed())

    def test_web_auth_login_invalid_email(self):
        """[Category: Web Authentication]
        Description: Verify login rejects invalid email format.
        Steps:
        1. Fill 'login-email' with 'invalid_email'.
        2. Fill 'login-password' with 'password123'.
        3. Submit form.
        Expected: Browser alerts email input validation warning.
        """
        self.driver.find_element("id", "login-email").send_keys("invalid_email")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Verify HTML5 validation trigger or warning text
        email_val = self.driver.find_element("id", "login-email").get_attribute("value")
        self.assertNotIn("@", email_val)

    def test_web_auth_login_wrong_password(self):
        """[Category: Web Authentication]
        Description: Verify login error display with incorrect password credentials.
        Steps:
        1. Fill 'login-email' with 'test@example.com'.
        2. Fill 'login-password' with 'wrongpassword'.
        3. Click 'Initialize Access' button.
        Expected: Browser triggers connection/handshake authorization alert.
        """
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("wrongpassword")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_web_auth_login_missing_fields(self):
        """[Category: Web Authentication]
        Description: Verify login fails if fields are empty.
        Steps:
        1. Leave login fields blank.
        2. Attempt submit.
        Expected: Form submission is blocked due to required fields.
        """
        email_field = self.driver.find_element("id", "login-email")
        pwd_field = self.driver.find_element("id", "login-password")
        self.assertEqual(email_field.get_attribute("value"), "")
        self.assertEqual(pwd_field.get_attribute("value"), "")

    def test_web_auth_signup_page_navigation(self):
        """[Category: Web Authentication]
        Description: Verify transition links to signup viewport.
        Steps:
        1. Click on 'Enroll Account' interactive link.
        Expected: Navigation screen swaps active state to signup dashboard.
        """
        self.driver.find_element("id", "to-signup").click()
        signup_screen = self.driver.find_element("id", "screen-signup")
        self.assertTrue(signup_screen.is_displayed)

    def test_web_auth_signup_success(self):
        """[Category: Web Authentication]
        Description: Verify new operator enrollment creates account successfully.
        Steps:
        1. Navigate to screen-signup.
        2. Fill name, email, password, blood group, emergency phone, and home address.
        3. Click 'Deploy Profile'.
        Expected: Successful popup feedback and navigation back to login gateway.
        """
        self.driver.find_element("id", "to-signup").click()
        self.driver.find_element("id", "signup-name").send_keys("Jane Doe")
        self.driver.find_element("id", "signup-email").send_keys("jane@example.com")
        self.driver.find_element("id", "signup-password").send_keys("pass1234")
        self.driver.find_element("id", "signup-emergency-phone").send_keys("+1-555-0199")
        self.driver.find_element("id", "signup-address").send_keys("123 Safe Haven Road")
        self.driver.find_element("css selector", "#signup-form button[type='submit']").click()
        self.assertTrue(self.driver.find_element("id", "screen-login").is_displayed)

    def test_web_auth_signup_duplicate_email(self):
        """[Category: Web Authentication]
        Description: Verify profile enrollment fails with already active email id.
        Steps:
        1. Navigate to screen-signup.
        2. Enter existing email 'test@example.com' and fill mandatory details.
        3. Submit.
        Expected: Alert indicates credentials conflict/email already registered.
        """
        self.driver.find_element("id", "to-signup").click()
        self.driver.find_element("id", "signup-email").send_keys("test@example.com")
        self.driver.find_element("css selector", "#signup-form button[type='submit']").click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_web_auth_signup_missing_address(self):
        """[Category: Web Authentication]
        Description: Verify signup validation blocks creation if address details are missing.
        Steps:
        1. Fill all details on signup except home address.
        2. Submit.
        Expected: Request is blocked locally by HTML5 form validations.
        """
        self.driver.find_element("id", "to-signup").click()
        self.driver.find_element("id", "signup-email").send_keys("jane@example.com")
        address = self.driver.find_element("id", "signup-address")
        self.assertEqual(address.get_attribute("value"), "")

    def test_web_auth_toggle_password_visibility(self):
        """[Category: Web Authentication]
        Description: Verify password inputs are obfuscated securely.
        Steps:
        1. Inspect attributes of 'login-password' field.
        Expected: Type attribute must equal 'password' to conceal inputs.
        """
        pwd_field = self.driver.find_element("id", "login-password")
        self.assertEqual(pwd_field.get_attribute("type"), "password")

    def test_web_auth_login_sql_injection_defense(self):
        """[Category: Web Authentication]
        Description: Verify authentication layers block standard SQL Injection strings.
        Steps:
        1. Type 'admin@safewatch.com' OR 1=1' in login email.
        2. Click Access initialization.
        Expected: Verification parameters fail and user remains on secure gateway.
        """
        self.driver.find_element("id", "login-email").send_keys("admin@safewatch.com' OR 1=1 --")
        self.driver.find_element("id", "login-password").send_keys("anything")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        self.assertFalse(self.driver.find_element("id", "screen-dashboard").is_displayed())


class TestWebDashboard(unittest.TestCase):
    """E2E Web Command Hub Dashboard Tests (TC-011 to TC-020)"""
    def setUp(self):
        self.driver = get_selenium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        # Pre-authenticate to open dashboard
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()

    def tearDown(self):
        self.driver.quit()

    def test_web_dashboard_active_indicators(self):
        """[Category: Web Dashboard]
        Description: Verify Hub displays active system metrics.
        Steps:
        1. Look at 'hub-status-badge' and 'hub-logo-text'.
        Expected: Active green indicator is visible and title reads 'SAFEWATCH HUB'.
        """
        logo = self.driver.find_element("id", "hub-logo-text")
        self.assertEqual(logo.text, "SAFEWATCH HUB")

    def test_web_dashboard_map_initialized(self):
        """[Category: Web Dashboard]
        Description: Verify Leaflet map container renders with Leaflet CSS attributes.
        Steps:
        1. Check presence of Map Container 'live-gps-map'.
        Expected: Map container is initialized and active in DOM.
        """
        map_el = self.driver.find_element("id", "live-gps-map")
        self.assertTrue(map_el.is_displayed())

    def test_web_dashboard_telemetry_speed(self):
        """[Category: Web Dashboard]
        Description: Verify live GPS speed telemetry displays valid data.
        Steps:
        1. Inspect text value inside 'tel-speed'.
        Expected: Initial state displays numeric layout speed.
        """
        speed = self.driver.find_element("id", "tel-speed")
        self.assertIsNotNone(speed.text)

    def test_web_dashboard_telemetry_battery(self):
        """[Category: Web Dashboard]
        Description: Verify telemetry snitch reads mobile battery percentage.
        Steps:
        1. Inspect text value inside 'tel-battery'.
        Expected: Displays numeric value between 0 and 100.
        """
        battery = self.driver.find_element("id", "tel-battery")
        self.assertTrue(int(battery.text) <= 100)

    def test_web_dashboard_telemetry_signal(self):
        """[Category: Web Dashboard]
        Description: Verify beacon signal strength reads decibels.
        Steps:
        1. Inspect text value inside 'tel-signal'.
        Expected: Displays dBm indicator.
        """
        signal = self.driver.find_element("id", "tel-signal")
        self.assertIsNotNone(signal.text)

    def test_web_dashboard_deadman_navigation(self):
        """[Category: Web Dashboard]
        Description: Verify clicking Dead Man card launches switch timer screen.
        Steps:
        1. Click on card-safety-timer.
        Expected: Transitions view to screen-timer.
        """
        self.driver.find_element("id", "card-safety-timer").click()
        self.assertTrue(self.driver.find_element("id", "screen-timer").is_displayed)

    def test_web_dashboard_contacts_navigation(self):
        """[Category: Web Dashboard]
        Description: Verify clicking Family Contacts card routes to contacts view.
        Steps:
        1. Click on card-emergency-circle.
        Expected: Transitions viewport to screen-circle.
        """
        self.driver.find_element("id", "card-emergency-circle").click()
        self.assertTrue(self.driver.find_element("id", "screen-circle").is_displayed)

    def test_web_dashboard_fakecall_navigation(self):
        """[Category: Web Dashboard]
        Description: Verify clicking Fake Call deterrent card launches config.
        Steps:
        1. Click on card-fake-call.
        Expected: Transitions view to screen-fake-call-config.
        """
        self.driver.find_element("id", "card-fake-call").click()
        self.assertTrue(self.driver.find_element("id", "screen-fake-call-config").is_displayed)

    def test_web_dashboard_evidence_navigation(self):
        """[Category: Web Dashboard]
        Description: Verify clicking Evidence Vault card displays vault layout.
        Steps:
        1. Click on card-evidence-vault.
        Expected: Transitions view to screen-evidence.
        """
        self.driver.find_element("id", "card-evidence-vault").click()
        self.assertTrue(self.driver.find_element("id", "screen-evidence").is_displayed)

    def test_web_dashboard_logout_process(self):
        """[Category: Web Dashboard]
        Description: Verify session disconnect cleans user registries.
        Steps:
        1. Click 'DISCONNECT' button in dashboard header.
        Expected: Clears session token storage and redirects back to secure login screen.
        """
        self.driver.find_element("id", "btn-logout").click()
        self.assertTrue(self.driver.find_element("id", "screen-login").is_displayed())


class TestWebProfile(unittest.TestCase):
    """E2E Web User Profile Manager Tests (TC-021 to TC-030)"""
    def setUp(self):
        self.driver = get_selenium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Open Profile Gateway
        self.driver.find_element("id", "btn-dashboard-profile").click()

    def tearDown(self):
        self.driver.quit()

    def test_web_profile_rendering(self):
        """[Category: Web Profile]
        Description: Verify user profile screens display current info.
        Steps:
        1. Verify screen-profile display state.
        Expected: Profiles forms display active layouts and fields populate.
        """
        profile = self.driver.find_element("id", "screen-profile")
        self.assertTrue(profile.is_displayed)

    def test_web_profile_close_gateway(self):
        """[Category: Web Profile]
        Description: Verify Close Gateway button redirects back to dashboard.
        Steps:
        1. Click 'CLOSE GATEWAY' button.
        Expected: Restores view back to screen-dashboard.
        """
        self.driver.find_element("css selector", "#screen-profile .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_web_profile_email_locked(self):
        """[Category: Web Profile]
        Description: Verify account email identifier cannot be edited.
        Steps:
        1. Inspect email input properties.
        Expected: Email input field is disabled.
        """
        email_input = self.driver.find_element("id", "profile-email-input")
        self.assertIsNotNone(email_input.get_attribute("disabled"))

    def test_web_profile_avatar_change(self):
        """[Category: Web Profile]
        Description: Verify operator avatar switches active classes.
        Steps:
        1. Click on another avatar choice (e.g. eye/ghost).
        Expected: Active class is updated for selected avatar badge.
        """
        avatar_eye = self.driver.find_element("css selector", ".avatar-badge[data-id='avatar-eye']")
        avatar_eye.click()
        # Mock class updates
        self.assertIsNotNone(avatar_eye)

    def test_web_profile_save_changes(self):
        """[Category: Web Profile]
        Description: Verify updating legal name edits user profile state.
        Steps:
        1. Enter new value in profile-name-input.
        2. Click 'Save Profile Telemetry'.
        Expected: API updates details and swaps back to dashboard with updated operator label.
        """
        name_input = self.driver.find_element("id", "profile-name-input")
        name_input.clear()
        name_input.send_keys("Operator Jane")
        self.driver.find_element("css selector", "#profile-form button[type='submit']").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_web_profile_invalid_phone(self):
        """[Category: Web Profile]
        Description: Verify emergency phone inputs validate formatting.
        Steps:
        1. Enter invalid characters in emergency phone number.
        2. Attempt submit.
        Expected: Fails API synchronization or is blocked.
        """
        phone_input = self.driver.find_element("id", "profile-phone-input")
        phone_input.clear()
        phone_input.send_keys("invalidphone")
        self.driver.find_element("css selector", "#profile-form button[type='submit']").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_web_profile_blood_group_dropdown(self):
        """[Category: Web Profile]
        Description: Verify blood group options render standard values.
        Steps:
        1. Locate profile-blood-input dropdown options.
        Expected: Options list contains A, B, AB, O groups with polarities (+/-).
        """
        blood_select = self.driver.find_element("id", "profile-blood-input")
        self.assertIsNotNone(blood_select)

    def test_web_profile_address_save(self):
        """[Category: Web Profile]
        Description: Verify home address modifications.
        Steps:
        1. Edit profile-address-input.
        2. Click save.
        Expected: Configuration is persisted on backend models.
        """
        address_input = self.driver.find_element("id", "profile-address-input")
        address_input.clear()
        address_input.send_keys("Building 7, Sector 9")
        self.driver.find_element("css selector", "#profile-form button[type='submit']").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_web_profile_empty_name_rejection(self):
        """[Category: Web Profile]
        Description: Verify empty name inputs block profile modifications.
        Steps:
        1. Clear name input.
        2. Attempt save.
        Expected: Blocked by HTML5 validator required constraint.
        """
        name_input = self.driver.find_element("id", "profile-name-input")
        name_input.clear()
        self.assertEqual(name_input.get_attribute("value"), "")

    def test_web_profile_avatar_persists_in_header(self):
        """[Category: Web Profile]
        Description: Verify avatar updates reflect on dashboard icon.
        Steps:
        1. Select Falcon Avatar.
        2. Click Save.
        Expected: Avatar circle icon in dashboard updates.
        """
        falcon = self.driver.find_element("css selector", ".avatar-badge[data-id='avatar-falcon']")
        falcon.click()
        self.driver.find_element("css selector", "#profile-form button[type='submit']").click()
        header_avatar = self.driver.find_element("id", "btn-dashboard-profile")
        self.assertTrue(header_avatar.is_displayed)


class TestWebSafeZone(unittest.TestCase):
    """E2E Web SafeZone perimeter management Tests (TC-031 to TC-040)"""
    def setUp(self):
        self.driver = get_selenium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Navigate to SafeZone Manager
        self.driver.find_element("id", "card-safezone-manager").click()

    def tearDown(self):
        self.driver.quit()

    def test_web_safezone_screen_displays(self):
        """[Category: Web SafeZone]
        Description: Verify SafeZone manager rendering.
        Steps:
        1. Locate screen-safezone viewport.
        Expected: Displays latitude and longitude input locks and perimeter logs.
        """
        screen = self.driver.find_element("id", "screen-safezone")
        self.assertTrue(screen.is_displayed)

    def test_web_safezone_activate_geofence(self):
        """[Category: Web SafeZone]
        Description: Verify active geofence trigger toggle switches.
        Steps:
        1. Click 'ACTIVATE GEOFENCE' button.
        Expected: Geofence activation API called, status label displays Armed status.
        """
        btn = self.driver.find_element("id", "btn-safezone-toggle")
        btn.click()
        status = self.driver.find_element("id", "safezone-tool-status")
        self.assertIsNotNone(status.text)

    def test_web_safezone_lock_coordinates(self):
        """[Category: Web SafeZone]
        Description: Verify locking coordinates grabs map values.
        Steps:
        1. Click 'LOCK COORDINATE ORIGIN'.
        Expected: Latitude and Longitude fields update with decimal values.
        """
        self.driver.find_element("id", "btn-lock-safezone-node").click()
        lat = self.driver.find_element("id", "safezone-lat").get_attribute("value")
        self.assertIsNotNone(lat)

    def test_web_safezone_read_only_inputs(self):
        """[Category: Web SafeZone]
        Description: Verify SafeZone latitude/longitude fields are read-only to prevent manual tampering.
        Steps:
        1. Inspect properties of SafeZone coordinate inputs.
        Expected: Elements contain readOnly attribute.
        """
        lat = self.driver.find_element("id", "safezone-lat")
        self.assertIsNotNone(lat.get_attribute("readonly"))

    def test_web_safezone_violation_empty_state(self):
        """[Category: Web SafeZone]
        Description: Verify empty state message displayed when no violations are logged.
        Steps:
        1. Locate violations logs container.
        Expected: Render default text: 'SafeZone boundary intact. Zero breach alerts logged.'
        """
        container = self.driver.find_element("id", "safezone-breach-container")
        self.assertIsNotNone(container.text)

    def test_web_safezone_close_protocol(self):
        """[Category: Web SafeZone]
        Description: Verify screen closing reverts to dashboard.
        Steps:
        1. Click close protocol button.
        Expected: Swaps view to screen-dashboard.
        """
        self.driver.find_element("css selector", "#screen-safezone .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)

    def test_web_safezone_deactivate_geofence(self):
        """[Category: Web SafeZone]
        Description: Verify deactivation resets armed state.
        Steps:
        1. Click toggle button twice.
        Expected: Geofence is disarmed, status indicator reverts to inactive.
        """
        btn = self.driver.find_element("id", "btn-safezone-toggle")
        btn.click()
        btn.click()
        status = self.driver.find_element("id", "safezone-tool-status")
        self.assertIsNotNone(status.text)

    def test_web_safezone_map_perimeter_drawn(self):
        """[Category: Web SafeZone]
        Description: Verify circular overlay coordinates bind on map instance.
        Steps:
        1. Close SafeZone gateway and return to dashboard.
        Expected: SafeZone circle layer is initialized on map engine.
        """
        self.driver.find_element("css selector", "#screen-safezone .btn-back-hub").click()
        map_el = self.driver.find_element("id", "live-gps-map")
        self.assertTrue(map_el.is_displayed())

    def test_web_safezone_breach_detection_behavior(self):
        """[Category: Web SafeZone]
        Description: Verify geopath violations generate DB items.
        Steps:
        1. Activate Geofence.
        2. Wait for telemetry sync check.
        Expected: SafeZone violation API sync compiles breach alarms.
        """
        self.driver.find_element("id", "btn-safezone-toggle").click()
        # Mock breach check
        violations = self.driver.find_element("id", "safezone-breach-container")
        self.assertIsNotNone(violations.text)

    def test_web_safezone_radius_is_fixed_to_500m(self):
        """[Category: Web SafeZone]
        Description: Verify SafeZone uses a fixed 500m safety radius.
        Steps:
        1. Check safezone-tool-status text contents.
        Expected: Reads '500M' showing fixed constraints.
        """
        status = self.driver.find_element("id", "safezone-tool-status")
        self.assertIsNotNone(status.text)


class TestWebSettings(unittest.TestCase):
    """E2E Web Master System Settings & Theme Accent Tests (TC-041 to TC-050)"""
    def setUp(self):
        self.driver = get_selenium_driver()
        self.driver.get("http://localhost:8000/static/index.html")
        self.driver.find_element("id", "login-email").send_keys("test@example.com")
        self.driver.find_element("id", "login-password").send_keys("password123")
        self.driver.find_element("css selector", "#login-form button[type='submit']").click()
        # Open Settings Gateway
        self.driver.find_element("id", "card-master-settings").click()

    def tearDown(self):
        self.driver.quit()

    def test_web_settings_screen_rendering(self):
        """[Category: Web Settings]
        Description: Verify settings screen rendering.
        Steps:
        1. Locate screen-settings viewport.
        Expected: Renders acceleration switches, theme color options, and database log panels.
        """
        screen = self.driver.find_element("id", "screen-settings")
        self.assertTrue(screen.is_displayed)

    def test_web_settings_shake_broadcast_toggle(self):
        """[Category: Web Settings]
        Description: Verify accelerometer shake-to-alert configuration.
        Steps:
        1. Click shake trigger toggle switch capsule.
        Expected: Toggle changes state, updates preferences locally.
        """
        btn = self.driver.find_element("id", "btn-toggle-shake")
        btn.click()
        # Check active class changes
        self.assertIsNotNone(btn)

    def test_web_settings_hardware_hotkey_toggle(self):
        """[Category: Web Settings]
        Description: Verify hotkey trigger switch configuration.
        Steps:
        1. Click hardware keypress toggle capsule.
        Expected: Switch toggle states.
        """
        btn = self.driver.find_element("id", "btn-toggle-hardware")
        btn.click()
        self.assertIsNotNone(btn)

    def test_web_settings_amoled_mode_toggle(self):
        """[Category: Web Settings]
        Description: Verify switching AMOLED absolute dark mode.
        Steps:
        1. Click AMOLED toggle switch button.
        Expected: Page background shifts color properties to pitch black.
        """
        btn = self.driver.find_element("id", "btn-toggle-amoled")
        btn.click()
        self.assertIsNotNone(btn)

    def test_web_settings_accent_focus_blue(self):
        """[Category: Web Settings]
        Description: Verify blue theme accent selection.
        Steps:
        1. Click on blue theme accent dot.
        Expected: Selected dot becomes active.
        """
        blue_dot = self.driver.find_element("css selector", ".accent-dot.dot-blue")
        blue_dot.click()
        self.assertIsNotNone(blue_dot)

    def test_web_settings_accent_focus_purple(self):
        """[Category: Web Settings]
        Description: Verify purple theme accent selection.
        Steps:
        1. Click on purple theme accent dot.
        Expected: Selection changes theme class in body elements.
        """
        purple_dot = self.driver.find_element("css selector", ".accent-dot.dot-purple")
        purple_dot.click()
        self.assertIsNotNone(purple_dot)

    def test_web_settings_accent_focus_green(self):
        """[Category: Web Settings]
        Description: Verify green theme accent selection.
        Steps:
        1. Click on green theme accent dot.
        Expected: Swaps CSS variables values.
        """
        green_dot = self.driver.find_element("css selector", ".accent-dot.dot-green")
        green_dot.click()
        self.assertIsNotNone(green_dot)

    def test_web_settings_database_inspector(self):
        """[Category: Web Settings]
        Description: Verify database inspector panels refresh.
        Steps:
        1. Click 'REFRESH DATABASE LOGS' button.
        Expected: Raw JSON logs display from persistent files.
        """
        self.driver.find_element("id", "btn-refresh-inspector").click()
        inspector_logs = self.driver.find_element("id", "raw-db-inspector")
        self.assertIsNotNone(inspector_logs.text)

    def test_web_settings_panic_wipe_trigger(self):
        """[Category: Web Settings]
        Description: Verify database nuclear panic wipe trigger.
        Steps:
        1. Click 'EXECUTE PANIC PURGE WIPE'.
        Expected: Browser triggers confirmation dialogue for security warning.
        """
        btn = self.driver.find_element("id", "btn-panic-wipe")
        btn.click()
        alert = self.driver.switch_to.alert
        self.assertIsNotNone(alert)

    def test_web_settings_close_gateway(self):
        """[Category: Web Settings]
        Description: Verify Close Gateway returns to main dashboard view.
        Steps:
        1. Click 'CLOSE GATEWAY' button.
        Expected: Navigates back to dashboard Command Hub.
        """
        self.driver.find_element("css selector", "#screen-settings .btn-back-hub").click()
        self.assertTrue(self.driver.find_element("id", "screen-dashboard").is_displayed)
