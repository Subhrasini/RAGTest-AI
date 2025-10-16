package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class SuppressSonatypeRecalculateRollupTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site and AUTO-TAM should create static scan")
    @Test(groups = {"hf", "regression"}, priority = 1)
    public void prepareTestData() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        var entitlement = EntitlementDTO.createDefaultInstance();
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        EntitlementsActions.createEntitlements(defaultTenantDTO, true, entitlement);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setFileToUpload("payloads/fod/10JavaDefectsAnt.zip");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("549002")
    @Description("AUTO-TAM should be able to suppress Sonatype- Open Source Issues")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"prepareTestData"})
    public void suppressSonatypeIssuesTest() {
        var yourApplicationsPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases();
        var yourApplicationDetails = yourApplicationsPage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());
        var issuesPage = yourApplicationDetails.openIssues();
        issuesPage.filters.expandAllFilters();
        var issue = issuesPage.getAllIssues().get(0);
        issuesPage.selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        issue.setAuditorStatus(FodCustomTypes.AuditorStatus.RiskAccepted.getTypeValue());
        issue.submitChanges();

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin should find task - RecalculateRollupHistoryByVersion relative to tenantDTO")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"suppressSonatypeIssuesTest"})
    public void findRecalculateRollupTaskTest() {
        var tasksPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration().openTaskService().openTasks();
        var checkedFilters = tasksPage.appliedFilters;
        checkedFilters.clearAll();
        waitForAnyTaskFiltered();
        TaskServiceThreadsActions.waitForTaskCondition("RecalculateRollupHistoryByVersion", true);
    }

    public void waitForAnyTaskFiltered() {
        var tasksOpenPage = new AdminTopNavbar().openConfiguration().openTaskService().openTasks();
        tasksOpenPage.filters.setFilterByName("TENANT").expand().clickFilterOptionByName(defaultTenantDTO.getTenantName());
        WaitUtil.waitForTrue(() -> !tasksOpenPage.getPendingTaskGrid().isEmpty(), Duration.ofMinutes(15), false);
        assertThat(!tasksOpenPage.getPendingTaskGrid().isEmpty())
                .as("Task table shouldn't be empty")
                .isTrue();
    }
}



