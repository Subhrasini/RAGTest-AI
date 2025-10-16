package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.exceptions.FodElementNotCreatedException;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.mobile.mobile_scan_details.tasks.ManualTestingPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class MobileTaskAndDeletionTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;
    AdminUserDTO mobileManager;
    AdminUserDTO mobileOperator;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare mobile application and mobile scan")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileManager = AdminUserDTO.createDefaultInstance();
        mobileManager.setRole(FodCustomTypes.AdminUserRole.MobileManager);
        mobileManager.setTenant(defaultTenantDTO.getTenantCode());
        mobileOperator = AdminUserDTO.createDefaultInstance();
        mobileOperator.setRole(FodCustomTypes.AdminUserRole.MobileTester);
        mobileOperator.setTenant(defaultTenantDTO.getTenantCode());
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        AdminUserActions.createAdminUsers(mobileManager, mobileOperator);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign mobile scan to Mobile Tester User from monitor tab")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            enabled = false)
    public void assignMonitorMobileTasks() {
        assignMobileTasks(Phase.Monitor);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Process monitor tasks by Mobile Tester")
    @Test(groups = {"regression"}, dependsOnMethods = {"assignMonitorMobileTasks"},
            enabled = false)
    public void workMonitorMobileTasks() {
        workMobileTasksTest(Phase.Monitor);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign mobile scan to Mobile Tester User from manual tab")
    @Test(groups = {"regression"}, dependsOnMethods = {"workMonitorMobileTasks"},
            enabled = false)
    public void assignManualMobileTasks() {
        assignMobileTasks(Phase.Manual);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Process manual tasks by Mobile Tester")
    @Test(groups = {"regression"}, dependsOnMethods = {"assignManualMobileTasks"},
            enabled = false)
    public void workManualMobileTasks() {
        workMobileTasksTest(Phase.Manual);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign mobile scan to Mobile Tester User from audit tab")
    @Test(groups = {"regression"}, dependsOnMethods = {"workManualMobileTasks"},
            enabled = false)
    public void assignAuditMobileTasks() {
        assignMobileTasks(Phase.Audit);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Process audit tasks by Mobile Tester")
    @Test(groups = {"regression"}, dependsOnMethods = {"assignAuditMobileTasks"},
            enabled = false)
    public void workManualAuditTasks() {
        workMobileTasksTest(Phase.Audit);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Delete Users")
    @Test(groups = {"regression"}, dependsOnMethods = {"workManualAuditTasks"},
            enabled = false)
    public void deleteUsers() {
        LogInActions.adminLogIn();
        AdminUserActions.deleteUser(mobileManager, mobileOperator);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Delete Application")
    @Test(groups = {"regression"}, dependsOnMethods = {"deleteUsers"},
            enabled = false)
    public void deleteApplication() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationActions.purgeApplication(applicationDTO);
    }

    public void assignMobileTasks(Phase phase) {
        var mobilePage = AdminLoginPage.navigate().login(mobileManager).adminTopNavbar.openMobile();
        String userName;
        switch (phase) {
            case Audit:
                mobilePage.openAuditTab().findWithSearchBox(applicationDTO.getApplicationName());
                var auditTask = mobilePage.getAuditTaskByAppDto(applicationDTO);
                if (auditTask == null) throw new FodElementNotCreatedException("Audit task not found");

                auditTask.pressAssignButton()
                        .getUserByDto(mobileOperator)
                        .clickAssign()
                        .pressSave();

                userName = mobilePage.getAuditTaskByAppDto(applicationDTO).getUserName();
                break;
            case Manual:
                mobilePage.openManualTab().findWithSearchBox(applicationDTO.getApplicationName());
                var manualTask = mobilePage.getManualTaskByAppDto(applicationDTO);
                if (manualTask == null) throw new FodElementNotCreatedException("Manual task not found");

                manualTask.pressAssignButton()
                        .getUserByDto(mobileOperator)
                        .clickAssign()
                        .pressSave();

                userName = mobilePage.getManualTaskByAppDto(applicationDTO).getUserName();
                break;
            case Monitor:
                mobilePage.openMonitorTab().findWithSearchBox(applicationDTO.getApplicationName());

                var monitorTask = mobilePage.getMonitorTaskByAppDto(applicationDTO);
                if (monitorTask == null) throw new FodElementNotCreatedException("Monitor task not found");

                monitorTask.pressAssignButton()
                        .getUserByDto(mobileOperator)
                        .clickAssign()
                        .pressSave();

                userName = mobilePage.getMonitorTaskByAppDto(applicationDTO).getUserName();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + phase);
        }

        assertThat(userName)
                .as("User should be assigned to mobile %s task", phase)
                .isEqualTo(mobileOperator.getFirstName() + " " + mobileOperator.getLastName());
    }

    public void workMobileTasksTest(Phase phase) {
        var mobilePage = AdminLoginPage.navigate().login(mobileOperator).adminTopNavbar.openMobile();
        ManualTestingPage manualTestingPage;
        boolean isCompleted = false;
        switch (phase) {
            case Audit:
                manualTestingPage = mobilePage.getAuditTaskByAppDto(applicationDTO)
                        .openScanDetails()
                        .openManualTesting();
                isCompleted = manualTestingPage.completeAllStepsAndTasks(false).isWorkflowCompleted();
                assertThat(isCompleted)
                        .as("Workflow should be competed")
                        .isTrue();
                manualTestingPage.adminTopNavbar.openMobile().openAuditTab().getAuditTaskByAppDto(applicationDTO)
                        .pressPublish().pressOk().findWithSearchBox(applicationDTO.getApplicationName());

                assertThat(Table.isEmpty()).as("Scan should be moved from table").isTrue();
                break;
            case Manual:
                manualTestingPage = mobilePage.getManualTaskByAppDto(applicationDTO)
                        .openScanDetails()
                        .openManualTesting();
                isCompleted = manualTestingPage.completeAllStepsAndTasks(false).isWorkflowCompleted();
                assertThat(isCompleted)
                        .as("Workflow should be competed")
                        .isTrue();
                manualTestingPage.adminTopNavbar.openMobile().openManualTab().getManualTaskByAppDto(applicationDTO)
                        .pressDone().findWithSearchBox(applicationDTO.getApplicationName());
                assertThat(Table.isEmpty()).as("Scan should be moved from table").isTrue();
                break;
            case Monitor:
                manualTestingPage = mobilePage.getMonitorTaskByAppDto(applicationDTO)
                        .openScanDetails()
                        .openManualTesting();
                isCompleted = manualTestingPage.completeAllStepsAndTasks(true).isWorkflowCompleted();
                assertThat(isCompleted)
                        .as("Workflow should be competed")
                        .isTrue();

                manualTestingPage.adminTopNavbar.openMobile().openMonitorTab().getMonitorTaskByAppDto(applicationDTO)
                        .pressDone().findWithSearchBox(applicationDTO.getApplicationName());

                assertThat(Table.isEmpty()).as("Scan should be moved from table").isTrue();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + phase);
        }
    }

    public enum Phase {
        Monitor, Manual, Audit
    }
}
