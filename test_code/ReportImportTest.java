package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReportDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class ReportImportTest extends FodBaseTest {

    ReportDTO reportDTO;

    @MaxRetryCount(3)
    @Description("AUTO-TAM should create PDF report, download it, import it and compare it")
    @Test(groups = {"hf", "regression"})
    public void downLoadAndComparePDFTest() {
        var useThisApplicationDTO = executeAndCompleteDynamicScan();

        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setApplicationDTO(useThisApplicationDTO);
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openReports()
                .createReport(reportDTO);
        BrowserUtil.clearCookiesLogOff();

        var reports = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openReports()
                .getReportByName(reportDTO.getReportName());

        reports.waitForReportStatus(FodCustomTypes.ReportStatus.Completed);
        var reportFile = reports.downloadReport();
        assertThat(reportFile).isNotEmpty().hasExtension("pdf");
        BrowserUtil.clearCookiesLogOff();

        var reportsPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(useThisApplicationDTO.getApplicationName())
                .openReports();

        String pdfReportName = "PDFReport-" + UniqueRunTag.generate();
        reportsPage.pressImportReportBtn()
                .setReportName(pdfReportName)
                .setRelease(useThisApplicationDTO.getReleaseName())
                .uploadFile(reportFile.getAbsolutePath())
                .pressUpload();

        assertThat(reportFile).hasSameBinaryContentAs(reportsPage.downloadReport(pdfReportName));

    }

    public ApplicationDTO executeAndCompleteDynamicScan() {
        ApplicationDTO applicationDTOName = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTOName, defaultTenantDTO, true);
        DynamicScanActions.importDynamicScanTenant(applicationDTOName, "payloads/fod/DynSuper1.fpr");

        BrowserUtil.clearCookiesLogOff();
        return applicationDTOName;
    }
}
