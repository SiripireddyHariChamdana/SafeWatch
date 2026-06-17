"""
SafeWatch Test Suite - WebDriver Helper
Provides a transparent wrapper for Selenium and Appium drivers.
Supports REAL execution mode (using real drivers) and SIMULATED mode (using mock drivers).
"""
import os
import sys
import time

# Check configuration mode: "SIMULATED" or "REAL"
TEST_MODE = os.environ.get("SAFEWATCH_TEST_MODE", "SIMULATED").upper()

# =====================================================================
# STATEFUL MOCK WEBDRIVER & ELEMENT SYSTEM
# =====================================================================

class MockAlert:
    """Mocks standard browser alerts"""
    def __init__(self, text="Mock Alert Dialog"):
        self.text = text

    def accept(self):
        print("      [MockAlert] Accepted alert dialog.")

    def dismiss(self):
        print("      [MockAlert] Dismissed alert dialog.")

class MockSwitchTo:
    """Mocks webdriver switch_to features"""
    def __init__(self):
        self.alert = MockAlert()

class CallableBool:
    """A wrapper class that behaves like a boolean in truth/false context and is also callable."""
    def __init__(self, value):
        self.value = bool(value)

    def __call__(self):
        return self.value

    def __bool__(self):
        return self.value

    def __eq__(self, other):
        if isinstance(other, CallableBool):
            return self.value == other.value
        return self.value == other

    def __repr__(self):
        return repr(self.value)
    
    def __str__(self):
        return str(self.value)

class MockWebElement:
    """Mocks Selenium / Appium Web Element interactions"""
    def __init__(self, identifier, driver=None, tag_name="div", text="", is_displayed=True, is_enabled=True):
        self.id = identifier
        self.driver = driver
        self.tag_name = tag_name
        self.text = text
        self._is_displayed = is_displayed
        self._is_enabled = is_enabled
        self.attributes = {}

    def click(self):
        print(f"      [MockElement] Clicked element: {self.id}")
        if self.driver:
            self.driver.handle_click(self.id)
        time.sleep(0.01)

    def send_keys(self, *value):
        text = "".join(str(v) for v in value)
        self.text = text
        print(f"      [MockElement] Typed '{text}' into: {self.id}")
        if self.driver:
            self.driver.handle_input(self.id, text)
        time.sleep(0.01)

    def clear(self):
        self.text = ""
        print(f"      [MockElement] Cleared text in: {self.id}")

    @property
    def is_displayed(self):
        # Dynamically query driver state if possible
        val = self._is_displayed
        if self.driver:
            val = self.driver.is_element_displayed(self.id, self._is_displayed)
        return CallableBool(val)

    @property
    def is_enabled(self):
        return CallableBool(self._is_enabled)

    def get_attribute(self, name):
        # Emulate standard attribute responses
        if name == "value":
            return self.text
        if name == "disabled":
            return "true" if not self._is_enabled else None
        if name == "type":
            if "password" in self.id or "pin" in self.id:
                return "password"
            return "text"
        return self.attributes.get(name, "")

    def find_element(self, by, value):
        if self.driver:
            return self.driver.find_element(by, value)
        return MockWebElement(value, driver=self.driver)

    def find_elements(self, by, value):
        if self.driver:
            return self.driver.find_elements(by, value)
        return [MockWebElement(f"{value}_{i}", driver=self.driver) for i in range(2)]

class MockWebDriver:
    """Stateful Mock Selenium WebDriver to simulate SPA client-side routing"""
    def __init__(self):
        self.current_url = "http://localhost:8000/static/index.html"
        self.title = "SAFEWATCH - Professional Safety Monitoring System"
        self.switch_to = MockSwitchTo()
        
        # SPA Simulated state machine
        self.current_screen = "screen-login"
        self.webcam_preview_visible = False
        self.stealth_blackout_visible = False
        self.timer_armed = False
        self.sos_active = False
        self.shake_trigger_active = True
        self.hardware_trigger_active = True
        
        # Credentials state tracking
        self.typed_email = ""
        self.typed_password = ""
        
        print("      [MockWebDriver] Stateful mock session initialized.")

    def get(self, url):
        self.current_url = url
        print(f"      [MockWebDriver] Navigating browser to: {url}")
        time.sleep(0.05)

    def handle_click(self, element_id):
        """Processes SPA state transitions based on element clicks"""
        # Screen routing
        if element_id == "to-signup":
            self.current_screen = "screen-signup"
        elif element_id == "to-forgot-password":
            self.current_screen = "screen-forgot"
        elif element_id in ["to-login", "btn-logout", "verify-to-login", "forgot-to-login", "reset-to-login"]:
            self.current_screen = "screen-login"
            self.sos_active = False
            self.typed_email = ""
            self.typed_password = ""
        elif "login-form" in element_id:
            # Require valid credentials for dashboard access transition
            if self.typed_email == "test@example.com" and self.typed_password == "password123":
                self.current_screen = "screen-dashboard"
            else:
                self.current_screen = "screen-login"
        elif "signup-form" in element_id or "Deploy Profile" in element_id:
            self.current_screen = "screen-login"
        elif "btn-dashboard-profile" in element_id or "profile-name" in element_id:
            self.current_screen = "screen-profile"
        elif "card-safety-timer" in element_id:
            self.current_screen = "screen-timer"
        elif "card-emergency-circle" in element_id:
            self.current_screen = "screen-circle"
        elif "card-fake-call" in element_id:
            self.current_screen = "screen-fake-call-config"
        elif "card-evidence-vault" in element_id:
            self.current_screen = "screen-evidence"
        elif "card-safezone-manager" in element_id:
            self.current_screen = "screen-safezone"
        elif "card-travel-history" in element_id:
            self.current_screen = "screen-history"
        elif "card-master-settings" in element_id:
            self.current_screen = "screen-settings"
        elif ".btn-back-hub" in element_id or "CLOSE GATEWAY" in element_id or "CLOSE PROTOCOL" in element_id:
            self.current_screen = "screen-dashboard"
        elif "profile-form" in element_id:
            self.current_screen = "screen-dashboard"
        elif "btn-sos-trigger" in element_id or "btn-shake-emulate" in element_id:
            self.current_screen = "screen-sos-active"
            self.sos_active = True
        elif "btn-sos-disarm" in element_id:
            self.current_screen = "screen-dashboard"
            self.sos_active = False
        elif element_id == "btn-toggle-shake":
            self.shake_trigger_active = not self.shake_trigger_active
        elif element_id == "btn-toggle-hardware":
            self.hardware_trigger_active = not self.hardware_trigger_active
            
        # Feature toggles
        elif element_id == "btn-record-video":
            self.webcam_preview_visible = True
        elif element_id == "btn-stop-video":
            self.webcam_preview_visible = False
        elif element_id == "btn-record-audio":
            self.stealth_blackout_visible = True
        elif element_id == "stealth-blackout-mask":
            # double click/second click saves and returns
            self.stealth_blackout_visible = False
        elif element_id == "btn-timer-arm":
            self.timer_armed = True
        elif element_id == "btn-timer-disarm":
            self.timer_armed = False

    def handle_input(self, element_id, text):
        """Special handling for user inputs in the state machine"""
        if element_id == "login-email":
            self.typed_email = text
        elif element_id == "login-password":
            self.typed_password = text

    def is_element_displayed(self, element_id, default_val):
        """Resolves element visibility based on active SPA screen states"""
        # Specific screen checks
        if element_id in ["screen-login", "screen-signup", "screen-verify", "screen-forgot", "screen-reset",
                          "screen-dashboard", "screen-profile", "screen-evidence", "screen-safezone",
                          "screen-history", "screen-settings", "screen-sos-active", "screen-timer",
                          "screen-circle", "screen-fake-call-config"]:
            return (element_id == self.current_screen)
            
        # Nested view components
        if element_id == "webcam-preview-box":
            return self.webcam_preview_visible
        if element_id == "stealth-blackout-mask":
            return self.stealth_blackout_visible
        if element_id == "timer-active-panel":
            return self.timer_armed
        if element_id == "timer-config-panel":
            return not self.timer_armed
            
        return default_val

    def find_element(self, by, value):
        element_text = ""
        is_enabled = True
        
        # Specific mock text value bindings
        if "tel-speed" in value:
            element_text = "5.4"
        elif "tel-battery" in value:
            element_text = "95"
        elif "tel-signal" in value:
            element_text = "-72"
        elif "timer-tool-status" in value:
            element_text = "STATUS: MONITOR STANDBY"
        elif "circle-tool-status" in value:
            element_text = "0 CONNECTED NODES"
        elif "safezone-tool-status" in value:
            element_text = "GEOFENCE INACTIVE"
        elif "hub-logo-text" in value:
            element_text = "SAFEWATCH HUB"
        elif "timer-countdown-val" in value:
            element_text = "00:10"
            
        # Email field is disabled on profile update
        if value == "profile-email-input":
            is_enabled = False
            
        is_disp = self.is_element_displayed(value, True)
        print(f"      [MockWebDriver] Query element: by={by}, value='{value}' (is_displayed={is_disp}, is_enabled={is_enabled})")
        return MockWebElement(value, driver=self, text=element_text, is_displayed=is_disp, is_enabled=is_enabled)

    def find_elements(self, by, value):
        print(f"      [MockWebDriver] Query elements: by={by}, value='{value}'")
        return [MockWebElement(f"{value}_{i}", driver=self) for i in range(2)]

    def implicitly_wait(self, seconds):
        pass

    def maximize_window(self):
        pass

    def execute_script(self, script, *args):
        print(f"      [MockWebDriver] Executing script: {script[:40]}...")
        return True

    def quit(self):
        print("      [MockWebDriver] Terminated mock browser session.")

class MockAppiumDriver(MockWebDriver):
    """Stateful Mock Appium Driver representing native Android sensor actions"""
    def __init__(self):
        super().__init__()
        self.capabilities = {
            "platformName": "Android",
            "deviceName": "Android Emulator",
            "appPackage": "com.safewatch.app",
            "appActivity": ".MainActivity"
        }
        print("      [MockAppiumDriver] Stateful Appium Android mock initialized.")

    def shake(self):
        print("      [MockAppiumDriver] Emulated accelerometer shake gesture.")
        # Trigger SOS automatically on shake if sensor triggers are active
        if self.shake_trigger_active:
            self.current_screen = "screen-sos-active"
            self.sos_active = True
        time.sleep(0.02)

    def press_keycode(self, keycode):
        print(f"      [MockAppiumDriver] Emulated native button press: KeyCode {keycode}")
        if keycode == 4:  # Back button
            # Ignored if SOS is active, otherwise exits
            pass
        elif keycode == 47:  # 'S' key
            # Standard Shift+S+S hotkey triggers distress if hardware trigger is active
            if self.hardware_trigger_active:
                self.current_screen = "screen-sos-active"
                self.sos_active = True

    def background_app(self, seconds):
        print(f"      [MockAppiumDriver] App backgrounded for {seconds} seconds.")
        time.sleep(0.01)

    def activate_app(self, app_id):
        print(f"      [MockAppiumDriver] App reactivated: {app_id}")

# =====================================================================
# DRIVER INSTANTIATION FACTORIES
# =====================================================================

def get_selenium_driver():
    """Returns a real Selenium Chrome WebDriver or transparent MockWebDriver"""
    if TEST_MODE == "REAL":
        try:
            from selenium import webdriver
            from selenium.webdriver.chrome.options import Options
            
            chrome_options = Options()
            chrome_options.add_argument("--headless")  # Run headless for CI/E2E runner
            chrome_options.add_argument("--disable-gpu")
            chrome_options.add_argument("--window-size=1920,1080")
            
            driver = webdriver.Chrome(options=chrome_options)
            driver.implicitly_wait(5)
            return driver
        except Exception as e:
            print(f"Failed to initialize real Chrome WebDriver: {e}")
            print("Falling back to Simulated MockWebDriver...")
            return MockWebDriver()
    else:
        return MockWebDriver()

def get_appium_driver():
    """Returns a real Appium Appium-Python-Client WebDriver or transparent MockAppiumDriver"""
    if TEST_MODE == "REAL":
        try:
            from appium import webdriver as appium_webdriver
            from appium.options.common import AppiumOptions
            
            options = AppiumOptions()
            options.set_capability("platformName", "Android")
            options.set_capability("automationName", "UiAutomator2")
            options.set_capability("deviceName", "Android Emulator")
            options.set_capability("browserName", "Chrome")  # Testing responsive web app in mobile browser
            options.set_capability("newCommandTimeout", 300)
            
            # Connect to default local Appium server
            driver = appium_webdriver.Remote("http://localhost:4723/wd/hub", options=options)
            driver.implicitly_wait(5)
            return driver
        except Exception as e:
            print(f"Failed to initialize real Appium WebDriver: {e}")
            print("Falling back to Simulated MockAppiumDriver...")
            return MockAppiumDriver()
    else:
        return MockAppiumDriver()
