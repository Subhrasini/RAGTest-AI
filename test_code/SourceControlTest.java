package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.pages.bitbucket.BitbucketLoginPage;
import com.fortify.common.ui.pages.github.GithubOAuthLoginPage;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.applications.application.settings.SourceControlPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Owner("oradchenko@opentext.com")
public class SourceControlTest extends FodBaseTest {

    // Svetlana Pecherskaia (svetlana.pecherskaia@opentext.com) is the owner of the FodQA OAuth app on GH.
    final String gitHubApiClientId = "d3041def1c6527afe9ea";
    final String gitHubApiClientSecret = "1ebbc7cef433e02b06dbec95b246b92bff59b879";
    final String bitbucketClientKey = "HW72Jz8YgjrSSTqR3S";
    final String bitbucketClientSecret = "fgnJDm3Mr4z9f9umcSXPvaYRMbEcdUjK";
    final String bitbucketFeatureFlag = "Bitbucket_UseWorkspacesAPI=true";
    final String githubBitBucketUserName = "fodqa@proton.me";
    final String githubOrgName = "fodqa";
    final String githubRepoName = "symmetrical-eureka";
    final String bitbucketRepoName = "test";
    TenantDTO tenantDTO;
    ApplicationDTO githubApp;
    ApplicationDTO bitbucketApp;
    String githubRedirectUrl;
    String bitbucketRedirectUrl;

    @Test(groups = {"regression"})
    @Description("Create tenant, set up Github credentials on Admin site, create applications")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        var entitlement = EntitlementDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO, true, false);
        new AdminTopNavbar().openTenants();
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlement);

        SiteSettingsActions.setValueInSettings("GitHubApiClientId", gitHubApiClientId, false);
        SiteSettingsActions.setValueInSettings("GitHubApiClientSecret", gitHubApiClientSecret, false);

        SiteSettingsActions.setFeatureFlag(bitbucketFeatureFlag, false);
    }


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should be able to authorize on Github, should be able to select github repository as source control, scan should be successful")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    void githubSourceControlTest() {
        githubApp = ApplicationDTO.createDefaultInstance();

        LogInActions.tamUserLogin(tenantDTO);
        githubApp = ApplicationDTO.createDefaultInstance();
        var page = ApplicationActions.createApplications(githubApp)
                .tenantTopNavbar.openApplications().openDetailsFor(githubApp.getApplicationName())
                .openSettings().openSourceControlTab();
        page.selectGithub().clickAuthenticate();
        var githubPage = new GithubOAuthLoginPage();
        githubPage.loginField.setValue(githubBitBucketUserName);
        githubPage.passwordField.setValue(FodConfig.ADMIN_PASSWORD);
        githubPage.pressSubmit();
        sleep(3000);

        if ($("#otp").exists()) {
            BrowserUtil.openNewTab();
            Selenide.switchTo().window(1);
            open("https://mail.proton.me/u/0/inbox");
            $("#username").shouldBe(Condition.interactable).setValue("fodqa@proton.me");
            sleep(2000);
            $("#password").shouldBe(Condition.interactable).setValue(FodConfig.ADMIN_PASSWORD);
            sleep(2000);
            $("[type='submit']").shouldBe(Condition.interactable).click();
            $("[data-testid='main-logo']").shouldBe(Condition.visible, Duration.ofMinutes(2));
            $$("[class='item-container-wrapper relative']").asDynamicIterable().stream()
                    .filter(x -> x.text().contains("[GitHub] Please verify your device")).findFirst().orElseThrow()
                    .click();

            sleep(3000);
            switchTo().frame($("[data-testid='content-iframe']"));
            var codeText = $("#proton-root")
                    .shouldBe(Condition.exist, Duration.ofSeconds(30))
                    .text();

            String regex = "(\\d{6})";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(codeText);
            String code = matcher.group();

            closeWindow();
            Selenide.switchTo().window(0);

            $("#otp").shouldBe(Condition.interactable).setValue(code);
            $("button[type='submit'].btn-primary").shouldBe(Condition.enabled).click();
        } else if (githubPage.submitButton.isDisplayed()) {
            githubPage.pressSubmit();
        }

        WaitUtil.waitFor(
                WaitUtil.Operator.Equals, false,
                () -> Selenide.webdriver().driver().url().contains("github.com"),
                Duration.ofSeconds(30), false);
        githubRedirectUrl = Selenide.webdriver().driver().url();
        assertThat(githubRedirectUrl)
                .as("Redirect url should not contain 'github.com'").doesNotContain("github.com");
        closeDriverAttachVideo();
        setupDriver("githubSourceControlTest", false);
        Selenide.open(githubRedirectUrl, TenantLoginPage.class).loginAsTam(tenantDTO);
        page = page(SourceControlPage.class);
        page.selectOrgAndWaitForRepositoryExist(githubOrgName);
        var repos = page.repoDropdown.$$("option").texts();
        var repo = repos.stream().filter(t -> t.contains(githubRepoName)).findFirst().orElse("");
        assertThat(repo)
                .as(String.format("%s should be in repositories", githubRepoName)).contains(githubRepoName);

        page.selectRepository(githubRepoName).pressSave();
        var scan = StaticScanDTO.createDefaultSourceControlInstance();
        scan.setRepositoryNameToValidate(githubRepoName);
        scan.setUseSourceControl(true);
        StaticScanActions.createStaticScan(scan, githubApp);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(githubApp, true);
        BrowserUtil.clearCookiesLogOff();

        var scansPage = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(githubApp.getApplicationName()).openScans();

        assertThat(scansPage.getScanByType(FodCustomTypes.ScanType.Static).getStatus())
                .as("Scan should be completed").isEqualToIgnoringCase("completed");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should be able to authorize on Bitbucket, should be able to select bitbucket repository as source control, scan should be successful")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    void bitbucketSourceControlTest() {
        bitbucketApp = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenantDTO);
        var page = ApplicationActions.createApplications(bitbucketApp)
                .tenantTopNavbar.openApplications().openDetailsFor(bitbucketApp.getApplicationName())
                .openSettings().openSourceControlTab();
        page.selectBitbucket().setBitucketCredentials(bitbucketClientKey, bitbucketClientSecret).clickAuthenticate();
        new BitbucketLoginPage().login(githubBitBucketUserName, FodConfig.ADMIN_PASSWORD);

        if ($(byText("Continue without two-step verification")).exists()) {
            $(byText("Continue without two-step verification")).click();
        }

        WaitUtil.waitFor(
                WaitUtil.Operator.Equals, false,
                () -> Selenide.webdriver().driver().url().contains("atlassian"),
                Duration.ofSeconds(180), false);
        bitbucketRedirectUrl = Selenide.webdriver().driver().url();
        assertThat(bitbucketRedirectUrl)
                .as("Redirect url should not contain 'atlassian'").doesNotContain("atlassian");

        closeDriverAttachVideo();
        setupDriver("githubSourceControlTest", false);

        Selenide.open(bitbucketRedirectUrl, TenantLoginPage.class).loginAsTam(tenantDTO);
        page = page(SourceControlPage.class);
        page.selectOrgAndWaitForRepositoryExist(githubOrgName);
        assertThat(page.repoDropdown.$$("option").texts())
                .as(String.format("%s should be in repositories", bitbucketRepoName)).contains(bitbucketRepoName);

        page.selectRepository(bitbucketRepoName).pressSave();
        var scan = StaticScanDTO.createDefaultSourceControlInstance();
        scan.setUseSourceControl(true);
        scan.setRepositoryNameToValidate(String.format("%s/%s", githubOrgName, bitbucketRepoName));
        StaticScanActions.createStaticScan(scan, bitbucketApp);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(bitbucketApp, true);
        BrowserUtil.clearCookiesLogOff();

        var scansPage = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(bitbucketApp.getApplicationName()).openScans();

        AssertionsForClassTypes.assertThat(scansPage.getScanByType(FodCustomTypes.ScanType.Static).getStatus())
                .as("Scan should be completed").isEqualToIgnoringCase("completed");
    }
}
