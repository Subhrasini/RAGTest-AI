package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Spinner;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.exceptions.FodElementNotFoundException;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.tasks.TasksPage;
import com.fortify.fod.ui.pages.admin.tasks.popups.AssignTaskPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.files.FileFilters.withExtension;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class DynamicTaskAndDeletionTest extends FodBaseTest {
    AdminUserDTO dynamicTester, dynamicManager;
    ApplicationDTO applicationDTO;
    DynamicScanDTO dynamicScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a Tenant with Entitlement, Dynamic Tester and Dynamic Manager users." +
            " Prepare Application and Dynamic Scan")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        AllureReportUtil.info("Setting Dynamic Tester user");
        dynamicTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicTester);
        dynamicTester.setTenant(defaultTenantDTO.getTenantCode());
        dynamicTester.setSkills(new String[]{
                "Dynamic - Access Control",
                "Dynamic - Advanced",
                "Dynamic - Authentication",
                "Dynamic - Automated",
                "Dynamic - Input Validation",
                "Dynamic - Monitor WebInspect",
                "Dynamic - Preparation and Setup",
                "Dynamic - Preparation",
                "Dynamic - Quality 1",
                "Dynamic - Quality 2",
                "Dynamic - Quality",
                "Dynamic - Remediation 1",
                "Dynamic - Remediation 2",
                "Dynamic - Review",
                "Dynamic - Session Management",
                "Dynamic - Setup WebInspect",
                "Dynamic - Validate Issues",
                "Dynamic - Vulnerability Challenge",
        });
        AllureReportUtil.info("Setting Dynamic Manager user");
        dynamicManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicManager);
        dynamicManager.setTenant(defaultTenantDTO.getTenantCode());
        dynamicManager.setSkills(new String[]{
                "Dynamic - Access Control",
                "Dynamic - Advanced",
                "Dynamic - Authentication",
                "Dynamic - Automated",
                "Dynamic - Input Validation",
                "Dynamic - Monitor WebInspect",
                "Dynamic - Preparation and Setup",
                "Dynamic - Preparation",
                "Dynamic - Quality 1",
                "Dynamic - Quality 2",
                "Dynamic - Quality",
                "Dynamic - Remediation 1",
                "Dynamic - Remediation 2",
                "Dynamic - Review",
                "Dynamic - Session Management",
                "Dynamic - Setup WebInspect",
                "Dynamic - Validate Issues",
                "Dynamic - Vulnerability Challenge",
        });
        AllureReportUtil.info("Setting Dynamic Scan");
        dynamicScanDTO.setAdditionalDocumentationFile("payloads/fod/_test.txt");
        AllureReportUtil.info("Creating Dynamic Tester and Dynamic Manager users");
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        AdminUserActions.createAdminUsers(dynamicManager, dynamicTester);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Creating Application");
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        AllureReportUtil.info("Creating Dynamic Scan");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Additional Scan File")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            enabled = true)
    public void verifyAdditionalScanFile() {
        LogInActions.adminLogIn().adminTopNavbar.openDynamic().clearSearchBox()
                .openDetailsFor(applicationDTO.getApplicationName()).openFiles();
        File additionalDocumentFile;
        AllureReportUtil.info("Downloading Additional Document file");
        additionalDocumentFile = $x("//*[@class = 'action-cell']/*[contains(text(), 'Download')]")
                .download(Duration.ofMinutes(3).toMillis(), withExtension("txt"));
        var expectedAdditionalDocumentFileName = "0_" + defaultTenantDTO.getTenantName() + "__test.txt";
        assertThat(additionalDocumentFile)
                .as("File name should be {expectedAdditionalDocumentFileName]")
                .hasName(expectedAdditionalDocumentFileName);
        assertThat(additionalDocumentFile.length())
                .as("Additional documentation file should not be empty").isGreaterThan(5);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign Dynamic Tasks")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation", "verifyAdditionalScanFile"},
            enabled = true)
    public void assignDynamicTasks() {
        var tasksPage = LogInActions.adminLogIn().adminTopNavbar.openTasks();
        tasksPage.appliedFilters.clearAll();
        tasksPage.filters.expandAllFilters().setSearchFilterValue(applicationDTO.getApplicationName());
        assignAllTasksOnPage(dynamicTester.getFirstName(), dynamicTester.getLastName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Work Dynamic Tasks")
    @Test(groups = {"regression"}, dependsOnMethods = {"assignDynamicTasks"},
            enabled = true)
    public void workDynamicTasks() {
        LogInActions.adminUserLogIn(dynamicTester).adminTopNavbar.openTasks().getAllTasks().get(0).pressWork()
                .completeAllStepsAndTasksAndPressTaskDone();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Import and Complete Dynamic Scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"workDynamicTasks"},
            enabled = true)
    public void importAndCompleteDynamicScan() {
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), "payloads/fod/dynamic.zero.fpr",
                false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Delete Users")
    @Test(groups = {"regression"}, dependsOnMethods = {"importAndCompleteDynamicScan"},
            enabled = true)
    public void deleteUsers() {
        LogInActions.adminLogIn();
        AdminUserActions.deleteUser(dynamicTester, dynamicManager);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Delete Application")
    @Test(groups = {"regression"}, dependsOnMethods = {"deleteUsers"},
            enabled = true)
    public void deleteApplication() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationActions.purgeApplication(applicationDTO);
    }

    public void assignAllTasksOnPage(String firstName, String lastName) {
        var scans = new TasksPage().getAllTasksForAssigning();
        var fullName = firstName + " " + lastName;
        for (var scan : scans) {
            scan.pressAssign();
            new Spinner().waitTillLoading();
            AssignTaskPopup assignTaskPopup = new AssignTaskPopup();
            assignTaskPopup.fillSearchBox(fullName);
            var element = IterableUtils.toList(new Table($("#tblUsersSkillOnly"))
                            .getAllDataRows().asDynamicIterable())
                    .stream().filter(x -> x.$x("./td[2]").text().equals(fullName))
                    .findFirst().orElseThrow(() -> new FodElementNotFoundException(fullName + " not found!"));
            element.$x("./td[1]").click();
            assignTaskPopup.pressSubmit();
            new Spinner().waitTillLoading();
        }
    }
}