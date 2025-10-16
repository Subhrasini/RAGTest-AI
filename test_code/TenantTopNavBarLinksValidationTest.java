package com.fortify.fod.ui.test.regression;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class TenantTopNavBarLinksValidationTest extends FodBaseTest {
    String debrickPageUrl = "https://debricked.com/";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to visit swagger page from tenant top navbar, authenticate, send requests and view responses")
    @Test(groups = {"regression"})
    public void apiSwaggerTest() {
        var autoTenant = "AUTO-TENANT";
        var secLeadName = "AUTO-SL";
        var swaggerPage = LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, autoTenant)
                .tenantTopNavbar.openApiSwagger();

        swaggerPage.titleElement.shouldBe(Condition.visible, Duration.ofMinutes(1));
        swaggerPage.authenticate(secLeadName, FodConfig.TAM_PASSWORD, autoTenant);
        assertThat(swaggerPage.getAccessToken()).as("Check that access token is not empty").isNotEmpty();
        assertThat(swaggerPage.getTitle()).as("Check that title is correct")
                .isEqualTo("OpenText™ Core Application Security Web API Explorer");

        swaggerPage.expandResource("Users").expandOperation("Users_UsersV3_GetUsers").clickTryItOut();
        assertThat(swaggerPage.getResponseCode()).as("Check if response code is 200").isEqualTo("200");
        assertThat(swaggerPage.getResponseBody()).as("Check if AUTO-SL in users")
                .contains("\"roleName\": \"Security Lead\"");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Owner("svpillai@opentext.com")
    @FodBacklogItem("597035")
    @Description("Verify Debricked Open source select tab exist in tenant site")
    @Test(groups = {"regression"})
    public void verifyDebrickLink() {
        var page = LogInActions.tamUserLogin(defaultTenantDTO);
        assertThat(page.tenantTopNavbar.debrickedOpenSourceSelect.isDisplayed())
                .as("Icon is Present on the top navigation bar")
                .isTrue();
        page.tenantTopNavbar.openOpenSourceSelect();
        assertThat(BrowserUtil.getTabsCount())
                .as("2 tabs should be opened in browser")
                .isEqualTo(2);
        assertThat(Selenide.webdriver().driver().url())
                .as("Page Url validation")
                .contains(debrickPageUrl);
    }
}
