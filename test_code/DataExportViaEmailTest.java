package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.applications.OpenSourceComponentsPage;
import com.fortify.fod.ui.pages.tenant.applications.application.issues.ApplicationIssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.*;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

public class DataExportViaEmailTest extends FodBaseTest {

    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;
    static MailUtil mailUtil = new MailUtil();
    String targetToList_TAM = "tam@fod.auto";

    @FodBacklogItem("1772006")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the Data Export email functionality from Release Issues, Application Issues and Open Source Components page")
    @Test(groups = {"hf"})
    public void dataExportViaEmailTest() {

        LogInActions.tamUserLogin(defaultTenantDTO);
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/JavaDebrickedTest.zip");
        staticScanDTO.setOpenSourceComponent(true);
        var scanOverviewPage = StaticScanActions
                .createStaticScan(staticScanDTO, webApp, FodCustomTypes.SetupScanPageStatus.Completed);
        scanOverviewPage.openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        SoftAssertions softAssertions = new SoftAssertions();
        ModalDialog modalDialog = new ModalDialog();

        AllureReportUtil.info("To verify the data export email for opensource components in tenant site");
        OpenSourceComponentsPage openSourceComponentsPage = scanOverviewPage.tenantTopNavbar.openApplications().openOpenSourceComponents();
        openSourceComponentsPage.pressExportButton().pressYes();
        modalDialog.pressClose();

        String opensourceExportTargetSubject = "[OpenText™ Core Application Security] Data Export is Ready - " + defaultTenantDTO.getTenantName();
        var extractedOpensourceExportEmailContent = mailUtil.findEmailByRecipientAndSubject(opensourceExportTargetSubject, targetToList_TAM);
        softAssertions.assertThat(mailUtil.getEmailByMailSubjectAndRecipient(extractedOpensourceExportEmailContent, opensourceExportTargetSubject, targetToList_TAM))
                .as("Verify TAM should get the mail for the data export from opensource components page")
                .contains("Your issues export for " + defaultTenantDTO.getTenantName() + " has completed and is now available for download.  " +
                        "The link below will be valid for the next 7 days.");

        AllureReportUtil.info("To verify the data export email for release issues page");
        ReleaseIssuesPage issuesPage = openSourceComponentsPage.tenantTopNavbar.openApplications().getAppByName(webApp.getApplicationName())
                .openDetails().getReleaseByName(webApp.getReleaseName()).openReleaseDetails().openIssues();
        issuesPage.pressExportButton().pressYes();
        modalDialog.pressClose();

        String releaseExportTargetSubject = "[OpenText™ Core Application Security] Data Export is Ready - " + webApp.getApplicationName();
        var extractedReleaseIssuesExportEmailContent = mailUtil.findEmailByRecipientAndSubject(releaseExportTargetSubject, targetToList_TAM);
        softAssertions.assertThat(mailUtil.getEmailByMailSubjectAndRecipient(extractedReleaseIssuesExportEmailContent, releaseExportTargetSubject, targetToList_TAM))
                .as("Verify TAM should get the mail for the data export from release issues page")
                .contains("Your issues export for " + webApp.getApplicationName() + " has completed and is now available for download.  " +
                        "The link below will be valid for the next 7 days.");

        AllureReportUtil.info("To verify the data export email for application issues page");
        ApplicationIssuesPage applicationIssuesPage = issuesPage.tenantTopNavbar.openApplications().getAppByName(webApp.getApplicationName()).openDetails().openIssues();
        applicationIssuesPage.pressExportButton().pressYes();
        modalDialog.pressClose();

        String applicationExportTargetSubject = "[OpenText™ Core Application Security] Data Export is Ready - " + webApp.getApplicationName();
        var extractedApplicationIssuesExportEmailContent = mailUtil.findEmailByRecipientAndSubject(applicationExportTargetSubject, targetToList_TAM);
        softAssertions.assertThat(mailUtil.getEmailByMailSubjectAndRecipient(extractedApplicationIssuesExportEmailContent, applicationExportTargetSubject, targetToList_TAM))
                .as("Verify TAM should get the mail for the data export from application issues page")
                .contains("Your issues export for " + webApp.getApplicationName() + " has completed and is now available for download.  " +
                        "The link below will be valid for the next 7 days.");

        softAssertions.assertAll();
    }
}
