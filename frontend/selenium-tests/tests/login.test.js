const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const assert = require('assert');
const http = require('http');

// ---------------------------------------------------------------------------
// Helper: Make an HTTP POST request to the backend API (no external deps)
// ---------------------------------------------------------------------------
function apiPost(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const options = {
      hostname: 'localhost',
      port: 8000,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
      },
    };
    const req = http.request(options, (res) => {
      let raw = '';
      res.on('data', (chunk) => { raw += chunk; });
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(raw) }); }
        catch (e) { resolve({ status: res.statusCode, body: raw }); }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

// ---------------------------------------------------------------------------
// Test Suite
// ---------------------------------------------------------------------------
describe('SafeWatch E2E Login Automation Test', function () {
  this.timeout(60000); // 60-second timeout for CI stability
  let driver;

  // -------------------------------------------------------------------------
  // Setup: create Chrome driver + pre-seed the test user via API
  // -------------------------------------------------------------------------
  before(async function () {
    console.log('[*] Initializing Headless Chrome WebDriver...');
    const options = new chrome.Options();
    options.addArguments('--headless=new');
    options.addArguments('--disable-gpu');
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    options.addArguments('--window-size=1280,800');
    options.addArguments('--disable-extensions');

    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();

    console.log('[+] WebDriver session established.');

    // Pre-seed test user via backend API (idempotent — ignore "already registered")
    console.log('[*] Pre-seeding test user via API...');
    const signupRes = await apiPost('/api/auth', {
      action: 'signup',
      email: 'test@example.com',
      password: 'password123',
      name: 'CI Test Operator',
      blood_group: 'O+',
      address: '123 Continuous Integration Ave',
      emergency_phone: '+15550199',
    });
    if (signupRes.status === 200) {
      console.log('[+] Test user created successfully.');
    } else if (
      signupRes.status === 400 &&
      typeof signupRes.body.detail === 'string' &&
      signupRes.body.detail.toLowerCase().includes('already')
    ) {
      console.log('[i] Test user already exists — skipping signup.');
    } else {
      console.log(`[!] Unexpected signup response ${signupRes.status}:`, signupRes.body);
    }
  });

  // -------------------------------------------------------------------------
  // Teardown: print browser console logs on failure, close driver
  // -------------------------------------------------------------------------
  afterEach(async function () {
    if (this.currentTest.state === 'failed' && driver) {
      console.log('=== BROWSER CONSOLE LOGS ===');
      try {
        const logs = await driver.manage().logs().get('browser');
        for (const entry of logs) {
          console.log(`  [${entry.level.name}] ${entry.message}`);
        }
      } catch (e) {
        console.log('[!] Could not retrieve browser logs:', e.message);
      }
      console.log('=== PAGE SOURCE (truncated) ===');
      try {
        const src = await driver.getPageSource();
        console.log(src.substring(0, 3000));
      } catch (e) {
        console.log('[!] Could not get page source:', e.message);
      }
      console.log('============================');
    }
  });

  after(async function () {
    if (driver) {
      console.log('[*] Tearing down WebDriver session...');
      await driver.quit();
      console.log('[+] WebDriver session closed.');
    }
  });

  // -------------------------------------------------------------------------
  // TEST: Login with valid credentials → dashboard appears
  // -------------------------------------------------------------------------
  it('should log in with valid credentials and reach the dashboard', async function () {
    const baseUrl = process.env.TEST_URL || 'http://localhost:8000/static/index.html';
    console.log(`[*] Loading target URL: ${baseUrl}`);
    await driver.get(baseUrl);

    // -----------------------------------------------------------------------
    // Wait for login screen to be ready
    // -----------------------------------------------------------------------
    console.log('[*] Waiting for login screen to load...');
    const loginEmailField = await driver.wait(
      until.elementLocated(By.id('login-email')), 15000
    );
    await driver.wait(until.elementIsVisible(loginEmailField), 10000);

    // -----------------------------------------------------------------------
    // Fill in login credentials
    // -----------------------------------------------------------------------
    console.log('[*] Entering credentials...');
    await loginEmailField.clear();
    await loginEmailField.sendKeys('test@example.com');

    const loginPasswordField = await driver.findElement(By.id('login-password'));
    await loginPasswordField.clear();
    await loginPasswordField.sendKeys('password123');

    const emailVal = await loginEmailField.getAttribute('value');
    const passVal  = await loginPasswordField.getAttribute('value');
    console.log(`[i] Email: "${emailVal}"  |  Password length: ${passVal.length}`);

    // -----------------------------------------------------------------------
    // Click login button via JavaScript (reliable in headless mode)
    // -----------------------------------------------------------------------
    console.log('[*] Clicking login button...');
    const loginButton = await driver.findElement(By.id('login-button'));
    await driver.executeScript('arguments[0].click();', loginButton);

    // -----------------------------------------------------------------------
    // Wait for dashboard to become active (check CSS class, not just existence)
    // The SPA adds class "active" to the visible screen via showScreen()
    // -----------------------------------------------------------------------
    console.log('[*] Waiting for dashboard screen to become active...');
    await driver.wait(async () => {
      try {
        const dashboardEl = await driver.findElement(By.id('screen-dashboard'));
        const classes = await dashboardEl.getAttribute('class');
        return classes && classes.includes('active');
      } catch (e) {
        return false;
      }
    }, 20000, 'Dashboard screen did not become active within 20 seconds');

    // -----------------------------------------------------------------------
    // Final assertion: dashboard is displayed
    // -----------------------------------------------------------------------
    const dashboardEl = await driver.findElement(By.id('screen-dashboard'));
    const classes = await dashboardEl.getAttribute('class');
    assert.ok(
      classes && classes.includes('active'),
      `Expected screen-dashboard to have class "active", got: "${classes}"`
    );

    console.log('[+] SUCCESS: Dashboard is active. Login E2E test passed!');
  });
});
