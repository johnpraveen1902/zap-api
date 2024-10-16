package tests;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;

import pages.PetStoreMenuPage;

public class PetStoreMenuTest {
	private WebDriver driver;
	private Properties locators;
	private WebDriverWait waiter;
	private ClientApi zapClient;

	@BeforeClass
	@Parameters("browser")
	public void setup(String browser) throws Exception {
		// Initialize the ZAP client API with default settings
		zapClient = new ClientApi("localhost", 8081, null); // Ensure ZAP is running on port 8080

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
	}

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

	@AfterClass
	public void afterClass() {
		try {
			// Start active scan on the target URL
			String targetUrl = locators.getProperty("storeMenuUrl");
			System.out.println("Starting active scan on: " + targetUrl);
			zapClient.ascan.scan(targetUrl, "true", "false", null, null, null);

			// Check the status of the scan and wait until it is 100% complete
			int status = 0;
			while (status < 100) {
				Thread.sleep(5000); // Wait 5 seconds before checking the scan status again
				ApiResponse statusResponse = zapClient.ascan.status("0"); // Scan ID is usually "0"
				status = Integer.parseInt(((ApiResponseElement) statusResponse).getValue());
				System.out.println("Active scan progress: " + status + "%");
			}

			System.out.println("Scan completed!");

			// Generate the HTML report after the scan is complete
			@SuppressWarnings("deprecation")
			byte[] report = zapClient.core.htmlreport();
			String reportPath = "zap_report_petstore_menu.html"; // Path where the report will be saved
			try (FileOutputStream fos = new FileOutputStream(reportPath)) {
				fos.write(report);
				System.out.println("ZAP report generated at: " + reportPath);
			}

		} catch (Exception e) {
			e.printStackTrace(); // Print any errors that occur
		} finally {
			if (driver != null) {
				driver.quit(); // Close the browser session after tests
			}
		}
	}
}
