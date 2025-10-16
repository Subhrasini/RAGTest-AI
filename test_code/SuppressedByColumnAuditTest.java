package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Owner("yliben@opentext.com")
@FodBacklogItem("420018")
@Slf4j

public class SuppressedByColumnAuditTest extends FodBaseTest {
    ApplicationDTO staticApplicationDTO;
    ApplicationDTO mobileApplicationDTO;
    private Integer releaseId;
    StaticScanDTO staticScanDTO;
    MobileScanDTO mobileScanDTO;
    TenantDTO tenantDTO;
    final String ruleId = "9C5BD1B5-C296-48d4-B5F5-5D2958661BC4";
    int entitlementId;
    ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Create tenant, applications and setup audit template")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission"
                });

        entitlementId = TenantActions.createTenant(tenantDTO).openEntitlements().getAll().get(0).getId();
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        var auditToolsPage = new TenantTopNavbar()
                .openAdministration().openAuditTools();


        AllureReportUtil.info("Create template for Static Scan");
        var filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Static).pressAddFilter();
        filter.setCondition(
                "Rule ID",
                FodCustomTypes.FilterOperators.Contains.getTypeValue(),
                ruleId);
        filter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        var filters = auditToolsPage.getAllFilters();
        assertThat(filters).as("There should be static filter").hasSize(1);
        BrowserUtil.clearCookiesLogOff();
    }


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Completed static scan and  create global audit template to suppress")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void globalAuditTemplateSuppressTest() {
        staticApplicationDTO = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload("payloads/fod/demo.zip");
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("17");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(staticApplicationDTO).tenantTopNavbar
                .openApplications().openDetailsFor(staticApplicationDTO.getApplicationName());


        StaticScanActions.createStaticScan(staticScanDTO, staticApplicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(staticApplicationDTO, true);
        BrowserUtil.clearCookiesLogOff();


        var releasePage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        releasePage.openDetailsForRelease(staticApplicationDTO.getApplicationName(), staticApplicationDTO.getReleaseName());


        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        AllureReportUtil.info("verify issue is suppress and suppressedBy should be null");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("includeSuppressed", true);

        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseIdAndQueryPrams(releaseId, queryParams);

        response.then()
                .assertThat()
                .body("items.isSuppressed[0]", equalTo(true))
                .body("items.suppressedBy[0]", equalTo(null));
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("complete mobile scan and create application level audit template filters to suppress")
    @Test(groups = {"regression"}, dependsOnMethods = {"globalAuditTemplateSuppressTest"})
    public void applicationAuditTemplateSuppressTest() {
        mobileApplicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setFileToUpload("payloads/fod/flickrj.apk");
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(mobileApplicationDTO);

        AllureReportUtil.info("Create template for Mobile Scan");
        var auditToolsPage = new TenantTopNavbar().openApplications().openDetailsFor(mobileApplicationDTO.getApplicationName()).openAuditTools();


        var filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Mobile).pressAddFilter();
        filter.setCondition("Category", FodCustomTypes.FilterOperators.Contains.getTypeValue(), "Unhandled Exception");
        filter.setSuppress();
        auditToolsPage.pressSave().close();


        new TenantTopNavbar().openApplications()
                .openDetailsFor(mobileApplicationDTO.getApplicationName());
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.importMobileScanAdmin(mobileApplicationDTO.getApplicationName(),
                FodCustomTypes.ImportFprScanType.Mobile,
                "payloads/fod/dynamic.zero.fpr",
                false,
                false,
                true
        );
        MobileScanActions.completeMobileScan(mobileApplicationDTO, false);
        BrowserUtil.clearCookiesLogOff();


        var releasePage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        releasePage.openDetailsForRelease(mobileApplicationDTO.getApplicationName(), mobileApplicationDTO.getReleaseName());


        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));


        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        AllureReportUtil.info("verify issue is suppress and suppressedBy should be nul");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("includeSuppressed", true);
        queryParams.put("filters", "category:Poor Error Handling: Unhandled Exception");

        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseIdAndQueryPrams(releaseId, queryParams);

        response.then()
                .assertThat()
                .body("items.isSuppressed[0]", equalTo(true))
                .body("items.suppressedBy[0]", equalTo(null));
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Completed dynamic scan and change auditor status to risk accepted for an issue to suppress")
    @Test(groups = {"regression"}, dependsOnMethods = {"applicationAuditTemplateSuppressTest"})
    public void auditorStatusIssuesTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(applicationDTO);
        DynamicScanActions.importDynamicScanTenant(applicationDTO, "payloads/fod/dynamic.zero.fpr");
        BrowserUtil.clearCookiesLogOff();

        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();

        issuesPage.openIssueByIndex(0).setSeverity("Medium");
        issuesPage.clickMedium();

        var suppressedMedium = issuesPage.clickMedium().openIssueByIndex(0);
        suppressedMedium.setAuditorStatus("Risk Accepted");
        issuesPage.clickMedium();
        BrowserUtil.clearCookiesLogOff();

        var releasePage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        releasePage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());


        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        AllureReportUtil.info("verify issue is suppress and suppressedBy auditor name");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("includeSuppressed", true);
        queryParams.put("filters", "auditorStatus:Risk Accepted");
        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseIdAndQueryPrams(releaseId, queryParams);

        response.then()
                .assertThat()
                .body("items.isSuppressed[0]", equalTo(true))
                .body("items.suppressedBy[0]", equalTo("AUTO-TAM"));
        BrowserUtil.clearCookiesLogOff();
    }
}