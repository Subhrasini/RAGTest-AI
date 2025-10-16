package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.elements.AppliedFilters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.exceptions.FodUnexpectedConditionsException;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.langfileext.LangFileExtPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.popups.CloneJobPopup;
import com.fortify.fod.ui.pages.common.common.cells.ReleaseScanCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static com.codeborne.selenide.Selenide.*;
import static com.fortify.fod.common.custom_types.FodCustomTypes.TechnologyStack.Infrastructure;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class ManageLanguageFileExtensionsTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to visit languages and file extensions page and edit values")
    @Test(groups = "{regression}")
    public void manageLanguageFileExtensionsTest() {

        var extension = ".noexe" + UniqueRunTag.generate().substring(9);
        log.info("Current extension: {}", extension);
        var expectedLanguages = new ArrayList<>(Arrays.asList(
                ".NET",
                ".Net Core",
                "ABAP",
                "Apex/Visualforce",
                "ASP",
                "CFML",
                "COBOL",
                "Go",
                "Infrastructure-As-Code/Dockerfile",
                "JAVA/J2EE/Kotlin",
                "JS/TS/HTML",
                "MBS/C/C++/Scala",
                "PHP",
                "PYTHON",
                "Ruby",
                "Swift/Objective C/C++",
                "VB6",
                "VBScript"));

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD).adminTopNavbar.openStatic();
        var staticPage = page(ScanJobsPage.class);
        staticPage.openLangFileExt();

        //  check against predefined list
        var langFileExtPage = page(LangFileExtPage.class);
        var actualLanguages = langFileExtPage.getLangNames();

        for (var lang : expectedLanguages) {
            assertThat(actualLanguages.contains(lang))
                    .as(String.format("Check if %s is present in language names", lang))
                    .isTrue();
        }

        // check adding of new extension
        var languageToEdit = expectedLanguages.get(new Random().nextInt(expectedLanguages.size()));

        var editPopup = langFileExtPage.editLanguageExtension(languageToEdit);

        assertThat(editPopup.getPopupTitle()).as("Check if edit popup opened").isEqualTo("Edit Language File Extensions");

        var actualExtensions = editPopup.getExtensions();

        String newExtensions = "";
        if (actualExtensions.isBlank()) {
            newExtensions = extension;
        } else {
            newExtensions = extension + ", " + actualExtensions;
        }

        editPopup.setExtensions(newExtensions).save();
        refresh();

        assertThat(langFileExtPage.getExtensionsForLanguage(languageToEdit))
                .as("Check if new extension updated in table")
                .isEqualTo(newExtensions);

        // set back old extensions
        editPopup = langFileExtPage.editLanguageExtension(languageToEdit);
        editPopup.setExtensions(actualExtensions).save();
        refresh();

        assertThat(langFileExtPage.getExtensionsForLanguage(languageToEdit))
                .as("Check if old extension restored")
                .isEqualTo(actualExtensions);


        // check not valid extensions
        langFileExtPage.editLanguageExtension(languageToEdit);
        var notValidExtensions = extension.substring(1) + " " + newExtensions;
        editPopup.setExtensions(notValidExtensions);
        editPopup.save();
        var validationMessage = langFileExtPage.modal.getMessage();
        assertThat(validationMessage.equals("Invalid characters in message") || validationMessage.contains("did not start"))
                .as("Check modal message for validation error")
                .isTrue();
        langFileExtPage.modal.clickButtonByText("Close");
        editPopup.close();
        refresh();
        var blacklistPopup = langFileExtPage.editBlacklistLanguageExtension(languageToEdit);

        assertThat(blacklistPopup.getPopupTitle()).as("Check if edit popup opened").isEqualTo("Edit Blacklist");

        blacklistPopup.addBlacklistedExtension(extension).save();

        assertThat(langFileExtPage.modal.getMessage())
                .as("Check modal message for success")
                .isEqualTo("Saved Successfully.");
        langFileExtPage.modal.clickButtonByText("Close");

        var updatedPopup = langFileExtPage.editBlacklistLanguageExtension(languageToEdit);
        assertThat(updatedPopup.getBlacklistedItems().contains(extension))
                .as("Check if blacklisted extension is present in the popup list")
                .isTrue();

        // check removing from blacklist
        updatedPopup.selectItem(extension).removeItem().save();
        assertThat(langFileExtPage.modal.getMessage())
                .as("Check modal message for success")
                .isEqualTo("Saved Successfully.");
        langFileExtPage.modal.clickButtonByText("Close");

        var popupAfterDelete = langFileExtPage.editBlacklistLanguageExtension(languageToEdit);
        assertThat(popupAfterDelete.getBlacklistedItems(0).contains(extension))
                .as("Check if blacklisted extension is deleted from the popup list")
                .isFalse();
        popupAfterDelete.close();
    }

    @FodBacklogItem("490001")
    @Severity(SeverityLevel.NORMAL)
    @Description("When setting a blacklist item, it should be in 'Exclude' file in logs")
    @Test(enabled = false, groups = "{regression}", dependsOnMethods = "manageLanguageFileExtensionsTest")
    public void checkExcludesTest() throws IOException, ZipException {

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD).adminTopNavbar.openStatic();
        var staticPage = page(ScanJobsPage.class);
        staticPage.openLangFileExt();

        var jsLang = "JS/TS/HTML";
        var jsBlacklistItem = "bootstrap*";

        var langFileExtPage = page(LangFileExtPage.class);
        var blacklistPopup = langFileExtPage.editBlacklistLanguageExtension(jsLang);

        // in case if item already exist we don't add a new one
        if (!blacklistPopup.getBlacklistedItems(0).contains(jsBlacklistItem)) {
            blacklistPopup.addBlacklistedExtension(jsBlacklistItem).save();

            assertThat(langFileExtPage.modal.getMessage())
                    .as("Check modal message for success")
                    .isEqualTo("Saved Successfully.");
            langFileExtPage.modal.clickButtonByText("Close");
        } else {
            blacklistPopup.close();
        }


        var tenant = defaultTenantDTO;
        BrowserUtil.openNewTab();
        switchTo().window(1);

        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenant.getTenantCode());

        var application = ApplicationDTO.createDefaultInstance();
        var staticScan = StaticScanDTO.createDefaultInstance();
        staticScan.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScan.setFileToUpload("payloads/fod/webgoat-sc.zip");
        staticScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        ApplicationActions.createApplication(application);
        StaticScanActions.createStaticScan(staticScan, application);

        switchTo().window(0);

        var scanJobsPage = page(ScanJobsPage.class);
        scanJobsPage.adminTopNavbar.openStatic();
        new AppliedFilters().clearAll();
        var scanCell = scanJobsPage.findScanByAppName(application.getApplicationName());
        var originalJob = scanCell.getLastJob();
        originalJob.waitForJobStatus(FodCustomTypes.JobStatus.Success);
        var originalLogs = originalJob.getActionButton(Actions.JobAction.DownloadLog).download();
        String[] excludesToValidate = {"bootstrap"};

        try {
            StaticScanActions.validateScanLogs(originalLogs, "/excludes.txt", excludesToValidate, true);
        } catch (NoSuchFileException e) {
            throw new FodUnexpectedConditionsException("No excludes in zip file, test failed");
        }


        originalJob.getActionButton(Actions.JobAction.Clone).click();
        var clonePopup = page(CloneJobPopup.class);
        clonePopup.setBuildArgsElement("-ignoreGlobalExcludes").setRescanReason("Test excludes").pressRescan(true);
        scanCell = scanJobsPage.findScanByAppName(application.getApplicationName());
        var excludedJob = scanCell.getLastJob();
        excludedJob.waitForJobStatus(FodCustomTypes.JobStatus.Success);
        var excludedLogs = excludedJob.getActionButton(Actions.JobAction.DownloadLog).download();

        // we need to validate the absence of the file inside zip, so we need to handle an exception
        try {
            StaticScanActions.validateScanLogs(excludedLogs, "/excludes.txt", excludesToValidate, true);
            throw new FodUnexpectedConditionsException("File contains excludes, test failed");
        } catch (NoSuchFileException e) {
            log.info("Catching exception because we expect that there's no excludes.txt in the zip");
        }


        // remove bootstrap* for extended test

        scanJobsPage.openLangFileExt();

        blacklistPopup = langFileExtPage.editBlacklistLanguageExtension(jsLang);
        blacklistPopup.selectItem(jsBlacklistItem).removeItem().save();

        assertThat(langFileExtPage.modal.getMessage())
                .as("Check modal message for success")
                .isEqualTo("Saved Successfully.");
        langFileExtPage.modal.clickButtonByText("Close");

    }

    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @FodBacklogItem("589753")
    @Severity(SeverityLevel.NORMAL)
    @Description("Static Start Scan page testing - Validate Infrastructure-As-Code/Dockerfile is available in Technology Stack dropdown")
    @Test(groups = "{regression}")
    public void validateInfrastructureInTsDropDownStaticScanPage() {
        // Check the Language and Extensions in Admin Language Extension page
        var langFileExtPage = LogInActions.adminLogIn()
                .adminTopNavbar.openStatic().openLangFileExt();
        var infrastructureAsLang = "Infrastructure-As-Code/Dockerfile";
        var infrastructureFileExt = ".json, .yaml, dockerfile, .xml, .dockerfile, .tf";
        langFileExtPage.getExtensionsForLanguage(infrastructureAsLang);
        var soft = new SoftAssertions();
        soft.assertThat(langFileExtPage.getExtensionsForLanguage(infrastructureAsLang))
                .as("Validate the Language and file extensions for Infrastructure are present in Admin Language File Extension page ")
                .isEqualTo(infrastructureFileExt);
        soft.assertAll();
        // Check in Tenant page user is able to create a static  scan page using Infrastructure as Technology Stack
        ApplicationDTO applicationDTO;
        LogInActions.tamUserLogin(defaultTenantDTO);
        var staticScan = StaticScanDTO.createDefaultInstance();
        staticScan.setTechnologyStack(Infrastructure);
        staticScan.setFileToUpload("payloads/fod/webgoat-sc.zip");
        staticScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, false);
        var staticScanSetupPage = StaticScanActions.createStaticScan(staticScan, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        assertThat(staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .getSelectedTechnologyStack())
                .as("Validate Stack should be Infrastructure")
                .isEqualTo("Infrastructure-As-Code/Dockerfile");

        staticScanSetupPage.openScans().getAllScans(false)
                .stream().filter(x -> x.getStatus().equals(FodCustomTypes.ScansPageStatus.InProgress.getTypeValue()))
                .findFirst().ifPresent(ReleaseScanCell::cancelScan);
    }
}