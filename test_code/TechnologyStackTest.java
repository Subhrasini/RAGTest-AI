package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.api.payloads.StaticScanPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
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
import org.assertj.core.api.AssertionsForClassTypes;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.FodBacklogItems;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("tmagill@opentext.com")
@Slf4j
public class TechnologyStackTest extends FodBaseTest {
    ApplicationDTO applicationJavaTestDTO, applicationDotNetTestDTO;
    StaticScanDTO staticScanJavaTestDTO, staticScanDotNetTestDTO;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site and AUTO-TAM should create static scan")
    @Test(groups = {"hf", "regression"})
    public void prepareTestData() {
        applicationJavaTestDTO = ApplicationDTO.createDefaultInstance();
        applicationDotNetTestDTO = ApplicationDTO.createDefaultInstance();
        staticScanJavaTestDTO = StaticScanDTO.createDefaultInstance();
        staticScanDotNetTestDTO = StaticScanDTO.createDefaultInstance();

        ApplicationActions.createApplication(applicationJavaTestDTO, defaultTenantDTO, true);
        staticScanJavaTestDTO.setAssessmentType("Static+ Assessment");
        staticScanJavaTestDTO.setLanguageLevel("11");
        staticScanJavaTestDTO.setFileToUpload("payloads/fod/11Java_550016.zip");
        StaticScanActions.createStaticScan(staticScanJavaTestDTO, applicationJavaTestDTO, FodCustomTypes.SetupScanPageStatus.InProgress);

        ApplicationActions.createApplication(applicationDotNetTestDTO, defaultTenantDTO, false);
        staticScanDotNetTestDTO.setAssessmentType("Static+ Assessment");
        staticScanDotNetTestDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        staticScanDotNetTestDTO.setLanguageLevel("4.8");
        staticScanDotNetTestDTO.setFileToUpload("payloads/fod/11Java_550016.zip");
        StaticScanActions.createStaticScan(staticScanDotNetTestDTO, applicationDotNetTestDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @FodBacklogItem("550016")
    @FodBacklogItem("677004")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Admin user should be able to see expected Tech Stacks associated with each scan")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = "prepareTestData")
    public void autodetectTechnologyStackTest() {

        var locateApplication = LogInActions.adminLogIn().adminTopNavbar.openStatic().findScanByAppName(applicationJavaTestDTO.getApplicationName(), true);
        var techStackJava = locateApplication.getTechnologyStack();
        assertThat(techStackJava).isEqualTo("JAVA/J2EE/Kotlin 11");

        var locateOtherApp = locateApplication.findScanByAppName(applicationDotNetTestDTO.getApplicationName(), true);
        var techStackOther = locateOtherApp.getTechnologyStack();
        assertThat(techStackOther).isEqualTo("JAVA/J2EE/Kotlin 1.8");
    }

    @MaxRetryCount(3)
    @Owner("oradchenko@opentext.com")
    @FodBacklogItem("586012")
    @Severity(SeverityLevel.NORMAL)
    @Description(".NET 6 static scans should work, and scan results should not be empty")
    @Test(groups = {"regression"})
    public void dotNet6ScanTest() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        staticScanDTO.setLanguageLevel("6.0");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload("payloads/fod/dotNET_6.zip");

        LogInActions.tamUserLogin(defaultTenantDTO);
        var applicationDTO = ApplicationActions.createApplication();
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var scansPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName()).openScans();

        AssertionsForClassTypes.assertThat(scansPage.getScanByType(FodCustomTypes.ScanType.Static).getStatus())
                .as("Scan should be completed").isEqualToIgnoringCase("completed");

        var issuesPage = scansPage.tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();

        var issuesCount = WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual,
                0, issuesPage::getAllCount, Duration.ofSeconds(90), true);

        AssertionsForClassTypes.assertThat(issuesCount).as("There should be issue findings").isPositive();
    }

    @Owner("oradchenko@opentext.com")
    @FodBacklogItem("585009")
    @Severity(SeverityLevel.NORMAL)
    @Description("Java 17 static scans should work, and scan results should not be empty")
    @Test(groups = {"regression"})
    public void java17ScanTest() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("17");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload("payloads/fod/java17.zip");

        LogInActions.tamUserLogin(defaultTenantDTO);
        var applicationDTO = ApplicationActions.createApplication();
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var scansPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName()).openScans();

        AssertionsForClassTypes.assertThat(scansPage.getScanByType(FodCustomTypes.ScanType.Static).getStatus())
                .as("Scan should be completed").isEqualToIgnoringCase("completed");

        var issuesPage = scansPage.tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();

        var issuesCount = WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual,
                0, issuesPage::getAllCount, Duration.ofSeconds(90), true);

        AssertionsForClassTypes.assertThat(issuesCount).as("There should be issue findings").isPositive();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test Data preparation")
    @Test(groups = {"regression"})
    public void createTenant() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Allow scanning with no entitlements",
                        //"Scan Third Party Libraries",
                        "Enable Source Download"
                });
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
    }

    @Owner("sbehera3@opentext.com")
    @MaxRetryCount(1)
    @FodBacklogItem("765001")
    @FodBacklogItem("583007")
    @Severity(SeverityLevel.NORMAL)
    @Description("ReactNative and IAC New tech stack in mobile/web app")
    @Test(groups = {"regression"}, dependsOnMethods = {"createTenant"},
            dataProvider = "payloadFiles")
    public void staticScanWithDifferentTechStackTest(FodCustomTypes.TechnologyStack techStack, String filePath) {

        StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();
        String techStackName = techStack.getTypeValue();
        ApplicationDTO applicationDTO;
        staticScanDTO.setTechnologyStack(techStack);
        staticScanDTO.setFileToUpload(filePath);
        if (techStack.equals(FodCustomTypes.TechnologyStack.ReactNative)) {
            applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        } else {
            applicationDTO = ApplicationDTO.createDefaultInstance();
        }
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));
        var setupPage = StaticScanActions.createStaticScan(staticScanDTO, applicationDTO
                , FodCustomTypes.SetupScanPageStatus.InProgress);

        AllureReportUtil.info("User should be able to select " + techStackName + " in Technology Stack drop-down");
        assertThat(setupPage.getAdvancedSettingsPanel().expand().getTechnologyStackDropdown().getSelectedOption())
                .as("Verify user should be able to select " + techStackName + " in Technology Stack drop-down")
                .isEqualTo(techStackName);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("User should be able to run static scan with " + techStackName + " tech stack successfully in FoD");
        assertThat(LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByAppName(applicationDTO.getApplicationName(), true)
                .getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded)
                .getStatus())
                .as("Validate scan job row status should change to ImportSucceeded")
                .isEqualTo(FodCustomTypes.JobStatus.ImportSucceeded.getTypeValue());

        AllureReportUtil.info("User should be able to update Static Scan Setup with " + techStackName + " tech stack via API");
        FodApiActions apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                        FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(),
                AccessScopeRestrictionsFodApi.API_TENANT);
        StaticScanPayload staticPayload = StaticScanPayload.defaultJavaScanInstance();
        staticPayload.setLanguageLevel(null);
        staticPayload.setEntitlementFrequencyType(FodCustomTypes.EntitlementFrequencyType.Subscription.getTypeValue());
        staticPayload.setTechnologyStack(techStack.getTypeValue());
        Response startScanResponse = apiActions.startStaticScan(releaseId, staticPayload,
                filePath);

        AllureReportUtil.info("User should be able to run static scan with " + techStackName + " tech stack successfully via API");
        var scanId = startScanResponse.jsonPath().getInt("scanId");
        Response scanSummaryInfoResponse = apiActions
                .getResponseByStatusCode(20, HTTP_OK, () -> apiActions.getReleasesApiProvider()
                        .getScan(releaseId, scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        assertThat(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"))
                .as("Validate scan should have status as In Progress or Queued")
                .isTrue();
    }

    @MaxRetryCount(1)
    @Owner("kbadia@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItems({@FodBacklogItem("833003"), @FodBacklogItem("783006")})
    @Parameters({"techStack", "languageLevel", "filePath"})
    @Description("Static scans should work for .NET 7 and Python 3 (Django), and scan results should not be empty")
    @Test(groups = {"regression"}, dataProvider = "techStackData")
    public void techStackScanTest(FodCustomTypes.TechnologyStack techStack, String languageLevel, String filePath) {
        StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(techStack);
        staticScanDTO.setLanguageLevel(languageLevel);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload(filePath);
        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationDTO applicationDTO = ApplicationActions.createApplication();
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();
        var scansPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName()).openScans();
        assertThat(scansPage.getScanByType(FodCustomTypes.ScanType.Static).getStatus())
                .as("Scan should be completed")
                .isEqualToIgnoringCase("completed");
        var issuesPage = scansPage.tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        assertThat(WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual,
                0, issuesPage::getAllCount, Duration.ofSeconds(90), true))
                .as("There should be issue findings")
                .isPositive();
    }

    @DataProvider(name = "techStackData", parallel = true)
    public Object[][] techStackData() {
        return new Object[][]{
                {FodCustomTypes.TechnologyStack.PYTHON, "3 (Django)", "payloads/fod/Python_Django_3.zip"},
                {FodCustomTypes.TechnologyStack.DotNet, "7.0", "payloads/fod/dotNET_7.zip"}
        };
    }

    @DataProvider(name = "payloadFiles", parallel = true)
    public Object[][] payloadFiles() {
        return new Object[][]{
                {FodCustomTypes.TechnologyStack.ReactNative, "payloads/fod/react-native-wordle-main.zip"},
                {FodCustomTypes.TechnologyStack.Infrastructure, "payloads/fod/10JavaDefects.zip"},
                {FodCustomTypes.TechnologyStack.Infrastructure, "payloads/fod/dockerfiles-master.zip"},
                {FodCustomTypes.TechnologyStack.Infrastructure, "payloads/fod/XML-JAVA.zip"},
                {FodCustomTypes.TechnologyStack.Infrastructure, "payloads/fod/json-develop.zip"}
        };
    }
}