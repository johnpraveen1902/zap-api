package tests;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import pages.CartPage;
import pages.HomePage;
import pages.PetStoreMenuPage;
import pages.RegistrationPage;
import pages.SignInPage;
import pages.StoreItemPage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class CombinedTestWithDynamicZap {

    private WebDriver driver;
    private Properties locators;
    private WebDriverWait waiter;
    private static final String ZAP_ADDRESS = "localhost";
    private static final int ZAP_PORT = 8081;
    private static final String ZAP_API_KEY = "";  // Set your ZAP API key if required
    private static final ClientApi zapClient = new ClientApi(ZAP_ADDRESS, ZAP_PORT, ZAP_API_KEY);

    // Load properties file for dynamic URLs
    @BeforeClass
    @Parameters({"browser", "zapEnabled"})
    public void setup(String browser, boolean zapEnabled) throws Exception {
        locators = new Properties();
        locators.load(new FileInputStream("config/project.properties"));

        // Set up the WebDriver with ZAP proxy
        if (zapEnabled) {
            setupZapProxy();  // Call ZAP proxy setup if zapEnabled is true
        }

        // Browser configuration
        if (browser.equalsIgnoreCase("firefox")) {
            System.setProperty("webdriver.gecko.driver", "driver-lib\\geckodriver.exe");
            FirefoxOptions options = new FirefoxOptions();
            if (zapEnabled) {
                options.setProxy(getZapProxy());  // Add ZAP proxy to browser
            }
            options.addArguments("--headless");  // Enable headless mode for Firefox
            driver = new FirefoxDriver(options);
        } else if (browser.equalsIgnoreCase("chrome")) {
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\JOHN PRAVEEN\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
            ChromeOptions options = new ChromeOptions();
            if (zapEnabled) {
                options.setProxy(getZapProxy());  // Add ZAP proxy to browser
            }
            driver = new ChromeDriver(options);
        } else if (browser.equalsIgnoreCase("Edge")) {
            System.setProperty("webdriver.edge.driver", "driver-lib\\msedgedriver.exe");
            driver = new EdgeDriver();
        } else {
            throw new Exception("Browser is not correct");
        }

        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        waiter = new WebDriverWait(driver, 10);
    }

    private Proxy getZapProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(ZAP_ADDRESS + ":" + ZAP_PORT);
        proxy.setSslProxy(ZAP_ADDRESS + ":" + ZAP_PORT);
        return proxy;
    }

    private void setupZapProxy() throws ClientApiException {
        System.out.println("ZAP Proxy configured: " + ZAP_ADDRESS + ":" + ZAP_PORT);
    }

    // ===================== TEST CASES =====================

    // Test 1: Enter Store Test
    @Test
    public void enterTest() {
        driver.navigate().to("https://petstore.octoperf.com/"); 

        HomePage hp = new HomePage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        hp.clickEnter();
        sa.assertTrue(hp.isEntered());
        sa.assertAll();
    }

    // Test 2: Pet Store Menu Test
    @Test
    public void verifyUrlTest() {
		PetStoreMenuPage psmp = new PetStoreMenuPage(driver, locators, waiter);
		SoftAssert sa = new SoftAssert();

		sa.assertTrue(psmp.checkLeftNavLinks());
		sa.assertTrue(psmp.checkTopNavLinks());
		sa.assertTrue(psmp.checkImgNavLinks());
	}

	@Test
	public void linkToRightPageTest() {
		driver.navigate().to(this.locators.getProperty("storeMenuUrl"));

		PetStoreMenuPage psmp = new PetStoreMenuPage(driver, locators, waiter);
		SoftAssert sa = new SoftAssert();
		List<String> species = new ArrayList<>(Arrays.asList("fish", "dogs", "reptiles", "cats", "birds"));

		for (String specie : species) {
			sa.assertTrue(psmp.isLeftNavRight(specie));
		}

		for (String specie : species) {
			sa.assertTrue(psmp.isTopNavRight(specie));
		}

		for (String specie : species) {
			sa.assertTrue(psmp.isImgNavRight(specie));
		}
	}

	@Test
	public void topMenuContentTest() {
		PetStoreMenuPage psmp = new PetStoreMenuPage(driver, locators, waiter);
		SoftAssert sa = new SoftAssert();

		psmp.clickCartPage();
		sa.assertTrue(psmp.isClickedCartPage());

		psmp.clickSignInPage();
		sa.assertTrue(psmp.isClickedSignInPage());

		psmp.clickHelpPage();
		sa.assertTrue(psmp.isClickedHelpPage());
	}

    // Test 3: Registration Test
    @Test
    public void registrationTest() {
        driver.navigate().to(locators.getProperty("registrationUrl"));
        RegistrationPage rp = new RegistrationPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();
        rp.register();
        sa.assertTrue(rp.checkRegistration(), "Registration failed!");
        sa.assertAll();
    }

    // Test 4: Sign In Test
    @Test
    public void signInTest() {
        driver.navigate().to(locators.getProperty("signInUrl"));
        SignInPage sip = new SignInPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();
        sip.signIn();
        sa.assertTrue(sip.checkSignIn(), "Sign in failed!");
        sa.assertAll();
    }

    // Test 5: Cart Test (with ZAP integration)
    @Test
    public void addToCartTest() {
        StoreItemPage sip = new StoreItemPage(driver, locators, waiter);
        CartPage cp = new CartPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        sip.addAllToCart();
        sa.assertTrue(sip.isAdded());
        sa.assertAll();
    }

    @AfterClass
    public void generateZapReport() {
        // Generate ZAP report
        try {
            @SuppressWarnings("deprecation")
			byte[] report = zapClient.core.htmlreport();
            String filePath = "zap-report.html";
            File reportFile = new File(filePath);
            FileUtils.writeByteArrayToFile(reportFile, report);
            System.out.println("ZAP report generated: " + filePath);
        } catch (ClientApiException | IOException e) {
            e.printStackTrace();
        }

        // Close the browser after tests
        driver.quit();
    }
}
