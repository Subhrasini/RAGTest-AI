package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("731004")
public class OpenSourceScanLabelCheckTest extends FodBaseTest {
    ApplicationDTO applicationDTO;

    List<String> expectedScanTypes = new ArrayList<String>() {{
        add("Static");
        add("Open Source");
        add("Dynamic");

    }};

    List<String> expectedScanTypesPlusAviator = new ArrayList<String>() {{
        add("Static");
        add("Open Source");
        add("Dynamic");
        add("Aviator");
    }};

    List<String> expectedScanTypesPlusMobile = new ArrayList<String>() {{
        add("Static");
        add("Open Source");
        add("Dynamic");
        add("Mobile");
    }};

    @MaxRetryCount(2)
    @Description("TAM should be able to create Application")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(2)
    @Description("Validate column order of Static, Open Source, Dynamic on Your Applications page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkOpenSourceStatusYourApplications() {
        var getTheScanTypes = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .findWithSearchBox(applicationDTO.getApplicationName())
                .getAppByName(applicationDTO.getApplicationName())
                .getScanTypesList();
        assertThat(getTheScanTypes)
                .as(getTheScanTypes + " should match " + expectedScanTypesPlusAviator)
                .isEqualTo(expectedScanTypesPlusAviator);
    }

    @MaxRetryCount(2)
    @Description("Validate column order of Static, Open Source, Dynamic on Applications Details page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkOpenSourceStatusApplicationDetail() {
        var detailsPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .findWithSearchBox(applicationDTO.getApplicationName())
                .getAppByName(applicationDTO.getApplicationName())
                .openDetails();

        var columnsList = detailsPage.releasesTable.getColumnHeaders();
        assertThat(columnsList)
                .as(columnsList + " should contains " + expectedScanTypes)
                .containsAll(expectedScanTypes);

    }

    @MaxRetryCount(2)
    @Description("Validate column order of Static, Open Source, Dynamic, and Mobile on Your Releases page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkOpenSourceStatusYourReleases() {
        var yourReleasesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases();

        var columnsList = yourReleasesPage.getTable().getColumnHeaders();
        assertThat(columnsList)
                .as(columnsList + " should contains " + expectedScanTypesPlusMobile)
                .containsAll(expectedScanTypesPlusMobile);
    }

    @MaxRetryCount(0)
    @Description("Validate column order of Static, Open Source, Dynamic on Release Details page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkOpenSourceStatusReleaseDetail() {
        var getScanList = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .getScanTypesList();

        assertThat(getScanList)
                .as(getScanList + " should match " + expectedScanTypesPlusAviator)
                .isEqualTo(expectedScanTypesPlusAviator);
    }
}
