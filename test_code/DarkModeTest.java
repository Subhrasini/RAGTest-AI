package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.SelenideElement;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("760050")
@Slf4j
public class DarkModeTest extends FodBaseTest {
    List<String> expectedThemeValues = new ArrayList<>() {{
        add("Browser Setting");
        add("Dark");
        add("Light");
    }};
    String darkModeColor = "rgba(16, 22, 41, 1)";
    String lightModeColor = "rgba(241, 242, 243, 1)";
    SelenideElement bodyElement = $(By.tagName("body"));

    @BeforeClass
    public void setup() {
        setupDriver("DarkModeTest", false, false, true);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Theme settings in Tenant Site")
    @Test(groups = {"regression"})
    public void tenantDarkModeTest() {
        var accountPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAccountSettings();
        assertThat(accountPage.getThemeOptions())
                .as("Verify 3 options are available in Themes")
                .isEqualTo(expectedThemeValues);

        accountPage.setTheme("Browser Setting");
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Tenant portal would render in Dark mode")
                .isEqualTo(darkModeColor);
        accountPage.tenantTopNavbar.logOff();
        LogInActions.tamUserLogin(defaultTenantDTO);
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Tenant portal would render in Dark mode when user logout and log back in with same " +
                        "user and browser")
                .isEqualTo(darkModeColor);

        new TenantTopNavbar().openAccountSettings().setTheme("Light");
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Tenant portal would render in Light mode")
                .isEqualTo(lightModeColor);
        new TenantTopNavbar().logOff();
        LogInActions.tamUserLogin(defaultTenantDTO);
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Tenant portal should render in light mode when user logout and log back in with same " +
                        "user and browser")
                .isEqualTo(lightModeColor);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Theme settings in Admin Site")
    @Test(groups = {"regression"}, dependsOnMethods = {"tenantDarkModeTest"})
    public void adminDarkModeTest() {
        var accountPage = LogInActions.adminLogIn().adminTopNavbar.openMyAccount();
        assertThat(accountPage.getThemeOptions())
                .as("Verify 3 options are available in Themes")
                .isEqualTo(expectedThemeValues);

        accountPage.setTheme("Dark");
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Admin portal should render in dark mode")
                .isEqualTo(darkModeColor);
        accountPage.adminTopNavbar.logOff();
        LogInActions.adminLogIn();
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Admin portal should render in dark mode when user logout and log back in with same " +
                        "user and browser")
                .isEqualTo(darkModeColor);

        new AdminTopNavbar().openMyAccount().setTheme("Light");
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Admin portal should render in light mode")
                .isEqualTo(lightModeColor);
        new AdminTopNavbar().logOff();
        LogInActions.adminLogIn();
        assertThat(bodyElement.getCssValue("background-color"))
                .as("Admin portal should render in light mode when user logout and log back in with same " +
                        "user and browser")
                .isEqualTo(lightModeColor);
    }
}
