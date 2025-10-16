package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.fortify.common.ui.config.AllureAttachmentsUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReportDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReportActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;
import static com.codeborne.selenide.files.FileFilters.withExtension;

@Owner("ysmal@opentext.com")
@Slf4j
public class SonatypeIssueDetailsTest extends FodBaseTest {

    ApplicationDTO application;
    StaticScanDTO staticScanDTO;
    String staticPayload = "payloads/fod/10JavaDefects_Small(OS).zip";
    String staticFpr = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    String templateType = "Open Source";
    String templateName;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create application and open source scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        application = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAssessmentType("Static Standard");
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setFileToUpload(staticPayload);
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(application, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, application);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(application, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(application)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should be able to create open source type report")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void createAndValidateReportIssueDetail() {
        templateName = "AutoReport" + UniqueRunTag.generate();
        var templatesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openReports().openTemplates();

        templatesPage.createTemplate(templateName, templateType,
                "Issue Detail (Extended)", "Issue Detail");

        var report = ReportDTO.createInstance(application, templateName);
        var reportsPage = ReportActions.createReport(report);
        var file = reportsPage.getReportByName(report.getReportName())
                .getActionButton(Actions.ReportAction.Download)
                .download(Duration.ofMinutes(3).toMillis(), withExtension("pdf"));
        PDF reportFile = new PDF(file);
        AllureAttachmentsUtil.attachFile(file, report.getReportName(), "pdf", "application/pdf");
        assertThat(reportFile).containsExactText("Summary");
        assertThat(reportFile).containsExactText("Component Name");
        assertThat(reportFile).containsExactText("Component Version");
        assertThat(reportFile).containsExactText("CVSS Base Score");
        assertThat(reportFile).containsExactText("Issue Detail");
        assertThat(reportFile).containsExactText("CVE-2011-3923");
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should be able to create open source type report")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void createAndValidateReportOWASP() {
        templateName = "AutoReport" + UniqueRunTag.generate();
        var templatesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openReports().openTemplates();

        templatesPage.createTemplate(templateName, templateType,
                "OWASP Top 10 2017", "PCI 3.2 Executive Summary", "PCI 3.2 Issue Breakdown");

        var report = ReportDTO.createInstance(application, templateName);
        var reportsPage = ReportActions.createReport(report);
        var file = reportsPage.getReportByName(report.getReportName())
                .getActionButton(Actions.ReportAction.Download)
                .download(Duration.ofMinutes(3).toMillis(), withExtension("pdf"));
        PDF reportFile = new PDF(file);
        AllureAttachmentsUtil.attachFile(file, report.getReportName(), "pdf", "application/pdf");
        assertThat(reportFile).containsExactText("A9 - Using Components with Known");
        assertThat(reportFile).containsExactText("6.2 Ensure all system components and softwar");
        assertThat(reportFile).containsExactText("Open");
        assertThat(reportFile).containsExactText("Source");
        assertThat(reportFile).containsExactText("PCI DSS 3.2 Compliance Overview");
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("332011")
    @Description("Sonatype Opt-In Text and Tab should not be present under Settings page")
    @Test(groups = {"regression"})
    public void verifySonatypeTabRemovedTest() {
        var settingsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openSettings();
        var sonatypeTab = settingsPage.getTabByName("Sonatype").exists();
        Assertions.assertThat(sonatypeTab)
                .as("Verify Sonatype tab is not exists under settings page")
                .isFalse();
    }
}
