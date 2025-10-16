package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
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
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static utils.api.ResponseCodes.HTTP_CREATED;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("776071")
public class ManifestDownloadCancelledScanTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;
    FodApiActions apiActions;
    int scanID;

    @MaxRetryCount(2)
    @Description("Initiates scan of 'bad' static source")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setFileToUpload("payloads/fod/correct_doc.zip");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Canceled);
        scanID = LogInActions.adminLogIn().adminTopNavbar.openStatic().findScanByAppName(applicationDTO.getApplicationName(), true)
                .getId();
    }

    @MaxRetryCount(2)
    @Description("Checks that Manifest Download link is available via Release page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyDownloadManifestLinkReleaseScansPageTest() throws FileNotFoundException {
        assertThat(LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openScans()
                .getFirstScan()
                .downloadManifest())
                .as("Download Manifest link should be accessible and file should be downloaded")
                .exists();
    }

    @MaxRetryCount(2)
    @Description("Checks that Manifest Download link is available via Application page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyDownloadManifestLinkApplicationScansPageTest() throws FileNotFoundException {
        assertThat(LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static)
                .downloadManifest())
                .as("Download Manifest link should be accessible and file should be downloaded")
                .exists();
    }

    @MaxRetryCount(2)
    @Description("Checks that Manifest Download link is available via all Scans page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyDownloadManifestLinkAllScansPageTest() throws FileNotFoundException {
        assertThat(LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications()
                .openYourScans()
                .getAllScans(false)
                .get(0)
                .downloadManifest())
                .as("Download Manifest link should be accessible and file should be downloaded")
                .exists();
    }

    @MaxRetryCount(2)
    @Description("Checks that Manifest Download link is available via Admin portal static details page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyDownloadLinksAdminPortalTest() throws FileNotFoundException {
        var adminScanDetails = LogInActions.adminLogIn()
                .adminTopNavbar
                .openStatic()
                .findScanByAppName(applicationDTO.getApplicationName(), true)
                .openDetails();
        assertThat(adminScanDetails.downloadFullSourceManifestFile())
                .as("Full Source Manifest File link should be accessible and file should be downloaded")
                .exists();
        assertThat(adminScanDetails.downloadFilteredSourceManifestFile())
                .as("Filtered Source Manifest File link should be accessible and file should be downloaded")
                .exists();
    }

    @MaxRetryCount(2)
    @Description("Checks that Manifest Download link is available via API download manifest call")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyManifestDownloadAPITest() {
        apiActions = FodApiActions
                .init(new UserPayload("ZeusAdmin", FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
        Response response = apiActions.getScansApiProvider().downloadScanManifest(scanID);
        response.then().statusCode(anyOf(is(HTTP_OK), is(HTTP_CREATED)));
        Assert.assertNotNull(response);
    }
}
