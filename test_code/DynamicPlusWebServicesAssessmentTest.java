package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AssessmentTypesDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
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
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@Slf4j
public class DynamicPlusWebServicesAssessmentTest extends FodBaseTest {

    TenantDTO tenantDTO;
    final String dynamicSiteURL = "http://stg.motortrend.com/";
    final String subdomainURL = "https://dev-auth.motortrend.com/create-account";
    String dynamicPlusWebServices;
    String invalidFile = "payloads/fod/_test.txt";
    String uploadError_graphQL = "Invalid file extension. Please select from the following file types: .json";
    String uploadError_Grpc = "Invalid file extension. Please select from the following file types: .proto";
    String validationSummary = "The URL to the GraphQL is not valid";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and enable Dynamic Plus APIs " +
            "from admin side and set entitlement from admin side")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        TenantActions.createTenant(tenantDTO, true, false);
        var assessmentTypesDTO = AssessmentTypesDTO.createDefaultInstance();
        assessmentTypesDTO.setAssessmentTypeName("Dynamic Plus APIs" + UniqueRunTag.generate());
        assessmentTypesDTO.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        assessmentTypesDTO.setAssessmentCategory("Dynamic Plus APIs");
        assessmentTypesDTO.setWorkflow("Dynamic Custom");
        assessmentTypesDTO.setRemediationWorkflow("Dynamic Custom");
        assessmentTypesDTO.setTenantToAssign(tenantDTO.getTenantCode());
        var assessmentType = AssessmentTypesActions.createAssessmentType(assessmentTypesDTO, false);
        dynamicPlusWebServices = assessmentTypesDTO.getAssessmentTypeName();

        var entitlementsPage = assessmentType.adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openAssessmentTypes()
                .selectAllowSingleScan(dynamicPlusWebServices).openEntitlements();
        var entitlementsPopup = entitlementsPage.pressAddEntitlement();
        entitlementsPopup.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        entitlementsPopup.setAssessmentType(dynamicPlusWebServices);
        entitlementsPopup.setFrequency(FodCustomTypes.ScanFrequency.SingleScan);
        entitlementsPopup.setPurchasedQuantity(300);
        entitlementsPopup.pressSaveButton();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("371016")
    @Description("Verify tenant user should be able to initiate a Dynamic+ Web Services " +
            "Assessment scan which having entitlement between 6 and 998 eg. 300")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void validateDynamicScanWithSpecificEntitlement() {
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        dynamicScanDTO.setAssessmentType(dynamicPlusWebServices);
        dynamicScanDTO.setEntitlement("Single Scan");
        dynamicScanDTO.setDynamicSiteUrl(dynamicSiteURL);
        dynamicScanDTO.setExcludeUrl(subdomainURL);
        var setupPage = DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
        assertThat(setupPage.getEntitlementDropdownValues().get(1))
                .as("Verifying if correct entitlement option " +
                        "available under entitlements dropdown")
                .contains("Single Scan");
        assertThat(setupPage.isAvailableEntitlementsExist())
                .as("Verify Available Entitlements group is " +
                        "present under entitlements dropdown")
                .isTrue();
        assertThat(setupPage.getSetupScanStatus())
                .as("Setup status should be Valid")
                .isEqualToIgnoringCase("Setup Status: Valid");
    }

    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("799040")
    @MaxRetryCount(2)
    @Ignore("Testcase is no longer valid according to defect Defect 1628006 - Dynamic + API Assessment : " +
            "Remove GraphQL and gRPC from API dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify graphQL/gRPC scan using file as source for Dynamic+ APIs Assessment")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"}, dataProvider = "apiScanFiles")
    public void verifyGraphQlGrpcApiScanFileTest(FodCustomTypes.DynamicScanApiType apiType, String fileName, FodCustomTypes.DynamicScanApiSchemeType schemaType, String host,
                                                 String servicePath, String scanFileName, String apiTypeDesc, String uploadErr) {
        AllureReportUtil.info("Verify " + apiType + " scan using file as source for Dynamic+ APIs Assessment");
        SoftAssertions softAssert = new SoftAssertions();
        var dynamicScanDTO = DynamicScanDTO.createDefaultDynamicPlusAPIsInstance();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        dynamicScanDTO.setEntitlement("Single Scan");
        dynamicScanDTO.setAssessmentType(dynamicPlusWebServices);
        dynamicScanDTO.setWebServiceType(apiType);
        dynamicScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE);
        dynamicScanDTO.setWebServiceDocument("payloads/fod/" + fileName);
        dynamicScanDTO.setApiSchemeType(schemaType);
        dynamicScanDTO.setApiHost(host);
        dynamicScanDTO.setApiServicePath(servicePath);

        var dynamicScanSetupPage = ApplicationActions.createApplication(applicationDTO,
                tenantDTO, true).pressStartDynamicScan();
        var apiPanel =
                dynamicScanSetupPage.setAssessmentType(dynamicScanDTO.getAssessmentType())
                        .setEntitlement(dynamicScanDTO.getEntitlement())
                        .setTimeZone(dynamicScanDTO.getTimezone())
                        .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing())
                        .getApiPanel().expand()
                        .enableApi(true);

        softAssert.assertThat(apiPanel.getApiTypeDropdown().getOptionsTextValues())
                .as("Verify API Type drop down list gRPC ,GraphQL, OpenAPI, Postman")
                .contains("SOAP", "REST", "GraphQL", "gRPC");
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
                false, false, true, false, true);
        softAssert.assertThat(DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false)
                        .getAPIType())
                .as("Verify API type is " + apiType + " in Dynamic Scan Overview Page")
                .contains(apiType.getTypeValue());
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


    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("799040")
    @MaxRetryCount(2)
    @Ignore("Testcase is no longer valid according to defect Defect 1628006 - Dynamic + API Assessment : " +
            "Remove GraphQL and gRPC from API dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify graphQL scan using url as source for Dynamic+ APIs Assessment")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyGraphQlApiScanUrlTest() {

        SoftAssertions softAssert = new SoftAssertions();
        var dynamicScanDTO = DynamicScanDTO.createDefaultDynamicPlusAPIsInstance();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();

        dynamicScanDTO.setEnvironmentalFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL);
        dynamicScanDTO.setEntitlement("Single Scan");
        dynamicScanDTO.setAssessmentType(dynamicPlusWebServices);
        dynamicScanDTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.GRAPH_QL);
        dynamicScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.URL);
        dynamicScanDTO.setApiDefinitionUrl("http://172.16.10.170:8080/graphql");

        var dynamicScanSetupPage = ApplicationActions.createApplication(applicationDTO,
                tenantDTO, true).pressStartDynamicScan();
        dynamicScanSetupPage.setAssessmentType(dynamicScanDTO.getAssessmentType())
                .setEntitlement(dynamicScanDTO.getEntitlement())
                .setTimeZone(dynamicScanDTO.getTimezone())
                .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing())
                .getApiPanel().expand()
                .enableApi(true)
                .setApiType(dynamicScanDTO.getWebServiceType())
                .setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.URL)
                .setIntrospectionUrl("http:/172.16.10.170:8080/graphql")
                .getParentPage()
                .pressStartScanBtn();
        softAssert.assertThat(dynamicScanSetupPage.getValidationMessages())
                .as("Verify the error message after uploading files")
                .contains(validationSummary);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), "payloads/fod/graphQL_URL.scan",
                false, false, true, false, true);
        softAssert.assertThat(DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false)
                        .getAPIType())
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
