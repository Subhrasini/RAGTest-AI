package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.common.tenant.popups.TicketsPopup;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class ReleaseScansTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("When pausing a scan a ticket should be created and visible on tenant and admin sites, user should be able to leave comments, comments should be visible on both sites")
    @Test(groups = {"regression"})
    public void releaseScansTest() throws FileNotFoundException {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Allow scanning with no entitlements",
                        //"Scan Third Party Libraries",
                        "Enable Microservices option for web applications"
                });
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        var autoComment = "New Auto Comment";
        var scanSummaryValues = new String[][]{
                {"Scan Method", "Browser"},
                {"Scan Tool", "WebUI"},
                {"Pause Count", "1"},
                {"Started By User", "AUTO-TAM"},
                {"Audit Preference", "Manual"}
        };

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setFileToUpload("payloads/fod/static.java.zip");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setEntitlement("Subscription");
        staticScanDTO.setAssessmentType("Static Premium");

        applicationDTO = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        ApplicationActions.createApplication(applicationDTO, tenantDTO, true).tenantTopNavbar.openApplications();
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);

        var page = new TenantTopNavbar().openApplications()
                .openYourReleases();

        page.findWithSearchBox(applicationDTO.getApplicationName());

        var count = page.getActiveTable().getRowsCount();
        var scanStatus = page.getScanStatus(applicationDTO.getApplicationName(), FodCustomTypes.ScanType.Static);

        assertThat(count).as("There should be 1 scan").isEqualTo(1);
        assertThat(scanStatus)
                .as("Scan status should be In Progress or Queued").containsIgnoringCase("in-progress");

        BrowserUtil.openNewTab();
        Selenide.switchTo().window(1);

        var overViewPage = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByAppName(applicationDTO.getApplicationName())
                .openDetails()
                .pressPauseButton()
                .pressOkButton();

        Selenide.refresh();
        var statusText = overViewPage.getStatus();
        assertThat(statusText).as("Scan should be paused").containsIgnoringCase("Waiting - Customer");

        Selenide.switchTo().window(0);

        var scansPage = page.tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans().getAllScans(true)
                .get(0).waitForTicketsEnabled(15);

        TicketsPopup ticketsPopup;
        ticketsPopup = scansPage.openTickets(true);

        var ticketList = ticketsPopup.getAllTickets();
        assertThat(ticketList).as("There should be 1 ticket").hasSize(1);

        var status = ticketList.get(0).chooseTicket().getStatus();
        assertThat(status).as("Ticket Status should be 'Pending'").isEqualToIgnoringCase("Pending");

        var subject = ticketList.get(0).getSubject();
        assertThat(subject).as("Subject should be: Assessment Waiting for Customer Action")
                .isEqualToIgnoringCase("Assessment Waiting for Customer Action");

        ticketsPopup.addComment(autoComment).pressAddComment();
        var comment = ticketList.get(0).getAllDetails().get(0);
        var author = comment.getAuthor().toLowerCase(Locale.ROOT);
        assertThat(author.contains("auto") && author.contains("tam")).as("Comment should be from Auto Tam")
                .isTrue();
        assertThat(comment.getBody()).as("Comment body should be: New Auto Comment")
                .isEqualToIgnoringCase(autoComment);

        ticketsPopup.close();
        Selenide.switchTo().window(1);
        var ticketsPage = overViewPage.openTickets().waitForTickets();

        var adminSiteComment = ticketsPage.getAllTickets().get(0).getAllDetails().get(0);
        author = adminSiteComment.getAuthor().toLowerCase(Locale.ROOT);
        assertThat(author.contains("auto") && author.contains("tam")).as("Admin comment should be from Auto Tam")
                .isTrue();
        assertThat(adminSiteComment.getBody()).as("Comment body should be: New Auto Comment")
                .isEqualToIgnoringCase(autoComment);

        overViewPage = ticketsPage.openOverview();
        overViewPage.pressResumeButton().pressOkButton();
        var newStatus = overViewPage.getStatus();
        assertThat(newStatus).as("Scan should be in progress").containsIgnoringCase("In Progress");

        Selenide.switchTo().window(0);

        var appScansPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans();

        ticketsPopup = appScansPage.getAllScans(true).get(0).waitForTicketsEnabled(5)
                .openTicketsAndWaitStatus(true, "Solved");
        ticketList = ticketsPopup.getAllTickets();
        var solvedStatus = ticketList.get(0).getStatus();
        var solvedComment = ticketList.get(0).getAllDetails().get(0).getBody();
        assertThat(solvedStatus).as("Ticket Status should be 'Solved'").isEqualToIgnoringCase("Solved");
        assertThat(solvedComment).as("Ticket comment should contain: '...the scan has been moved back into the scanner Queue'")
                .containsIgnoringCase(String.format("Assessment for %s - %s has been moved back into the scanner Queue.",
                        applicationDTO.getApplicationName(), applicationDTO.getReleaseName()));
        var scanSummaryPopup = ticketsPopup.close().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans().getAllScans(true)
                .get(0).openDropdown().pressScanSummary();
        var previousValues = scanSummaryPopup.table.getAllColumnValues(2);
        for (var val : previousValues) {
            assertThat(val).as("Previous Value should be ---").isEqualTo("---");
        }

        for (var val : scanSummaryValues) {
            assertThat(scanSummaryPopup.getValueByName(val[0]))
                    .as("Verify scan summary values").isEqualTo(val[1]);
        }
        var scan = scanSummaryPopup.pressClose().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans().getAllScans(true).get(0);
        File manifest;
        manifest = scan.downloadManifest();
        var expectedManifestFileName = scan.getScanId() + "_manifest.txt";
        assertThat(manifest).as("File name should be {scan_id}_manifest.txt")
                .hasName(expectedManifestFileName);
        assertThat(manifest.length()).as("Manifest file should not be empty")
                .isGreaterThan(50);

        scan.cancelScan();
        Selenide.refresh();
        scan = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans().getAllScans(true).get(0);

        scan.waitStatus(FodCustomTypes.ScansPageStatus.CancelledByCustomer);
        assertThat(scan.getStatus()).as("Scan should have 'Cancelled by customer'")
                .isEqualTo("Cancelled by customer");

        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO);

        Selenide.switchTo().window(1);

        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), "payloads/fod/DynSuper5.fpr", true, true, true, false);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        Selenide.switchTo().window(0);
        Selenide.refresh();
        var detectedHostsPopup = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openScans().getScanByType(FodCustomTypes.ScanType.Dynamic).openDetectedHosts();

        assertThat(detectedHostsPopup.popupIsOpened()).as("Detected Hosts Popup should be opened").isTrue();
    }
}
