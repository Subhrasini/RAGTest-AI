package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("820007")
@Slf4j
public class OssComponentExportTest extends FodBaseTest {
    TenantUserDTO tenantEmailDTO;
    String targetToList;
    String targetSubject;
    ApplicationDTO applicationDTO;
    ReleaseDTO firstRelease, secondRelease;
    String jsonFile = "payloads/fod/21210_51134_cyclonedx.json";
    String confirmationMessage = "Data exports run in the background and may take up to several hours to process based on the size of" +
            " the export and system load. Once complete, you will receive an email to download the export. Do you want " +
            "to run the export?";
    String exportDownloadMessage = "The export has been queued. You will receive an email when it is ready to download.";
    String settingName = "OSSComponentExportRowsPerFile";
    String defaultRowCount = "10000";
    String updatedRowCount = "30";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant user and applications from tenant site")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantEmailDTO = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.SecurityLead);
        tenantEmailDTO.setTenant(defaultTenantDTO.getTenantName());
        applicationDTO = ApplicationDTO.createDefaultInstance();
        firstRelease = ReleaseDTO.createDefaultInstance();
        secondRelease = ReleaseDTO.createDefaultInstance();

        LogInActions.tamUserLogin(defaultTenantDTO);
        TenantUserActions.createTenantUser(tenantEmailDTO);
        targetToList = tenantEmailDTO.getUserEmail();
        targetSubject = "[OpenText™ Core Application Security] Data Export is Ready - " + defaultTenantDTO.getTenantName();

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, false);
        ReleaseActions.createReleases(applicationDTO, firstRelease, secondRelease);
        TenantScanActions.importScanTenant(applicationDTO, firstRelease, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        TenantScanActions.importScanTenant(applicationDTO, secondRelease, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        TenantScanActions.importScanTenant(applicationDTO, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .cancelScan();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify OSSComponentExportRowsPerFile site setting in admin site")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyOSSComponentExportRowsSiteSetting() {
        var siteSettingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        assertThat(siteSettingsPage.isSettingExists(settingName))
                .as("New site settings has been added to configure the CSV file size limit")
                .isTrue();
        assertThat(siteSettingsPage.getSettingValueByName(settingName))
                .as("Default value of site setting should be 10000 rows")
                .isEqualTo(defaultRowCount);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Data Export created from OSS components page , oss component from each release should be present in the export")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "verifyOSSComponentExportRowsSiteSetting"})
    public void verifyDataExportFromOssComponentsPage() {
        LogInActions.tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantEmailDTO.getTenant())
                .tenantTopNavbar.openApplications().openOpenSourceComponents().exportBtn.click();
        var modal = new ModalDialog();
        assertThat(modal.getMessage())
                .as("An alert page should be shown with message asking for confirmation to run the report")
                .contains(confirmationMessage);
        modal.pressYes();
        assertThat(modal.getMessage())
                .as("An alert page should be shown with message export has been queued and will receive the" +
                        " email to download the file")
                .contains(exportDownloadMessage);
        modal.close();
        AllureReportUtil.info("File should be downloaded and file has generated a row entry for component for each release");
        File zipFile = getZipFile();
        validateReleaseNameInZipFile(zipFile, firstRelease.getReleaseName(), true, defaultRowCount);
        validateReleaseNameInZipFile(zipFile, secondRelease.getReleaseName(), true, defaultRowCount);
        validateReleaseNameInZipFile(zipFile, applicationDTO.getReleaseName(), false, defaultRowCount);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify OSS export from Release Oss components page")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyOSSComponentExportRowsSiteSetting"})
    public void verifyDataExportFromReleaseOssComponentsPage() {
        LogInActions.tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantEmailDTO.getTenant())
                .tenantTopNavbar.openApplications().openYourReleases().openDetailsForRelease(applicationDTO, firstRelease)
                .openOpenSourceComponents().exportBtn.click();
        var modal = new ModalDialog();
        modal.pressYes();
        modal.pressClose();
        AllureReportUtil.info("File should be downloaded and file has generated a row entry for component for release");
        File zipFile = getZipFile();
        validateReleaseNameInZipFile(zipFile, firstRelease.getReleaseName(), true, defaultRowCount);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Edit the site setting and verify multiple files are created if row number exceeds site setting value")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyDataExportFromOssComponentsPage", "verifyDataExportFromReleaseOssComponentsPage"})
    public void verifyDataExportAfterEditingSiteSetting() {
        SiteSettingsActions.setValueInSettings(settingName, updatedRowCount, true);
        BrowserUtil.clearCookiesLogOff();
        var applicationsPage = LogInActions.tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD,
                tenantEmailDTO.getTenant()).tenantTopNavbar.openApplications();
        sleep(Duration.ofMinutes(2).toMillis());
        refresh();
        applicationsPage.openOpenSourceComponents().exportBtn.click();
        var modal = new ModalDialog();
        modal.pressYes();
        modal.pressClose();
        AllureReportUtil.info("Zip file should be downloaded with multiple csv files and file has generated a row entry " +
                "for component for each release");
        File zipFile = getZipFile();
        validateReleaseNameInZipFile(zipFile, firstRelease.getReleaseName(), true, updatedRowCount);
        validateReleaseNameInZipFile(zipFile, secondRelease.getReleaseName(), true, updatedRowCount);
    }

    @AfterClass
    public void setDefaultPayloadSize() {
        setupDriver("setDefaultOSSComponentExportRowsPerFile");
        SiteSettingsActions.setValueInSettings(settingName, defaultRowCount, true);
        attachTestArtifacts();
    }

    @SneakyThrows
    File getZipFile() {
        Supplier<String> sup = () -> {
            try {
                var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
                Document html = Jsoup.parse(mailBody);
                return html.selectXpath("//*[@href][contains(text(), 'Download Export')]").attr("href");
            } catch (Exception | Error e) {
                log.error(e.getMessage());
                return "";
            }
        };
        WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual, "",
                sup, Duration.ofMinutes(2), false);
        return download(sup.get());
    }

    @SneakyThrows
    public void validateReleaseNameInZipFile(File zipFile, String releaseName, boolean expectedRelease, String rowCount) {
        String zipFilePath = zipFile.getPath();
        String unzippedFilePath = zipFile.getParent();
        new ZipFile(zipFilePath).extractAll(unzippedFilePath);
        var unzippedFiles = Arrays.stream(Objects.requireNonNull(new File(unzippedFilePath).listFiles()))
                .filter(f -> f.getName().contains(".csv")).collect(Collectors.toList());
        boolean isReleaseNamePresent = false;
        for (var csvFile : unzippedFiles) {
            if (isReleaseNamePresentInFile(csvFile, releaseName, rowCount)) {
                isReleaseNamePresent = true;
                break;
            }
        }
        if (expectedRelease)
            assertThat(isReleaseNamePresent).as("release name should exist").isTrue();
        else
            assertThat(isReleaseNamePresent).as("release name should exist").isFalse();
    }

    public boolean isReleaseNamePresentInFile(File csvFile, String releaseName, String rowCount) {
        var dataFromCsv = new CSVHelper(csvFile).getAllRows(Collections.singletonList("Release"));
        boolean releaseExist = false;
        assertThat(dataFromCsv.size())
                .as("Generated csv should meet under the number of rows defined in the site-setting")
                .isLessThanOrEqualTo(Integer.parseInt(rowCount));
        for (var cell : dataFromCsv) {
            String printCellValue = Arrays.asList(cell).toString();
            if (printCellValue.contains(releaseName)) {
                releaseExist = true;
                break;
            }
        }
        return releaseExist;
    }
}
