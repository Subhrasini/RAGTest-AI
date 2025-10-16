package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReportDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
public class VendorCustomerRelationshipSetupTest extends FodBaseTest {
    TenantDTO tenantDTO_A, tenantDTO_B;
    ApplicationDTO webApp_A;
    StaticScanDTO staticScanDTO_A;
    String tenant_ALinkId;
    List<String> expectedCustomerColumnList = new ArrayList<>() {{
        add("Customer");
        add("Notes");
        add("Last Activity Date");
        add("Status");
        add("");
    }};
    List<String> expectedVendorColumnList = new ArrayList<>() {{
        add("Vendor");
        add("Notes");
        add("Last Activity Date");
        add("Status");
        add("");
    }};

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenants from admin site")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Create two tenants , tenantA and tenantB");
        tenantDTO_A = TenantDTO.createDefaultInstance();
        tenantDTO_A.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO_A, true, false);
        tenantDTO_B = TenantDTO.createDefaultInstance();
        tenantDTO_B.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO_B, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Vendor Link ")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyVendorLink() {
        AllureReportUtil.info("Create a vendor link id from customers tab");
        var tenant_AVendorPage = LogInActions.tamUserLogin(tenantDTO_A).tenantTopNavbar.openAdministration()
                .openVendors();
        var vendorA_popup = tenant_AVendorPage.openCustomersTab().pressRequestToBeVendor();
        vendorA_popup.pressGenerate();
        tenant_ALinkId = vendorA_popup.getLinkId();
        vendorA_popup.pressClose();
        assertThat(tenant_AVendorPage.openCustomersTab().getCustomerColumnHeaders())
                .as("Customer page should contain columns")
                .hasSameElementsAs(expectedCustomerColumnList);
        assertThat(tenant_AVendorPage.openCustomersTab().customerTable.rows)
                .as("Customer page should contain one row")
                .hasSize(1);
        assertThat(tenant_AVendorPage.openCustomersTab().customerTable.getCellValue(3, 0))
                .as("Customer page should contain one row with status")
                .isEqualTo("Request Initiated");
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify  vendor link id from My Vendors tab");
        var tenant_BVendorPage = LogInActions.tamUserLogin(tenantDTO_B).tenantTopNavbar.openAdministration()
                .openVendors();
        var vendorB_popup = tenant_BVendorPage.openMyVendorsTab().pressVerifyVendorLink();
        vendorB_popup.setLinkId(tenant_ALinkId);
        vendorB_popup.pressSubmit();
        assertThat(tenant_BVendorPage.openMyVendorsTab().getVendorColumnHeaders())
                .as("Vendor  page should contain columns")
                .hasSameElementsAs(expectedVendorColumnList);
        assertThat(tenant_AVendorPage.openMyVendorsTab().vendorTable.rows)
                .as("Vendor page should contain one row")
                .hasSize(1);
        assertThat(tenant_AVendorPage.openMyVendorsTab().getStatusByTenantName(tenantDTO_A.getTenantName()))
                .as("Vendor page should contain one row with TenantA ,Pending Approval Status")
                .isEqualTo("Pending Approval");
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Approve the tenant from Customers tab , tenantB should be approved");
        tenant_AVendorPage = LogInActions.tamUserLogin(tenantDTO_A)
                .tenantTopNavbar.openAdministration().openVendors();
        tenant_AVendorPage.openCustomersTab();
        assertThat(tenant_AVendorPage.openCustomersTab().getStatusByTenantName(tenantDTO_B.getTenantName()))
                .as("Customer page,TenantB  request initiated status should be changed to Pending Approval")
                .isEqualTo("Pending Approval");
        tenant_AVendorPage.openCustomersTab().pressApprove().pressYes();
        assertThat(tenant_AVendorPage.openCustomersTab().getStatusByTenantName(tenantDTO_B.getTenantName()))
                .as("Customer page Pending Approval status should be changed to Approved")
                .isEqualTo("Approved");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify vendor report in tenant site")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyVendorLink"})
    public void verifyVendorReport() {
        AllureReportUtil.info("Start and complete a static scan for tenantA");
        startAndCompleteScanTenantA();

        AllureReportUtil.info("Create a report for tenantA for the completed scan");
        LogInActions.tamUserLogin(tenantDTO_A);
        var report = ReportDTO.createDefaultInstance();
        report.setReportTemplate("Static Summary");
        report.setApplicationDTO(webApp_A);
        log.info("Report DTO created" + report);
        var reportsPage = ReportActions.createReport(report);
        reportsPage.getReportByName(report.getReportName()).clickPublish(Actions.ReportAction.Publish)
                .selectCompany(tenantDTO_B.getTenantName())
                .pressPublish();
        Selenide.refresh();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify Vendor Report Tab of Tenant B , Published report should be present in tenantB");
        var vendorReportPage = LogInActions.tamUserLogin(tenantDTO_B).tenantTopNavbar.openReports()
                .openVendorReports();
        assertThat(vendorReportPage.sideNavTabExists(TenantSideNavTabs.Reports.VendorReport))
                .as("Validate that Vendor Report tab  exists for TenantB")
                .isTrue();
        assertThat(vendorReportPage.vendorReportTable.getTableElement().exists())
                .as("Table should exist with published report from tenantA")
                .isTrue();
        assertThat(vendorReportPage.getVendorTenantName(webApp_A.getApplicationName()))
                .as("Validate Vendor column of table , should contain vendor name as tenantA")
                .isEqualToIgnoringCase(tenantDTO_A.getTenantName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify report in tenant site")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyVendorReport"})
    public void deleteVendor() {
        AllureReportUtil.info("Delete vendor from Customer tab");
        var tenant_AVendorPage = LogInActions.tamUserLogin(tenantDTO_A).tenantTopNavbar.openAdministration()
                .openVendors();
        tenant_AVendorPage.openCustomersTab().pressDelete().pressYes();
        var customerTableRowsCount = tenant_AVendorPage.openCustomersTab().customerTable.getRowsCount();
        assertThat(customerTableRowsCount)
                .as("Row should be deleted")
                .isEqualTo(0);
        BrowserUtil.clearCookiesLogOff();

        var tenant_BVendorPage = LogInActions.tamUserLogin(tenantDTO_B).tenantTopNavbar.openAdministration()
                .openVendors();
        var vendorTablePresent = tenant_BVendorPage.openMyVendorsTab().vendorTable.getTableElement().exists();
        assertThat(vendorTablePresent)
                .as("Vendor should be deleted and table will not exist")
                .isFalse();
    }

    public void startAndCompleteScanTenantA() {
        webApp_A = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp_A, tenantDTO_A, true);
        staticScanDTO_A = StaticScanDTO.createDefaultInstance();
        StaticScanActions.importScanTenant(webApp_A, "payloads/fod/static.java.fpr");
        BrowserUtil.clearCookiesLogOff();
    }
}