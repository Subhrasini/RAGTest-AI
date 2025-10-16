package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.user_menu.ChangePasswordTenantPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
public class TenantMyAccountTest extends FodBaseTest {
    AdminUserDTO adminUserDTO;
    TenantDTO tenantDTO;
    public String tenantTestFirstName = "TestFirstName";
    public String tenantTestLastName = "TestLastName";
    public String tenantTestPhoneNumber = "+3806388888888";
    public String tenantTestNewPassword = "!7abCDefgQ9x";
    public String tenantChallengeAnswer = "Black";
    public String incorrectPassword = "56789q@zWS123";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a TAM user and tenant from admin site ")
    @Test(groups = {"regression"})
    public void createTamUser() {
        adminUserDTO = AdminUserDTO.createDefaultInstance();
        AdminUserActions.createAdminUser(adminUserDTO, true);
        BrowserUtil.clearCookiesLogOff();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setAssignedUser(adminUserDTO.getUserName());
        TenantActions.createTenant(tenantDTO);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify My account settings page in Tenant site")
    @Test(groups = {"regression"}, dependsOnMethods = {"createTamUser"})
    public void verifyTenantMyAccount() {
        AllureReportUtil.info("Verify the fields in My Account Page in tenant site");
        var myAccountTenantPage = LogInActions.tamUserLogin(adminUserDTO.getUserName(),
                adminUserDTO.getPassword(), tenantDTO.getTenantCode()).tenantTopNavbar.openAccountSettings();
        myAccountTenantPage.setFirstName(tenantTestFirstName)
                .setLastName(tenantTestLastName)
                .setPhoneNumber(tenantTestPhoneNumber)
                .setPasswordChallengeAnswer(tenantChallengeAnswer);
        var dateFormatOptions = myAccountTenantPage.getDateFormatOptions();
        var timeFormatOptions = myAccountTenantPage.getTimeFormatOptions();
        var languageOptions = myAccountTenantPage.getLanguageOptions();
        var passwordQuestionOptions = myAccountTenantPage.getPasswordChallengeQuestions();
        myAccountTenantPage.pressSave();

        assertThat(myAccountTenantPage.getFirstNameValue()).as("First name").isEqualTo(tenantTestFirstName);
        assertThat(myAccountTenantPage.getLastNameValue()).as("Last name").isEqualTo(tenantTestLastName);
        assertThat(myAccountTenantPage.getPhoneNumberValue()).as("Phone Number").isEqualTo(tenantTestPhoneNumber);
        assertThat(myAccountTenantPage.getLanguageValue()).as("Language").isEqualTo("English");
        assertThat(dateFormatOptions).as("Date Format Options has 3 values in the dropdown").hasSize(3);
        assertThat(timeFormatOptions).as("Time Format Options has 2 values in the dropdown").hasSize(2);
        assertThat(languageOptions).as("Language Options has 3 values in the dropdown").hasSize(3);
        assertThat(passwordQuestionOptions).as("Password Challenge Questions Options has 6 values in the dropdown")
                .hasSize(6);
        log.info(myAccountTenantPage.getPasswordChallengeQuestion());
        assertThat(myAccountTenantPage.getPasswordChallengeQuestion()).as("Password Challenge Question")
                .isEqualTo("What is your favorite pet's name?");
        assertThat(myAccountTenantPage.getPasswordChallengeAnswer()).as("Password Challenge answer")
                .isEqualTo(tenantChallengeAnswer);
        assertThat(myAccountTenantPage.getInfoMessage()).as("Info message")
                .isEqualTo("Data has been saved successfully.");

        for (var option : dateFormatOptions) {
            myAccountTenantPage.setDateFormat(option);
            myAccountTenantPage.pressSave();
            refresh();

            assertThat(myAccountTenantPage.getDateFormatValue())
                    .as("Date format")
                    .isEqualTo(option);
        }

        for (var option : timeFormatOptions) {
            myAccountTenantPage.setTimeFormat(option);
            myAccountTenantPage.pressSave();
            refresh();

            assertThat(myAccountTenantPage.getTimeFormatValue())
                    .as("Time format")
                    .isEqualTo(option);
        }
        for (var option : languageOptions) {
            myAccountTenantPage.setLanguage(option);
            myAccountTenantPage.pressSave();
            refresh();
            myAccountTenantPage.spinner.waitTillLoading();

            assertThat(myAccountTenantPage.getLanguageValue())
                    .as("Language :" + myAccountTenantPage.getLanguageValue())
                    .isEqualTo(option);
            myAccountTenantPage.setLanguage("English");
            myAccountTenantPage.pressSave();
            refresh();
        }
        for (var option : passwordQuestionOptions) {
            myAccountTenantPage.setPasswordChallengeQuestion(option);
            myAccountTenantPage.pressSave();
            refresh();
            myAccountTenantPage.spinner.waitTillLoading();

            assertThat(myAccountTenantPage.getPasswordChallengeQuestion())
                    .as("Password Challenge Questions")
                    .isEqualTo(option);
            myAccountTenantPage.setPasswordChallengeQuestion("What is your favorite pet's name?");
            myAccountTenantPage.pressSave();
            refresh();
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify change password page of tenant site")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyTenantMyAccount"})
    public void verifyChangePassword() {
        String oldPasswordValidation = "The Old Password field is required.";
        String newPasswordValidation = "The New Password field is required.";
        String confirmPasswordValidation = "Confirm New Password' and 'New Password' do not match.";
        String wrongPasswordValidation = "The previous password does not match.";
        String passwordIsChanged = "Your password has been changed.";

        AllureReportUtil.info("Verify change password page in tenant site");
        var myAccountPage = LogInActions.tamUserLogin(adminUserDTO.getUserName(),
                adminUserDTO.getPassword(), tenantDTO.getTenantCode()).tenantTopNavbar.openAccountSettings();
        var changePasswordTenantPage = page(ChangePasswordTenantPage.class);
        myAccountPage.pressChangePassword();

        changePasswordTenantPage.setOldPass(adminUserDTO.getPassword());
        changePasswordTenantPage.setNewPass(tenantTestNewPassword);
        changePasswordTenantPage.setConfirmPass(tenantTestNewPassword);
        changePasswordTenantPage.pressBackToAccountBtn();
        Assert.assertTrue(myAccountPage.pageTitleIsVisible());
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(adminUserDTO.getUserName(), adminUserDTO.getPassword(), tenantDTO.getTenantCode())
                .tenantTopNavbar.openAccountSettings();
        myAccountPage.pressChangePassword();
        changePasswordTenantPage.pressOkBtn();

        assertThat(changePasswordTenantPage.getValidationErrorMessage())
                .as("Old password required message should be displayed")
                .contains(oldPasswordValidation, newPasswordValidation);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        changePassword(adminUserDTO.getPassword(), "", "");
        assertThat(changePasswordTenantPage.getValidationErrorMessage())
                .as("New password required message should be displayed")
                .contains(newPasswordValidation);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        changePassword(adminUserDTO.getPassword(), tenantTestNewPassword, incorrectPassword);
        assertThat(changePasswordTenantPage.getValidationErrorMessage())
                .as("New Password don't match message should be displayed")
                .contains(confirmPasswordValidation);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        changePassword(incorrectPassword, tenantTestNewPassword, tenantTestNewPassword);
        assertThat(changePasswordTenantPage.getValidationErrorMessage())
                .as("Previous password doesn't match error message should be displayed")
                .contains(wrongPasswordValidation);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        changePassword(adminUserDTO.getPassword(), tenantTestNewPassword, tenantTestNewPassword);
        assertThat(changePasswordTenantPage.getInfoMessage())
                .as("Password has been changed message should be displayed")
                .contains(passwordIsChanged);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(adminUserDTO.getUserName(), tenantTestNewPassword, tenantDTO.getTenantCode())
                .tenantTopNavbar.openAccountSettings();
        assertThat(myAccountPage.pageTitleIsVisible())
                .as("User should successfully logged in with new password")
                .isTrue();
    }

    public void changePassword(String oldPass, String newPass, String confirmPass) {
        var changePass = page(ChangePasswordTenantPage.class);
        if (!oldPass.equals(""))
            changePass.setOldPass(oldPass);
        if (!newPass.equals(""))
            changePass.setNewPass(newPass);
        if (!confirmPass.equals(""))
            changePass.setConfirmPass(confirmPass);
        changePass.pressOkBtn();
    }
}
