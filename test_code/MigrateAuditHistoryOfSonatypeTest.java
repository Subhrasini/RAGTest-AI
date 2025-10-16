package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.HistoryCell;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.ScreenshotsCell;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
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

import java.util.List;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("801023")
@Slf4j
public class MigrateAuditHistoryOfSonatypeTest extends FodBaseTest {
    TenantDTO tenantDTO;
    EntitlementDTO sonatypeEntitlement, debrickedEntitlement;
    String zipFilePath = "payloads/fod/WebGoat_test.zip";
    StaticScanDTO ossStaticScanDto;
    ApplicationDTO webApp;
    String findingAuditId, cveValue;
    String issueName = "org.webjars:bootstrap@3.3.7";
    Integer scanId;
    List<String> historyEvents;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , applications and entitlements for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(100);
        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(100);

        ossStaticScanDto = StaticScanDTO.createDefaultInstance();
        ossStaticScanDto.setLanguageLevel("1.8");
        ossStaticScanDto.setOpenSourceComponent(true);
        ossStaticScanDto.setFileToUpload(zipFilePath);
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, tenantDTO, true);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Run a Sonatype scan for the application and add some audit details")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void runSonatypeScanAndAddAuditInfo() {
        AllureReportUtil.info("Run a sonatype scan and add screenshot and some audit changes");
        EntitlementsActions.createEntitlements(tenantDTO, true, sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(ossStaticScanDto, webApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(webApp, true, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        var issuesPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(webApp.getApplicationName(), webApp.getReleaseName())
                .openIssues();
        issuesPage.groupBy("Scan Type").clickAll().expandAllGroupByElements();
        var tenantIssueCell = issuesPage.selectIssueByText(issueName);
        findingAuditId = tenantIssueCell.getId();
        scanId = getScanId(findingAuditId);
        issuesPage.getTable()
                .getCellByTextAndIndex(findingAuditId, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();

        Selenide.switchTo().window(1);
        var releaseIssuesPage = page(ReleaseIssuesPage.class);
        cveValue = releaseIssuesPage.getPrimaryRuleId();
        releaseIssuesPage.setDeveloperStatus("In Remediation")
                .setAuditorStatus("Remediation Required")
                .pressAddButton();
        new ScreenshotsCell().openScreenshots().addScreenShot();
        historyEvents = new HistoryCell().openHistory().getAllEvents();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Rerun a scan on same application for debricked and verify sonatype audit details are migrated for " +
            "the new scan")
    @Test(groups = {"regression"}, dependsOnMethods =
            {"runSonatypeScanAndAddAuditInfo", "prepareTestData"})
    public void rerunScanWithDebrickedAndVerifyAuditInfo() {
        EntitlementsActions.disableEntitlementsByEntitlementType(tenantDTO, FodCustomTypes.EntitlementType.SonatypeEntitlement, true);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Rerun the same scan with a debricked entitlement and verify all the audit information " +
                "from sonatype scan is migrated to the debricked scan");
        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(ossStaticScanDto, webApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(webApp, true, true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        var issuesPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(webApp.getApplicationName(), webApp.getReleaseName())
                .openIssues();
        issuesPage.groupBy("Scan Type").clickAll().expandAllGroupByElements();
        var tenantIssueCell = issuesPage.selectIssueByText(issueName);
        assertThat(getScanId(tenantIssueCell.getId()))
                .as("LastFoundInScanId should be consistent between the Debricked and sonatype scans")
                .isEqualTo(scanId);
        issuesPage.getTable()
                .getCellByTextAndIndex(tenantIssueCell.getId(), issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();

        Selenide.switchTo().window(1);
        assertThat(page(ReleaseIssuesPage.class).getPrimaryRuleId())
                .as("Cve value should be same for both scans")
                .isEqualTo(cveValue);
        assertThat(new ScreenshotsCell().openScreenshots().getAllScreenshots())
                .as("Screenshots should be migrated and  present in the scan")
                .isNotEmpty();
        assertThat(new HistoryCell().openHistory().getAllEvents())
                .as("History events should be same as that of sonatype scan")
                .isEqualTo(historyEvents);
    }

    private int getScanId(String findingAuditId) {
        var query = "select LastFoundInScanId from  dbo.OpenSourceVulnerabilityFindingAudit  where " +
                "FindingAuditId=" + "'" + findingAuditId + "'";
        return Integer.parseInt(new FodSQLUtil().getStringValueFromDB(query));
    }
}
