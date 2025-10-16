package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.DastBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("dgochev@opentext.com")
@DastBacklogItem("988001")

public class ExportSiteTreeTest extends FodBaseTest {

    private ApplicationDTO app;
    private DynamicScanDTO scanDto;
    private final String fileName = "payloads/fod/dynamic_with_traffic_monitor.scan";
    private FodApiActions apiActions;
    private final String JSON_VALIDATION_PATH = "fod/JSON_schema_validation/scans/%s";

    @BeforeClass
    public void setup() {
        app = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(app, defaultTenantDTO, true);
        scanDto = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(scanDto, app, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .openDetailsFor(app.getReleaseName())
                .importScan(fileName, false, true, false)
                .publishScan();
        BrowserUtil.clearCookiesLogOff();

        apiActions = FodApiActions.init(new UserPayload(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD),
                defaultTenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify site tree files from application page")
    @Test(groups = {"regression"})
    public void exportSiteTreeFromAppPage() {
         var scan = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openApplications()
                .openDetailsFor(app.getApplicationName())
                .openScans()
                .getScanByAppDto(app);

         var csvFile = scan.downloadSiteTreeCSV();
         var jsonFile = scan.downloadSiteTreeJSON();

        SoftAssertions softAssert = new SoftAssertions();
        softAssert.assertThat(csvFile.length()).as("CSV file is empty").isNotZero();
        softAssert.assertThat(jsonFile.length()).as("JSON file is empty").isNotZero();
        softAssert.assertAll();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify site tree files from application release page")
    @Test(groups = {"regression"})
    public void exportSiteTreeFromReleasePage() {
         var scan = LogInActions.tamUserLogin(defaultTenantDTO)
                 .tenantTopNavbar.openApplications()
                 .openDetailsFor(app.getApplicationName())
                 .getReleaseByName(app.getReleaseName())
                 .openReleaseDetails()
                 .openScans()
                 .getFirstScan();

         var csvFile = scan.downloadSiteTreeCSV();
         var jsonFile = scan.downloadSiteTreeJSON();

        SoftAssertions softAssert = new SoftAssertions();
        softAssert.assertThat(csvFile.length()).as("CSV file is empty").isNotZero();
        softAssert.assertThat(jsonFile.length()).as("JSON file is empty").isNotZero();
        softAssert.assertAll();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify site tree files from API")
    @Test(groups = {"regression"})
    public void exportSiteTreeFromApi() {
        Response response = apiActions.getApplicationsApiProvider().getApplications();
        var appId = response.jsonPath().getList("items", ApplicationPayload.class)
                .stream().filter(e -> e.getApplicationName().equals(app.getApplicationName()))
                .map(e -> e.getApplicationId()).findFirst().get();

        int scanId = apiActions.getApplicationsApiProvider().getApplicationScans(appId)
                .jsonPath().getInt("items.scanId[0]");
        response = apiActions.getScansApiProvider().getScanTreeForRequestedScanId(scanId);
        response.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .assertThat().body(matchesJsonSchemaInClasspath(String.format(JSON_VALIDATION_PATH, "get_scan_site_tree.json")));

        assertThat(response.jsonPath().getInt("totalCount"))
                .as("Site tree response is empty")
                .isNotZero();

    }
}
