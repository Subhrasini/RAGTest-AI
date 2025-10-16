package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.administration.configuration.SiteSettingsPage;
import com.fortify.fod.ui.pages.admin.administration.users.AdminUsersPage;
import com.fortify.fod.ui.pages.admin.user_menu.MyAccountAdminPage;
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
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class UpdateEmailAddressTest extends FodBaseTest {

    private String EmailAlreadyExistsMsg = "The email address already exists";
    private String InvalidEmailMsg = "The 'Email' is invalid. Emails from free providers cannot be registered.";
    private String InvalidEmail = "test@mail.ru";
    private String DomainForBlackList = "@mail.ru";
    private String SuccessMsg = "Data has been saved successfully.";
    private String defaultTenantSlEmail = "AUTO-SL@fod.auto";

    AdminUserDTO admin, staticOperator, dynamicTester, tam, staticManager, mobileTester,
            dynamicManager, mobileManager, staticAuditor, tamManager, seniorTam, operations;

    public void prepareTestData() {
        admin = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.Admin);
        staticOperator = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticOperator);
        dynamicTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicTester);
        tam = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAM);
        mobileTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.MobileTester);
        staticManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticManager);
        dynamicManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicManager);
        mobileManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.MobileManager);
        staticAuditor = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticAuditor);
        tamManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAMManager);
        seniorTam = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.SeniorTAM);
        operations = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.Operations);

        LogInActions.adminLogIn();
        AdminUserActions.createAdminUsers(admin, staticOperator, dynamicTester, tam, mobileTester, staticManager,
                dynamicManager, mobileManager, staticAuditor, tamManager, seniorTam, operations);
        BrowserUtil.clearCookiesLogOff();
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(1)
    @Description("Admin user should be able to add email excludes and configure email addresses")
    @Test(groups = {"regression"})
    public void updateEmailAddressTest() {
        prepareTestData();

        LogInActions.adminLogIn()
                .adminTopNavbar
                .openConfiguration();

        AllureReportUtil.info("Creating black list email value");
        var siteSettingsPage = page(SiteSettingsPage.class);
        var settingsPopup = siteSettingsPage.openSettingByName("EmailBlacklist");
        var oldValue = settingsPopup.getSettingValue();
        var newBlackList = DomainForBlackList + ";" + oldValue;

        settingsPopup.setValue(newBlackList).save();

        AllureReportUtil.info("Check email which contains black list domain");
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminUserLogIn(admin)
                .adminTopNavbar
                .openMyAccount();

        var myAccountPage = page(MyAccountAdminPage.class);
        var message = myAccountPage.setEmail(InvalidEmail).pressSave().getErrorMessages();

        assertThat(message).as("Email from black list domain shouldn't be applied")
                .contains(InvalidEmailMsg);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        myAccountPage.adminTopNavbar.openUsers();
        var usersPage = page(AdminUsersPage.class);
        var addEditUserPopup = usersPage.editUserByName(tam.getUserName());
        addEditUserPopup.setEmailField(InvalidEmail).pressSaveBtn();
        assertThat(addEditUserPopup.getErrorsList()).as("Email from black list domain shouldn't be applied")
                .contains(InvalidEmailMsg);

        addEditUserPopup.pressCloseBtn();
        usersPage.pressAddUserBtn()
                .setEmailField(InvalidEmail)
                .setUserName("TestUser")
                .setFirstName("TestName")
                .setLastName("TestLastName")
                .setMobileNumber("+380990007658")
                .selectCountry("Albania")
                .selectRole(FodCustomTypes.AdminUserRole.Admin)
                .pressSaveBtn();

        assertThat(addEditUserPopup.getErrorsList()).as("Email from black list domain shouldn't be applied")
                .contains(InvalidEmailMsg);

        addEditUserPopup.pressCloseBtn();
        AllureReportUtil.info("Check creation new user and updating email with already exists email");
        AllureReportUtil.info("Case with creating user with already exists email");

        usersPage.pressAddUserBtn()
                .setEmailField(admin.getUserEmail())
                .setUserName("TestUser")
                .setFirstName("TestName")
                .setLastName("TestLastName")
                .setMobileNumber("+380990007658")
                .selectCountry("Albania")
                .selectRole(FodCustomTypes.AdminUserRole.Admin)
                .pressSaveBtn();

        assertThat(addEditUserPopup.getErrorsList()).as("Existing email shouldn't be applied to new user")
                .contains(EmailAlreadyExistsMsg);

        addEditUserPopup.pressCloseBtn();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Test change email on already exists from my account");

        LogInActions.adminUserLogIn(seniorTam)
                .adminTopNavbar
                .openMyAccount();

        myAccountPage.setEmail(admin.getUserEmail()).pressSave();
        message = myAccountPage.setEmail(tam.getUserEmail()).pressSave().getErrorMessages();

        assertThat(message).as("Existing Email shouldn't be applied").contains(EmailAlreadyExistsMsg);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Check Email of inactive user");
        LogInActions.adminUserLogIn(admin)
                .adminTopNavbar
                .openUsers();

        usersPage.editUserByName(seniorTam.getUserName()).setInactive(true).pressSaveBtn();
        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(tamManager)
                .adminTopNavbar
                .openMyAccount();

        message = myAccountPage.setEmail(seniorTam.getUserEmail()).pressSave().getErrorMessages();
        assertThat(message).as("Existing Email of inactive user shouldn't be applied")
                .contains(EmailAlreadyExistsMsg);
        assertThat(myAccountPage.getValidationErrorsElem().isDisplayed())
                .as("Message should be displayed").isTrue();

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Check that all user has possibility to change email");

        LogInActions.adminUserLogIn(admin)
                .adminTopNavbar
                .openUsers();

        usersPage.editUserByName(seniorTam.getUserName()).setInactive(false).pressSaveBtn();

        changeUserEmailValidation(admin, staticOperator, dynamicTester, tam, mobileTester, staticManager,
                dynamicManager, mobileManager, staticAuditor, tamManager, seniorTam, operations);

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Use email of Sec Lead from created tenant 'AUTO-TENANT'");

        LogInActions.adminUserLogIn(mobileManager)
                .adminTopNavbar
                .openMyAccount();

        myAccountPage.setEmail(defaultTenantSlEmail).pressSave();
        assertThat(message).as("Existing Email of tenant SL user shouldn't be applied")
                .contains(EmailAlreadyExistsMsg);
        assertThat(myAccountPage.getValidationErrorsElem()
                .isDisplayed()).as("Message should be displayed").isTrue();

        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Delete user and use his email for another user");

        LogInActions.adminUserLogIn(admin)
                .adminTopNavbar
                .openUsers();

        usersPage.pressDeleteUserByName(operations.getUserName()).clickButtonByText("Yes");
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminUserLogIn(mobileTester)
                .adminTopNavbar
                .openMyAccount();

        var successMessage = myAccountPage.setEmail(operations.getUserEmail()).pressSave().getInfoMessage();

        assertThat(successMessage).as("Email of deleted user should be applied").isEqualTo(SuccessMsg);
        assertThat(myAccountPage.getInfoBlockElem().isDisplayed()).as("Message should be displayed").isTrue();
    }

    private void changeUserEmailValidation(AdminUserDTO... userDTOS) {
        SoftAssertions softAssertions = new SoftAssertions();
        for (var user : userDTOS) {
            BrowserUtil.clearCookiesLogOff();
            AdminLoginPage.navigate().login(user)
                    .adminTopNavbar
                    .openMyAccount();
            var myAccountPage = page(MyAccountAdminPage.class);
            var newEmail = user.getUserName() + "@opentext.com";
            user.setUserEmail(newEmail);
            var message = myAccountPage.setEmail(newEmail)
                    .pressSave().getInfoMessage();
            softAssertions.assertThat(message).as("New email should be applied")
                    .isEqualTo(SuccessMsg);
            softAssertions.assertThat(myAccountPage.getInfoBlockElem().isDisplayed())
                    .as("Message should be displayed")
                    .isTrue();
        }
        softAssertions.assertAll();
    }
}
