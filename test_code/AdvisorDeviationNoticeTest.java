package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.api.providers.BaseRestClient;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;
import static com.codeborne.selenide.Selenide.switchTo;
import static utils.api.ResponseCodes.*;

@Owner("kbadia@opentext.com")
@FodBacklogItem("638002")
@Slf4j
public class AdvisorDeviationNoticeTest extends FodBaseTest {

    ApplicationDTO applicationDTO, application;
    StaticScanDTO staticScanDTO;
    TenantDTO tenantDTO;
    String pythonPayload = "payloads/fod/PythonNumpy.zip";
    String pythonFpr = "payloads/fod/50282_Python_scandata.fpr";
    String url = "https://portal.securecodewarrior.com/";
    String javaFpr = "payloads/fod/27633_scandata.fpr";

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on " +
            "admin site and AUTO-TAM should create and complete static scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Creating tenant with sonatype entitlement");
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Starting static scan with selecting the " +
                "check box of Open Source Component");
        applicationDTO = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(pythonPayload);
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(applicationDTO, pythonFpr, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Completing static scan with open source scan");
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify a new line available between Explanation " +
            "and Advisory Deviation Notice under recommendations tab and bold the text")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyAdvisorDeviationNotice() {
        AllureReportUtil.info("Verify a new line available between Explanation " +
                "and Advisory Deviation Notice under recommendations tab and bold the text");
        assertThat(LogInActions
                .tamUserLogin(tenantDTO)
                .openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues()
                .getAllIssues().get(0)
                .openDetails()
                .openRecommendations()
                .getContentElementByTitle("Explanation")
                .innerHtml())
                .as("Verify Advisory Deviation Notice with " +
                        "bold text starting in new line under recommendations tab")
                .contains("<br><br><strong>*Advisory Deviation Notice:*</strong>");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("741007")
    @Description("Recommendation title under Recommendation tab from Debricked in SBOM")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyRecommendationForDebricked() {
        AllureReportUtil.info("Creating application and import " +
                "SBOM .json file with vulnerabilities");
        var application = ApplicationDTO.createDefaultInstance();
        var recommendationsCell = ApplicationActions
                .createApplication(application, tenantDTO, true)
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(application).openScans()
                .pressImportOpenSourceScan()
                .uploadFile("payloads/fod/21210_51134_cyclonedx.json")
                .pressImportButton().openIssues()
                .getAllIssues().get(0).openDetails()
                .openRecommendations();
        assertThat(recommendationsCell.getAllTitlesNames())
                .as("Verify that in Issues page added a new section Recommendation " +
                        "on the Recommendations tab from the Debricked in SBOM")
                .contains("Recommendation");
        assertThat(recommendationsCell
                .getContentElementByTitle("Recommendation")
                .innerHtml())
                .as("Verify Recommendation title on the Recommendations tab which will" +
                        " display the recommendation information from the Debricked in SBOM")
                .isNotEmpty();
    }

    @MaxRetryCount(2)
    @Description("Create Application and import scan")
    @Test(groups = {"regression"})
    public void prepareTestDataForTrainingLink() {
        application = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(application, defaultTenantDTO, true);
        StaticScanActions.importScanTenant(application, javaFpr);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1463003")
    @Description("Secure Code Warrior should start successfully, after clicking on " +
            "Launch Training button in recommendation tab of issues page")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"prepareTestDataForTrainingLink"})
    public void launchTrainingLinkTest() {
        var softAssert = new SoftAssertions();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openYourApplications()
                .openDetailsFor(application.getApplicationName())
                .openIssues()
                .getAllIssues().get(0)
                .openDetails()
                .openRecommendations()
                .getInteractiveTrainingElement()
                .click();
        switchTo().window(1);
        var openedUrl = WebDriverRunner.url();
        assertThat(openedUrl)
                .as("Launch training button should open correct Secure Code warrior website url")
                .contains(url);
        Response response = new BaseRestClient().getRequest(openedUrl);
        Assert.assertEquals(response.getStatusCode(), HTTP_OK);
        AllureReportUtil.info("Secure Code warrior website url should not encounter error code 422, 400 or 500");
        softAssert.assertThat(response.getStatusCode())
                .as("Secure Code warrior website url should not encounter error code 422")
                .isNotEqualTo(HTTP_UNPROCESSABLE_ENTITY);
        softAssert.assertThat(response.getStatusCode())
                .as("Secure Code warrior website url should not encounter error code 400")
                .isNotEqualTo(HTTP_BAD_REQUEST);
        softAssert.assertThat(response.getStatusCode())
                .as("Secure Code warrior website url should not encounter error code 500")
                .isNotEqualTo(HTTP_INTERNAL_SERVER_ERROR);
        softAssert.assertAll();
    }
}

