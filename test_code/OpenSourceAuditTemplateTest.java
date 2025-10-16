package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.exceptions.FodUnExpectedScanStatusException;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.HashMap;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class OpenSourceAuditTemplateTest extends FodBaseTest {
    TenantDTO tenantDTO;
    TenantDTO tenantDTO2;

    StaticScanDTO staticScanDTO;

    private void createScanInstances() {
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAssessmentType("Static Express");
        staticScanDTO.setFileToUpload("payloads/fod/10JavaDefects_Small(OS).zip");
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and Sonatype entitlements")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        var entitlement = EntitlementDTO.createDefaultInstance();
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlement);
        tenantDTO2 = TenantDTO.createDefaultInstance();
        tenantDTO2.setEntitlementDTO(entitlement);


        TenantActions.createTenant(tenantDTO, true);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantActions.createTenant(tenantDTO2, true);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Open Source scans globally (per tenant) depending on application attribute")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void openSourceAuditTemplateGlobalApplicationAttributeTest() {
        createScanInstances();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        AttributeDTO attributeDTO = AttributeDTO.createDefaultInstance();

        attributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        attributeDTO.setPickListValues(new String[]{"A", "B", "C"});

        HashMap<AttributeDTO, String> attributesMap = new HashMap<>() {{
            put(attributeDTO, "A");
        }};
        applicationDTO.setAttributesMap(attributesMap);

        LogInActions.tamUserLogin(tenantDTO);
        CustomAttributesActions.createCustomAttribute(attributeDTO, tenantDTO, false);
        var auditToolsPage = ApplicationActions.createApplication(applicationDTO).tenantTopNavbar
                .openAdministration().openAuditTools();
        auditToolsPage.deleteAllFilters().pressSave().pressClose();
        auditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .deleteAllFilters().pressSave().pressClose();
        auditToolsPage = new TenantTopNavbar().openAdministration().openAuditTools();
        auditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .deleteAllFilters().pressSave().pressClose();
        auditToolsPage.pressAddFilter()
                .setCondition(
                        attributeDTO.getAttributeName(),
                        FodCustomTypes.FilterOperators.Contains.getTypeValue(),
                        attributeDTO.getPickListValues()[0])
                .setSeverity(FodCustomTypes.Severity.Low);
        auditToolsPage.pressSave().pressClose();

        var issues = proceedScan(applicationDTO, tenantDTO).openScans().getAllScans(false).get(0).getIssuesCounters();
        assertThat(issues.getCritical()).as("There should be no Critical issues").isZero();
        assertThat(issues.getHigh()).as("There should be no High issues").isZero();
        assertThat(issues.getMedium()).as("There should be no Medium issues").isZero();
    }


    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Open Source scans globally (per tenant)")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void openSourceAuditTemplateGlobalTest() {
        createScanInstances();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();

        var auditToolsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openAdministration().openAuditTools();
        auditToolsPage.deleteAllFilters().pressSave().pressClose();
        auditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .deleteAllFilters().pressSave().pressClose();

        var filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .pressAddFilter();
        filter.setCondition(FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                        FodCustomTypes.FilterOperators.DoesNotEqual.getTypeValue(),
                        FodCustomTypes.Severity.Critical.getTypeValue())
                .setSuppress();
        auditToolsPage.pressSave().pressClose();
        ApplicationActions.createApplication(applicationDTO);

        var page = proceedScan(applicationDTO, tenantDTO).openScans();
        var scans = page.getAllScans(false);
        var sonatypeIssues = scans.get(0).getIssuesCounters();
        var staticIssues = scans.get(1).getIssuesCounters();
        var afterSuppressedSonatypeIssues = new IssuesCounters(sonatypeIssues.getCritical(), 0, 0, 0);
        var issuesPage = page.openIssues().getShowFixedSuppressedDropdown();
        issuesPage.setShowSuppressed(false);
        IssuesActions.validateIssuesTenant(staticIssues.plus(afterSuppressedSonatypeIssues));
        issuesPage.setShowSuppressed(true);
        IssuesActions.validateIssuesTenant(staticIssues.plus(sonatypeIssues));
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Open Source scans on application level")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void openSourceAuditTemplateApplicationTest() {
        final String issue1Name = "CVE-2018-1000632";
        final String issue2Name = "hsqldb:hsqldb@1.8.0.1";
        final String issue3Name = "com.opensymphony:xwork";

        createScanInstances();
        var applicationDTO = ApplicationDTO.createDefaultInstance();

        LogInActions.tamUserLogin(tenantDTO2);
        ApplicationActions.createApplication(applicationDTO);

        var appAuditToolsPage = new TenantTopNavbar()
                .openApplications().openDetailsFor(applicationDTO.getApplicationName()).openAuditTools()
                .setScanType(FodCustomTypes.ScanType.OpenSource)
                .deleteAllFilters();
        AllureReportUtil.info("Add filter template: 'com.opensymphony:xwork'");
        var filter = appAuditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .pressAddFilter();
        filter.setCondition(
                        FodCustomTypes.FilterOptions.ComponentName.getTypeValue(),
                        FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                        issue3Name)
                .setSuppress();
        appAuditToolsPage.pressSave().pressClose();

        var issuesPage = proceedScan(applicationDTO, tenantDTO2).openIssues();
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(false);
        var issues = issuesPage.findWithSearchBox(issue3Name).getAllIssues();
        assertThat(issues).as("Issue should be suppressed").isEmpty();
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(true);

        issues = issuesPage.findWithSearchBox(issue3Name).getAllIssues();
        assertThat(issues).as("Suppressed issue should be found").isNotEmpty();
        issues.forEach(i -> assertThat(i.isSuppressed).as("Issue should be suppressed").isTrue());

        appAuditToolsPage = issuesPage.tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openAuditTools();

        appAuditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource)
                .deleteAllFilters().pressSave().pressClose();

        AllureReportUtil.info("Create filter from Issues -> Add audit filter (medium)");
        new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues().findWithSearchBox(issue1Name)
                .getAllIssues()
                .get(0).
                pressAddAuditFilter().setSeverity(FodCustomTypes.Severity.Medium).pressCreateFilter().pressClose();

        AllureReportUtil.info("Create filter from Issues -> Add audit filter (suppressed)");
        new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues().findWithSearchBox(issue2Name)
                .getAllIssues().get(0).pressAddAuditFilter().setSeverityCheckbox(false).setComponentVersionCheckbox(false)
                .setComponentNameCheckbox(false).pressCreateFilter().pressClose();


        issuesPage = proceedScan(applicationDTO, tenantDTO2).openIssues();
        var criticalIssues = issuesPage.findWithSearchBox(issue1Name).getCriticalIssues();
        assertThat(criticalIssues).as("There should not be critical issues").isEmpty();
        var mediumIssues = issuesPage.getMediumIssues();
        assertThat(mediumIssues).as("There should be medium issues").isNotEmpty();

        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(false);
        issues = issuesPage.findWithSearchBox(issue2Name).getAllIssues();
        assertThat(issues).as("Issue should be suppressed").isEmpty();
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(true);
        issues = issuesPage.findWithSearchBox(issue2Name).getAllIssues();
        assertThat(issues).as("Suppressed issue should be found").isNotEmpty();
        issues.forEach(i -> assertThat(i.isSuppressed).as("Issue should be suppressed").isTrue());
    }

    private ReleaseDetailsPage proceedScan(ApplicationDTO applicationDTO, TenantDTO tenantDTO) {
        AllureReportUtil.info("Proceed with scan");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        try {
            StaticScanActions.completeStaticScan(applicationDTO, true);
        } catch (FodUnExpectedScanStatusException e) {
            log.info(e.getMessage());
        }
        BrowserUtil.clearCookiesLogOff();
        var page = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases().openDetailsForRelease(applicationDTO).openScans();
        page.waitLastScanComplete();
        var overviewPage = page.openOverview();
        overviewPage.showFixedSuppressedDropdown.setShowSuppressed(false);
        return page(ReleaseDetailsPage.class);
    }
}
