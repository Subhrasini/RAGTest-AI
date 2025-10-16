package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.MobileScanResultsDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class ApplicationIssuesExportTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;
    MobileScanResultsDTO mobileScanResultsDTO;
    TenantUserDTO tenantUserDTO;


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Add a user with developer role")
    @Test(groups = {"hf", "regression"})
    public void addUserWithDeveloperRoleTest() {

        tenantUserDTO = TenantUserDTO.createDefaultInstance();
        LogInActions.tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        var usersPage = TenantLoginPage.navigate().tenantTopNavbar.openAdministration().openUsers();
        tenantUserDTO.setRole(FodCustomTypes.TenantUserRole.Developer);
        usersPage.createUser(tenantUserDTO);
        assertThat(usersPage.getUserNames())
                .as("Verify that new user is present in table")
                .contains(tenantUserDTO.getUserName());

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a mobile scan and assign the application to the developer")
    @Test(dependsOnMethods = {"addUserWithDeveloperRoleTest"}, groups = {"hf", "regression"})
    public void startAndCompleteScanTest() {

        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanResultsDTO = MobileScanResultsDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO);

        TenantLoginPage.navigate().tenantTopNavbar.openAdministration().openUsers()
                .pressAssignApplicationsByUser(tenantUserDTO.getUserName()).openAvailableTab()
                .selectAssignAllCheckbox().pressSave();
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions.completeMobileScan(applicationDTO, mobileScanResultsDTO, true);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("557003")
    @Description("Unable to export issues at the Application Issues page")
    @Test(dependsOnMethods = {"startAndCompleteScanTest"}, groups = {"hf", "regression"})
    public void exportIssueTest() {
        LogInActions.tenantUserLogIn(tenantUserDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        var confirmModal = TenantLoginPage.navigate().tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues().pressExportButton();
        confirmModal.pressYes();
        var modalMessage = confirmModal.getMessage();
        assertThat(modalMessage)
                .as("Validate Access Denied message should not appear")
                .doesNotContainIgnoringCase("Access Denied.");

        assertThat(modalMessage)
                .as("Validate expected modal message")
                .containsIgnoringCase("The export has been queued. You will receive an email when it is ready to download.");

        confirmModal.pressClose();
    }

}
