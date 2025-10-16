package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("804020")
@Slf4j
public class FilterSuppressedIssuesFromIssueDataExportTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO application;
    StaticScanDTO staticScanDTO;
    String staticPayload = "payloads/fod/10JavaDefects_Small(OS).zip";
    String staticFpr = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    List<String> isSuppressedCheckbox = new ArrayList<>() {{
        add("true");
        add("false");
    }};

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare test data for test execution")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        AllureReportUtil.info("Create tenant and entitlements");
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        var entitlement = EntitlementDTO.createDefaultInstance();
        entitlement.setEntitlementType(FodCustomTypes.EntitlementType.FortifyEntitlement);
        entitlement.setQuantityPurchased(1000);
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlement);
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(1000);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(1000);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);
        EntitlementsActions.disableEntitlementsByEntitlementType(tenantDTO, FodCustomTypes.EntitlementType.DebrickedEntitlement, false);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Starting static scan with selecting the " +
                "check box of Open Source Component");
        application = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setFileToUpload(staticPayload);
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(application, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, application);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(application, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Completing static scan with open source scan");
        LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(application)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        AllureReportUtil.info("Suppressing one of the issue under issues page");
        var issuesPage = new TenantTopNavbar()
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(application.getApplicationName(), application.getReleaseName())
                .openIssues();
        issuesPage.groupBy("Scan Type");
        var issue = issuesPage.clickAll().getIssues().get(0);
        issue.clickCheckbox();
        issue.setAuditorStatus(FodCustomTypes.AuditorStatus.RiskAccepted.getTypeValue());
        issue.submitChanges();
    }

    @SneakyThrows
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Parameters({"Select Checkbox", "Expected Value1", "Expected Value2"})
    @Description("Verify that suppressed issues are showing correct value under Is Suppressed column")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"},
            dataProvider = "isSuppressedData")
    public void validateIsSuppressedExportedDataTest(String selectCheckbox, String expectedValue1, String expectedValue2) {
        AllureReportUtil.info("Creating Data Export with " +
                "Issues Data Export Template");
        var dataExportDTO = DataExportDTO
                .createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        switch (selectCheckbox) {
            case "trueChecked":
                dataExportDTO.setIsSuppressed(Collections.singletonList(isSuppressedCheckbox.get(0)));
                break;
            case "falseChecked":
                dataExportDTO.setIsSuppressed(Collections.singletonList(isSuppressedCheckbox.get(1)));
                break;
            case "bothTrueAndFalseChecked":
                dataExportDTO.setIsSuppressed(isSuppressedCheckbox);
                break;
            case "bothTrueAndFalseUnChecked":
                dataExportDTO.setIsSuppressed(null);
                break;
            default:
                throw new IllegalStateException("Unexpected checkbox: " + selectCheckbox);
        }
        LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        var dataExportFile = DataExportActions
                .createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(dataExportFile)
                .getAllRows(List.of("Is Suppressed"));
        assertThat(dataFromCsv.stream()
                .anyMatch(x -> Arrays.toString(x).replaceAll("[\\[\\]]", "").equals(expectedValue1)))
                .as("Verify that when 'Is Suppressed' filter is checked/uncheck for both True/False boxes," +
                        " 'Is Suppressed'  column should be having both True or False Entries")
                .isTrue();
        if (selectCheckbox.equals("bothTrueAndFalseChecked") || selectCheckbox.equals("bothTrueAndFalseUnChecked")) {
            assertThat(dataFromCsv.stream()
                    .anyMatch(x -> Arrays.toString(x).replaceAll("[\\[\\]]", "").equals(expectedValue2)))
                    .as("verify that when 'Is Suppressed' filter is checked/uncheck for both True/False boxes," +
                            " 'Is Suppressed'  column should be having both True & False Entries")
                    .isTrue();
        } else {
            assertThat(dataFromCsv.stream()
                    .anyMatch(x -> Arrays.toString(x).replaceAll("[\\[\\]]", "").equals(expectedValue2)))
                    .as("verify that when 'Is Suppressed' filter is checked for particular True/False boxes, " +
                            "'Is Suppressed'  column should be having only that True/False Entries")
                    .isFalse();
        }
    }

    @DataProvider(name = "isSuppressedData", parallel = true)
    public Object[][] isSuppressedData() {
        return new Object[][]{
                {"trueChecked", "True", "False"},
                {"falseChecked", "False", "True"},
                {"bothTrueAndFalseChecked", "True", "False"},
                {"bothTrueAndFalseUnChecked", "True", "False"}
        };
    }
}
