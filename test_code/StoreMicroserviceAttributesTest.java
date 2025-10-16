package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.fortify.common.ui.config.AllureAttachmentsUtil;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.HashMap;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("763007")
@Slf4j
public class StoreMicroserviceAttributesTest extends FodBaseTest {

    ApplicationDTO webAppDTO;
    HashMap<String, String> microAttributesMap;
    TenantDTO tenantDTO;
    AttributeDTO microAttributeDto;
    String microAttributeValue, microserviceAttributeName;
    String dynamicFprFile = "payloads/fod/DynSuper1.fpr";

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create microservice attributes and applications for the test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        AllureReportUtil.info("Creating Tenant with enabled 'Microservices option for web applications'");
        microserviceAttributeName = "Micro" + UniqueRunTag.generate();
        microAttributeValue = "Version" + UniqueRunTag.generate();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Enable Microservices option for web applications",
                "Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        microAttributeDto = AttributeDTO.createDefaultInstance();
        microAttributeDto.setAttributeType(FodCustomTypes.AttributeType.Microservice);
        microAttributeDto.setRequired(true);
        webAppDTO = ApplicationDTO.createDefaultInstance();
        microAttributesMap = new HashMap<>() {{
            put(microAttributeDto.getAttributeName(), microAttributeValue);
        }};
        webAppDTO.setMicroservicesEnabled(true);
        webAppDTO.setApplicationMicroservices(new String[]{microserviceAttributeName});
        webAppDTO.setMicroserviceToChoose(microserviceAttributeName);
        webAppDTO.setMicroserviceAttributesMap(microAttributesMap);
        CustomAttributesActions.createCustomAttribute(microAttributeDto, tenantDTO, true);
        ApplicationActions.createApplication(webAppDTO, tenantDTO, false);
        var dynamicScanWADTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanWADTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(webAppDTO.getReleaseName(), dynamicFprFile,
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(webAppDTO, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Microservice Attributes needs to be stored at Microservice level")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void storeMicroserviceAttributesAtMicroserviceLevelTest() {
        String microserviceName = "Microservice" + UniqueRunTag.generate();
        String newMicroserviceName = "Microservice" + UniqueRunTag.generate();
        String microserviceVersion = "Version" + UniqueRunTag.generate();
        var microservicesPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openDetailsFor(webAppDTO.getApplicationName())
                .openSettings()
                .openMicroservicesTab();
        assertThat(microservicesPage.getMicroservicesTable().getAllDataRows().texts().toString())
                .as("Inside the Microservices display, verify the available Microservices attributes.")
                .contains(microserviceAttributeName);
        microservicesPage.clickAdd()
                .setMicroserviceName(microserviceName)
                .setAttributeName(microserviceVersion)
                .clickSave();
        WaitUtil.waitForTrue(() -> microservicesPage
                        .getMicroservicesTable()
                        .getAllDataRows()
                        .texts()
                        .toString()
                        .contains(microserviceName),
                Duration.ofMinutes(1),
                true);
        microservicesPage.editMicroservice(microserviceName)
                .setMicroserviceName(newMicroserviceName)
                .clickSave();
        WaitUtil.waitForTrue(() -> microservicesPage
                        .getMicroservicesTable()
                        .getAllDataRows()
                        .texts()
                        .toString()
                        .contains(newMicroserviceName),
                Duration.ofMinutes(1),
                true);
        microservicesPage.deleteMicroservice(newMicroserviceName);
        new ModalDialog().pressYes();
        var table = microservicesPage.getMicroservicesTable();
        assertThat(table.getRowsCount())
                .as("The attribute table row count should be one")
                .isEqualTo(1);
        assertThat(table.getAllDataRows().texts().toString())
                .as("Verify that the deleted Attribute record is no longer present.")
                .doesNotContain(newMicroserviceName);
    }

    @SneakyThrows
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("PDF Report File should contains microservice attribute details")
    @Test(groups = {"regression"},
            dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroserviceAttributeInDataExportTest() {
        AllureReportUtil.info("Verify the presence of the Microservice name for the application " +
                "and the applicable Microservice attribute details.");
        var releases = LogInActions.tamUserLogin(tenantDTO).openYourReleases().
                findWithSearchBox(webAppDTO.getApplicationName()).getAllReleases();
        var releaseName = releases.get(0).getFullReleaseName();
        var microServiceName = releases.get(0).getMicroserviceName();
        webAppDTO.setReleaseName(releaseName);
        var report = ReportDTO.createDefaultInstance();
        report.setReportTemplate("Dynamic Summary");
        report.setApplicationDTO(webAppDTO);
        var reportsPage = ReportActions.createReport(report);
        var file = reportsPage
                .getReportByName(report.getReportName())
                .downloadReport("pdf");
        PDF reportFile = new PDF(file);
        AllureAttachmentsUtil.attachFile(file, report.getReportName(), "pdf", "application/pdf");
        assertThat(reportFile)
                .as("PDF Report File should contains microservice name and microservice attribute details")
                .containsExactText(String.format("Microservice Name: %s", microServiceName))
                .containsExactText(String.format("%s: %s", microAttributeDto.getAttributeName(), microAttributeValue));
    }
}
