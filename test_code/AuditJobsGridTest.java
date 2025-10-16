package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminSideNavTabs;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.popups.ReleaseScanPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.util.ArrayList;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AuditJobsGridTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO firstApp;
    ApplicationDTO secondApp;
    ApplicationDTO thirdApp;

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate functionality of auditor view page")
    @Test(groups = {"regression"})
    public void auditJobsGridTest() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        firstApp = ApplicationDTO.createDefaultInstance();
        secondApp = ApplicationDTO.createDefaultInstance();
        thirdApp = ApplicationDTO.createDefaultInstance();

        ApplicationActions.createApplication(firstApp, tenantDTO, true);
        ApplicationActions.createApplication(secondApp, tenantDTO, false);
        ApplicationActions.createApplication(thirdApp, tenantDTO, false);
        var scanDtoSecond = StaticScanDTO.createDefaultInstance();
        scanDtoSecond.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(scanDtoSecond, firstApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        StaticScanActions.createStaticScan(scanDtoSecond, secondApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        StaticScanActions.createStaticScan(scanDtoSecond, thirdApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var jobsPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        assertThat(jobsPage.getTabByName(AdminSideNavTabs.Static.AuditorView.getTabName()).exists())
                .as("Auditor view tab should be present")
                .isTrue();

        for (int i = 0; i < 3; i++) {
            jobsPage.appliedFilters.clearAll();
            jobsPage.findWithSearchBox(tenantDTO.getTenantName());
            jobsPage.getAllScans().get(i).getLastJob()
                    .waitForJobStatus(FodCustomTypes.JobStatus.Success, FodCustomTypes.JobStatus.ImportSucceeded,
                            FodCustomTypes.JobStatus.AuditRejected)
                    .clickAction(Actions.JobAction.ReleaseToAudit);
        }

        var auditorViewPage = jobsPage.openAuditorView();

        for (var app : new String[]{
                firstApp.getApplicationName(),
                secondApp.getApplicationName(),
                thirdApp.getApplicationName()}) {

            auditorViewPage.findWithSearchBox(app);
            assertThat(auditorViewPage.getAllScans())
                    .as("Search by " + app + "should return only 1 row")
                    .hasSize(1);
        }

        auditorViewPage.findWithSearchBox(tenantDTO.getTenantName());
        assertThat(auditorViewPage.getAllScans())
                .as("Search by " + tenantDTO.getTenantName() + "should return 3 rows")
                .hasSize(3);

        auditorViewPage.clearSearchBox();

        var expectedColumnHeaders = new ArrayList<String>() {{
            add("SLA");
            add("Scan ID");
            add("Job ID");
            add("Tenant");
            add("Application");
            add("Release");
            add("Language");
            add("Version");
            add("Owner");
            add("Assessment Type");
            add("Exclude Third Party Libraries");
            add("Audit Preference");
            add("Status");
            add("Action");
        }};
        assertThat(auditorViewPage.table.getColumnHeaders())
                .hasSameElementsAs(expectedColumnHeaders);

        var ordering = new Ordering(auditorViewPage.table);

        ordering.verifyOrderForColumn("Scan ID");
        ordering.verifyOrderForColumn("Job ID");
        ordering.verifyOrderForColumn("Tenant");
        ordering.verifyOrderForColumn("Application");
        ordering.verifyOrderForColumn("Release");
        ordering.verifyOrderForColumn("Language");
        ordering.verifyOrderForColumn("Version");
        ordering.verifyOrderForColumn("Owner");
        ordering.verifyOrderForColumn("Assessment Type");
        ordering.verifyOrderForColumn("Audit Preference");
        ordering.verifyOrderForColumn("Status");

        auditorViewPage = auditorViewPage.openAuditorView();
        auditorViewPage.findWithSearchBox(firstApp.getApplicationName());

        var firstScan = auditorViewPage.getAllScans().get(0);
        assertThat(firstScan.getOwner()).isEqualTo("NA");
        assertThat(firstScan.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.AuditPending.getTypeValue());
        assertThat(firstScan.getActionButton(Actions.JobAction.ClaimAudit)).isNotNull();

        firstScan.clickAction(Actions.JobAction.ClaimAudit);
        assertThat(firstScan.getActionButton(Actions.JobAction.AuditInAwb)).isNotNull();
        assertThat(firstScan.getActionButton(Actions.JobAction.UnclaimAudit)).isNotNull();
        assertThat(firstScan.getActionButton(Actions.JobAction.MarkCompleted)).isNotNull();
        assertThat(firstScan.getActionButton(Actions.JobAction.RejectAudit)).isNotNull();
        refresh();
        assertThat(firstScan.updateRow().getOwner().toLowerCase()).contains("zeusadmin");
        assertThat(firstScan.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.Auditing.getTypeValue());

        firstScan.clickAction(Actions.JobAction.AuditInAwb);

        var modal = page(ModalDialog.class);
        if (modal.modalElement.isDisplayed()) {
            modal.clickButtonByText("Close");
        }

        var awbLink = $x("//*[@class = 'SPITabContentGroup']/div/a");
        var responseCode = given()
                .when()
                .log().all()
                .get(awbLink.getAttribute("href"))
                .then()
                .extract()
                .response()
                .statusCode();

        assertThat(awbLink.text().trim()).isEqualTo("Click here if the click-once application doesn't launch.");
        assertThat(responseCode).isEqualTo(200);

        back();
        firstScan.updateRow().clickAction(Actions.JobAction.UnclaimAudit);
        assertThat(firstScan.updateRow().getOwner()).isEqualTo("NA");
        assertThat(firstScan.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.AuditPending.getTypeValue());
        assertThat(firstScan.getActionButton(Actions.JobAction.ClaimAudit)).isNotNull();

        firstScan.clickAction(Actions.JobAction.ClaimAudit).clickAction(Actions.JobAction.MarkCompleted);
        assertThat(firstScan.updateRow().getOwner()).isEqualTo("NA");
        assertThat(firstScan.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.AuditComplete.getTypeValue());
        assertThat(firstScan.getActionButton(Actions.JobAction.ReleaseToAudit)).isNotNull();
        assertThat(firstScan.getActionButton(Actions.JobAction.ReleaseJob)).isNotNull();
        firstScan.clickAction(Actions.JobAction.ReleaseToAudit);
        assertThat(firstScan.updateRow().getOwner()).isEqualTo("NA");
        assertThat(firstScan.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.AuditPending.getTypeValue());
        assertThat(firstScan.getActionButton(Actions.JobAction.ClaimAudit)).isNotNull();

        firstScan.clickAction(Actions.JobAction.ClaimAudit)
                .clickAction(Actions.JobAction.MarkCompleted)
                .clickAction(Actions.JobAction.ReleaseJob);

        var releaseScanPopup = page(ReleaseScanPopup.class);
        assertThat(releaseScanPopup.popupElement.isDisplayed())
                .as("Release popup should be opened")
                .isTrue();
        releaseScanPopup.pressRelease();
        auditorViewPage.findWithSearchBox(firstApp.getApplicationName());

        assertThat(Table.isEmpty())
                .as("Released scan shouldn't displaying in the table")
                .isTrue();

        auditorViewPage.findWithSearchBox(secondApp.getApplicationName());

        var scanToReject = auditorViewPage.getAllScans().get(0);
        scanToReject.clickAction(Actions.JobAction.ClaimAudit)
                .clickAction(Actions.JobAction.RejectAudit);

        assertThat(scanToReject.getOwner()).isEqualTo("NA");
        assertThat(scanToReject.getStatus()).isEqualTo(FodCustomTypes.AuditViewJobStatus.AuditRejected.getTypeValue());

        refresh();
        auditorViewPage.findWithSearchBox(secondApp.getApplicationName());
        assertThat(Table.isEmpty())
                .as("Released scan shouldn't displaying after rejection")
                .isTrue();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate paging and pagination functionality on auditor view page")
    @Test(groups = {"regression"}, dependsOnMethods = {"auditJobsGridTest"},
            alwaysRun = true)
    public void auditJobsPagePagingTest() {
        LogInActions.adminLogIn().adminTopNavbar.openStatic().openAuditorView();
        new PagingActions().validatePaging();
        new Paging().setRecordsPerPage(100);
    }
}
