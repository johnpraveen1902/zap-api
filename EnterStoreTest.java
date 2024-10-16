package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;
import org.testng.annotations.BeforeClass;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;

import java.util.concurrent.TimeUnit;

import java.io.FileInputStream;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import pages.HomePage;

public class EnterStoreTest {
    private WebDriver driver;
    private Properties locators;
    private WebDriverWait waiter;

    // ZAP Client
    private static final String ZAP_PROXYHOST = "localhost";
    private static final int ZAP_PROXYPORT = 8081;
    private static final String ZAP_APIKEY = ""; // Set the ZAP API key here if required
    private ClientApi zapClient;

    @BeforeClass
    @Parameters("browser")
    public void setup(String browser) throws Exception {
        // Setup for different browsers
        if (browser.equalsIgnoreCase("firefox")) {
            System.setProperty("webdriver.gecko.driver", "driver-lib\\geckodriver.exe");
            driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("chrome")) {
            System.setProperty("webdriver.chrome.driver", "C:\\\\Users\\\\JOHN PRAVEEN\\\\Downloads\\\\chromedriver-win64\\\\chromedriver-win64\\\\chromedriver.exe");
            driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("Edge")) {
            System.setProperty("webdriver.edge.driver", "driver-lib\\msedgedriver.exe");
            driver = new EdgeDriver();
        } else {
            throw new Exception("Browser is not correct");
        }

        // ZAP client initialization
        zapClient = new ClientApi(ZAP_PROXYHOST, ZAP_PROXYPORT, ZAP_APIKEY);

        // Load the locators
        this.locators = new Properties();
        locators.load(new FileInputStream("config/project.properties"));
        
        // Browser configuration
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    @Test
    public void enterTest() {
        driver.navigate().to("https://petstore.octoperf.com/"); // Replace with your hardcoded target URL

        HomePage hp = new HomePage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        hp.clickEnter();
        sa.assertTrue(hp.isEntered());
    }

    @AfterClass
    public void afterClass() {
        try {
            // Hardcoded target URL
            String targetUrl = "https://petstore.octoperf.com/"; // Replace with your actual target URL

            // Start ZAP Active Scan on the target URL
            zapClient.ascan.scan(targetUrl, "true", "false", null, null, null);

            // Poll the status of the scan until it's complete
            int scanProgress = 0;
            while (scanProgress < 100) {
                ApiResponse response = zapClient.ascan.status("0");
                scanProgress = Integer.parseInt(((ApiResponseElement) response).getValue());
                System.out.println("Scan progress: " + scanProgress + "%");
                Thread.sleep(5000);
            }

            // Generate ZAP HTML report
            @SuppressWarnings("deprecation")
			byte[] report = zapClient.core.htmlreport();
            String reportPath = "zap_report.html"; // Specify your report path
            java.nio.file.Files.write(java.nio.file.Paths.get(reportPath), report);

            System.out.println("ZAP Report generated: " + reportPath);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}
