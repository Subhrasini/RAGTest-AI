package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@FodBacklogItem("775061")
@Slf4j
public class SnippetsImportTest extends FodBaseTest {
    TenantDTO snippetTenantDTO, simpleTenantDTO;
    ApplicationDTO snippetApplicationDTO, simpleApplicationDTO;
    StaticScanDTO staticScanDTO;
    String snippetTenantId;
    Integer snippetScanId, simpleScanId;
    String query = "SELECT * FROM FVDLSnippet WHERE ScanID = '";

    private void init() {
        snippetTenantDTO = TenantDTO.createDefaultInstance();
        snippetTenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        simpleTenantDTO = TenantDTO.createDefaultInstance();
        simpleTenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        snippetApplicationDTO = ApplicationDTO.createDefaultInstance();
        simpleApplicationDTO = ApplicationDTO.createDefaultInstance();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload("payloads/fod/SmallJavaTestPayload.zip");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("This test initializes the test environment by creating tenants with and without snippet support, " +
            "setting site settings, and performing static scans.")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        init();
        log.info("Create Tenants with and without snippet");
        TenantActions.createTenants(true, snippetTenantDTO, simpleTenantDTO);

        log.info("Retrieve the Tenant ID and add it to the 'SnippetEnabledTenantIds' site setting");
        snippetTenantId = new FodSQLUtil().getTenantIdByName(snippetTenantDTO.getTenantName());
        SiteSettingsActions.setValueInSettings("SnippetEnabledTenantIds", snippetTenantId, false, false);
        BrowserUtil.clearCookiesLogOff();

        log.info("Create Application, start, complete Static Scan for Tenant with Snippet setting enabled and " +
                "retrieve ScanId for static scan");
        ApplicationActions.createApplication(snippetApplicationDTO, snippetTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, snippetApplicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(snippetApplicationDTO, true);
        snippetScanId = new ScanJobsPage().getLatestScanByAppDto(snippetApplicationDTO).getId();
        BrowserUtil.clearCookiesLogOff();

        log.info("Create Application, start, complete Static Scan for Tenant with Snippet setting disabled and " +
                "retrieve ScanId for static scan");
        ApplicationActions.createApplication(simpleApplicationDTO, simpleTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, simpleApplicationDTO);
        StaticScanActions.completeStaticScan(simpleApplicationDTO, true);
        simpleScanId = new ScanJobsPage().getLatestScanByAppDto(simpleApplicationDTO).getId();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("This test verifies that the database entries for code snippets are correctly populated after static" +
            "scan import.")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void snippetsImportTestAfterStaticImport() {
        assertThat(new FodSQLUtil().getStringValueFromDB(query + snippetScanId + "'"))
                .as("ScanID " + snippetScanId + " should have results")
                .isNotBlank();
        assertThat(new FodSQLUtil().getStringValueFromDB(query + simpleScanId + "'"))
                .as("ScanID " + simpleScanId + " should not have results")
                .isEmpty();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("This test imports a static scan on Tenant site and verifies the visibility of code snippets and" +
            "database entries.")
    @Test(groups = {"regression"},
            dependsOnMethods = "snippetsImportTestAfterStaticImport", dataProvider = "tenantsAndScans")
    public void snippetsImportTest(TenantDTO tenant, String scanFileName, boolean shouldHaveResults) {
        log.info("Logging in as a TAM user and importing a static scan.");
        LogInActions.tamUserLogin(tenant);
        var page = TenantScanActions
                .importScanTenant(ApplicationActions.createApplication(),
                        "payloads/fod/" + scanFileName, FodCustomTypes.ScanType.Static)
                .openIssues()
                .waitTillAllCountGreaterThanOrEqual(250)
                .clickAll();

        log.info("Extracting the Release ID from the current URL and retrieving the corresponding Scan ID from the database.");
        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Issues")));
        String scanId = new FodSQLUtil()
                .getStringValueFromDB("SELECT * FROM ProjectVersionScan WHERE ProjectVersionId = '"
                        + releaseId + "'");

        log.info("Open Code tab in Issues Page");
        var codeCell = page.getIssues()
                .get(0)
                .openDetails()
                .openCode();

        log.info("Verifying that the code section is visible on the page and that no error message is displayed." +
                "Checking the database for expected results based on the scan type.");
        assertThat(codeCell.isCodeSectionVisible())
                .as("Code section should be populated")
                .isTrue();
        assertThat(page.getMainText())
                .as("Error message should not be displayed")
                .doesNotContain("(The source code associated with this issue was not included in the scan submission.)");
        String dbResult = new FodSQLUtil().getStringValueFromDB(query + scanId + "'");
        if (shouldHaveResults) {
            assertThat(dbResult).isNotBlank();
        } else {
            assertThat(dbResult).isBlank();
        }
    }

    @DataProvider(name = "tenantsAndScans")
    public Object[][] tenantsAndScans() {
        return new Object[][]{
                {snippetTenantDTO, "WebGoat50withoutsource.fpr", true},
                {simpleTenantDTO, "WebGoat50withoutsource.fpr", true},
                {snippetTenantDTO, "WebGoat50withsource.fpr", true},
                {simpleTenantDTO, "WebGoat50withsource.fpr", false}
        };
    }
}