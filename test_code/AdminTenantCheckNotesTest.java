package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("733001")
public class AdminTenantCheckNotesTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    DynamicScanDTO dynamicScanDTO;
    String notesString = "These are some notes from ZeusAdmin";

    @MaxRetryCount(1)
    @Description("Checking the Has Notes filter. Includes validating issue and text entered")
    @Test(groups = {"regression"})
    public void checkHasNotesFilterTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var dynamicScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar
                .openDynamic()
                .findWithSearchBox(applicationDTO.getApplicationName())
                .openDetailsFor(applicationDTO.getApplicationName());
        var dynamicScanIssuesPage = dynamicScanOverviewPage.openIssues();
        dynamicScanIssuesPage.filters.openSettings().addFilterByName("Has Notes");
        dynamicScanOverviewPage
                .pressImportScanButton()
                .uploadFile("payloads/fod/dynamic.zero.fpr")
                .pressUpload(false);
        WaitUtil.waitForTrue(() -> !dynamicScanOverviewPage.openIssues().getCriticalIssues().isEmpty(),
                Duration.ofSeconds(60), false);
        AllureReportUtil.info("Getting the title of the first critical issue for later use");
        var issueName = dynamicScanOverviewPage.openIssues().getCriticalIssues().get(0).openDetails().getTitle();
        var notesField = dynamicScanOverviewPage.openIssues()
                .getCriticalIssues()
                .get(0)
                .openDetails()
                .openNotes();
        notesField.setNotes(notesString);
        dynamicScanOverviewPage.pressPublishScanButton().pressOk();
        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openIssues();
        var settingsPopup = issuesPage.filters.openSettings();
        settingsPopup.openFiltersTab().addFilterByName("Has Notes");
        AllureReportUtil.info("Setting 'Has Notes' and Clicking 'true'");
        issuesPage.filters.expandAllFilters();
        issuesPage.filters.setFilterByName("Has Notes").clickFilterOptionByName("True");
        var tenantIssueName = issuesPage.getCriticalIssues().get(0).openDetails().getTitle();
        assertThat(issueName)
                .as("Ensuring that issue is the same as on the admin issue page")
                .isEqualTo(tenantIssueName);
        var notesCell = issuesPage.openIssues().getIssues().get(0).openDetails().openNotes();
        assertThat(notesCell.getNotes())
                .as("Ensuring notes match what was entered on admin issue page")
                .isEqualTo(notesString);

    }

}
