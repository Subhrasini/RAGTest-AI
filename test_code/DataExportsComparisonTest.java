package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.common.utils.WaitProvider;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.exceptions.FodFileDownloadException;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.download;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class DataExportsComparisonTest extends FodBaseTest implements WaitProvider {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO, mobileAppDto;
    AttributeDTO attributeDTO;
    MobileScanDTO mobileScanDTO;
    DynamicScanDTO dynamicScanDTO;
    HashMap<AttributeDTO, String> attributeMapForDynamicApp, attributeMapForMobileApp;

    DataExportDTO scansDto, issuesDto, applicationsDto, entitlementConsumptionDto, appReleasesDto, newScansDto;

    public void initTestsData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        mobileScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType("AUTO-DYNAMIC");
        dynamicScanDTO.setStartInFuture(true);

        applicationDTO = ApplicationDTO.createDefaultInstance();
        mobileAppDto = ApplicationDTO.createDefaultMobileInstance();

        attributeDTO = AttributeDTO.createDefaultInstance();
        attributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        attributeDTO.setAttributeName("AUTO-Attr-ExportCompare");
        attributeDTO.setPickListValues(new String[]{"A", "B", "C"});
        attributeMapForDynamicApp = new HashMap<>() {{
            put(attributeDTO, "A");
        }};

        attributeMapForMobileApp = new HashMap<>() {{
            put(attributeDTO, "B");
        }};

        applicationDTO.setAttributesMap(attributeMapForDynamicApp);
        mobileAppDto.setAttributesMap(attributeMapForMobileApp);
    }


    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create test data for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        initTestsData();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        CustomAttributesActions.createCustomAttribute(attributeDTO, tenantDTO, true);
        ApplicationActions.createApplication(applicationDTO, tenantDTO, false);
        ApplicationActions.createApplication(mobileAppDto, tenantDTO, false);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.Scheduled);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileAppDto, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), "payloads/fod/dynamic.zero.fpr",
                true, true, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        MobileScanActions.publishMobileScanWithoutImportFpr(mobileAppDto, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate issues export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation", "releaseExportTest"})
    public void issueExportTest() {
        issuesDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        var issuesPage = LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openIssues();
        issuesPage.clickAll();
        issuesPage.pressExportButton().pressYes();
        new ModalDialog().pressClose();

        var downloadedByLink = generateUniqueLinkAdnDownload();

        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Application");
            add("Release");
            add("Severity");
            add("Is Suppressed");
            add("Closed Status");
        }};

        refresh();
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(issuesDto);
        var dataFromCsv1 = new CSVHelper(downloadedByLink).getAllRows(csvColumnsToValidate);
        var dataFromCsv2 = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);
        assertThat(dataFromCsv2).containsAll(dataFromCsv1);
    }


    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate entitlements export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void entitlementsExportTest() {
        entitlementConsumptionDto =
                DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.EntitlementConsumption);
        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openReports();

        List<String> columnsTenantExport = new ArrayList<>() {{
            add("EntitlementId");
            add("EntitlementUnitsConsumed");
        }};

        List<String> columnsAdminExport = new ArrayList<>() {{
            add("Entitlement Id");
            add("Quantity Consumed");
        }};

        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(entitlementConsumptionDto);
        var dataFromCsv1 = new CSVHelper(generatedFromReportsPage).getAllRows(columnsTenantExport);
        BrowserUtil.clearCookiesLogOff();
        var downloadedFromAdminSite = LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openEntitlements().downloadEntitlementsExportFile();
        var dataFromCsv2 = new CSVHelper(downloadedFromAdminSite).getAllRows(columnsAdminExport);

        assertThat(dataFromCsv1).hasSize(2);
        assertThat(dataFromCsv2).hasSize(1);
        assertThat(dataFromCsv1.get(0)[0])
                .as("Entitlement Id should be the same")
                .isEqualTo(dataFromCsv2.get(0)[0]);

        var quantityTenant = Integer.parseInt(dataFromCsv1.get(0)[1]) + Integer.parseInt(dataFromCsv1.get(1)[1]);
        var quantityAdmin = Integer.parseInt(dataFromCsv2.get(0)[1]);

        assertThat(quantityAdmin).isEqualTo(quantityTenant);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate release export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void releaseExportTest() throws IOException {
        appReleasesDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.ApplicationReleases);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Application");
            add("Release");
            add("AUTO-Attr-ExportCompare");
        }};

        var releasesPage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();

        var fileFromReleasesPage = releasesPage.downloadReleasesExportFile();
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(appReleasesDto);

        var dataFromCsv1 = new CSVHelper(fileFromReleasesPage).getAllRows(csvColumnsToValidate);
        var dataFromCsv2 = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);

        assertThat(dataFromCsv1).hasSize(2);
        assertThat(dataFromCsv2).hasSize(2);

        var arrayToCompareFirstRelease =
                new String[]{applicationDTO.getApplicationName(), applicationDTO.getReleaseName(), "A"};
        var arrayToCompareSecondRelease =
                new String[]{mobileAppDto.getApplicationName(), mobileAppDto.getReleaseName(), "B"};

        assertThat(dataFromCsv1).contains(arrayToCompareFirstRelease, arrayToCompareSecondRelease);
        assertThat(dataFromCsv2).contains(arrayToCompareFirstRelease, arrayToCompareSecondRelease);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate applications export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void applicationExportTest() throws FileNotFoundException {
        applicationsDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Applications);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Application ID");
            add("Application");
        }};

        var releasesPage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();

        var fileFromReleasesPage = releasesPage.downloadReleasesExportFile();
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(applicationsDto);

        var dataFromCsv1 = new CSVHelper(fileFromReleasesPage).getAllRows(csvColumnsToValidate);
        var dataFromCsv2 = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);

        assertThat(dataFromCsv1).hasSize(2);
        assertThat(dataFromCsv2).hasSize(2);
        assertThat(dataFromCsv1).containsAll(dataFromCsv2);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate scans export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void scansExportTest() {
        scansDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Scans);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Application");
            add("Release");
            add("Scan Type");
            add("IssueCountLow");
            add("IssueCountMedium");
            add("IssueCountHigh");
            add("IssueCountCritical");
        }};

        var scansPage = LogInActions.tamUserLogin(tenantDTO).openYourScans();
        var scans = scansPage.getAllScans(false);
        assertThat(scans).hasSize(2);


        var firstScanInfo = new String[]{scans.get(0).getApplicationName(), scans.get(0).getReleaseName(),
                scans.get(0).getScanType(),
                Integer.toString(scans.get(0).getLowCount()),
                Integer.toString(scans.get(0).getMediumCount()),
                Integer.toString(scans.get(0).getHighCount()),
                Integer.toString(scans.get(0).getCriticalCount())
        };

        var secondScanInfo = new String[]{scans.get(0).getApplicationName(), scans.get(0).getReleaseName(),
                scans.get(0).getScanType(),
                Integer.toString(scans.get(0).getLowCount()),
                Integer.toString(scans.get(0).getMediumCount()),
                Integer.toString(scans.get(0).getHighCount()),
                Integer.toString(scans.get(0).getCriticalCount())
        };

        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(scansDto);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);

        assertThat(dataFromCsv)
                .as("Validate scans export for Dynamic and Mobile Scan")
                .contains(firstScanInfo, secondScanInfo);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("EntitlementId, EntitlementUnitsConsumed, IsSubscriptionEntitlement " +
            "are displayed with proper values")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void addEntitlementDataToScansDataExportTest() {
        newScansDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Scans);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("EntitlementId");
            add("EntitlementUnitsConsumed");
            add("IsSubscriptionEntitlement");
        }};
        var entitlementsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements();
        var entitlementId = entitlementsPage.getAll().stream().map(e -> e.getId()).collect(Collectors.toList());

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        var firstScanInfo = new String[]{entitlementId.get(0).toString(), "31", "True"};

        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(newScansDto);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);
        assertThat(dataFromCsv)
                .as("verifying value of EntitlementId, EntitlementUnitsConsumed, IsSubscriptionEntitlement")
                .contains(firstScanInfo);
    }

    public File generateUniqueLinkAdnDownload() {
        Supplier<File> file = () -> {
            var tenantId = new FodSQLUtil().getTenantIdByName(tenantDTO.getTenantName());
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

        waitForValueEquals(true, () -> file.get().getName().contains("csv"), Duration.ofMinutes(3));
        return file.get();
    }
}
