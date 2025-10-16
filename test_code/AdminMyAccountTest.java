package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.user_menu.ChangePasswordPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AdminMyAccountTest extends FodBaseTest {
    public AdminUserDTO admin, admin2;

    public String testsFirstName;
    public String testsLastName;
    public String testsEmail;
    public String testsPhoneNumber;
    public String newPassword;

    public void initTestObjects() {
        admin = AdminUserDTO.createDefaultInstance();
        testsFirstName = "TestFirstName";
        testsLastName = "TestLastName";
        testsEmail = admin.getUserName() + "@email.com";
        testsPhoneNumber = "+3806377777777";
        newPassword = "!1jtDgQ4kQ9x";
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Delivery my account")
    @Test(groups = {"regression"},
            suiteName = "Admin My Account Test")

    public void deliveryMyAccount() {
        initTestObjects();
        AdminUserActions.createAdminUser(admin, true);
        BrowserUtil.clearCookiesLogOff();

        var myAccountPage = AdminLoginPage.navigate()
                .login(admin.getUserName(), admin.getPassword())
                .adminTopNavbar.openMyAccount();

        SoftAssertions soft = new SoftAssertions();
        myAccountPage.setFirstName(testsFirstName)
                .setLastName(testsLastName)
                .setEmail(testsEmail)
                .setPhoneNumber(testsPhoneNumber);

        var dateFormatOptions = myAccountPage.getDateFormatOptions();
        var timeFormatOptions = myAccountPage.getTimeFormatOptions();
        var languageOptions = myAccountPage.getLanguageOptions();

        myAccountPage.pressSave();

        soft.assertThat(dateFormatOptions).as("Date Format Options").hasSize(3);
        soft.assertThat(timeFormatOptions).as("Time Format Options").hasSize(2);
        soft.assertThat(languageOptions).as("Language Options").hasSize(3);
        soft.assertThat(myAccountPage.getInfoMessage()).as("Info message")
                .isEqualTo("Data has been saved successfully.");
        soft.assertThat(myAccountPage.getFirstNameValue()).as("First name").isEqualTo(testsFirstName);
        soft.assertThat(myAccountPage.getLastNameValue()).as("Last name").isEqualTo(testsLastName);
        soft.assertThat(myAccountPage.getEmailValue()).as("Email").isEqualTo(testsEmail);
        soft.assertThat(myAccountPage.getPhoneNumberValue()).as("Phone").isEqualTo(testsPhoneNumber);
        soft.assertAll();

        for (var option : dateFormatOptions) {
            myAccountPage.setDateFormat(option);
            myAccountPage.pressSave();
            refresh();

            soft.assertThat(myAccountPage.getDateFormatValue())
                    .as("Date format")
                    .isEqualTo(option);
        }

        soft.assertAll();

        for (var option : timeFormatOptions) {
            myAccountPage.setTimeFormat(option);
            myAccountPage.pressSave();
            refresh();

            soft.assertThat(myAccountPage.getTimeFormatValue())
                    .as("Time format")
                    .isEqualTo(option);
        }

        soft.assertAll();

        for (var option : languageOptions) {
            myAccountPage.setLanguage(option);
            myAccountPage.pressSave();
            refresh();
            myAccountPage.spinner.waitTillLoading();

            assertThat(myAccountPage.getLanguageValue())
                    .as("Language = " + myAccountPage.getLanguageValue())
                    .isEqualTo(option);

            String expectedTitle;
            switch (option) {
                case "Spanish":
                    expectedTitle = "Mi cuenta";
                    break;
                case "Japanese":
                    expectedTitle = "自分のアカウント";
                    break;
                default:
                    expectedTitle = "My Account";
                    break;
            }

            soft.assertThat(myAccountPage.getPageTitle())
                    .as("Validate Title")
                    .isEqualTo(expectedTitle);
        }

        myAccountPage.setLanguage("English");
        myAccountPage.pressSave();
        refresh();

        assertThat(myAccountPage.getPageTitle()).as("Title").isEqualTo("My Account");
        soft.assertAll();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Change password")
    @Test(groups = {"regression"},
            suiteName = "Admin My Account Test",
            alwaysRun = true)

    public void changePassword() {
        String oldPassValidation = "The Old Password field is required.";
        String newPassValidation = "The New Password field is required.";
        String confirmPassValidation = "Confirm New Password' and 'New Password' do not match.";
        String wrongPassValidation = "The previous password does not match.";
        String passIsChanged = "Your password has been changed.";
        var incorrectPassword = "12345q@zWS123";
        newPassword = "!1jtDgQ4kQ9x";

        admin2 = AdminUserDTO.createDefaultInstance();
        AdminUserActions.createAdminUser(admin2, true);
        BrowserUtil.clearCookiesLogOff();

        var myAccountPage = LogInActions.adminUserLogIn(admin2)
                .adminTopNavbar.openMyAccount();

        var changePasswordPage = myAccountPage.pressChangePassword();

        changePasswordPage.setOldPass(FodConfig.ADMIN_PASSWORD);
        changePasswordPage.setNewPass(newPassword);
        changePasswordPage.setConfirmPass(newPassword);
        changePasswordPage.pressBackToAccountBtn();

        assertThat(myAccountPage.pageTitleIsVisible()).as("Title should be displayed").isTrue();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminUserLogIn(admin2)
                .adminTopNavbar.openMyAccount();
        assertThat(myAccountPage.pageTitleIsVisible()).as("Title should be displayed").isTrue();

        myAccountPage.pressChangePassword();
        changePasswordPage.pressOkBtn();
        assertThat(changePasswordPage.getValidationErrorMessage())
                .as("Save new password")
                .contains(oldPassValidation, newPassValidation);

        changePassword(FodConfig.ADMIN_PASSWORD, "", "");
        assertThat(changePasswordPage.getValidationErrorMessage())
                .as("Error message")
                .contains(newPassValidation);

        changePassword(FodConfig.ADMIN_PASSWORD, newPassword, incorrectPassword);
        assertThat(changePasswordPage.getValidationErrorMessage())
                .as("Error message")
                .contains(confirmPassValidation);

        changePassword(incorrectPassword, newPassword, newPassword);
        assertThat(changePasswordPage.getValidationErrorMessage())
                .as("Error message")
                .contains(wrongPassValidation);

        changePassword(FodConfig.ADMIN_PASSWORD, newPassword, newPassword);
        assertThat(changePasswordPage.getInfoMessage())
                .as("Info message")
                .contains(passIsChanged);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminUserLogIn(admin2.getUserName(), newPassword)
                .adminTopNavbar.openMyAccount();
        assertThat(myAccountPage.pageTitleIsVisible()).as("Title should be displayed").isTrue();

        admin2.setPassword(newPassword);
    }

    private void changePassword(String oldPass, String newPass, String confirmPass) {
        var changePass = page(ChangePasswordPage.class);
        if (!oldPass.equals(""))
            changePass.setOldPass(oldPass);
        if (!newPass.equals(""))
            changePass.setNewPass(newPass);
        if (!confirmPass.equals(""))
            changePass.setConfirmPass(confirmPass);
        changePass.pressOkBtn();
    }
}