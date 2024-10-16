package tests;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import pages.RegistrationPage;

import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

public class RegistrationTest {
    private WebDriver driver;
    private Properties locators;
    private WebDriverWait waiter;

    // ZAP Client API
    private static final String ZAP_ADDRESS = "localhost";
    private static final int ZAP_PORT = 8080;
    private static final String ZAP_API_KEY = ""; // Provide API key if required
    private static final String TARGET_URL = "https://petstore.octoperf.com/actions/Account.action?newAccountForm="; // Hardcoded target URL

    private ClientApi zapClient;

    @BeforeClass
    @Parameters("browser")
    public void setup(String browser) throws Exception {
        if (browser.equalsIgnoreCase("firefox")) {
            System.setProperty("webdriver.gecko.driver", "driver-lib\\geckodriver.exe");
            driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("chrome")) {
            System.setProperty("webdriver.chrome.driver", "C:\\\\Users\\\\JOHN PRAVEEN\\\\Downloads\\\\chromedriver-win64\\\\chromedriver-win64\\\\chromedriver.exe");
            driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("Edge")) {
            System.setProperty("webdriver.edge.driver", "driver-lib\\\\msedgedriver.exe");
            driver = new EdgeDriver();
        } else {
            throw new Exception("Browser is not correct");
        }
        this.locators = new Properties();
        locators.load(new FileInputStream("config/project.properties"));
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Initialize ZAP client
        zapClient = new ClientApi(ZAP_ADDRESS, ZAP_PORT, ZAP_API_KEY);
        
        // Start ZAP scan
        startZAPScan();
    }

    private void startZAPScan() {
        try {
            // ZAP needs to capture traffic from the Selenium WebDriver
            // Ensure the WebDriver is configured to use ZAP as a proxy
            System.setProperty("webdriver.chrome.args", "--proxy-server=http://localhost:8081");
            System.setProperty("webdriver.firefox.profile", "your_profile_here"); // You may need to configure Firefox to use the ZAP proxy
            driver.get(TARGET_URL); // Navigates to the target URL
            
            // Give ZAP some time to capture the request
            Thread.sleep(5000); // Adjust this time if necessary
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void fillInFormTest() {
        driver.navigate().to(this.locators.getProperty("registrationUrl"));

        RegistrationPage rp = new RegistrationPage(driver, locators, waiter);
        SoftAssert sa = new SoftAssert();

        rp.register();
        sa.assertTrue(rp.checkRegistration());
    }

    @AfterClass
    public void afterClass() {
        // Perform ZAP Active Scan after tests are completed
        try {
            System.out.println("Active scan is starting...");
            zapClient.ascan.scan(TARGET_URL, null, null, null, null, null);

            // Wait for scan to complete
            while (true) {
                String status = zapClient.ascan.status("0").toString();
                System.out.println("Scan status: " + status + "%");
                if ("100".equals(status)) {
                    break;
                }
                Thread.sleep(5000);
            }

            // Generate the report in HTML format
            @SuppressWarnings("deprecation")
			byte[] report = zapClient.core.htmlreport();
            java.nio.file.Files.write(java.nio.file.Paths.get("zap-report.html"), report);
            System.out.println("ZAP report generated: zap-report.html");

        } catch (ClientApiException | InterruptedException | java.io.IOException e) {
            e.printStackTrace();
        }

        this.driver.close();
    }
}
