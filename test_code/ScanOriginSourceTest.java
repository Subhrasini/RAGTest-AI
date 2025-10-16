package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.DynamicScansPayload;
import com.fortify.fod.api.payloads.MobileScansPayload;
import com.fortify.fod.api.payloads.StaticScanPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.common.tenant.popups.ScanSummaryPopup;
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

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("ysmal@opentext.com")
@FodBacklogItem("291015")
@Slf4j
public class ScanOriginSourceTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO appForStaticScan, appForMobileScan, appForDynamicScan;
    DynamicScanDTO dynamicScanDTO;
    StaticScanDTO staticScanDTO;
    MobileScanDTO mobileScanDTO;
    int entitlementId;


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare tenant/applications and all scan types for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{
                "Enable False Positive Challenge flag and submission",
                "Allow scanning with no entitlements",
                //"Scan Third Party Libraries",
                "Enable Source Download"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        appForStaticScan = ApplicationDTO.createDefaultInstance();
        appForStaticScan.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Low);
        appForStaticScan.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        appForMobileScan = ApplicationDTO.createDefaultMobileInstance();
        appForMobileScan.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        appForMobileScan.setSdlcStatus(FodCustomTypes.Sdlc.QaTest);

        appForDynamicScan = ApplicationDTO.createDefaultInstance();
        appForDynamicScan.setBusinessCriticality(FodCustomTypes.BusinessCriticality.High);
        appForDynamicScan.setSdlcStatus(FodCustomTypes.Sdlc.Production);

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        entitlementId = TenantActions.createTenant(tenantDTO).openEntitlements().getAll().get(0).getId();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplications(appForStaticScan, appForMobileScan, appForDynamicScan);
        StaticScanActions.createStaticScan(staticScanDTO, appForStaticScan,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        MobileScanActions.createMobileScan(mobileScanDTO, appForMobileScan,
                FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, appForDynamicScan,
                FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued);
        TenantUserActions.activateSecLead(tenantDTO, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Static scan on all Scan Pages")
    @Test(groups = {"regression", "ui-scanOriginSource"}, dependsOnMethods = {"testDataPreparation"})
    public void scanOriginSourceStaticScanTest() {
        validateScan(appForStaticScan, FodCustomTypes.ScanType.Static, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Mobile scan on all Scan Pages")
    @Test(groups = {"regression", "ui-scanOriginSource"}, dependsOnMethods = {"testDataPreparation"})
    public void scanOriginSourceMobileScanTest() {
        validateScan(appForMobileScan, FodCustomTypes.ScanType.Mobile, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Dynamic scan on all Scan Pages")
    @Test(groups = {"regression", "ui-scanOriginSource"}, dependsOnMethods = {"testDataPreparation"})
    public void scanOriginSourceDynamicScanTest() {
        validateScan(appForDynamicScan, FodCustomTypes.ScanType.Dynamic, false);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Api Mobile scan on all Scan Pages")
    @Test(groups = {"regression"}, dependsOnGroups = {"ui-scanOriginSource"},
            alwaysRun = true)
    public void scanSummaryScanOriginSourceMobileScanAPITest() {
        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(),
                        AccessScopeRestrictionsFodApi.START_SCANS);
        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                releaseId,
                FodCustomTypes.ScanType.Mobile,
                "AUTO-MOBILE");
        MobileScansPayload mobileScansPayload = MobileScansPayload.defaultMobileScanSetup(assessmentTypeId);
        apiActions.getMobileScansApiProvider().saveMobileScanSetup(releaseId, mobileScansPayload);
        File mobileApp = ApiHelper.retrievePayloadFile("payloads/fod/iGoat.ipa");
        String startDate = LocalDateTime.now().plusSeconds(10).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("assessmentTypeId", assessmentTypeId);
        queryParams.put("entitlementId", entitlementId);
        queryParams.put("timeZone", "FLE Standard Time");
        queryParams.put("frameworkType", FodCustomTypes.MobileFrameworkType.IOS.getTypeValue());
        queryParams.put("platformType", FodCustomTypes.MobilePlatformType.Phone.getTypeValue());
        queryParams.put("entitlementFrequencyType", FodCustomTypes.EntitlementFrequencyType.SingleScan.getTypeValue());
        queryParams.put("startDate", startDate);
        queryParams.put("scanMethodType", "Other");
        queryParams.put("scanTool", "WebUI");
        queryParams.put("scanToolVersion", FodConfig.FOD_VERSION);

        Response mobileScanResponse = apiActions.getMobileScansApiProvider()
                .startMobileScan(releaseId, queryParams, mobileApp);
        mobileScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);
        BrowserUtil.clearCookiesLogOff();
        validateScan(applicationDTO, FodCustomTypes.ScanType.Mobile, true);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Api Dynamic scan on all Scan Pages")
    @Test(groups = {"regression"}, dependsOnGroups = {"ui-scanOriginSource"},
            alwaysRun = true)
    public void scanSummaryScanOriginSourceDynamicScanAPITest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(),
                        AccessScopeRestrictionsFodApi.START_SCANS);
        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                releaseId,
                FodCustomTypes.ScanType.Dynamic,
                "AUTO-DYNAMIC");
        DynamicScansPayload dynamicScansPayload = DynamicScansPayload
                .dynamicScanSetup(assessmentTypeId, entitlementId);
        apiActions.getDynamicScansApiProvider().saveDynamicScanSetup(releaseId, dynamicScansPayload);
        DynamicScansPayload startScanPayload = DynamicScansPayload.startDynamicScan(assessmentTypeId, entitlementId);
        startScanPayload.setScanMethodType("Other");
        startScanPayload.setScanTool("WebUI");
        startScanPayload.setScanToolVersion(FodConfig.FOD_VERSION);
        Response response = apiActions.getDynamicScansApiProvider()
                .startDynamicScan(releaseId, startScanPayload);
        response.then().
                assertThat().
                statusCode(HTTP_OK);
        BrowserUtil.clearCookiesLogOff();
        validateScan(applicationDTO, FodCustomTypes.ScanType.Dynamic, true);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate origin source for Api Static scan on all Scan Pages")
    @Test(groups = {"regression"}, dependsOnGroups = {"ui-scanOriginSource"},
            alwaysRun = true)
    public void scanSummaryScanOriginSourceStaticScanAPITest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));
        var apiActions = FodApiActions.init(
                new UserPayload(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD),
                tenantDTO.getTenantCode(),
                AccessScopeRestrictionsFodApi.API_TENANT
        );
        var staticScanPayload = StaticScanPayload.defaultJavaScanInstance();
        staticScanPayload.setScanTool("WebUI");
        staticScanPayload.setScanToolVersion(FodConfig.FOD_VERSION);
        Response staticScanResponse = apiActions.startStaticScan(releaseId,
                staticScanPayload,
                FodConfig.API_PATH_STATIC_SCAN_ZIP);
        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);
        BrowserUtil.clearCookiesLogOff();
        validateScan(applicationDTO, FodCustomTypes.ScanType.Static, true);
    }

    private void validateScan(ApplicationDTO applicationDTO, FodCustomTypes.ScanType scanType, boolean isApi) {
        var scanSummaryPopup = LogInActions.tamUserLogin(tenantDTO).openYourScans()
                .getScanByType(scanType).openDropdown().pressScanSummary();

        var topNav = validatePopup(scanSummaryPopup, isApi);
        var scan = topNav.openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openScans().getScanByType(scanType);

        scanSummaryPopup = scan.openDropdown().pressScanSummary();
        validatePopup(scanSummaryPopup, isApi);

        scanSummaryPopup = topNav.openApplications().openYourReleases().openDetailsForRelease(applicationDTO)
                .openScans().getScanByType(scanType).openDropdown().pressScanSummary();
        validatePopup(scanSummaryPopup, isApi);
    }

    private TenantTopNavbar validatePopup(ScanSummaryPopup scanSummaryPopup, boolean isApi) {
        if (isApi) {
            assertThat(scanSummaryPopup.getValueByName("Scan Method"))
                    .as("Scan method should be equal: Other")
                    .isEqualTo("Other");
        } else {
            assertThat(scanSummaryPopup.getValueByName("Scan Method"))
                    .as("Scan method should be equal: Browser")
                    .isEqualTo("Browser");
        }
        assertThat(scanSummaryPopup.getValueByName("Scan Tool"))
                .as("Scan tool should be equal: WebUI")
                .isEqualTo("WebUI");
        assertThat(scanSummaryPopup.getValueByName("Scan Tool Version"))
                .as("Scan Tool Version should be equal: " + FodConfig.FOD_VERSION)
                .isEqualTo(FodConfig.FOD_VERSION);

        return scanSummaryPopup.pressClose();
    }
}