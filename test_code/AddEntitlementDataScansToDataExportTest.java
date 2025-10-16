package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class AddEntitlementDataScansToDataExportTest extends FodBaseTest {

    TenantDTO tenantDTOAssessmentUnit, tenantDTOScans;
    DataExportDTO dataExportDTOAssessmentUnit, dataExportDTOScans;
    EntitlementDTO entitlementDTOScans;
    StaticScanDTO scanDTOAssessmentUnit, scanDTOScans;
    ApplicationDTO webAppDTOAssessmentUnit, webAppDTOScan;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site with entitlements and assessments")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTOAssessmentUnit = TenantDTO.createDefaultInstance();
        tenantDTOAssessmentUnit.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        webAppDTOAssessmentUnit = ApplicationDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTOAssessmentUnit, true, false);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(webAppDTOAssessmentUnit, tenantDTOAssessmentUnit, true);

        scanDTOAssessmentUnit = StaticScanDTO.createDefaultInstance();
        scanDTOAssessmentUnit.setLanguageLevel("1.8");
        scanDTOAssessmentUnit.setFileToUpload("payloads/fod/static.java.zip");
        StaticScanActions.createStaticScan(scanDTOAssessmentUnit, webAppDTOAssessmentUnit, FodCustomTypes.SetupScanPageStatus.Completed);
        dataExportDTOAssessmentUnit = DataExportDTO.createDefaultInstance();
    }

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site with entitlements and assessments")
    @Test(groups = {"regression"})
    public void prepareTestDataSecondTenant() {
        tenantDTOScans = TenantDTO.createDefaultInstance();
        entitlementDTOScans = EntitlementDTO.createScanInstance();
        entitlementDTOScans.setEntitlementType(FodCustomTypes.EntitlementType.FortifyEntitlement);
        entitlementDTOScans.setFrequency(FodCustomTypes.ScanFrequency.Subscription);
        entitlementDTOScans.setAnalysisType(FodCustomTypes.AnalysisType.Static);
        entitlementDTOScans.setAssessmentType("AUTO-STATIC");

        tenantDTOScans.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenantDTOScans.setSubscriptionModel(FodCustomTypes.SubscriptionModel.StartOnFirstScan);
        tenantDTOScans.setEntitlementDTO(entitlementDTOScans);

        TenantActions.createTenant(tenantDTOScans, true, false);
    }

    @MaxRetryCount(3)
    @Description("Prepare Static Scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "prepareTestDataSecondTenant"})
    public void createApplicationAndScan() {
        webAppDTOScan = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webAppDTOScan, tenantDTOScans, true);

        scanDTOScans = StaticScanDTO.createDefaultInstance();
        scanDTOScans.setLanguageLevel("1.8");
        scanDTOScans.setFileToUpload("payloads/fod/static.java.zip");
        StaticScanActions
                .createStaticScan(scanDTOScans, webAppDTOScan, FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @MaxRetryCount(3)
    @Description("TAM should be able to create Data exports and download them")
    @Test(groups = {"regression"}, dependsOnMethods = {"createApplicationAndScan"}, alwaysRun = true)
    public void exportDataScanTest() {
        dataExportDTOScans = DataExportDTO.createDefaultInstance();
        dataExportDTOScans.setSelectAllStatuses(true);
        LogInActions.tamUserLogin(tenantDTOScans);
        dataExportDTOScans = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Scans);
        BrowserUtil.clearCookiesLogOff();

        checkTheCsv(tenantDTOScans, dataExportDTOScans, "1", "True");
    }

    @MaxRetryCount(3)
    @Description("TAM should be able to create Data exports and download them")
    @Test(groups = {"regression"}, dependsOnMethods = {"createApplicationAndScan"}, alwaysRun = true)
    public void exportDataAssessmentUnitTest() {
        dataExportDTOAssessmentUnit = DataExportDTO.createDefaultInstance();
        dataExportDTOAssessmentUnit.setSelectAllStatuses(true);
        LogInActions.tamUserLogin(tenantDTOAssessmentUnit);
        dataExportDTOAssessmentUnit = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Scans);
        BrowserUtil.clearCookiesLogOff();

        checkTheCsv(tenantDTOAssessmentUnit, dataExportDTOAssessmentUnit, "11", "True");
    }

    public void checkTheCsv(TenantDTO tenantDTO, DataExportDTO dataExportDTO, String value1, String value2) {
        List<Integer> entitlementList = getEntitlementIDList(tenantDTO);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("EntitlementId");
            add("EntitlementUnitsConsumed");
            add("IsSubscriptionEntitlement");
        }};

        LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports().openDataExport();
        var firstScanInfo = new String[]{entitlementList.get(0).toString(), value1, value2};

        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage).getAllRows(csvColumnsToValidate);
        assertThat(dataFromCsv)
                .as("verifying value of EntitlementId, EntitlementUnitsConsumed, IsSubscriptionEntitlement")
                .contains(firstScanInfo);
    }

    private List<Integer> getEntitlementIDList(TenantDTO tenantDTO) {
        var entitlementsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements();
        var entitlementIdList = entitlementsPage.getAll().stream()
                .map(EntitlementCell::getId).collect(Collectors.toList());
        BrowserUtil.clearCookiesLogOff();
        return entitlementIdList;
    }
}