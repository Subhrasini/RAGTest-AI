package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.parser.JSONParser;
import com.nimbusds.jose.shaded.json.parser.ParseException;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("784027")
public class SBOMLockFileTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;
    TenantDTO tenantDTO;
    File sbomFile;
    String dependencyPackage = "pkg:npm/qs@6.7.0";

    @MaxRetryCount(1)
    @Description("Admin should create tenant, app, Debricked entitlements, open source scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();

        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        EntitlementsActions.createEntitlements(debrickedEntitlement);
    }

    @MaxRetryCount(1)
    @Description("Wait for Open Source scan, download SBOM, parse it, verify dependency")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkSBOMLockfileTest() throws FileNotFoundException, ParseException {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setFileToUpload("payloads/fod/yarn.zip");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed)
                .tenantTopNavbar
                .openApplications()
                .openYourScans();
        sbomFile = page(YourScansPage.class)
                .getScanByType(applicationDTO, FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .downloadSBOM();
        assertThat(sbomFile.getName())
                .as("Verify file is downloaded")
                .contains("_cyclonedx.json");
        assertThat(sbomFile)
                .as("File should be not be empty")
                .isNotEmpty();

        JSONParser jsonParser = new JSONParser(2048);
        Object obj = jsonParser.parse(new FileReader(sbomFile.getAbsolutePath()));
        JSONObject jsonObject = (JSONObject) obj;
        AllureReportUtil.info("Accessing the 'dependencies' node in JSON document");
        JSONArray dependencies = (JSONArray) jsonObject.get("dependencies");
        AllureReportUtil.info("Removing forward slash delimiter from immediate entry under dependencies");
        var parsed = dependencies.stream().map(x -> x.toString().replace("\\", ""))
                .toList();
        assertThat(parsed)
                .as("Modified literalString should contain " + dependencyPackage)
                .anySatisfy(row -> assertThat(row).contains(dependencyPackage));

        AllureReportUtil.info("Counting of occurrences of package under dependencies\\dependsOn");
        int k = 0;
        for (Object dependency : dependencies) {
            JSONObject jsonObject1 = (JSONObject) dependency;
            JSONArray dependsOn = (JSONArray) jsonObject1.get("dependsOn");
            for (Object o : dependsOn) {
                if (o.toString().contentEquals(dependencyPackage)) {
                    k++;
                }
            }
        }
        assertThat(k)
                .as("There should be at least 3 occurrences of package name under depends on")
                .isPositive();
    }

}
