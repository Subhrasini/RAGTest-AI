package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.exceptions.FodElementNotFoundException;
import com.fortify.fod.ui.pages.tenant.applications.OpenSourceComponentsPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.Ordering;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;


public class OSSComponentsSortAndFilterTest extends FodBaseTest {
    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Starting and checking the completion status for debricked and sonatype scans triggered")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        LogInActions.tamUserLogin(defaultTenantDTO);
        webApp = ApplicationDTO.createDefaultInstance();
        var ReleaseDetailsPage = ApplicationActions.createApplication(webApp);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/JavaDebrickedTest.zip");
        staticScanDTO.setOpenSourceComponent(true);
        var scanOverviewPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        scanOverviewPage.openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        ReleaseDetailsPage.openReleaseSettings().setToRunDebrickedScan().pressSave();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/JavaDebrickedTest.zip");
        staticScanDTO.setOpenSourceComponent(true);
        StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        scanOverviewPage.openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @FodBacklogItem("1740010")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the sort and filter options on Open Source Components page for Debricked and Sonatype scan")
    @Test(groups = {"Regression"}, dependsOnMethods = {"prepareTestData"})
    public void openSourceComponentsPageSortAndFilterTest() {
        YourApplicationsPage yourApplicationPage = LogInActions.tamUserLogin(defaultTenantDTO);

        AllureReportUtil.info("To Validate the filter options functionality on Open Source Components page for an Application");
        OpenSourceComponentsPage applicationOpenSourceComponentsPage = yourApplicationPage.openOpenSourceComponents();
        var scanToolTypeOptions = applicationOpenSourceComponentsPage.filters.openFilterContainer().expandAllFilters()
                .setFilterByName("Scan Tool")
                .getAllOptions();
        for (var scanToolTypeOption : scanToolTypeOptions) {
            switch (scanToolTypeOption) {
                case "Debricked":
                    applicationOpenSourceComponentsPage.filters.expandAllFilters().setFilterByName("Scan Tool")
                            .clickFilterOptionByName("Debricked");
                    assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("Scan Tool").getValue())
                            .as("All components which have scan tool type as Debricked should be displayed")
                            .isEqualTo("Debricked");
                    applicationOpenSourceComponentsPage.appliedFilters.clearAll();
                    break;
                case "sonatype":
                    applicationOpenSourceComponentsPage.filters.openFilterContainer().setFilterByName("Scan Tool")
                            .clickFilterOptionByName("sonatype");
                    assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("Scan Tool").getValue())
                            .as("All components which have scan tool type as sonatype should be displayed")
                            .isEqualTo("sonatype");
                    applicationOpenSourceComponentsPage.appliedFilters.clearAll();
                    break;
                default:
                    throw new FodElementNotFoundException(applicationOpenSourceComponentsPage + " scan tool type not found");
            }
            applicationOpenSourceComponentsPage = page(OpenSourceComponentsPage.class);
        }

        if (applicationOpenSourceComponentsPage.filters.openFilterContainer().getAllFilters().contains("License")) {
            var vulnerabilityLicenseOptions = applicationOpenSourceComponentsPage.filters
                    .setFilterByName("License").getAllOptions();
            applicationOpenSourceComponentsPage.filters.expand().setFilterByName("License")
                    .clickFilterOptionByName(vulnerabilityLicenseOptions.get(0));
            assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("License").getValue())
                    .as("Respective vulnerable License filtered records should be applied and displayed")
                    .isEqualTo(vulnerabilityLicenseOptions.get(0));
            applicationOpenSourceComponentsPage.appliedFilters.clearAll();
        }

        if (applicationOpenSourceComponentsPage.filters.openFilterContainer().getAllFilters().contains("Component")) {
            var vulnerabilityComponentOptions = applicationOpenSourceComponentsPage.filters
                    .setFilterByName("Component").getAllOptions();
            applicationOpenSourceComponentsPage.filters.expand().setFilterByName("Component")
                    .clickFilterOptionByName(vulnerabilityComponentOptions.get(0));
            assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("Component").getValue())
                    .as("Respective vulnerable components filtered records should be applied and displayed")
                    .isEqualTo(vulnerabilityComponentOptions.get(0));
            applicationOpenSourceComponentsPage.appliedFilters.clearAll();
        }

        if (applicationOpenSourceComponentsPage.filters.openFilterContainer().getAllFilters().contains("Scope")) {
            var scopeOptions = applicationOpenSourceComponentsPage.filters
                    .setFilterByName("Scope").getAllOptions();
            applicationOpenSourceComponentsPage.filters.expand().setFilterByName("Scope")
                    .clickFilterOptionByName(scopeOptions.get(0));
            assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("Scope").getValue())
                    .as("Respective scope filtered records should be applied and displayed")
                    .isEqualTo(scopeOptions.get(0));
            applicationOpenSourceComponentsPage.appliedFilters.clearAll();

            applicationOpenSourceComponentsPage.filters.openFilterContainer().expand().setFilterByName("Scope")
                    .clickFilterOptionByName(scopeOptions.get(1));
            assertThat(applicationOpenSourceComponentsPage.appliedFilters.getFilterByName("Scope").getValue())
                    .as("Respective scope filtered records should be applied and displayed")
                    .isEqualTo(scopeOptions.get(1));
            applicationOpenSourceComponentsPage.appliedFilters.clearAll();
        }

        AllureReportUtil.info("To Validate the sorting functionality on Open Source Components page for an Application");

        Ordering sorting = new Ordering(applicationOpenSourceComponentsPage.getTable());
        sorting.verifyOrderForColumn("Scan Tool");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Scan Tool ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("Component");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Component ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("Type");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Type ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("License");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on License ordering").contains("sortdir=Desc");
    }

    @FodBacklogItem("1740010")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the sort and filter options on application release open source components page for Debricked and Sonatype scan")
    @Test(groups = {"Regression"}, dependsOnMethods = {"prepareTestData"})
    public void releaseOSSPageSortAndFilterTest() {
        YourApplicationsPage yourApplicationPage = LogInActions.tamUserLogin(defaultTenantDTO);

        AllureReportUtil.info("To Validate the filter options functionality on application release open source components page for an Application");
        OpenSourceComponentsPage releaseOSSPage =  yourApplicationPage.openYourReleases().openDetailsForRelease(webApp)
                .openOpenSourceComponents();
        var scanToolTypeOptions = releaseOSSPage.filters.openFilterContainer().expandAllFilters()
                .setFilterByName("Scan Tool")
                .getAllOptions();
        for (var scanToolTypeOption : scanToolTypeOptions) {
            switch (scanToolTypeOption) {
                case "Debricked":
                    releaseOSSPage.filters.expandAllFilters().setFilterByName("Scan Tool")
                            .clickFilterOptionByName("Debricked");
                    assertThat(releaseOSSPage.appliedFilters.getFilterByName("Scan Tool").getValue())
                            .as("All components which have scan tool type as Debricked should be displayed")
                            .isEqualTo("Debricked");
                    releaseOSSPage.appliedFilters.clearAll();
                    break;
                case "sonatype":
                    releaseOSSPage.filters.openFilterContainer().setFilterByName("Scan Tool")
                            .clickFilterOptionByName("sonatype");
                    assertThat(releaseOSSPage.appliedFilters.getFilterByName("Scan Tool").getValue())
                            .as("All components which have scan tool type as sonatype should be displayed")
                            .isEqualTo("sonatype");
                    releaseOSSPage.appliedFilters.clearAll();
                    break;
                default:
                    throw new FodElementNotFoundException(releaseOSSPage + " scan tool type not found");
            }
            releaseOSSPage = page(OpenSourceComponentsPage.class);
        }

        if (releaseOSSPage.filters.openFilterContainer().getAllFilters().contains("License")) {
            var vulnerabilityLicenseOptions = releaseOSSPage.filters
                    .setFilterByName("License").getAllOptions();
            releaseOSSPage.filters.expand().setFilterByName("License")
                    .clickFilterOptionByName(vulnerabilityLicenseOptions.get(0));
            assertThat(releaseOSSPage.appliedFilters.getFilterByName("License").getValue())
                    .as("Respective vulnerable License filtered records should be applied and displayed")
                    .isEqualTo(vulnerabilityLicenseOptions.get(0));
            releaseOSSPage.appliedFilters.clearAll();
        }

        if (releaseOSSPage.filters.openFilterContainer().getAllFilters().contains("Component")) {
            var vulnerabilityComponentOptions = releaseOSSPage.filters
                    .setFilterByName("Component").getAllOptions();
            releaseOSSPage.filters.expand().setFilterByName("Component")
                    .clickFilterOptionByName(vulnerabilityComponentOptions.get(0));
            assertThat(releaseOSSPage.appliedFilters.getFilterByName("Component").getValue())
                    .as("Respective vulnerable components filtered records should be applied and displayed")
                    .isEqualTo(vulnerabilityComponentOptions.get(0));
            releaseOSSPage.appliedFilters.clearAll();
        }

        if (releaseOSSPage.filters.openFilterContainer().getAllFilters().contains("Scope")) {
            var scopeOptions = releaseOSSPage.filters
                    .setFilterByName("Scope").getAllOptions();
            releaseOSSPage.filters.expand().setFilterByName("Scope")
                    .clickFilterOptionByName(scopeOptions.get(0));
            assertThat(releaseOSSPage.appliedFilters.getFilterByName("Scope").getValue())
                    .as("Respective scope filtered records should be applied and displayed")
                    .isEqualTo(scopeOptions.get(0));
            releaseOSSPage.appliedFilters.clearAll();

            releaseOSSPage.filters.openFilterContainer().expand().setFilterByName("Scope")
                    .clickFilterOptionByName(scopeOptions.get(1));
            assertThat(releaseOSSPage.appliedFilters.getFilterByName("Scope").getValue())
                    .as("Respective scope filtered records should be applied and displayed")
                    .isEqualTo(scopeOptions.get(1));
            releaseOSSPage.appliedFilters.clearAll();
        }

        AllureReportUtil.info("To Validate the sorting functionality on application release open source components page for an Application");

        Ordering sorting = new Ordering(releaseOSSPage.getTable());
        sorting.verifyOrderForColumn("Scan Tool");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Scan Tool ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("Component");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Component ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("Type");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on Type ordering").contains("sortdir=Desc");

        sorting.verifyOrderForColumn("License");
        assertThat(WebDriverRunner.url()).as("The list gets sorted based on License ordering").contains("sortdir=Desc");
    }
}