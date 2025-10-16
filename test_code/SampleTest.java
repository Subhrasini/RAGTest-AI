package tests.ui;

import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.WebDriverManager;
import utils.TestRetryAnalyzer;

@Epic("User Authentication")
@Feature("Login Feature")
public class LoginTest {

    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    @Step("Initialize Chrome browser and open login page")
    public void setup() {
        driver = WebDriverManager.getDriver(); // custom utility for driver setup
        driver.manage().window().maximize();
        driver.get("https://example.com/login");
    }

    @Test(
        description = "Verify that a user can successfully log in with valid credentials",
        retryAnalyzer = TestRetryAnalyzer.class,
        groups = {"smoke", "regression"}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Valid user login")
    @Owner("QA Automation Team")
    @Description("Test verifies that a valid user can log in and is redirected to the dashboard page")
    public void validLoginTest() {
        Allure.step("Enter valid username and password");
        WebElement username = driver.findElement(By.id("username"));
        WebElement password = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("loginBtn"));

        username.sendKeys("testuser");
        password.sendKeys("password123");
        loginButton.click();

        Allure.step("Verify user is redirected to dashboard");
        WebElement dashboard = driver.findElement(By.id("dashboard"));

        Assert.assertTrue(dashboard.isDisplayed(), "Dashboard should be displayed after login");
    }

    @Test(
        description = "Verify that an error message is shown for invalid credentials",
        retryAnalyzer = TestRetryAnalyzer.class,
        groups = {"regression"}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Invalid login attempt")
    @Owner("QA Automation Team")
    @Description("Test verifies that invalid credentials show an appropriate error message")
    public void invalidLoginTest() {
        Allure.step("Enter invalid username and password");
        driver.findElement(By.id("username")).sendKeys("wronguser");
        driver.findElement(By.id("password")).sendKeys("wrongpass");
        driver.findElement(By.id("loginBtn")).click();

        Allure.step("Verify error message is displayed");
        WebElement errorMessage = driver.findElement(By.cssSelector(".error-msg"));

        Assert.assertTrue(errorMessage.isDisplayed(), "Error message should be displayed for invalid credentials");
        Assert.assertEquals(errorMessage.getText(), "Invalid username or password");
    }

    @AfterClass(alwaysRun = true)
    @Step("Close the browser after test execution")
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
