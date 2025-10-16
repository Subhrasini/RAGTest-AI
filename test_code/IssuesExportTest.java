package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.exceptions.FodFileDownloadException;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.download;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("816001")
@Owner("vdubovyk@opentext.com")
@Slf4j
public class IssuesExportTest extends FodBaseTest {

    ApplicationDTO sonatypeApp;
    StaticScanDTO sonatypeStaticScanDto;
    String sonatypeFilePath = "payloads/fod/10JavaDefects_Small(OS).zip";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , applications and entitlements for the test")
    @Test(groups = {"regression"}, enabled = false)
    public void prepareTestData() {
        sonatypeStaticScanDto = StaticScanDTO.createDefaultInstance();
        sonatypeStaticScanDto.setLanguageLevel("11");
        sonatypeStaticScanDto.setFileToUpload(sonatypeFilePath);
        sonatypeStaticScanDto.setOpenSourceComponent(true);

        sonatypeApp = ApplicationActions.createApplication(defaultTenantDTO, true);

        StaticScanActions
                .createStaticScan(sonatypeStaticScanDto, sonatypeApp, FodCustomTypes.SetupScanPageStatus.InProgress);

        StaticScanActions.completeStaticScan(sonatypeApp, true, true);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies issues export for sonatype findings")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"}, enabled = false)
    public void issuesExportTest() throws FileNotFoundException {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(sonatypeApp.getApplicationName())
                .openIssues().pressExportButton().pressYes();
        new ModalDialog().pressClose();
        refresh();

        var downloadedByLink = generateUniqueLinkAdnDownload();
        assertThat(new CSVHelper(downloadedByLink).getColumnHeaders())
                .as("Verifying presence of 'Audits', 'File Locations' column")
                .contains("Audits", "File Locations");

        String expectedAuditEntry = "Changed Auditor Status from 'Pending Review' to 'Remediation Required'\n" +
                "Changed Auditor Status from 'Remediation Required' to 'Remediation Deferred'\n" +
                "Changed Auditor Status from 'Remediation Deferred' to 'Risk Mitigated'\n" +
                "Changed Auditor Status from 'Risk Mitigated' to 'Risk Accepted'\n" +
                "Changed Issue from 'Unsuppressed to 'Suppressed'\n" +
                "Changed Auditor Status from 'Risk Accepted' to 'Not an Issue'";
        List<String> auditValues = new CSVHelper(downloadedByLink).getColumnValues("Audits");
        assertThat(auditValues)
                .as("Verifying that the 'Audits' column contains the expected audit entries with line breaks")
                .anyMatch(value -> value.contains(expectedAuditEntry));

        var releasesExportFile = new TenantTopNavbar().openApplications().openYourReleases().downloadReleasesExportFile();
        assertThat(new CSVHelper(releasesExportFile).getColumnHeaders())
                .as("Verifying presence of 'Audits', 'File Locations' column")
                .contains("Audits", "File Locations");
        auditValues = new CSVHelper(releasesExportFile).getColumnValues("Audits");
        assertThat(auditValues)
                .as("Verifying that the 'Audits' column contains the expected audit entries with line breaks")
                .anyMatch(value -> value.contains(expectedAuditEntry));
    }

    public File generateUniqueLinkAdnDownload() {
        Supplier<File> file = () -> {
            var tenantId = new FodSQLUtil().getTenantIdByName(defaultTenantDTO.getTenantName());
            var keyQuery = "SELECT UniqueKey FROM ExportTemplate WHERE TenantId='"
                    + tenantId
                    + "' and Name LIKE 'IssuesExport%' ORDER BY ModifiedDate DESC";
            var uniqueKey = new FodSQLUtil().getStringValueFromDB(keyQuery);
            var link = FodConfig.TEST_ENDPOINT_TENANT + "/Reports/EmailLinkExportFile/" + uniqueKey;
            try {
                return download(link, Duration.ofMinutes(3).toMillis());
            } catch (Exception | Error e) {
                e.printStackTrace();
                throw new FodFileDownloadException("File is not downloaded!");
            }
        };

        WaitUtil.waitForTrue(() -> file.get().getName().contains("csv"), Duration.ofMinutes(3), false);
        return file.get();
    }
}