package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.exceptions.FodUnexpectedConditionsException;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
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
import utils.RetryAnalyzer;

import java.io.FileNotFoundException;

@Owner("sbehera3@opentext.com")
@FodBacklogItem("799040")
@Slf4j
public class GraphqlGrpcAPITypeScanTest extends FodBaseTest {

    TenantDTO tenantDTO;
    String dynamicWebServices;
    String invalidFile = "payloads/fod/_test.txt";
    String uploadError_graphQL = "Invalid file extension. Please select from the following file types: .json";
    String uploadError_Grpc = "Invalid file extension. Please select from the following file types: .proto";
    String validationSummary = "The URL to the GraphQL is not valid";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and Dynamic APIs assessment type")
    @Test(groups = {"regression"})
    public void testDataPreparation() {

        var assessmentTypesDTO = AssessmentTypesDTO.createDefaultInstance();
        assessmentTypesDTO.setAssessmentTypeName("Dynamic APIs" + UniqueRunTag.generate());
        assessmentTypesDTO.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        assessmentTypesDTO.setAssessmentCategory("Dynamic APIs");
        assessmentTypesDTO.setWorkflow("Dynamic Standard");
        assessmentTypesDTO.setRemediationWorkflow("Dynamic Basic");
        AssessmentTypesActions.createAssessmentType(assessmentTypesDTO, true);
        dynamicWebServices = assessmentTypesDTO.getAssessmentTypeName();

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        var entitlementDTO = EntitlementDTO.createScanInstance();
        entitlementDTO.setAssessmentType(dynamicWebServices);
        entitlementDTO.setFrequency(FodCustomTypes.ScanFrequency.Subscription);
        tenantDTO.setEntitlementDTO(entitlementDTO);
        TenantActions.createTenant(tenantDTO, false, false).
                adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openAssessmentTypes()
                .selectAllowSingleScan(dynamicWebServices).openEntitlements();

    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify graphQL/gRPC scan using file as source for Dynamic APIs Assessment")
    @Test(groups = {"regression"}, dataProvider = "apiScanFiles",
            dependsOnMethods = {"testDataPreparation"})
    public void verifyGraphQlGrpcApiScanFileTest(FodCustomTypes.DynamicScanApiType apiType, String fileName, FodCustomTypes.DynamicScanApiSchemeType schemaType, String host,
                                                 String servicePath, String scanFileName, String apiTypeDesc, String uploadErr) {

        AllureReportUtil.info("Verify " + apiType + " scan using file as source for Dynamic APIs Assessment");

        SoftAssertions softAssert = new SoftAssertions();
        var dynamicScanDTO = DynamicScanDTO.createDefaultDynamicAPIsInstance();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();

        dynamicScanDTO.setAssessmentType(dynamicWebServices);
        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        dynamicScanDTO.setWebServiceType(apiType);
        dynamicScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE);
        dynamicScanDTO.setWebServiceDocument("payloads/fod/" + fileName);
        dynamicScanDTO.setApiSchemeType(schemaType);
        dynamicScanDTO.setApiHost(host);
        dynamicScanDTO.setApiServicePath(servicePath);

        var dynamicScanSetupPage = ApplicationActions.createApplication(applicationDTO,
                tenantDTO, true).pressStartDynamicScan();
        var apiPanel = dynamicScanSetupPage
                .setAssessmentType(dynamicScanDTO.getAssessmentType())
                .setEntitlement(dynamicScanDTO.getEntitlement())
                .setTimeZone(dynamicScanDTO.getTimezone())
                .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing())
                .getApiPanel().expand();

        softAssert.assertThat(apiPanel.getApiTypeDropdown().getOptionsTextValues())
                .as("Verify API Type drop down list gRPC ,GraphQL, OpenAPI, Postman")
                .contains("gRPC", "GraphQL", "OpenAPI", "Postman");
        if (apiType.getTypeValue().equals("GraphQL")) {
            apiPanel.setApiType(apiType)
                    .uploadWebServiceDocument(invalidFile);
        } else {
            apiPanel.setApiType(apiType)
                    .uploadWebServiceDocument(invalidFile);

        }
        softAssert.assertThat(dynamicScanSetupPage.getUploaderErrorAPI().getText())
                .as("Verify the error message after uploading files")
                .isEqualTo(uploadErr);
        var allSchemas = apiPanel.getApiSchemeDropdown().getOptionsTextValues();
        softAssert.assertThat(allSchemas)
                .as("Verify API Schema Type Drop down has HTTP, HTTPS, HTTP and HTTPS")
                .contains("HTTP", "HTTPS", "HTTP and HTTPS");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), scanFileName,
                false, false, false, false, true);
        var dynamicScanOverviewPage = DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        softAssert.assertThat(dynamicScanOverviewPage.getAPIType())
                .as("Verify API type is " + apiType + " in Dynamic Scan Overview Page")
                .contains(apiType.getTypeValue());
        softAssert.assertThat(dynamicScanOverviewPage.getAPIsManifestPath())
                .as("Verify APIs Manifest Path is the file name in Dynamic Scan Overview Page")
                .contains(fileName);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        var releaseScanCell = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(),
                        applicationDTO.getReleaseName()).openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic);
        var scanSummaryPopup = releaseScanCell.pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("API Type"))
                .as("Verify API type field in Scan Summary popup should have value " + apiTypeDesc + " in Scan Summary")
                .contains(apiTypeDesc);
        scanSummaryPopup.pressClose();

        softAssert.assertThat(releaseScanCell.downloadSiteTreeJSON().getName())
                .as("Verify user should be able to down the site tree in JSON format")
                .contains(".json");
        softAssert.assertThat(releaseScanCell.downloadSiteTreeCSV().getName())
                .as("Verify user should be able to down the site tree in CSV format")
                .contains(".csv");

        softAssert.assertAll();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify graphQL scan using url as source for Dynamic APIs Assessment")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyGraphQlApiScanUrlTest() {

        SoftAssertions softAssert = new SoftAssertions();
        var dynamicScanDTO = DynamicScanDTO.createDefaultDynamicAPIsInstance();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();

        dynamicScanDTO.setAssessmentType(dynamicWebServices);
        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        dynamicScanDTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.GRAPH_QL);
        dynamicScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.URL);
        dynamicScanDTO.setApiDefinitionUrl("http://172.16.10.170:8080/graphql");
        var releaseDetailsPage = ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        var dynamicScanSetupPage = releaseDetailsPage.pressStartDynamicScan();
        dynamicScanSetupPage.setAssessmentType(dynamicScanDTO.getAssessmentType())
                .setEntitlement(dynamicScanDTO.getEntitlement())
                .setTimeZone(dynamicScanDTO.getTimezone())
                .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing())
                .getApiPanel().expand()
                .setApiType(dynamicScanDTO.getWebServiceType())
                .setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.URL)
                .setIntrospectionUrl("http:/172.16.10.170:8080/graphql");
        dynamicScanSetupPage.pressStartScanBtn();
        softAssert.assertThat(dynamicScanSetupPage.getValidationMessages())
                .as("Verify the error message after uploading files")
                .contains(validationSummary);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), "payloads/fod/graphQL_URL.scan",
                false, false, false, false, true);
        var dynamicScanOverviewPage = DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        softAssert.assertThat(dynamicScanOverviewPage.getAPIType())
                .as("Verify API type is graphQL in Dynamic Scan Overview Page")
                .contains(FodCustomTypes.APIType.GraphQL.getTypeValue());
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        var releaseScanCell = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(),
                        applicationDTO.getReleaseName()).openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic);
        var scanSummaryPopup = releaseScanCell.pressScanSummary();
        softAssert.assertThat(scanSummaryPopup.getValueByName("API Type"))
                .as("Verify API type field in Scan Summary popup should have value GraphQL (URL) in Scan Summary")
                .contains("GraphQL (URL)");
        scanSummaryPopup.pressClose();

        softAssert.assertThat(releaseScanCell.downloadSiteTreeJSON().getName())
                .as("Verify user should be able to down the site tree in JSON format")
                .contains(".json");
        softAssert.assertThat(releaseScanCell.downloadSiteTreeCSV().getName())
                .as("Verify user should be able to down the site tree in CSV format")
                .contains(".csv");

        softAssert.assertAll();
    }

    @DataProvider(name = "apiScanFiles")
    public Object[][] apiScanFiles() {
        return new Object[][]{
                {FodCustomTypes.DynamicScanApiType.GRAPH_QL, "graphql.json",
                        FodCustomTypes.DynamicScanApiSchemeType.HTTP, "172.16.10.170:8080", "/graphql/",
                        "payloads/fod/graphQL_File.scan", "GraphQL (File)", uploadError_graphQL},
                {FodCustomTypes.DynamicScanApiType.G_RPC, "proto.proto",
                        FodCustomTypes.DynamicScanApiSchemeType.HTTPS, "172.16.10.151:5002", "/",
                        "payloads/fod/gRPC_File.scan", "gRPC", uploadError_Grpc}

        };
    }
}
