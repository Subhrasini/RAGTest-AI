package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ReportDTO;
import com.fortify.fod.common.entities.ReportTemplateDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReportActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Owner;
import lombok.SneakyThrows;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

@Owner("dgochev@opentext.com")
@FodBacklogItem("688009")
public class ReportTemplatePDFTest extends FodBaseTest {

    String releaseName;
    String payload = "payloads/fod/static.java.fpr";
    StaticScanDTO scanDTO = StaticScanDTO.createDefaultInstance();

    @SneakyThrows
    @MaxRetryCount(3)
    @Test(groups = {"regression"})
    public void ReportTemplatePDFTest() {
        var template = ReportTemplateDTO.createDefaultInstance();
        scanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Automated);
        var templatesPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openReports().openTemplates().createTemplate(template);

        assertThat(template.getName())
                .as("Verify that template is in template list").isIn(templatesPage.getAllTemplateNames());

        var application = ApplicationActions.createApplication();
        releaseName = application.getReleaseName();
        StaticScanActions.importScanTenant(application, payload);
        var report = ReportDTO.createInstance(application, template.getName());
        ReportActions.createReport(report);
        PDF file = new PDF(templatesPage.openReports().downloadReport(report.getReportName()));
        var categories = new TenantTopNavbar().openApplications().openDetailsFor(application.getApplicationName())
                .openIssues().clickHigh().groupBy("Category").getGroupHeaders();

        assertThat(file).as("Report should contains module \"Appendix - Descriptions of Key Terminology\"")
                .containsExactText("Appendix - Descriptions of Key Terminology")
                .as("Report should contains module \"Issue Detail\"")
                .containsExactText("Issue Detail")
                .as("Report should contains module \"Analysis Traces\"")
                .containsExactText("Analysis Traces");

        categories.forEach(c ->
                assertThat(file).as("Report should contains category \"" + c + "\"").containsExactText(c));

    }
}
