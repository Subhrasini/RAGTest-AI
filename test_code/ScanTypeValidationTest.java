package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

@Owner("sbehera3@opentext.com")
@Slf4j
@FodBacklogItem("766013")
public class ScanTypeValidationTest extends FodBaseTest {

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify ScanType for workflow driven dynamic scans - .webmacro,.har,.burp")
    @Test(groups = {"regression"}, dataProvider = "workflowDrivenScanFiles", priority = 1)
    public void verifyWorkflowDrivenScanTest(String typeOfScan, String fileName, String scanFileName, String scanType) {

        AllureReportUtil.info("Verify Scan Type for :" + typeOfScan);
        SoftAssertions softAssert = new SoftAssertions();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType("Dynamic Website Assessment");
        dynamicScanDTO.setWorkflowDrivenScan(true);
        dynamicScanDTO.setWorkFlowDrivenScanFile("payloads/fod/" + fileName);
        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        String releaseName = applicationDTO.getReleaseName();

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                releaseName, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(releaseName, "payloads/fod/" + scanFileName,
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        var scanSummaryPopup = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(),
                        releaseName).openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic)
                .pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("Scan Type"))
                .as("Verify that Scan Type exists and a corresponding value of " + scanType +
                        " in Scan Summary-> Release Scans page")
                .isEqualTo(scanType);
        scanSummaryPopup.pressClose();
        scanSummaryPopup = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic)
                .pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("Scan Type"))
                .as("Verify that Scan Type exists and a corresponding value of " + scanType +
                        " in Scan Summary-> Application Scans page")
                .isEqualTo(scanType);
        scanSummaryPopup.pressClose();
        softAssert.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify ScanType for Dynamic APIs scans - Postman/OpenAPI/GraphQL/gRPC")
    @Test(groups = {"regression"}, dataProvider = "apiScanFiles")
    public void verifyApiScansTest(FodCustomTypes.DynamicScanApiType apiType, String fileName,
                                   FodCustomTypes.DynamicScanApiSchemeType schemaType, String host,
                                   String servicePath, String scanFileName) {

        AllureReportUtil.info("Verify Scan Type for :" + apiType.getTypeValue());
        SoftAssertions softAssert = new SoftAssertions();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultDynamicAPIsInstance();
        dynamicScanDTO.setAssessmentType("Dynamic APIs");
        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        dynamicScanDTO.setWebServiceType(apiType);
        dynamicScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE);
        dynamicScanDTO.setWebServiceDocument("payloads/fod/" + fileName);
        dynamicScanDTO.setApiSchemeType(schemaType);
        dynamicScanDTO.setApiHost(host);
        dynamicScanDTO.setApiServicePath(servicePath);
        String releaseName = applicationDTO.getReleaseName();

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                releaseName,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(releaseName, "payloads/fod/" + scanFileName,
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        var scanSummaryPopup = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(),
                        releaseName).openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic)
                .pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("Scan Type"))
                .as("Verify that Scan Type exists and a corresponding value of 'API' in Scan Summary-> " +
                        "Release Scans page")
                .isEqualTo("API");
        scanSummaryPopup.pressClose();
        scanSummaryPopup = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic)
                .pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("Scan Type"))
                .as("Verify that Scan Type exists and a corresponding value of 'API' in Scan Summary->" +
                        " Application Scans page")
                .isEqualTo("API");
        scanSummaryPopup.pressClose();
        softAssert.assertAll();
    }

    @DataProvider(name = "workflowDrivenScanFiles", parallel = true)
    public Object[][] workflowDrivenScanFiles() {
        return new Object[][]{
                {"Workflow Scan (Har)", "zero.har", "zero_har_Results.scan", "WorkFlow driven - har file"},
                {"Workflow Scan (Burp)", "zero.burp", "zero_burp_Results.scan", "WorkFlow driven - burp file"},
                {"Workflow Scan (WebMacro)", "ZeroWorkflowMacro.webmacro", "ZeroWorkflowMacro_Results.scan",
                        "WorkFlow driven - webmacro file"}
        };
    }

    @DataProvider(name = "apiScanFiles", parallel = true)
    public Object[][] apiScanFiles() {
        return new Object[][]{
                {FodCustomTypes.DynamicScanApiType.GRAPH_QL, "graphql.json",
                        FodCustomTypes.DynamicScanApiSchemeType.HTTP, "172.16.10.170:8080", "/graphql/",
                        "graphQL_File.scan"},
                {FodCustomTypes.DynamicScanApiType.G_RPC, "proto.proto",
                        FodCustomTypes.DynamicScanApiSchemeType.HTTPS, "172.16.10.151:5002", "/",
                        "gRPC_File.scan"},
                {FodCustomTypes.DynamicScanApiType.POSTMAN, "OpenCart.postman_collection.json",
                        FodCustomTypes.DynamicScanApiSchemeType.NONE, "", "", "Postman_Results.scan"},
                {FodCustomTypes.DynamicScanApiType.OPEN_API, "swagger.json",
                        FodCustomTypes.DynamicScanApiSchemeType.NONE, "", "", "OpenAPI_Results.scan"},

        };
    }
}
