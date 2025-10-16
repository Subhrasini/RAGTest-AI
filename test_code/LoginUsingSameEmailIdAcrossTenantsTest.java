package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.ResetPasswordPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.user_menu.ChangePasswordTenantPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("603017")
@FodBacklogItem("762012")
@Slf4j
public class LoginUsingSameEmailIdAcrossTenantsTest extends FodBaseTest {
    TenantDTO tenantDTO_A, tenantDTO_B;
    String userName1, userName2;
    String targetToList;
    String targetSubject = "Reset your Password!";
    String newPassword = "Spi!pass007^2";
    String changedPassword = "!7abCDefgQ9x";
    String expectedInfoBlockMessage = "If you have an account on this site, you will receive an email with " +
            "instructions on how to reset your password. If you do not receive an email (and are certain you have " +
            "an account registered to the email address provided), please check your spam or junk folder " +
            "for the email from fod-dev-notifications@hpe.com";
    String expectedResetPasswordStatusMessage = "Your password was successfully reset.";
    String passwordChangedMessage = "Your password has been changed.";
    String expectedSettingsSavedMessage = "Changes were saved successfully.";

    String firstLink = "";

    String firstAttemptCode = "";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenants from admin site")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Create two tenants , " +
                "tenantA and tenantB Using Same  EmailID");
        tenantDTO_A = TenantDTO.createDefaultInstance();
        this.targetToList = tenantDTO_A.getUserEmail();
        tenantDTO_B = TenantDTO.createDefaultInstance();
        tenantDTO_B.setUserEmail(targetToList);
        TenantActions.createTenant(tenantDTO_A, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO_A, true);
        BrowserUtil.clearCookiesLogOff();
        TenantActions.createTenant(tenantDTO_B, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO_B, true);
        userName1 = tenantDTO_A.getUserName();
        userName2 = tenantDTO_B.getUserName();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify forget password scenario " +
            "for user1 using same email Id")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"})
    public void forgetPasswordForUser1Test() {
        firstLink = forgetPasswordTest(userName1, tenantDTO_A, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify forget password scenario" +
            " for user2 using same email Id")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "forgetPasswordForUser1Test"})
    public void forgetPasswordForUser2Test() {
        forgetPasswordTest(userName2, tenantDTO_B, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify change password scenario for User1 and User2")
    @Test(groups = {"regression"},
            dependsOnMethods = {"forgetPasswordForUser2Test"},
            dataProvider = "verifyUsersData")
    public void changePasswordTest(String userName, TenantDTO tenantDTO) {
        AllureReportUtil.info("Verify change password page in tenant site");
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(),
                        newPassword, tenantDTO.getTenantCode())
                .tenantTopNavbar.openAccountSettings().pressChangePassword();
        assertThat(page(ChangePasswordTenantPage.class).setOldPass(newPassword)
                .setNewPass(changedPassword)
                .setConfirmPass(changedPassword)
                .pressOkBtn().getInfoMessage())
                .as("Password has been changed message should be displayed")
                .contains(passwordChangedMessage);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Verify user should be authenticated using changed password");
        LogInActions.tenantUserLogIn(userName, changedPassword, tenantDTO.getTenantCode());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify two factor authentication scenario for User1")
    @Test(groups = {"regression"}, dependsOnMethods = {"changePasswordTest"})
    public void twoFactorAuthenticationForUser1Test() {
        twoFactorAuthenticationTest(userName1, tenantDTO_A);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify two factor authentication scenario for User2")
    @Test(groups = {"regression"},
            dependsOnMethods = {"changePasswordTest", "twoFactorAuthenticationForUser1Test"})
    public void twoFactorAuthenticationForUser2Test() {
        twoFactorAuthenticationTest(userName2, tenantDTO_B);
    }

    public void twoFactorAuthenticationTest(String userName, TenantDTO tenantDTO) {
        assertThat(LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openAdministration().openSettings().openSecurityTab().set2Factor(true)
                .setEmail(true)
                .setSms(false)
                .pressSave()
                .resultMessage
                .shouldBe(Condition.visible, Duration.ofSeconds(20))
                .text())
                .as("User should be able to see Settings saved message")
                .contains(expectedSettingsSavedMessage);
        BrowserUtil.clearCookiesLogOff();

        Supplier<Boolean> login = () -> {
            var fod2FASecurityCodePopup = TenantLoginPage.navigate()
                    .loginAsTenantUser(userName, changedPassword, tenantDTO.getTenantCode())
                    .selectSecurityCodeDeliveryMethod("Email")
                    .clickOnRequestButton();
            AtomicReference<String> newCode = new AtomicReference<>("");

            WaitUtil.waitForTrue(() -> {
                        var code = getCode();
                        if (code.equals(firstAttemptCode) || code.isEmpty()) {
                            newCode.set("");
                            return false;
                        } else {
                            newCode.set(code);
                            return true;
                        }
                    },
                    Duration.ofMinutes(3), false);

            firstAttemptCode = newCode.get();
            fod2FASecurityCodePopup.setSecurityCode(newCode.get().substring(0, 6)).clickOnSubmitButton();
            WaitUtil.waitFor(WaitUtil.Operator.Equals,
                    true, new TenantTopNavbar().userName::isDisplayed,
                    Duration.ofSeconds(15), true);

            return new TenantTopNavbar().userName.isDisplayed();
        };

        WaitUtil.waitForTrue(login, Duration.ofMinutes(10), false);
        assertThat(new TenantTopNavbar().userName.exists())
                .as("User should be able to logged in!")
                .isTrue();
    }

    public String forgetPasswordTest(String userName, TenantDTO tenantDTO, boolean verifyLink) {
        assertThat(TenantLoginPage
                .navigate()
                .clickOnForgotYourPasswordLink()
                .fillFormAndPressSubmit(userName, tenantDTO.getTenantCode())
                .getInfoBlockMessage())
                .as("User should be able to see expected info" +
                        " block message after clicking on submit button")
                .contains(expectedInfoBlockMessage);
        Supplier<String> sup = () -> {
            try {
                var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
                Document html = Jsoup.parse(mailBody);
                return html.selectXpath("//*[@href][contains(text(), 'Reset your Password')]")
                        .attr("href");
            } catch (Exception | Error e) {
                log.error(e.getMessage());
                return "";
            }
        };
        WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual, "",
                sup, Duration.ofMinutes(2), false);
        String link = sup.get();

        if (verifyLink) {
            WaitUtil.waitForTrue(() -> !sup.get().equals(firstLink), Duration.ofMinutes(3), false);
            link = sup.get();
        }
        log.info(link);
        assertThat(open(link, ResetPasswordPage.class)
                .fillFormAndPressOk(newPassword, "some answer")
                .getResetPasswordStatusMessage())
                .as("Verify expected reset password status message")
                .contains(expectedResetPasswordStatusMessage);
        AllureReportUtil.info("Verify user should be authenticated after reset password");
        LogInActions.tenantUserLogIn(userName, newPassword, tenantDTO.getTenantCode());
        return link;
    }

    String getCode() {
        try {
            var mailBody = new MailUtil()
                    .findEmailByRecipientAndSubject("Your OpenText™ Core Application Security security code:", targetToList);
            Document html = Jsoup.parse(mailBody);
            return html.selectXpath("//p[1]").text().trim().replaceAll("[^0-9]", "");
        } catch (Exception | Error e) {
            log.error(e.getMessage());
            return "";
        }
    }

    @DataProvider(name = "verifyUsersData", parallel = true)
    public Object[][] verifyUsersData() {
        return new Object[][]{
                {userName1, tenantDTO_A},
                {userName2, tenantDTO_B}
        };
    }
}
