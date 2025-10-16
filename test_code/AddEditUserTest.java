package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.elements.SearchBox;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.ResetPasswordPage;
import com.fortify.fod.ui.pages.admin.administration.users.AdminUsersPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.Ordering;
import com.fortify.fod.ui.test.actions.PagingActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.ArrayList;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AddEditUserTest extends FodBaseTest {

    AdminUserDTO admin, staticOperator, dynamicTester, tam, mobileTester, staticManager,
            dynamicManager, mobileManager, staticAuditor, tamManager, seniorTam, operations;
    ArrayList<AdminUserDTO> users;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create all users types")
    @Test(groups = {"regression"})
    public void addUsersTest() {
        users = new ArrayList<>();
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

        AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        AdminUserActions.createAdminUsers(admin, staticOperator, dynamicTester, tam, mobileTester, staticManager,
                dynamicManager, mobileManager, staticAuditor, tamManager, seniorTam, operations);
        users.add(admin);
        users.add(staticOperator);
        users.add(dynamicTester);
        users.add(tam);
        users.add(mobileTester);
        users.add(staticManager);
        users.add(dynamicManager);
        users.add(mobileManager);
        users.add(staticAuditor);
        users.add(tamManager);
        users.add(seniorTam);
        users.add(operations);
    }

    @MaxRetryCount(1)
    @FodBacklogItem("639005")
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create all users types")
    @Test(groups = {"regression"}, dependsOnMethods = {"addUsersTest"})
    public void editUsersTest() {
        AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openUsers();

        var paging = new Paging();
        var usersPage = page(AdminUsersPage.class);

        var usersActualCount = usersPage.getAllUsers().size();
        var usersTotalCount = paging.getTotalRecords();

        assertThat(usersTotalCount)
                .as("Users count should be equal to users total count")
                .isEqualTo(usersActualCount);

        //Validate Search for all users from test data.
        for (var user : users) {
            usersPage.findUserByName(user.getUserName());
            usersPage.getAllUsers().get(0).getName();
            assertThat(usersPage.getAllUsers().get(0).getName())
                    .as("User name should be equal to: " + user.getUserName())
                    .isEqualTo(user.getUserName());
            assertThat(usersPage.getUsersTable().rows.size())
                    .as("There should be only one user row")
                    .isEqualTo(1);
        }

        //Ordering validation
        new SearchBox().searchFor("");
        paging.setRecordsPerPage(1000);
        var ordering = new Ordering(usersPage.getUsersTable());

        ordering.verifyOrderForColumn("User Name");
        ordering.verifyOrderForColumn("First Name");
        ordering.verifyOrderForColumn("Last Name");
        ordering.verifyOrderForColumn("Email");
        ordering.verifyOrderForColumn("Mobile Number");
        ordering.verifyOrderForColumn("Role");
        ordering.verifyOrderForColumn("Country");
        ordering.verifyOrderForColumn("Status");

        //Paging validation
        new PagingActions().validatePaging();

        //Pagination Validation
        new PagingActions().validatePagination();

        var user1 = users.get(1);
        var user2 = users.get(2);
        var newPass = "Test!123456789";

        usersPage.editUserByName(user1.getUserName())
                .setPassword(newPass)
                .setConfirmPassword(newPass)
                .setInactive(true)
                .pressSaveBtn();

        usersPage.editUserByName(user2.getUserName())
                .setPassword(newPass)
                .setConfirmPassword(newPass)
                .setMustChangeOnNextLogin(true)
                .pressSaveBtn();

        BrowserUtil.clearCookiesLogOff();
        AdminLoginPage.navigate()
                .login(user1.getUserName(), newPass);

        var loginPage = page(AdminLoginPage.class);
        assertThat(loginPage.getErrorMessage())
                .as("Inactive user wasn't logged in.")
                .isEqualTo("The username or password was incorrect.");

        AdminLoginPage.navigate()
                .login(user2.getUserName(), newPass);

        var resetPasswordPage = page(ResetPasswordPage.class);
        assertThat(resetPasswordPage.isOpen())
                .as("Reset password page should be opened")
                .isTrue();

        var changedPass = "Test!9876543210";
        var expectedInfoMessage = "Your password was successfully reset.";
        resetPasswordPage.doChangePass(changedPass);
        assertThat(resetPasswordPage.getInfoMessage())
                .as("Info message should be visible and equal: " + expectedInfoMessage)
                .isEqualTo(expectedInfoMessage);
        resetPasswordPage.pressLogin();

        var userName = AdminLoginPage.navigate()
                .login(user2.getUserName(), changedPass)
                .adminTopNavbar.userName;

        assertThat(userName.isDisplayed())
                .as("User should be logged on")
                .isTrue();
    }
}
