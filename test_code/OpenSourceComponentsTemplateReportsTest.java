package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.htmlValidator;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("agoyal3@opentext.com")
public class OpenSourceComponentsTemplateReportsTest extends FodBaseTest {
    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;
    TenantDTO tenantDTO;
    ReportDTO reportDTO;
    String[] expectedHeadingsInHTMLReport = {"Executive Summary", "Vulnerable Open Source Components", "" +
            "Open Source Bill-of-Materials", "Appendix - Descriptions of Key Terminology",
            "Security Rating", "Likelihood and Impact", "Likelihood",
            "Impact", "OpenText™ Core Application Security Priority Order", "Issue Status"};

    @MaxRetryCount(1)
    @Description("Creation of tenant, Debricked Entitlements and Sonatype Entitlements")
    @Test
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);

        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setQuantityPurchased(100);
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);

        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setQuantityPurchased(500);
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
    }

    @FodBacklogItem("1751004")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the Open Source Components Template Reports are being generating for Debricked scan")
    @Test(groups = {"Regression"}, dependsOnMethods = {"prepareTestData"})
    public void debrickedScanOpenSourceComponentsTemplateReports() {
        LogInActions.tamUserLogin(tenantDTO);
        webApp = ApplicationDTO.createDefaultInstance();
        var ReleaseDetailsPage = ApplicationActions.createApplication(webApp);

        ReleaseDetailsPage.openReleaseSettings().setToRunDebrickedScan().pressSave();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/JavaDebrickedTest.zip");
        staticScanDTO.setOpenSourceComponent(true);
        var scanOverviewPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        scanOverviewPage.openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        var scanTypeName = scanOverviewPage.openScans().getLastScanAssessmentType();

        AllureReportUtil.info("To Validate the status of PDF Debricked Open Source reports");
        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportName(scanTypeName + "-" + "PDF");
        reportDTO.setReportTemplate("Open Source Components");
        reportDTO.setFileType(FodCustomTypes.ReportFileType.PDF);
        reportDTO.setApplicationDTO(webApp);
        var reportsPage = ReportActions.createReport(reportDTO);

        assertThat(reportsPage.getReportByName(scanTypeName + "-" + "PDF").getStatus())
                .as("Verify the completion status of generated PDF report of the Debricked Scan")
                .isEqualTo("Completed");

        AllureReportUtil.info("To Validate the status of HTML Debricked Open Source reports");
        reportDTO.setReportName(scanTypeName + "-" + "HTML");
        reportDTO.setReportTemplate("Open Source Components");
        reportDTO.setFileType(FodCustomTypes.ReportFileType.HTML);
        reportsPage = ReportActions.createReport(reportDTO);

        assertThat(reportsPage.getReportByName(scanTypeName + "-" + "HTML").getStatus())
                .as("Verify the completion status of generated HTML report of the Debricked Scan")
                .isEqualTo("Completed");

    }

    @FodBacklogItem("1751004")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the Open Source Components Template Reports are being generating for Sonatype scan")
    @Test(groups = {"Regression"}, dependsOnMethods = {"prepareTestData"})
    public void sonatypeScanOpenSourceComponentsTemplateReports() {
        LogInActions.tamUserLogin(tenantDTO);
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/JavaDebrickedTest.zip");
        staticScanDTO.setOpenSourceComponent(true);
        var scanOverviewPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        scanOverviewPage.openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        var scanTypeName = scanOverviewPage.openScans().getLastScanAssessmentType();

        AllureReportUtil.info("To Validate the status of PDF Sonatype Open Source reports");
        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportName(scanTypeName + "-" + "PDF");
        reportDTO.setReportTemplate("Open Source Components");
        reportDTO.setFileType(FodCustomTypes.ReportFileType.PDF);
        reportDTO.setApplicationDTO(webApp);
        var reportsPage = ReportActions.createReport(reportDTO);

        assertThat(reportsPage.getReportByName(scanTypeName + "-" + "PDF").getStatus())
                .as("Verify the completion status of generated PDF report of the Sonatype Scan")
                .isEqualTo("Completed");

        AllureReportUtil.info("To Validate the status of HTML Sonatype Open Source reports");
        reportDTO.setReportName(scanTypeName + "-" + "HTML");
        reportDTO.setReportTemplate("Open Source Components");
        reportDTO.setFileType(FodCustomTypes.ReportFileType.HTML);
        reportsPage = ReportActions.createReport(reportDTO);

        assertThat(reportsPage.getReportByName(scanTypeName + "-" + "HTML").getStatus())
                .as("Verify the completion status of generated HTML report of the Sonatype Scan")
                .isEqualTo("Completed");

    }

    @FodBacklogItem("1752007")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the content of HTML report for Debricked scan")
    @Test(groups = {"hf"}, dependsOnMethods = {"prepareTestData", "debrickedScanOpenSourceComponentsTemplateReports"})
    public void htmlReportContentVerificationDebrickedScan() throws IOException, ZipException, InterruptedException {
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();

        AllureReportUtil.info("Download the report file");
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        File downloadedZippedReportFile = reportsPage.getReportByName("Debricked" + "-" + "HTML").downloadReport("zip");
        String unzippedFilePath = downloadedZippedReportFile.getParent();

        AllureReportUtil.info("Zip file is unzipped");
        new ZipFile(downloadedZippedReportFile.getPath()).extractAll(unzippedFilePath);

        var unzippedFiles = Arrays.stream(new File(unzippedFilePath).listFiles())
                .filter(file -> !file.getName().contains("zip")).collect(Collectors.toList());

        String htmlReportFilePath = unzippedFiles.get(0).getCanonicalPath();

        AllureReportUtil.info("Verify the extracted HTML file content and its structure");
        htmlValidator htmlValidation = new htmlValidator().loadHtml(htmlReportFilePath);

        assertThat(htmlValidation.validateTagPresence("table"))
                .as("Verify the table is present in the HTML report")
                .isTrue();

        assertThat(htmlValidation.validateHeadingsByLabel(expectedHeadingsInHTMLReport))
                .as("Verify all the required headings present")
                .isTrue();

        assertThat(htmlValidation.validateEssentialTags())
                .as("Verify essential tags are present in the HTML report")
                .isTrue();
    }

    @FodBacklogItem("1752007")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the content of HTML report for Sonatype scan")
    @Test(groups = {"hf"}, dependsOnMethods = {"prepareTestData", "sonatypeScanOpenSourceComponentsTemplateReports"})
    public void htmlReportContentVerificationSonatypeScan() throws IOException, ZipException {
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();

        AllureReportUtil.info("Download the report file");
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        File downloadedZippedReportFile = reportsPage.getReportByName("Sonatype" + "-" + "HTML").downloadReport("zip");
        String unzippedFilePath = downloadedZippedReportFile.getParent();

        AllureReportUtil.info("Zip file is unzipped");
        new ZipFile(downloadedZippedReportFile.getPath()).extractAll(unzippedFilePath);

        var unzippedFiles = Arrays.stream(new File(unzippedFilePath).listFiles())
                .filter(file -> !file.getName().contains("zip")).collect(Collectors.toList());

        String htmlReportFilePath = unzippedFiles.get(0).getCanonicalPath();

        AllureReportUtil.info("Verify the extracted HTML file content and its structure");
        htmlValidator htmlValidation = new htmlValidator().loadHtml(htmlReportFilePath);

        assertThat(htmlValidation.validateTagPresence("table"))
                .as("Verify the table is present in the HTML report")
                .isTrue();

        assertThat(htmlValidation.validateHeadingsByLabel(expectedHeadingsInHTMLReport))
                .as("Verify all the required headings present")
                .isTrue();

        assertThat(htmlValidation.validateEssentialTags())
                .as("Verify essential tags are present in the HTML report")
                .isTrue();
    }

}
