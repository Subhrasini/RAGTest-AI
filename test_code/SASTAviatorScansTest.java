package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.SearchBox;
import com.fortify.fod.common.elements.Spinner;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.fortify.common.utils.BrowserUtil.waitAjaxLoaded;

@Slf4j
public class SASTAviatorScansTest extends FodBaseTest {

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("1713023")
    @Description("Verify Static scans with SAST Aviator for all language level support (.Net/Java/Python/JS)")
    @Test(groups = {"regression"}, dataProvider = "TechStackTypes")
    public void allLanguageSupportSASTAviatorTest(String techStack, String filePath) {

        SoftAssertions softassert = new SoftAssertions();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();
        ReleaseDTO copiedRelease = ReleaseDTO.createDefaultInstance();
        staticScanDTO.setIncludeFortifyAviator(true);
        staticScanDTO.setFileToUpload(filePath);
        copiedRelease.setCopyState(true);
        copiedRelease.setCopyFromReleaseName(applicationDTO.getReleaseName());
        List<String> nonSuppressedAuditorStatus = Arrays.asList("Remediation Required", "Suspicious",
                "Proposed Not an Issue");
        List<String> withSuppressedAuditorStatus = Arrays.asList("Remediation Required", "Suspicious",
                "Proposed Not an Issue", "Not an Issue");

        AllureReportUtil.info("Start a static scan with aviator for tech stack" + techStack);
        ApplicationActions.createApplication(applicationDTO,
                defaultTenantDTO, true);
        var issuesPage = StaticScanActions
                .createStaticScan(staticScanDTO, applicationDTO,
                        FodCustomTypes.SetupScanPageStatus.Completed)
                .openIssues()
                .groupBy("Auditor Status")
                .clickAll();
        List<String> groupHeaders = issuesPage.getGroupHeaders();
        softassert.assertThat(nonSuppressedAuditorStatus.containsAll(groupHeaders))
                .as("Verify Auditor Status dropdown should have these Remediation Required,Proposed Not an Issue," +
                        " Suspicious Auditor status listed under Not Suppressed")
                .isTrue();
        issuesPage.setShowSuppressed(true);
        groupHeaders = issuesPage.getGroupHeaders();
        softassert.assertThat(groupHeaders.contains("Pending Review"))
                .as("Verify No issue coming under \"Pending Review\" status")
                .isFalse();
        softassert.assertThat(withSuppressedAuditorStatus.containsAll(groupHeaders))
                .as("Verify issues are grouped by Auditor status Remediation Required,Proposed Not an Issue , " +
                        "Not an Issue and Suspicious ")
                .isTrue();
        for (var header : groupHeaders) {
            var auditorStatus = issuesPage.getIssuesByHeaders(String.valueOf(header))
                    .get(0).openDetails().openHistory().getAuditContents();
            softassert.assertThat(auditorStatus.size())
                    .as("Verify History section Should have only one row of audit message")
                    .isEqualTo(1);
            softassert.assertThat(auditorStatus.get(0))
                    .as("Verify History section Should have message " +
                            "\"Changed Auditor Status from 'Pending Review' to" + header)
                    .isEqualTo("Changed Auditor Status from 'Pending Review' to '" + header + "'");
        }
        softassert.assertThat(issuesPage.groupBy("SAST Aviator").getGroupHeaders().contains("false"))
                .as("Verify all issues should come under \"true\"")
                .isFalse();

        AllureReportUtil.info("Verify issues for copied release");
        issuesPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .createNewRelease(copiedRelease)
                .openIssues()
                .groupBy("Auditor Status")
                .clickAll()
                .setShowSuppressed(true);
        groupHeaders = issuesPage.getGroupHeaders();
        softassert.assertThat(groupHeaders.contains("Pending Review"))
                .as("Verify No issue coming under \"Pending Review\" status for copied release")
                .isFalse();
        softassert.assertThat(withSuppressedAuditorStatus.containsAll(groupHeaders))
                .as("Verify issues are grouped by Auditor status Remediation Required,Proposed Not an Issue," +
                        " Not an Issue and Suspicious for copied release")
                .isTrue();
        for (var header : groupHeaders) {
            var auditorStatus = issuesPage.getIssuesByHeaders(String.valueOf(header))
                    .get(0).openDetails().openHistory().getAuditContents();
            softassert.assertThat(auditorStatus.size())
                    .as("Verify Copied Release History section Should have only one row of audit message")
                    .isEqualTo(1);
            softassert.assertThat(auditorStatus.get(0))
                    .as("Verify Copied Release History section for copied release should have message " +
                            "\"Changed Auditor Status from 'Pending Review' to" + header)
                    .isEqualTo("Changed Auditor Status from 'Pending Review' to '" + header + "'");
        }
        softassert.assertThat(issuesPage.groupBy("SAST Aviator").getGroupHeaders().contains("false"))
                .as("Verify all issues should come under \"true\" for copied release")
                .isFalse();
        softassert.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("1713023")
    @Description("Verify fortify aviator scan for more than 2500 issues")
    @Test(groups = {"regression"})
    public void aviatorValidationForMoreThan2500IssuesTest() {

        SoftAssertions softassert = new SoftAssertions();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO,
                defaultTenantDTO, true);
        TenantScanActions.importScanTenant(applicationDTO,
                        "payloads/fod/2500_scandata.fpr", FodCustomTypes.ScanType.Static)
                .getFirstScan()
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        var issuesPage = new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openIssues()
                .clickAll()
                .setShowSuppressed(true);
        issuesPage.waitTillAllCountEquals("3K");
        softassert.assertThat(issuesPage.getAllCount(true))
                .as("Verify all issues should come under \"true\" for copied release")
                .contains("3K");
        issuesPage.groupBy("Category");
        issuesPage.filters.setFilterByName("Auditor Status")
                .expand().clickFilterOptionByName("Suspicious");
        var groupHeaders = issuesPage.getGroupHeaders();
        for (var header : groupHeaders) {
            var auditorStatusMessage = issuesPage.getIssuesByHeaders(String.valueOf(header))
                    .get(0).openDetails()
                    .openHistory()
                    .getAuditContents().get(0);
            softassert.assertThat(auditorStatusMessage)
                    .as("Verify Fortify Aviator message in History section " +
                            "for each category under Fortify Aviator filter")
                    .contains("Changed Auditor Status from 'Pending Review' to 'Suspicious'");
        }

        issuesPage.appliedFilters.clearAll();
        List<String> categoryEqualTo500 = new ArrayList<>();
        var categoryLessThan500 = issuesPage.getIssueGroupsCounts()
                .entrySet().stream()
                .filter(t -> {
                    if (t.getValue() >= 500) {
                        categoryEqualTo500.add(t.getKey());
                        return false;
                    }
                    return true;
                })
                .map(Map.Entry::getKey)
                .toList();

        issuesPage.filters.setFilterByName("Auditor Status")
                .expand().clickFilterOptionByName("Pending Review");
        groupHeaders = issuesPage.getGroupHeaders();
        softassert.assertThat(groupHeaders.containsAll(categoryLessThan500))
                .as("Verify the categories having less than 500 issues shouldn't be displayed")
                .isFalse();
        softassert.assertThat(groupHeaders.containsAll(categoryEqualTo500))
                .as("Verify only the categories having equal to 500 issues will be displayed")
                .isTrue();
        //defect - 1811101
//        for (var header : groupHeaders) {
//            var auditorStatusMessage = issuesPage.getIssuesByHeaders(String.valueOf(header))
//                    .get(0).openDetails().openHistory().getCommentsContents().get(0);
//            softassert.assertThat(auditorStatusMessage)
//                    .as("Verify Fortify Aviator message in History section " +
//                            "for each category when limit exceeded")
//                    .contains("Fortify Aviator auditing was limited to the first 500.");
//        }
        issuesPage.appliedFilters.clearAll();
        var statusFilter = issuesPage.filters;
        var totalIssue = statusFilter.setFilterByName("Auditor Status")
                .expand()
                .getAllOptions().stream()
                .filter(t -> !t.equals("Pending Review"))
                .mapToInt(option -> statusFilter.getFilterBadgeCount(option))
                .sum();
        softassert.assertThat(totalIssue)
                .as("Verify the total issues count under the fortify aviator auditor status" +
                        " \"Remediation Required\"/\"Not an Issue\"/\"Suspicious\"/\"Proposed Not an Issue\" shouldn't exceed 2500")
                .isLessThanOrEqualTo(2500);
        softassert.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("1713023")
    @Description("Verify the issue status shouldn't get updated by FA which was updated by the user")
    @Test(groups = {"regression"})
    public void verifyAviatorScanIssueUpdate() {

        SoftAssertions softassert = new SoftAssertions();
        ApplicationDTO applicationDTO = ApplicationDTO.createDefaultInstance();
        StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setIncludeFortifyAviator(true);
        staticScanDTO.setFileToUpload("payloads/fod/WebGoat5.0.zip");

        AllureReportUtil.info("Start a static scan with aviator");
        ApplicationActions.createApplication(applicationDTO,
                defaultTenantDTO, true);
        var issuesPage = StaticScanActions
                .createStaticScan(staticScanDTO, applicationDTO,
                        FodCustomTypes.SetupScanPageStatus.Completed)
                .openIssues()
                .groupBy("Auditor Status")
                .clickAll();
        issuesPage.openIssueByIndex(0)
                .setAuditorStatus("Risk Accepted")
                .setComment("Test comment")
                .pressAddButton();
        var issueId = issuesPage.openIssueByIndex(0).getId();

        AllureReportUtil.info("Start another static scan with aviator on the same release");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Completed);

        issuesPage = new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openIssues()
                .groupBy("Auditor Status")
                .clickAll()
                .setShowSuppressed(true);
        new SearchBox().searchFor(issueId);
        new Spinner().waitTillLoading(2, true);
        waitAjaxLoaded();
        softassert.assertThat(issuesPage.openIssueByIndex(0).getAuditorStatus())
                .as("Verify the issue status shouldn't get updated by FA which was updated by the user")
                .isEqualTo("Risk Accepted");

        var historyCell = issuesPage.openIssueByIndex(0).openDetails().openHistory();
        softassert.assertThat(historyCell.getAuditorName())
                .as("Verify there should be no comments from aviator")
                .doesNotContain("Aviator");

        List<String> auditContents = historyCell.getAuditContents();
        softassert.assertThat(auditContents.size())
                .as("Verify there are no duplicate comments for the same scan id")
                .isEqualTo(auditContents.stream().distinct().count());

        AllureReportUtil.info("Verify scan summary in Release Scans tab");
        var scanSummaryPage = issuesPage.openScans().
                getScanByType(FodCustomTypes.ScanType.Static)
                .pressScanSummary();
        softassert.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Requested"))
                .as("Validate the value of SAST Aviator field")
                .isEqualTo("Yes");
        softassert.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Status"))
                .as("Validate the value of SAST Aviator Service Status field")
                .isEqualTo("No new vulnerabilities were found so the SAST Aviator service did not run.");
        scanSummaryPage.pressClose();

        softassert.assertAll();
    }

    @DataProvider(name = "TechStackTypes", parallel = true)
    public Object[][] scanTypes() {
        return new Object[][]
                {
                        {"Java", "payloads/fod/WebGoat5.0.zip"},
                        {"DotNet", "payloads/fod/dotNET_7.zip"},
                        {"Python", "payloads/fod/PythonDjango-5.zip"},
                        {"JS", "payloads/fod/JS_Payload.zip"},
                        {"Kotlin", "payloads/fod/CotlinStart-master.zip"},
                        {"Php", "payloads/fod/php.zip"}
                };
    }
}
