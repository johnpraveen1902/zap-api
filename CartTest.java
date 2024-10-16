package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.openqa.selenium.Proxy;
import pages.CartPage;
import pages.StoreItemPage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ApiResponse;

@SuppressWarnings("unused")
public class CartTest {
    private WebDriver driver;
    private Properties locators;
    private WebDriverWait waiter;
    private ClientApi zapClient;
    private static final String ZAP_ADDRESS = "localhost";
    private static final int ZAP_PORT = 8081;
    private static final String ZAP_API_KEY = ""; // Set your ZAP API key if needed

    @BeforeClass
    @Parameters("browser")
    public void setup(String browser) throws Exception {
        // Set up ZAP Proxy
        Proxy zapProxy = new Proxy();
        zapProxy.setHttpProxy(ZAP_ADDRESS + ":" + ZAP_PORT);
        zapProxy.setSslProxy(ZAP_ADDRESS + ":" + ZAP_PORT);

        // Set up WebDriver
        if (browser.equalsIgnoreCase("firefox")) {
            System.setProperty("webdriver.gecko.driver", "driver-lib\\geckodriver.exe");
            driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("chrome")) {
            System.setProperty("webdriver.chrome.driver", "C:\\\\Users\\\\JOHN PRAVEEN\\\\Downloads\\\\chromedriver-win64\\\\chromedriver-win64\\\\chromedriver.exe");
            ChromeOptions options = new ChromeOptions();
            options.setProxy(zapProxy);  // Set ZAP proxy for Chrome
            driver = new ChromeDriver(options);
        } else if (browser.equalsIgnoreCase("Edge")) {
            System.setProperty("webdriver.edge.driver", "driver-lib\\msedgedriver.exe");
            driver = new EdgeDriver();
        } else {
            throw new Exception("Browser is not correct");
        }
        this.locators = new Properties();
        locators.load(new FileInputStream("config/project.properties"));
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(50, TimeUnit.SECONDS);

        // Initialize ZAP API Client
        zapClient = new ClientApi(ZAP_ADDRESS, ZAP_PORT, ZAP_API_KEY);
    }

    @Test(priority = 1)
    public void addToCartTest() {
        StoreItemPage sip = new StoreItemPage(driver, locators, waiter);
        CartPage cp = new CartPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        sip.addAllToCart();
        sa.assertTrue(sip.isAdded());
        sa.assertAll();
    }

    @Test(priority = 2)
    public void totalCostTest() {
        CartPage cp = new CartPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        sa.assertTrue(cp.isEqual());
        sa.assertAll();
    }

    @Test(priority = 3)
    public void clearCookiesTest() {
        StoreItemPage sip = new StoreItemPage(driver, locators, waiter);
        CartPage cp = new CartPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        sip.addAllToCart();
        cp.deleteAllCookies();
        sa.assertTrue(cp.isEmpty());
        sa.assertAll();
    }

    @SuppressWarnings("deprecation")
	@AfterClass
   
    
    public void afterClass() {
        try {
            // Initialize ZAP Client API with correct address and port
            ClientApi api = new ClientApi("localhost", 8081, null); // Ensure port is correct

            // Perform the active scan on the target URL (adjust the URL as necessary)
            api.ascan.scan("https://petstore.octoperf.com/actions/Cart.action?viewCart=", "true", "false", null, null, null);

            // Check the status of the scan and wait for it to complete
            int status = 0;
            while (status < 100) {
                Thread.sleep(5000); // Wait for 5 seconds before checking the scan status again
                
                // Get the scan status
                ApiResponse statusResponse = api.ascan.status("0");
                status = Integer.parseInt(((ApiResponseElement) statusResponse).getValue());

                System.out.println("Scan status: " + status + "% complete");
            }

            System.out.println("Scan completed successfully");

            // Generate the HTML report after the scan completes
            byte[] report = api.core.htmlreport();  // Generates the HTML report as a byte array

            // Save the report to a file
            try (FileOutputStream fos = new FileOutputStream("zap_report.html")) {
                fos.write(report);
                System.out.println("ZAP report saved as 'zap_report.html'");
            }

        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
        } finally {
            if (driver != null) {
                driver.quit(); // Ensure WebDriver is properly closed
            }
        }
    }



}
