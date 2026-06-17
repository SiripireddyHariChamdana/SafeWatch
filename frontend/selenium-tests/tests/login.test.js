const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const assert = require('assert');

describe('SafeWatch E2E Login Automation Test', function () {
  this.timeout(45000); // Set timeout to 45 seconds for CI stability
  let driver;

  before(async function () {
    console.log('[*] Initializing Headless Chrome WebDriver...');
    const options = new chrome.Options();
    options.addArguments('--headless');
    options.addArguments('--disable-gpu');
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    options.addArguments('--window-size=1280,800');

    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();
    
    console.log('[+] WebDriver session established successfully.');
  });

  afterEach(async function () {
    if (this.currentTest.state === 'failed' && driver) {
      console.log('=== BROWSER CONSOLE LOGS ===');
      try {
        const logs = await driver.manage().logs().get('browser');
        for (const log of logs) {
          console.log(`[${log.level.name}] ${log.message}`);
        }
      } catch (e) {
        console.log('[!] Failed to retrieve browser logs:', e.message);
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

  it('should successfully enroll a new user and log in to reach the dashboard', async function () {
    const baseUrl = process.env.TEST_URL || 'http://localhost:8000/static/index.html';
    console.log(`[*] Loading target URL: ${baseUrl}`);
    await driver.get(baseUrl);

    // -----------------------------------------------------------------
    // STEP 1: NAVIGATION & SIGNUP (Ensure user exists in DB)
    // -----------------------------------------------------------------
    console.log('[*] Waiting for gateway login view to load...');
    await driver.wait(until.elementLocated(By.id('to-signup')), 10000);
    
    console.log('[*] Navigating to Enroll Profile screen...');
    const enrollLink = await driver.findElement(By.id('to-signup'));
    await enrollLink.click();
    
    console.log('[*] Filling out signup credentials...');
    await driver.wait(until.elementLocated(By.id('signup-email')), 5000);
    await driver.findElement(By.id('signup-name')).sendKeys('CI Test Operator');
    await driver.findElement(By.id('signup-email')).sendKeys('test@example.com');
    await driver.findElement(By.id('signup-password')).sendKeys('password123');
    await driver.findElement(By.id('signup-emergency-phone')).sendKeys('+1-555-0199');
    await driver.findElement(By.id('signup-address')).sendKeys('123 Continuous Integration Ave');
    
    console.log('[*] Submitting enrollment request...');
    const signupForm = await driver.findElement(By.id('signup-form'));
    await signupForm.submit();

    // Handle native browser alert triggered on successful signup
    try {
      console.log('[*] Waiting for signup success alert...');
      await driver.wait(until.alertIsPresent(), 8000);
      const alert = await driver.switchTo().alert();
      console.log(`[+] Alert dialog text: "${await alert.getText()}"`);
      await alert.accept();
      console.log('[+] Accepted signup alert.');
    } catch (e) {
      console.log('[i] Alert handling skipped or alert not found:', e.message);
    }

    // -----------------------------------------------------------------
    // STEP 2: VERIFICATION & LOGIN
    // -----------------------------------------------------------------
    console.log('[*] Waiting for redirection back to login gateway...');
    await driver.sleep(1500); // Wait for transition animation to settle

    const emailField = await driver.wait(until.elementLocated(By.id('login-email')), 10000);
    await driver.wait(until.elementIsVisible(emailField), 10000);
    
    // Clear and input credentials
    console.log('[*] Typing login credentials...');
    await emailField.clear();
    await emailField.sendKeys('test@example.com');
    
    const passwordField = await driver.wait(until.elementLocated(By.id('login-password')), 10000);
    await driver.wait(until.elementIsVisible(passwordField), 10000);
    await passwordField.clear();
    await passwordField.sendKeys('password123');

    console.log(`[i] Email field value: "${await emailField.getAttribute('value')}"`);
    console.log(`[i] Password field value: "${await passwordField.getAttribute('value')}"`);
    
    console.log('[*] Initializing secure gate access...');
    const loginButton = await driver.wait(until.elementLocated(By.id('login-button')), 10000);
    await driver.wait(until.elementIsVisible(loginButton), 10000);
    await driver.executeScript("arguments[0].click();", loginButton);

    // -----------------------------------------------------------------
    // STEP 3: DASHBOARD ROUTING VERIFICATION
    // -----------------------------------------------------------------
    console.log('[*] Verifying redirection to active dashboard hub...');
    const dashboard = await driver.wait(until.elementLocated(By.id('screen-dashboard')), 15000);
    const isDisplayed = await dashboard.isDisplayed();
    
    assert.strictEqual(isDisplayed, true, 'Dashboard screen should be visible after successful login.');
    console.log('[+] E2E Login validation passed. Operator successfully logged in.');
  });
});
