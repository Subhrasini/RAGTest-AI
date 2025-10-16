package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class CopyStateReleaseTest extends FodBaseTest {
    ApplicationDTO webAppDTO, mobileAppDTO, secondWebAppDTO, secondMobileAppDTO;
    DynamicScanDTO dynamicScanDTO, secondDynamicScanDTO, thirdDynamicScanDTO, fourthDynamicScanDTO, fifthDynamicScanDTO;
    MobileScanDTO mobileScanDTO, secondMobileScanDTO, thirdMobileScanDTO, fourthMobileScanDTO, fifthMobileScanDTO;
    ReleaseDTO validationWebCopyReleaseDTO, cancelThisReleaseWebDTO, releaseDynamicCompletedDTO, releaseDynamicImportedDTO,
            validateDynamicReleaseCopyDTO, cancelThisDynamicReleaseDTO,
            validateMobileCopyReleasesDTO, validateCancelMobileAppReleaseDTO, mobileScanCompletedReleaseDTO,
            mobileScanImportedReleaseDTO, secondValidateMobileCopyReleasesDTO, secondValidateCancelMobileAppReleaseDTO,
            thirdValidateCancelMobileAppReleaseDTO;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create static and dynamic scans as well as verify copy states for scans including cancelled and paused scans")
    @Test(groups = {"regression"}, priority = 1)

    public void webAppValidateStaticWithDynamicCopyStateReleaseTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        webAppDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webAppDTO, defaultTenantDTO, false);

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        StaticScanActions.importScanTenant(webAppDTO, "payloads/fod/chat_application_via_lan.fpr");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .openDetailsFor(webAppDTO.getApplicationName())
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        var query = "select count(*) from " +
                "ProjectScanDetailPauseDetail psdpd join ProjectVersionScan pvs on psdpd.DetailScanId = pvs.ScanId" +
                " where pvs.ProjectID = ";
        String pausedScans = getPausedScanCount(defaultTenantDTO.getTenantName(), webAppDTO.getApplicationName(), query);

        var copyFirstReleaseDTO =
                assertScanIDsEqual(webAppDTO, webAppDTO.getReleaseName());

        var pausedScans2 = getPausedScanCount(defaultTenantDTO.getTenantName(), webAppDTO.getApplicationName(), query);
        assertThat(pausedScans2).as("Paused scans should be: " + pausedScans).isEqualTo(pausedScans);

        var verifyPausedScanSecondRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(webAppDTO, copyFirstReleaseDTO)
                .openScans();
        assertThat(verifyPausedScanSecondRelease.getTable())
                .as("Scan table for " + copyFirstReleaseDTO.getReleaseName() + " should be empty").isNull();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .findWithSearchBox(webAppDTO.getApplicationName())
                .openDetailsFor(webAppDTO.getApplicationName())
                .pressResumeButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        checkDynamicScanStatus(webAppDTO.getApplicationName(), webAppDTO.getReleaseName(), "In Progress");

        var copySecondReleaseDTO =
                assertScanIDsEqual(webAppDTO, webAppDTO.getReleaseName());
        var pausedScans3 = getPausedScanCount(defaultTenantDTO.getTenantName(), webAppDTO.getApplicationName(), query);
        assertThat(pausedScans3).as("Paused scans should be: " + pausedScans).isEqualTo(pausedScans);

        var verifyPausedScanThirdRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(webAppDTO, copySecondReleaseDTO)
                .openScans();
        assertThat(verifyPausedScanThirdRelease.getTable()).as("Scan table for " +
                copySecondReleaseDTO.getReleaseName() + " should be empty").isNull();

        DynamicScanActions.importDynamicScanAdmin(webAppDTO.getReleaseName(), "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(webAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

    }

    @MaxRetryCount(3)
    @Description("Admin user should be able to create static and mobile scans as well as verify copy states for scans including cancelled and paused scans")
    @Test(groups = {"regression"})

    public void webAppValidateStaticWithMobileCopyStateReleasesTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        mobileAppDTO = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(mobileAppDTO, defaultTenantDTO, false);
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        StaticScanActions.importScanTenant(mobileAppDTO, "payloads/fod/chat_application_via_lan.fpr");
        MobileScanActions.createMobileScan(mobileScanDTO, mobileAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openMobile()
                .findScanByAppDto(mobileAppDTO)
                .openDetailsFor(mobileAppDTO.getApplicationName())
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        var query = "select count(*) from " +
                "ProjectScanDetailPauseDetail psdpd join ProjectVersionScan pvs on psdpd.DetailScanId = pvs.ScanId" +
                " where pvs.ProjectID = ";
        String pausedScans = getPausedScanCount(defaultTenantDTO.getTenantName(), mobileAppDTO.getApplicationName(), query);

        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openYourScans()
                .getScanByType(mobileAppDTO, FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        var copySecondScanMobileReleaseDTO =
                assertScanIDsEqual(mobileAppDTO, mobileAppDTO.getReleaseName());

        String pausedScans2 = getPausedScanCount(defaultTenantDTO.getTenantName(), mobileAppDTO.getApplicationName(), query);
        assertThat(pausedScans2).as("Paused scans should be: " + pausedScans).isEqualTo(pausedScans);

        var verifyPausedScanSecondRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(mobileAppDTO, copySecondScanMobileReleaseDTO)
                .openScans();
        assertThat(verifyPausedScanSecondRelease.getTable())
                .as("Scan table for " + copySecondScanMobileReleaseDTO.getReleaseName() + " should be empty")
                .isNull();

        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openMobile()
                .findWithSearchBox(mobileAppDTO.getApplicationName())
                .openDetailsFor(mobileAppDTO.getApplicationName())
                .pressResumeButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        checkMobileScanStatus(mobileAppDTO.getApplicationName(), mobileAppDTO.getReleaseName(), "In Progress");

        var copyAnotherSecondScanMobileReleaseDTO =
                assertScanIDsEqual(mobileAppDTO, mobileAppDTO.getReleaseName());
        String pausedScans3 = getPausedScanCount(defaultTenantDTO.getTenantName(), mobileAppDTO.getApplicationName(), query);
        assertThat(pausedScans3).as("Paused scans should be: " + pausedScans).isEqualTo(pausedScans);

        var verifyPausedScanThirdRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(mobileAppDTO, copyAnotherSecondScanMobileReleaseDTO)
                .openScans();
        assertThat(verifyPausedScanThirdRelease.getTable())
                .as("Scan table for " + copyAnotherSecondScanMobileReleaseDTO.getReleaseName()
                        + " should be empty")
                .isNull();
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions.importMobileScanAdmin(mobileAppDTO.getApplicationName(), FodCustomTypes.ImportFprScanType.Mobile,
                "payloads/fod/mobile_IOS.fpr", false, false, true);
        MobileScanActions.completeMobileScan(mobileAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

    }

    @MaxRetryCount(1)
    @Description("Admin user should be able to create static and dynamic scans and verify availability of Releases that include cancelled and paused scans")
    @Test(groups = {"regression"})

    public void webAppAbilityToCopyReleasesTest() {
        secondWebAppDTO = ApplicationDTO.createDefaultInstance();
        validateDynamicReleaseCopyDTO = ReleaseDTO.createDefaultInstance();
        cancelThisDynamicReleaseDTO = ReleaseDTO.createDefaultInstance();

        ApplicationActions.createApplication(secondWebAppDTO, defaultTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        importStaticScan(secondWebAppDTO.getApplicationName(), secondWebAppDTO.getReleaseName());

        var isScanImported = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), secondWebAppDTO.getReleaseName());
        WaitUtil.waitForTrue(() -> isScanImported.getStaticScanStatus().contains("Completed"), Duration.ofMinutes(25),
                false);
        assertThat(isScanImported.getStaticScanStatus()).as("Import wait status should be Completed")
                .isEqualTo("Completed");
        BrowserUtil.clearCookiesLogOff();

        var secondWebCopyReleaseDTO = runStaticScan("AUTO-STATIC",
                secondWebAppDTO.getApplicationName(),
                FodCustomTypes.SetupScanPageStatus.Completed, FodCustomTypes.AuditPreference.Automated);

        var thirdWebCopyReleaseDTO = cancelStaticScan("AUTO-STATIC",
                secondWebAppDTO.getApplicationName(),
                FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.AuditPreference.Automated);

        var fourthWebCopyReleaseDTO = runStaticScan("Static Standard",
                secondWebAppDTO.getApplicationName(),
                FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.AuditPreference.Manual);

        var fifthWebCopyReleaseDTO = runStaticScan("Static Standard",
                secondWebAppDTO.getApplicationName(),
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.AuditPreference.Manual);

        validationWebCopyReleaseDTO = ReleaseDTO.createDefaultInstance();
        var getReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(validationWebCopyReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowCount = getReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(rowCount).as("row count is expected to equal 2, was: " + rowCount).isEqualTo(2);
        assertThat(getReleaseNamesList(getReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondWebAppDTO.getReleaseName(),
                        secondWebCopyReleaseDTO.getReleaseName())
                .doesNotContain(thirdWebCopyReleaseDTO.getReleaseName(),
                        fourthWebCopyReleaseDTO.getReleaseName(),
                        fifthWebCopyReleaseDTO.getReleaseName());

        getReleasesToCopy.selectReleaseToCopyFrom(secondWebCopyReleaseDTO.getReleaseName());
        getReleasesToCopy.clickSave();
        BrowserUtil.clearCookiesLogOff();

        var releaseIDString = fourthWebCopyReleaseDTO.getReleaseName();
        var query8 =
                "select ScanID from ProjectVersionScan where ProjectVersionName = " + "'" + releaseIDString + "'" + ";";
        var queryScanID = Integer.parseInt(new FodSQLUtil().getStringValueFromDB(query8));
        LogInActions
                .adminLogIn()
                .adminTopNavbar.openStatic()
                .findScanByScanId(queryScanID)
                .openDetails()
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        cancelThisReleaseWebDTO = ReleaseDTO.createDefaultInstance();
        var getMoreReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(cancelThisReleaseWebDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var anotherRowCount = getMoreReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(anotherRowCount)
                .as("row count is expected to equal 3, was: " + rowCount).isEqualTo(3);
        assertThat(getReleaseNamesList(getMoreReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondWebAppDTO.getReleaseName(),
                        secondWebCopyReleaseDTO.getReleaseName(),
                        validationWebCopyReleaseDTO.getReleaseName())
                .doesNotContain(thirdWebCopyReleaseDTO.getReleaseName(),
                        fourthWebCopyReleaseDTO.getReleaseName(),
                        fifthWebCopyReleaseDTO.getReleaseName(),
                        cancelThisReleaseWebDTO.getReleaseName());

        getMoreReleasesToCopy.pressClose();
        BrowserUtil.clearCookiesLogOff();

        releaseDynamicCompletedDTO = ReleaseDTO.createDefaultInstance();
        secondDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        releaseDynamicCompletedDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        LogInActions.tamUserLogin(defaultTenantDTO);
        ReleaseActions.createRelease(secondWebAppDTO, releaseDynamicCompletedDTO);
        DynamicScanActions.createDynamicScan(secondDynamicScanDTO, secondWebAppDTO,
                releaseDynamicCompletedDTO.getReleaseName(), FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(releaseDynamicCompletedDTO.getReleaseName(),
                "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(secondWebAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

        checkDynamicScanStatus(secondWebAppDTO.getApplicationName(),
                releaseDynamicCompletedDTO.getReleaseName(), "Completed");

        releaseDynamicImportedDTO = ReleaseDTO.createDefaultInstance();
        releaseDynamicImportedDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .createNewRelease(releaseDynamicImportedDTO)
                .openScans()
                .pressImportByScanType(FodCustomTypes.ScanType.Dynamic)
                .uploadFile("payloads/fod/DynSuper1.fpr")
                .pressImportButton();
        BrowserUtil.clearCookiesLogOff();

        checkDynamicScanStatus(secondWebAppDTO.getApplicationName(),
                releaseDynamicImportedDTO.getReleaseName(), "Completed");

        thirdDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), thirdWebCopyReleaseDTO.getReleaseName())
                .pressStartDynamicScan();
        DynamicScanActions.createDynamicScan(thirdDynamicScanDTO).waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .findWithSearchBox(thirdWebCopyReleaseDTO.getReleaseName())
                .openDetailsFor(thirdWebCopyReleaseDTO.getReleaseName())
                .pressCancelBtn()
                .setReason("Other")
                .pressOkBtn();
        BrowserUtil.clearCookiesLogOff();

        checkDynamicScanStatus(secondWebAppDTO.getApplicationName(),
                thirdWebCopyReleaseDTO.getReleaseName(), "Canceled");

        fourthDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), fourthWebCopyReleaseDTO.getReleaseName())
                .pressStartDynamicScan();
        DynamicScanActions.createDynamicScan(fourthDynamicScanDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var getDynamicScanReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(validateDynamicReleaseCopyDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowCount2 = getDynamicScanReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(rowCount2).as("row count is expected to equal 5, was: " + rowCount).isEqualTo(5);
        assertThat(getReleaseNamesList(getDynamicScanReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondWebAppDTO.getReleaseName(),
                        secondWebCopyReleaseDTO.getReleaseName(),
                        validationWebCopyReleaseDTO.getReleaseName(),
                        releaseDynamicCompletedDTO.getReleaseName(),
                        releaseDynamicImportedDTO.getReleaseName())
                .doesNotContain(thirdWebCopyReleaseDTO.getReleaseName(),
                        fourthWebCopyReleaseDTO.getReleaseName(),
                        fifthWebCopyReleaseDTO.getReleaseName(),
                        cancelThisReleaseWebDTO.getReleaseName());

        getDynamicScanReleasesToCopy.selectReleaseToCopyFrom(releaseDynamicImportedDTO.getReleaseName())
                .clickSave();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .findWithSearchBox(fourthWebCopyReleaseDTO.getReleaseName())
                .openDetailsFor(fourthWebCopyReleaseDTO.getReleaseName())
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        var cancelThisCopyStateRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(cancelThisDynamicReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowsCount2 = cancelThisCopyStateRelease.getCopyStateTable().getRowsCount();
        assertThat(rowsCount2).as("row count is expected to equal 6, was: " + rowCount).isEqualTo(6);
        assertThat(getReleaseNamesList(cancelThisCopyStateRelease.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondWebAppDTO.getReleaseName(),
                        secondWebCopyReleaseDTO.getReleaseName(),
                        validationWebCopyReleaseDTO.getReleaseName(),
                        releaseDynamicCompletedDTO.getReleaseName(),
                        releaseDynamicImportedDTO.getReleaseName(),
                        validateDynamicReleaseCopyDTO.getReleaseName())
                .doesNotContain(thirdWebCopyReleaseDTO.getReleaseName(),
                        fourthWebCopyReleaseDTO.getReleaseName(),
                        fifthWebCopyReleaseDTO.getReleaseName(),
                        cancelThisReleaseWebDTO.getReleaseName(),
                        cancelThisDynamicReleaseDTO.getReleaseName());

        cancelThisCopyStateRelease.pressClose();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), fourthWebCopyReleaseDTO.getReleaseName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic)
                .cancelScan();
        BrowserUtil.clearCookiesLogOff();

        fifthDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        fifthDynamicScanDTO.setStartInFuture(true);
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), fourthWebCopyReleaseDTO.getReleaseName())
                .pressStartDynamicScan();
        DynamicScanActions.createDynamicScan(fifthDynamicScanDTO).waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        var cancelAnotherCopyStateRelease = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openDetailsFor(secondWebAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(cancelThisDynamicReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowsCount3 = cancelAnotherCopyStateRelease.getCopyStateTable().getRowsCount();
        assertThat(rowsCount3).as("row count is expected to equal 6, was: " + rowCount).isEqualTo(6);
        assertThat(getReleaseNamesList(cancelAnotherCopyStateRelease.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondWebAppDTO.getReleaseName(),
                        secondWebCopyReleaseDTO.getReleaseName(),
                        validationWebCopyReleaseDTO.getReleaseName(),
                        releaseDynamicCompletedDTO.getReleaseName(),
                        releaseDynamicImportedDTO.getReleaseName(),
                        validateDynamicReleaseCopyDTO.getReleaseName())
                .doesNotContain(thirdWebCopyReleaseDTO.getReleaseName(),
                        fourthWebCopyReleaseDTO.getReleaseName(),
                        fifthWebCopyReleaseDTO.getReleaseName(),
                        cancelThisReleaseWebDTO.getReleaseName(),
                        cancelThisDynamicReleaseDTO.getReleaseName());

        cancelAnotherCopyStateRelease.pressClose();
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(1)
    @Description("Admin user should be able to create static and mobile scans and verify availability of Releases that include cancelled and paused scans")
    @Test(groups = {"regression"})

    public void mobileAppAbilityToCopyReleasesTest() {
        secondMobileAppDTO = ApplicationDTO.createDefaultMobileInstance();
        validateMobileCopyReleasesDTO = ReleaseDTO.createDefaultInstance();
        validateCancelMobileAppReleaseDTO = ReleaseDTO.createDefaultInstance();
        mobileScanCompletedReleaseDTO = ReleaseDTO.createDefaultInstance();
        mobileScanImportedReleaseDTO = ReleaseDTO.createDefaultInstance();
        secondValidateMobileCopyReleasesDTO = ReleaseDTO.createDefaultInstance();
        secondValidateCancelMobileAppReleaseDTO = ReleaseDTO.createDefaultInstance();
        thirdValidateCancelMobileAppReleaseDTO = ReleaseDTO.createDefaultInstance();

        ApplicationActions.createApplication(secondMobileAppDTO, defaultTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        secondMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        importStaticScan(secondMobileAppDTO.getApplicationName(), secondMobileAppDTO.getReleaseName());

        var secondMobileAppCompletedReleaseDTO = runStaticScan("AUTO-STATIC",
                secondMobileAppDTO.getApplicationName(), FodCustomTypes.SetupScanPageStatus.Completed,
                FodCustomTypes.AuditPreference.Automated);

        var mobileAppCancelReleaseDTO = cancelStaticScan("AUTO-STATIC",
                secondMobileAppDTO.getApplicationName(),
                FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.AuditPreference.Automated);

        var mobileAppInProReleaseDTO = runStaticScan("Static Standard",
                secondMobileAppDTO.getApplicationName(), FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.AuditPreference.Manual);

        var mobileAppQueuedReleaseDTO = runStaticScan("Static Standard",
                secondMobileAppDTO.getApplicationName(), FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.AuditPreference.Manual);

        var checkReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(validateMobileCopyReleasesDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var countRows = checkReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(countRows).as("row count is expected to equal 2, was: " + countRows).isEqualTo(2);
        assertThat(getReleaseNamesList(checkReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondMobileAppDTO.getReleaseName(),
                        secondMobileAppCompletedReleaseDTO.getReleaseName())
                .doesNotContain(mobileAppCancelReleaseDTO.getReleaseName(),
                        mobileAppInProReleaseDTO.getReleaseName(),
                        mobileAppQueuedReleaseDTO.getReleaseName());

        checkReleasesToCopy.selectReleaseToCopyFrom(secondMobileAppCompletedReleaseDTO.getReleaseName())
                .clickSave();
        BrowserUtil.clearCookiesLogOff();

        var getReleaseIDString = mobileAppInProReleaseDTO.getReleaseName();
        var query8 = "select ScanID from ProjectVersionScan where ProjectVersionName = "
                + "'" + getReleaseIDString + "'" + ";";
        var anotherQueryScanID = Integer.parseInt(new FodSQLUtil().getStringValueFromDB(query8));
        LogInActions
                .adminLogIn()
                .adminTopNavbar.openStatic()
                .findScanByScanId(anotherQueryScanID)
                .openDetails()
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        var nextCheckMoreReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(validateCancelMobileAppReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var countRows2 = nextCheckMoreReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(countRows2).as("row count is expected to equal 3, was: " + countRows).isEqualTo(3);
        assertThat(getReleaseNamesList(nextCheckMoreReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondMobileAppDTO.getReleaseName(),
                        secondMobileAppCompletedReleaseDTO.getReleaseName(),
                        validateMobileCopyReleasesDTO.getReleaseName())
                .doesNotContain(mobileAppCancelReleaseDTO.getReleaseName(),
                        mobileAppInProReleaseDTO.getReleaseName(),
                        mobileAppQueuedReleaseDTO.getReleaseName(),
                        validateCancelMobileAppReleaseDTO.getReleaseName());

        nextCheckMoreReleasesToCopy.pressClose();
        BrowserUtil.clearCookiesLogOff();

        secondMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanCompletedReleaseDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .createNewRelease(mobileScanCompletedReleaseDTO)
                .pressStartMobileScan()
                .createMobileScan(secondMobileScanDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions
                .importMobileScanAdmin(secondMobileAppDTO.getApplicationName(), FodCustomTypes.ImportFprScanType.Mobile,
                        "payloads/fod/mobile_IOS.fpr", false, false, true);
        MobileScanActions.completeMobileScan(secondMobileAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

        checkMobileScanStatus(secondMobileAppDTO.getApplicationName(),
                mobileScanCompletedReleaseDTO.getReleaseName(), "Completed");

        mobileScanImportedReleaseDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .createNewRelease(mobileScanImportedReleaseDTO)
                .openScans()
                .pressImportByScanType(FodCustomTypes.ScanType.Static)
                .uploadFile("payloads/fod/static.java.fpr")
                .pressImportButton();
        BrowserUtil.clearCookiesLogOff();

        thirdMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondMobileAppDTO.getApplicationName(),
                        mobileAppCancelReleaseDTO.getReleaseName())
                .pressStartMobileScan()
                .createMobileScan(thirdMobileScanDTO).waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .adminLogIn()
                .adminTopNavbar.openMobile()
                .openDetailsFor(secondMobileAppDTO.getApplicationName(), true)
                .pressCancelBtn()
                .setReason("Cancelled by customer")
                .pressOkBtn();
        BrowserUtil.clearCookiesLogOff();

        fourthMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondMobileAppDTO.getApplicationName(),
                        mobileAppCancelReleaseDTO.getReleaseName())
                .pressStartMobileScan()
                .createMobileScan(fourthMobileScanDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var secondValidationReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(secondValidateMobileCopyReleasesDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var countRows3 = secondValidationReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(countRows3).as("row count is expected to equal 5, was: " + countRows).isEqualTo(5);
        assertThat(getReleaseNamesList(secondValidationReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondMobileAppDTO.getReleaseName(),
                        secondMobileAppCompletedReleaseDTO.getReleaseName(),
                        validateMobileCopyReleasesDTO.getReleaseName(),
                        mobileScanCompletedReleaseDTO.getReleaseName(),
                        mobileScanImportedReleaseDTO.getReleaseName())
                .doesNotContain(mobileAppCancelReleaseDTO.getReleaseName(),
                        mobileAppInProReleaseDTO.getReleaseName(),
                        mobileAppQueuedReleaseDTO.getReleaseName(),
                        validateCancelMobileAppReleaseDTO.getReleaseName());

        secondValidationReleasesToCopy.selectReleaseToCopyFrom(mobileScanCompletedReleaseDTO.getReleaseName())
                .clickSave();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn()
                .adminTopNavbar.openMobile()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        var secondCheckReleasesToCopy = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(secondValidateCancelMobileAppReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowsCount3 = secondCheckReleasesToCopy.getCopyStateTable().getRowsCount();
        assertThat(rowsCount3).as("row count is expected to equal 6, was: " + countRows).isEqualTo(6);
        assertThat(getReleaseNamesList(secondCheckReleasesToCopy.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondMobileAppDTO.getReleaseName(),
                        secondMobileAppCompletedReleaseDTO.getReleaseName(),
                        validateMobileCopyReleasesDTO.getReleaseName(),
                        mobileScanCompletedReleaseDTO.getReleaseName(),
                        mobileScanImportedReleaseDTO.getReleaseName(),
                        secondValidateMobileCopyReleasesDTO.getReleaseName())
                .doesNotContain(mobileAppCancelReleaseDTO.getReleaseName(),
                        mobileAppInProReleaseDTO.getReleaseName(),
                        mobileAppQueuedReleaseDTO.getReleaseName(),
                        validateCancelMobileAppReleaseDTO.getReleaseName(),
                        secondValidateCancelMobileAppReleaseDTO.getReleaseName());

        secondCheckReleasesToCopy.pressClose();
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .adminLogIn()
                .adminTopNavbar.openMobile()
                .findWithSearchBox(secondMobileAppDTO.getApplicationName())
                .openDetailsFor(secondMobileAppDTO.getApplicationName(), true)
                .pressCancelBtn()
                .setReason("Cancelled by customer")
                .pressOkBtn();
        BrowserUtil.clearCookiesLogOff();

        fifthMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        fifthMobileScanDTO.setStartInFuture(true);
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondMobileAppDTO.getApplicationName(),
                        mobileAppInProReleaseDTO.getReleaseName())
                .pressStartMobileScan()
                .createMobileScan(fifthMobileScanDTO).waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        var cancelAnotherCopyStateRelease2 = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(secondMobileAppDTO.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(thirdValidateCancelMobileAppReleaseDTO.getReleaseName())
                .setSdlc("Development")
                .clickNext();

        var rowsCount4 = cancelAnotherCopyStateRelease2.getCopyStateTable().getRowsCount();
        assertThat(rowsCount4).as("row count is expected to equal 6, was: " + countRows).isEqualTo(6);
        assertThat(getReleaseNamesList(cancelAnotherCopyStateRelease2.getCopyStateTable()))
                .as("Items should be present or not present depending on assert statement")
                .contains(secondMobileAppDTO.getReleaseName(),
                        secondMobileAppCompletedReleaseDTO.getReleaseName(),
                        validateMobileCopyReleasesDTO.getReleaseName(),
                        mobileScanCompletedReleaseDTO.getReleaseName(),
                        mobileScanImportedReleaseDTO.getReleaseName(),
                        secondValidateMobileCopyReleasesDTO.getReleaseName())
                .doesNotContain(mobileAppCancelReleaseDTO.getReleaseName(),
                        mobileAppInProReleaseDTO.getReleaseName(),
                        mobileAppQueuedReleaseDTO.getReleaseName(),
                        validateCancelMobileAppReleaseDTO.getReleaseName(),
                        secondValidateCancelMobileAppReleaseDTO.getReleaseName());

        cancelAnotherCopyStateRelease2.pressClose();
        BrowserUtil.clearCookiesLogOff();
    }

    public String dbCheckPopulation(String query) {
        String actual;
        Supplier<String> sup = () -> new FodSQLUtil().getStringValueFromDB(query);
        WaitUtil.waitFor(WaitUtil.Operator.Equals, "1", sup, Duration.ofSeconds(120), false);

        actual = sup.get();
        assertThat(sup.get()).as("Actual " + actual + " should be equal to " + "1").isEqualTo("1");

        return actual;
    }

    public String getPausedScanCount(String tenantName, String appName, String query) {
        var applicationID = LogInActions.adminLogIn()
                .adminTopNavbar.openTenants()
                .findTenant(tenantName)
                .openTenantByName(tenantName)
                .openApplications()
                .getApplicationByName(appName)
                .getId();
        BrowserUtil.clearCookiesLogOff();
        String tempQuery = query + applicationID + ";";

        return dbCheckPopulation(tempQuery);
    }

    public int getScanID(ApplicationDTO appName, FodCustomTypes.ScanType scanType) {
        var aScanID = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications()
                .openYourScans()
                .getScanByType(appName, scanType)
                .getScanId();
        BrowserUtil.clearCookiesLogOff();

        return aScanID;
    }

    public ReleaseDTO assertScanIDsEqual(ApplicationDTO appName, String copyFromReleaseName) {
        ReleaseDTO dtoName = ReleaseDTO.createDefaultInstance();
        var getStaticScanID = getScanID(appName, FodCustomTypes.ScanType.Static);

        dtoName.setCopyState(true);
        dtoName.setCopyFromReleaseName(copyFromReleaseName);
        dtoName.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        var getHiddenReleaseID = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(appName.getApplicationName())
                .createNewRelease(dtoName)
                .pressStartStaticScan()
                .getHiddenReleaseID();
        BrowserUtil.clearCookiesLogOff();

        var query2 = "select count(*) from ProjectVersionScan where ProjectVersionID = " + getHiddenReleaseID + ";";
        dbCheckPopulation(query2);

        var query3 = "select CopiedFromScanID from ProjectVersionScan where ProjectVersionID = " + getHiddenReleaseID + ";";
        var getCopiedFromID = new FodSQLUtil().getStringValueFromDB(query3);
        Integer intGetCopiedFromID = Integer.valueOf(getCopiedFromID);
        assertThat(intGetCopiedFromID)
                .as("Copied from id: " + intGetCopiedFromID
                        + " should match referenced static scan id " + getStaticScanID)
                .isEqualTo(getStaticScanID);

        return dtoName;
    }

    public void importStaticScan(String appName, String releaseName) {

        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(appName, releaseName)
                .openScans()
                .pressImportByScanType(FodCustomTypes.ScanType.Static)
                .uploadFile("payloads/fod/static.java.fpr")
                .pressImportButton();
        BrowserUtil.clearCookiesLogOff();
    }

    public ReleaseDTO runStaticScan(String assessmentType, String appName,
                                    FodCustomTypes.SetupScanPageStatus status, FodCustomTypes.AuditPreference preference) {

        ReleaseDTO releaseDTOName = ReleaseDTO.createDefaultInstance();
        StaticScanDTO staticScanDTOName = StaticScanDTO.createDefaultInstance();

        staticScanDTOName.setAssessmentType(assessmentType);
        staticScanDTOName.setLanguageLevel("1.8");
        staticScanDTOName.setAuditPreference(preference);
        staticScanDTOName.setFileToUpload("payloads/fod/static.java.zip");
        releaseDTOName.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(appName)
                .createNewRelease(releaseDTOName)
                .pressStartStaticScan()
                .createStaticScan(staticScanDTOName)
                .waitStatus(status);
        BrowserUtil.clearCookiesLogOff();

        return releaseDTOName;
    }

    public ReleaseDTO cancelStaticScan(String assessmentType, String appName,
                                       FodCustomTypes.SetupScanPageStatus status, FodCustomTypes.AuditPreference preference) {
        BrowserUtil.clearCookiesLogOff();
        ReleaseDTO releaseDTOName = ReleaseDTO.createDefaultInstance();
        StaticScanDTO staticScanDTOName = StaticScanDTO.createDefaultInstance();

        staticScanDTOName.setAssessmentType(assessmentType);
        staticScanDTOName.setLanguageLevel("1.8");
        staticScanDTOName.setAuditPreference(preference);
        staticScanDTOName.setFileToUpload("payloads/fod/static.java.zip");
        releaseDTOName.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(appName)
                .createNewRelease(releaseDTOName)
                .pressStartStaticScan()
                .createStaticScan(staticScanDTOName).waitStatus(status)
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static)
                .cancelScan();
        BrowserUtil.clearCookiesLogOff();

        return releaseDTOName;
    }

    public void checkDynamicScanStatus(String appName, String releaseName, String waitStatus) {
        var getDynamicStatus = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(appName, releaseName);
        WaitUtil.waitForTrue(() -> getDynamicStatus.getStaticScanStatus().contains(waitStatus), Duration.ofMinutes(10), false);
        assertThat(getDynamicStatus.getDynamicScanStatus())
                .as("Dynamic scan status is: " + getDynamicStatus.getDynamicScanStatus()
                        + " it should be " + waitStatus).isEqualTo(waitStatus);
        BrowserUtil.clearCookiesLogOff();
    }

    public void checkMobileScanStatus(String appName, String releaseName, String waitStatus) {
        var getMobileStatus = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(appName, releaseName);
        WaitUtil.waitForTrue(() -> getMobileStatus.getMobileScanStatus().contains(waitStatus), Duration.ofMinutes(10), false);
        assertThat(getMobileStatus.getMobileScanStatus()).as("Mobile scan status is: "
                + getMobileStatus.getMobileScanStatus()
                + " should be given wait status " + waitStatus).isEqualTo(waitStatus);
        BrowserUtil.clearCookiesLogOff();
    }

    List<String> getReleaseNamesList(Table table) {
        return table.getAllColumnValues(table.getColumnIndex("Release Name"));
    }
}