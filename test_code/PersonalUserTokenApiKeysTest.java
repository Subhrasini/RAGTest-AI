package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.exceptions.FodUnexpectedConditionsException;
import com.fortify.fod.ui.pages.swagger.SwaggerPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavTabs;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.time.LocalTime;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class PersonalUserTokenApiKeysTest extends FodBaseTest {

    TenantDTO tenantDTO;
    String settingName = "AllowPersonalAccessTokens";
    String expectedSettingsSavedMessage = "Changes were saved successfully.";
    String expectedAuthenticationFailedMessage = "Authentication failed.";
    String expectedAuthenticationSuccessMessage = "You are now authenticated.";
    String expectedWarningMessage = "The Personal Access Token secret will expire soon.";
    String expectedMessageForDeletedUser = "Invalid resource owner password credential.";
    String apiKeyUser = "apiKeyUserTest-".concat(UniqueRunTag.generate());
    String apiKeyValue, apiSecretValue;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare tenant with active SL for test execution")
    @Test(groups = {"regression"})
    public void validatePersonalAccessToken() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var tokenName = "Auto-Token" + UniqueRunTag.generate();
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openPersonalAccessTokens();
        var firstSecret = createToken(tokenName, "3");
        var tokens = personalAccessTokensPage.getAllTokens();
        assertThat(tokens).hasSize(1);
        var token = tokens.get(0);

        assertThat(token.getSecreteExpirationDateWarning())
                .as("Warning message should be equal: " + expectedWarningMessage)
                .isEqualTo(expectedWarningMessage);
        assertThat(token.getSecreteExpirationDateWarningIcon().isDisplayed())
                .as("Warning icon should be displayed")
                .isTrue();

        var newSecretPopup = token.pressNewSecret();
        assertThat(newSecretPopup.popupElement.isDisplayed()).isTrue();
        var modal = newSecretPopup.pressCreate();
        var secondSecret = modal.getMessage().split("\n\n")[1].trim();
        assertThat(secondSecret).isNotEqualTo(firstSecret);
        modal.pressClose();
        token.refreshTokenElement().pressDelete().pressYes();
        BrowserUtil.waitAjaxLoaded();
        assertThat(personalAccessTokensPage.getAllTokens()).isEmpty();

        var newSecretKey = createToken(tokenName, "180");

        BrowserUtil.clearCookiesLogOff();
        var loginPage = TenantLoginPage.navigate()
                .setTenantCode(tenantDTO.getTenantCode())
                .setLogin(tenantDTO.getUserName())
                .setPassword(newSecretKey);
        loginPage.pressLoginButton();
        var message = loginPage.getErrorMessage();

        assertThat(loginPage.loginErrorElement.isDisplayed()).isTrue();
        assertThat(message).isNotBlank();

        var topNavBar = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar;
        topNavBar.openApiSwagger();
        validateSwaggerAuthentication(expectedAuthenticationSuccessMessage, newSecretKey);

        var log = topNavBar.openAdministration().openEventLog().getAllLogs().stream()
                .filter(x -> x.getUser().equals(tenantDTO.getUserName()) && x.getType().equals("User Logged In Successfully"))
                .filter(x -> x.getNotes().contains(tokenName)).findFirst().orElse(null);

        assertThat(log).as("Log should be in log event")
                .isNotNull();

        var resultMessage = topNavBar.openAdministration().openSettings().openSecurityTab()
                .set2Factor(true)
                .setEmail(true)
                .setSms(false)
                .pressSave()
                .resultMessage.shouldBe(Condition.visible, Duration.ofSeconds(20)).text();

        assertThat(resultMessage).contains(expectedSettingsSavedMessage);

        validateSwaggerAuthentication(expectedAuthenticationSuccessMessage, newSecretKey);

        resultMessage = topNavBar.openAdministration().openSettings().openSecurityTab()
                .set2Factor(false)
                .pressSave()
                .resultMessage.shouldBe(Condition.visible, Duration.ofSeconds(20)).text();

        assertThat(resultMessage).contains(expectedSettingsSavedMessage);

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        validatePersonalAccessButton(false,
                String.format("for %s with enabled %s setting", tenantDTO.getAssignedUser(), settingName),
                tenantDTO.getAssignedUser(), false);

        topNavBar.openAdministration().openUsers().editUserByName(tenantDTO.getUserName()).setInactive(true)
                .pressSaveBtn();

        validateSwaggerAuthentication(expectedAuthenticationFailedMessage, newSecretKey);
        topNavBar.openAdministration().openUsers().deleteUserByName(tenantDTO.getUserName());
        validateSwaggerAuthentication(expectedMessageForDeletedUser, newSecretKey);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate availability of Personal Access Tokens with disabled AllowPersonalAccessTokens")
    @Test(groups = {"regression"}, dependsOnMethods = {"validatePersonalAccessToken"})
    public void validateAccessTokensButtonWithDisabledSiteSetting() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        SiteSettingsActions.setValueInSettings(settingName, "false", true);
        BrowserUtil.clearCookiesLogOff();
        validatePersonalAccessButton(false,
                String.format("for %s with disabled %s setting", tenantDTO.getUserName(), settingName),
                tenantDTO.getUserName(), true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate availability of Personal Access Tokens with enabled AllowPersonalAccessTokens")
    @Test(groups = {"regression"}, dependsOnMethods = {"validateAccessTokensButtonWithDisabledSiteSetting"})
    public void validateAccessTokensButtonWithEnabledSiteSetting() {
        SiteSettingsActions.setValueInSettings(settingName, "true", true);
        BrowserUtil.clearCookiesLogOff();
        validatePersonalAccessButton(true,
                String.format("for %s with enabled %s setting", tenantDTO.getUserName(), settingName),
                tenantDTO.getUserName(), true);
    }

    @FodBacklogItem("799038")
    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify spaces that are placed at the beginning API Key should not prevent users from logging in")
    @Test(groups = {"regression", "hf"})
    public void validateWhitespaceAcceptanceOfApiKey() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        var apiSettingsPage = new TenantTopNavbar().openAdministration().openSettings().openApiTab();
        apiSecretValue = apiSettingsPage.addKeyAndGetSecret(apiKeyUser, "Read Only", true);
        apiKeyValue = apiSettingsPage.getUserRowByName(apiKeyUser).toMap().get("ApiKey");

        var swaggerPage = new TenantTopNavbar().openApiSwagger();
        swaggerPage.titleElement.shouldBe(Condition.visible, Duration.ofMinutes(1));
        AllureReportUtil.info("Click on authenticate, select grant type as client credentials and provide API Key with " +
                "whitespace at the beginning");
        var authPopup = swaggerPage.pressAuthenticate();
        assertThat(authPopup
                .setGrantType("client_credentials")
                .setScope("api-tenant")
                .setClientId(" " + apiKeyValue)
                .setClientSecret(apiSecretValue)
                .submit()
                .waitAuthComplete()
                .getAuthResult())
                .as("Spaces that are placed at the beginning of API Key should not prevent users from Login")
                .isEqualTo(expectedAuthenticationSuccessMessage);
        authPopup.close();
    }

    private void validateSwaggerAuthentication(String expectedValidationMessage, String token) {
        refresh();
        switchTo().window(1);
        var swaggerPage = page(SwaggerPage.class);
        swaggerPage.titleElement.shouldBe(Condition.visible, Duration.ofMinutes(1));
        var authPopup = swaggerPage.pressAuthenticate();
        var authResultMessage = authPopup
                .setGrantType("password")
                .setScope("api-tenant")
                .setUserName(tenantDTO.getUserName())
                .setPassword(token)
                .setTenant(tenantDTO.getTenantCode())
                .submit()
                .waitAuthComplete()
                .getAuthResult();

        assertThat(authResultMessage).contains(expectedValidationMessage);
        authPopup.close();
        switchTo().window(0);
    }

    private void validatePersonalAccessButton(boolean shouldBe, String additionalMessage, String user,
                                              boolean needLogin) {
        var endTime = LocalTime.now().plusMinutes(7);
        TenantTopNavbar tenantTopNav;
        do {
            if (needLogin)
                tenantTopNav = LogInActions
                        .tenantUserLogIn(user, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                        .tenantTopNavbar;
            else tenantTopNav = new TenantTopNavbar();
            tenantTopNav.userName.click();
            var buttonIsPresent = tenantTopNav.getOpenDropdown()
                    .$(byText(TenantTopNavTabs.PersonalAccessTokens.getTabName())).isDisplayed();
            var validation = shouldBe == buttonIsPresent;
            var expectedMessage = shouldBe ? "should be" : "shouldn't be";

            if (!validation && LocalTime.now().isBefore(endTime)) {
                BrowserUtil.clearCookiesLogOff();
                sleep(Duration.ofMinutes(1).toMillis());
                continue;
            }

            assertThat(validation).as(expectedMessage + " " + additionalMessage).isTrue();
            tenantTopNav.userName.click();
            var accountSettings = tenantTopNav.openAccountSettings();
            validation = accountSettings.getTabByName(TenantSideNavTabs.AccountSettings.PersonalAccessTokens.getTabName())
                    .exists() == shouldBe;
            assertThat(validation)
                    .as("Option " + shouldBe + " " + additionalMessage)
                    .isTrue();
            return;
        } while (LocalTime.now().isBefore(endTime));
        throw new FodUnexpectedConditionsException("Site settings didn't accept.");
    }

    private String createToken(String tokenName, String lifeTimeDays) {
        var personalAccessTokensPage = new TenantTopNavbar().openPersonalAccessTokens();
        var addEditPersonalAccessTokenPopup = personalAccessTokensPage.pressAddAccessToken();

        assertThat(addEditPersonalAccessTokenPopup.popupElement.isDisplayed())
                .as("Popup should be opened")
                .isTrue();

        addEditPersonalAccessTokenPopup.setPersonalAccessTokenName(tokenName);
        addEditPersonalAccessTokenPopup.setSecretExpirationDays(lifeTimeDays);
        addEditPersonalAccessTokenPopup.setAllowedScopesByName("api-tenant", true);
        var modal = addEditPersonalAccessTokenPopup.pressSave();

        var popupIsOpened = modal.modalElement.isDisplayed();
        var secretKey = modal.getMessage().split("\n\n")[1].trim();

        assertThat(popupIsOpened).isTrue();
        assertThat(secretKey).isNotBlank();
        modal.pressClose();
        return secretKey;
    }

    @AfterClass
    public void setDefaultPersonalAccessTokenSetting() {
        setupDriver("setDefaultPersonalAccessTokenSetting");
        SiteSettingsActions.setValueInSettings(settingName, "true", true);
        attachTestArtifacts();
    }
}