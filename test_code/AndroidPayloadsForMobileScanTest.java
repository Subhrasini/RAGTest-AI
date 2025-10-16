package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.MobileScansPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.exceptions.FodElementNotCreatedException;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_BAD_REQUEST;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("kbadia@opentext.com")
@FodBacklogItem("507001")
@Slf4j
public class AndroidPayloadsForMobileScanTest extends FodBaseTest {

    TenantDTO tenantDTO;
    int entitlementId;
    File exe;
    String braveBrowserFilePath = "payloads/fod/BraveBrowserSetup.fpr";
    HashMap<String, String> extensionsToValidate = new HashMap<>() {{
        put("aab", "payloads/fod/bundleWithFusingModules.aab");
        put("apk", "payloads/fod/flickrj.apk");
        put("ipa", "payloads/fod/iGoat.ipa");
    }};
    List<String> filePath = new ArrayList<>() {{
        add("ipa");
        add("exe");
    }};

    List<String> changedFile = new ArrayList<>() {{
        add("AAB_CHANGED_FROM_APK_FILE");
        add("AAB_CHANGED_FROM_EXE_FILE");
    }};

    public File copyFile(String path, String name) {
        var file = new File(path);
        var copiedFile = new File("payloads/fod/" + name);
        try {
            FileUtils.copyFile(file, copiedFile);
        } catch (Exception e) {
            log.error("File was not copied...");
            throw new FodElementNotCreatedException("File was not copied...");
        }
        return copiedFile;
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test Data preparation")
    @Test(groups = {"regression"})
    public void createTenant() {
        exe = copyFile(braveBrowserFilePath, "newExeFile.exe");

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission",
                        //"Scan Third Party Libraries",
                        "Enable Source Download"
                });

        entitlementId = TenantActions.createTenant(tenantDTO).openEntitlements().getAll().get(0).getId();
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
    }

    @MaxRetryCount(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("Publishing a mobile scan with framework iOS")
    @Parameters({"Extension", "Path", "File Name"})
    @Test(groups = {"regression"}, dataProvider = "extensions",
            dataProviderClass = AndroidPayloadsForMobileScanTest.class,
            dependsOnMethods = "createTenant")
    public void validateMobScansWithPayload(String extension, String just, String forProvider) {
        setTestCaseName("Validate Mob Scan With Payload: " + extension);
        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        if (extension.equals("ipa")) {
            mobileScanDTO.setFrameworkType("iOS");
        }
        mobileScanDTO.setFileToUpload(extensionsToValidate.get(extension));
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO);

        BrowserUtil.clearCookiesLogOff();

        var overViewPage = LogInActions.adminLogIn().adminTopNavbar
                .openMobile()
                .openDetailsFor(applicationDTO.getApplicationName());

        assertThat(overViewPage.getScanStatus())
                .as("Check if scan imported and In Progress")
                .contains("In Progress");
        assertThat(overViewPage.getAppName())
                .as("Correct application found")
                .isEqualTo(applicationDTO.getApplicationName());
        if (!extension.equals("apk")) {
            File file = overViewPage.downloadBinary(extension);
            assertThat(file)
                    .as("Verifying the extension")
                    .hasExtension(extension);
        } else {
            log.info("Skip downloading for .apk file to avoid of Download Error");
        }

        overViewPage.waitForMobiusSuccess().publishScan()
                .waitScanCompleted();
        assertThat(overViewPage.getScanStatus())
                .as("Check if scan completed on Admin Side").contains("Completed");
    }

    @MaxRetryCount(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate mobile scan with Android/iOS framework types")
    @Parameters({"Extension", "Path", "File Name", "Framework Type"})
    @Test(groups = {"regression"}, dataProvider = "negativeExtensions",
            dataProviderClass = AndroidPayloadsForMobileScanTest.class,
            dependsOnMethods = "validateMobScansWithPayload")
    public void negativeValidationForFrameworkTypes(String ext, String filePath,
                                                    String fileName, String frameworkType) {

        setTestCaseName(String.format("Validate Framework type: %s with: %s", frameworkType, ext));

        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.START_SCANS);

        Integer assessmentTypeId = apiActions
                .getAssessmentIdByScanTypeAndName(
                        releaseId,
                        FodCustomTypes.ScanType.Mobile,
                        "AUTO-MOBILE");

        if (filePath.isEmpty()) {
            filePath = exe.getAbsolutePath();
            fileName = exe.getName();
        }

        MobileScansPayload mobileScansPayload = MobileScansPayload.defaultMobileScanSetup(assessmentTypeId);
        Response mobileSaveResponse = apiActions.getMobileScansApiProvider()
                .saveMobileScanSetup(releaseId, mobileScansPayload);
        mobileSaveResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        File mobileApp = ApiHelper.retrievePayloadFile(filePath);
        String startDate = LocalDateTime.now().plusSeconds(10).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("assessmentTypeId", assessmentTypeId);
        queryParams.put("entitlementId", entitlementId);
        queryParams.put("timeZone", "FLE Standard Time");
        queryParams.put("frameworkType", frameworkType);
        queryParams.put("platformType", FodCustomTypes.MobilePlatformType.Phone.getTypeValue());
        queryParams.put("entitlementFrequencyType", FodCustomTypes.EntitlementFrequencyType.SingleScan.getTypeValue());
        queryParams.put("startDate", startDate);
        queryParams.put("fileName", fileName);

        Response mobileScanResponse = apiActions.getMobileScansApiProvider()
                .startMobileScan(releaseId, queryParams, mobileApp);
        mobileScanResponse.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);
        String errorMessage = mobileScanResponse.asString();
        String expectedMessage = frameworkType.equals(FodCustomTypes.MobileFrameworkType.Android.getTypeValue())
                ? "The binary source file must be of type '.apk' or '.aab'."
                : "The binary source file must be of type '.ipa'.";

        assertThat(errorMessage)
                .as("Verify the error message")
                .contains(expectedMessage);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate mobile scan with Android framework and aab payload with changed from exe extension")
    @Test(groups = {"regression"},
            dependsOnMethods = "negativeValidationForFrameworkTypes")
    public void validateAndroidWithIPAAndEXEPayloadTest() {
        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        var mobileScanSetupPage = ApplicationActions.createApplication(applicationDTO,
                        defaultTenantDTO, true)
                .pressStartMobileScan()
                .setAssessmentType(mobileScanDTO.getAssessmentType())
                .setEntitlement(mobileScanDTO.getEntitlement())
                .setTimeZone(mobileScanDTO.getTimezone())
                .setFrameworkType("Android")
                .setAuditPreference(mobileScanDTO.getAuditPreference().getTypeValue())
                .pressStartScanBtn()
                .pressNextButton();
        for (var filesToValidate : filePath) {
            if (filesToValidate.equals("exe")) {
                mobileScanSetupPage.uploadFile(exe.getAbsolutePath());
            } else {
                mobileScanSetupPage.uploadFile("payloads/fod/iGoat.ipa");
            }
            assertThat(mobileScanSetupPage.getErrorMsg())
                    .as("Verify error message in the modal dialog")
                    .contains("Invalid file extension.");
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("595015")
    @Description("Validate mobile scan with Android framework and aab payload with changed from exe and apk extension")
    @Test(groups = {"regression"}, enabled = false)
    public void validateAndroidWithAABAndAPKPayloadTest() {
        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        var mobileScanSetupPage = ApplicationActions.createApplication(applicationDTO,
                        defaultTenantDTO, true)
                .pressStartMobileScan()
                .setAssessmentType(mobileScanDTO.getAssessmentType())
                .setEntitlement(mobileScanDTO.getEntitlement())
                .setTimeZone(mobileScanDTO.getTimezone())
                .setFrameworkType("Android")
                .setAuditPreference(mobileScanDTO.getAuditPreference().getTypeValue())
                .pressStartScanBtn()
                .pressNextButton();
        for (var filesToValidate : changedFile) {
            File file;
            if (filesToValidate.equals("AAB_CHANGED_FROM_EXE_FILE")) {
                file = copyFile(braveBrowserFilePath, "new.aab");
                mobileScanSetupPage.uploadFile(file.getAbsolutePath());
            } else {
                file = copyFile(braveBrowserFilePath, "new.apk");
                mobileScanSetupPage.uploadFile(file.getAbsolutePath());
            }
            assertThat(mobileScanSetupPage.getErrorMsg())
                    .as("Verify error message in the modal dialog")
                    .contains("Invalid file extension.");
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify a mobile scan with framework iOS/Android frameworks")
    @Test(dataProvider = "extensions", dataProviderClass = AndroidPayloadsForMobileScanTest.class,
            groups = {"regression"},
            dependsOnMethods = "validateAndroidWithIPAAndEXEPayloadTest")
    public void validateMobScansApi(String ext, String filePath, String fileName) {

        var applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(),
                        AccessScopeRestrictionsFodApi.START_SCANS);

        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                releaseId,
                FodCustomTypes.ScanType.Mobile,
                "AUTO-MOBILE");
        MobileScansPayload mobileScansPayload = MobileScansPayload.defaultMobileScanSetup(assessmentTypeId);
        apiActions.getMobileScansApiProvider().saveMobileScanSetup(releaseId, mobileScansPayload);
        File mobileApp = ApiHelper.retrievePayloadFile(filePath);
        String startDate = LocalDateTime.now().plusSeconds(10).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("assessmentTypeId", assessmentTypeId);
        queryParams.put("entitlementId", entitlementId);
        queryParams.put("timeZone", "FLE Standard Time");
        queryParams.put("frameworkType",
                ext.equals("ipa")
                        ? FodCustomTypes.MobileFrameworkType.IOS.getTypeValue()
                        : FodCustomTypes.MobileFrameworkType.Android.getTypeValue());
        queryParams.put("platformType", FodCustomTypes.MobilePlatformType.Phone.getTypeValue());
        queryParams.put("entitlementFrequencyType", FodCustomTypes.EntitlementFrequencyType.SingleScan.getTypeValue());
        queryParams.put("startDate", startDate);
        queryParams.put("fileName", fileName);

        Response mobileScanResponse = apiActions.getMobileScansApiProvider()
                .startMobileScan(releaseId, queryParams, mobileApp);

        mobileScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);
        var mobileScanSetupPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(),
                        applicationDTO.getReleaseName()).openMobileScanSetup();
        mobileScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        var overViewPage = LogInActions.adminLogIn().adminTopNavbar
                .openMobile()
                .openDetailsFor(applicationDTO.getApplicationName());

        assertThat(overViewPage.getScanStatus())
                .as("Check if scan imported and In Progress")
                .contains("In Progress");
        assertThat(overViewPage.getAppName())
                .as("Correct application found")
                .isEqualTo(applicationDTO.getApplicationName());
        if (!ext.equals("apk")) {
            File file = overViewPage.downloadBinary(ext);
            assertThat(file)
                    .as("Verifying the extension")
                    .hasExtension(ext);
        } else {
            log.info("Skip downloading for .apk file to avoid of Download Error");
        }

        overViewPage.waitForMobiusSuccess().publishScan()
                .waitScanCompleted();
        assertThat(overViewPage.getScanStatus())
                .as("Check if scan completed on Admin Side").contains("Completed");
    }

    @DataProvider(name = "extensions", parallel = false)
    public Object[][] extensions() {
        return new Object[][]{
                {"aab", "payloads/fod/bundleWithFusingModules.aab", "bundleWithFusingModules.aab"},
                {"apk", "payloads/fod/flickrj.apk", "flickrj.apk"},
                {"ipa", "payloads/fod/iGoat.ipa", "iGoat.ipa"}
        };
    }

    @DataProvider(name = "negativeExtensions", parallel = true)
    public Object[][] negativeExtensions() {
        return new Object[][]{
                {"aab", "payloads/fod/bundleWithFusingModules.aab", "bundleWithFusingModules.aab",
                        FodCustomTypes.MobileFrameworkType.IOS.getTypeValue()},
                {"apk", "payloads/fod/flickrj.apk", "flickrj.apk", FodCustomTypes.MobileFrameworkType.IOS.getTypeValue()},
                {"ipa", "payloads/fod/iGoat.ipa", "iGoat.ipa", FodCustomTypes.MobileFrameworkType.Android.getTypeValue()},
                {"exe", "", "", FodCustomTypes.MobileFrameworkType.Android.getTypeValue()},
                {"exe", "", "", FodCustomTypes.MobileFrameworkType.IOS.getTypeValue()}
        };
    }
}